package com.catload.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FfmpegManager {

    private static final String ZIP_URL_WIN = "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip";
    private static final String TAR_URL_LINUX = "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-linux64-gpl.tar.xz";

    private boolean available = false;
    private String ffmpegPath;
    private String version;

    public boolean checkAvailable() {
        available = false;
        if (tryPath("ffmpeg")) return true;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && tryPath(appData + "\\CatLoad\\ffmpeg\\ffmpeg.exe")) return true;
        } else if (!os.contains("mac")) {
            String home = System.getProperty("user.home");
            if (tryPath(home + "/.local/bin/ffmpeg")) return true;
        }
        return false;
    }

    private boolean tryPath(String exePath) {
        try {
            Process p = new ProcessBuilder(exePath, "-version")
                    .redirectErrorStream(true).start();
            if (p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0) {
                available = true;
                ffmpegPath = exePath;
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String firstLine = r.readLine();
                    if (firstLine != null) {
                        // "ffmpeg version X.Y.Z ..." -> extract X.Y.Z
                        int verIdx = firstLine.indexOf("version ");
                        if (verIdx >= 0) {
                            String rest = firstLine.substring(verIdx + 8);
                            int sp = rest.indexOf(' ');
                            version = sp > 0 ? rest.substring(0, sp) : rest;
                        }
                    }
                }
                System.out.println("ffmpeg version: " + version + " at " + exePath);
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public boolean install(Consumer<String> onProgress) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) return installWindows(onProgress);
            if (!os.contains("mac")) return installLinux(onProgress);
            System.err.println("ffmpeg auto-install not supported on macOS");
        } catch (Exception e) {
            System.err.println("ffmpeg install failed: " + e.getClass().getName() + ": " + e.getMessage());
        }
        return false;
    }

    private boolean installWindows(Consumer<String> onProgress) {
        String appData = System.getenv("APPDATA");
        if (appData == null) return false;
        File ffmpegDir = new File(appData, "CatLoad\\ffmpeg");
        ffmpegDir.mkdirs();

        accept(onProgress, "Downloading ffmpeg...");
        File tmpZip = new File(ffmpegDir, "ffmpeg.zip.tmp");
        if (!downloadZip(ZIP_URL_WIN, tmpZip, onProgress)) {
            tmpZip.delete();
            return false;
        }
        File zipFile = new File(ffmpegDir, "ffmpeg.zip");
        tmpZip.renameTo(zipFile);

        accept(onProgress, "Extracting ffmpeg...");
        boolean extracted = false;
        long zipSize = zipFile.length();
        System.out.println("ffmpeg zip size: " + zipSize + " bytes");
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (name.endsWith("ffmpeg.exe")) {
                    System.out.println("ffmpeg zip: extracting " + name);
                    File target = new File(ffmpegDir, "ffmpeg.exe");
                    try (FileOutputStream out = new FileOutputStream(target)) {
                        byte[] buf = new byte[8192];
                        int read;
                        while ((read = zis.read(buf)) >= 0) out.write(buf, 0, read);
                    }
                    if (target.length() == 0) {
                        System.err.println("ffmpeg extract: extracted file is empty");
                        target.delete();
                    } else {
                        extracted = true;
                        System.out.println("ffmpeg extracted to " + target.getAbsolutePath()
                                + " (" + target.length() + " bytes)");
                    }
                    break;
                }
            }
            if (!extracted) {
                System.err.println("ffmpeg zip: did not find ffmpeg.exe entry. Entries:");
                // Re-open to list entries for debugging
                try (ZipInputStream debug = new ZipInputStream(new FileInputStream(zipFile))) {
                    ZipEntry de;
                    while ((de = debug.getNextEntry()) != null) {
                        System.err.println("  - " + de.getName());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Zip extraction failed: " + e.getMessage());
        }
        zipFile.delete();
        if (!extracted) return false;

        accept(onProgress, "Verifying installation...");
        return tryPath(new File(ffmpegDir, "ffmpeg.exe").getAbsolutePath());
    }

    private boolean installLinux(Consumer<String> onProgress) {
        String home = System.getProperty("user.home");
        File binDir = new File(home, ".local/bin");
        binDir.mkdirs();

        accept(onProgress, "Downloading ffmpeg...");
        File tmpTar = new File(binDir, "ffmpeg.tar.xz.tmp");
        if (!downloadZip(TAR_URL_LINUX, tmpTar, onProgress)) {
            tmpTar.delete();
            return false;
        }
        File tarFile = new File(binDir, "ffmpeg.tar.xz");
        tmpTar.renameTo(tarFile);

        accept(onProgress, "Extracting ffmpeg...");
        File extractDir = new File(binDir, ".ffmpeg-extract");
        extractDir.mkdirs();
        boolean extracted = false;
        try {
            // Try --strip-components first (GNU tar)
            String[] cmd = {"tar", "-xf", tarFile.getAbsolutePath(),
                    "-C", extractDir.getAbsolutePath(),
                    "--strip-components=2", "--wildcards", "*/bin/ffmpeg"};
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            if (p.waitFor(30, TimeUnit.SECONDS) && p.exitValue() == 0) {
                File ffmpeg = new File(extractDir, "ffmpeg");
                if (ffmpeg.exists()) {
                    java.nio.file.Files.move(ffmpeg.toPath(), new File(binDir, "ffmpeg").toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    extracted = true;
                }
            }
            // Cleanup
            deleteDirectory(extractDir);
            tarFile.delete();
        } catch (Exception e) {
            System.err.println("tar extraction failed: " + e.getMessage());
            deleteDirectory(extractDir);
            tarFile.delete();
        }
        if (!extracted) return false;

        accept(onProgress, "Verifying installation...");
        File ffmpeg = new File(binDir, "ffmpeg");
        ffmpeg.setExecutable(true, false);
        return tryPath(ffmpeg.getAbsolutePath());
    }

    private boolean downloadZip(String url, File target, Consumer<String> onProgress) {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .build();
            Request request = new Request.Builder().url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("ffmpeg download failed: HTTP " + response.code()
                            + " " + response.message() + " for " + url);
                    return false;
                }
                long totalLen = response.body().contentLength();
                System.out.println("ffmpeg download: HTTP " + response.code()
                        + ", type=" + response.body().contentType()
                        + ", size=" + totalLen);
                try (InputStream in = response.body().byteStream();
                     FileOutputStream out = new FileOutputStream(target)) {
                    byte[] buf = new byte[8192];
                    int read;
                    long total = 0;
                    int lastPct = -1;
                    while ((read = in.read(buf)) >= 0) {
                        out.write(buf, 0, read);
                        total += read;
                        if (totalLen > 0) {
                            int pct = (int) (total * 100 / totalLen);
                            if (pct != lastPct) {
                                lastPct = pct;
                                accept(onProgress, "Downloading ffmpeg... " + pct + "%");
                            }
                        }
                    }
                }
                // Validate the downloaded file: must be > 1MB and look like a zip
                if (target.length() < 1_000_000) {
                    System.err.println("ffmpeg download: file too small (" + target.length()
                            + " bytes), probably not a valid zip");
                    return false;
                }
                return true;
            }
        } catch (Exception e) {
            System.err.println("ffmpeg download failed (" + url + "): " + e.getClass().getName() + ": " + e.getMessage());
            return false;
        }
    }

    private static void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) {
            if (f.isDirectory()) deleteDirectory(f);
            else f.delete();
        }
        dir.delete();
    }

    private static void accept(Consumer<String> c, String msg) {
        if (c != null) c.accept(msg);
    }

    public String getFfmpegPath() { return ffmpegPath; }
    public String getVersion() { return version; }
    public boolean isAvailable() { return available; }
    public String getFfmpegDir() {
        if (ffmpegPath == null) return null;
        return new File(ffmpegPath).getParent();
    }
}
