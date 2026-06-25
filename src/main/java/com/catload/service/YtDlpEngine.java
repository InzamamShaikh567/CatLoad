package com.catload.service;

import com.catload.model.Download;
import com.catload.model.DownloadFormat;
import com.catload.model.DownloadStatus;
import com.catload.model.PlaylistData;
import com.catload.model.PlaylistEntry;
import com.catload.model.StreamInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class YtDlpEngine {

    private static final String GITHUB_DL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp";

    private boolean available = false;
    private String version;
    private String ytDlpPath = "yt-dlp";
    private final ConcurrentHashMap<String, Process> activeProcesses = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> cancelledIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, File> downloadDirs = new ConcurrentHashMap<>();
    private String cookiesPath;
    private String ffmpegLocation;

    public boolean checkAvailable() {
        available = false;
        // Try bare name (PATH lookup)
        if (tryPath("yt-dlp")) return true;
        // Try previously tracked path (from installToLocalBin)
        if (!ytDlpPath.equals("yt-dlp") && tryPath(ytDlpPath)) return true;
        // Try known install location (AppData bin dir)
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && tryPath(appData + "\\CatLoad\\bin\\yt-dlp.exe")) return true;
        } else {
            String home = System.getProperty("user.home");
            if (tryPath(home + "/.local/bin/yt-dlp")) return true;
        }
        return false;
    }

    /** Try running yt-dlp at the given path and record version if it works. */
    private boolean tryPath(String exePath) {
        try {
            Process p = new ProcessBuilder(exePath, "--version")
                    .redirectErrorStream(true).start();
            int exit = p.waitFor();
            if (exit == 0) {
                available = true;
                ytDlpPath = exePath;
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    version = r.readLine();
                    System.out.println("yt-dlp version: " + version + " at " + exePath);
                }
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public boolean install() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Try extracting bundled EXE from JAR first
            if (extractBundledExe()) return true;
            // Fallback: download from GitHub
        }
        return installToLocalBin() != null;
    }

    public String getVersion() { return version; }

    public boolean selfUpdate() {
        if (!available) return false;
        return runCommand(List.of(ytDlpPath, "-U"));
    }

    public String update() {
        if (!available) {
            if (install()) {
                return "Installed yt-dlp " + version;
            }
            return "Installation failed. Check your internet connection.";
        }

        String oldVersion = version;

        // Try yt-dlp -U (self-update for standalone EXE)
        if (selfUpdate()) {
            checkAvailable();
            if (version != null && !version.equals(oldVersion)) {
                return "Updated to yt-dlp " + version;
            }
            return "Already up to date (yt-dlp " + version + ")";
        }

        // Direct download fallback — overwrite the managed binary
        String os = System.getProperty("os.name").toLowerCase();
        String url = GITHUB_DL + (os.contains("win") ? ".exe"
                : os.contains("mac") ? "_macos" : "_linux");
        File target = getManagedBinaryPath();
        target.getParentFile().mkdirs();

        if (downloadBinary(url, target)) {
            if (!os.contains("win")) target.setExecutable(true, false);
            checkAvailable();
            return "Updated to yt-dlp " + version;
        }

        return "Update failed. Try downloading manually from:\nhttps://github.com/yt-dlp/yt-dlp/releases/latest";
    }

    /** Extract yt-dlp.exe bundled inside the JAR to AppData. Windows only. */
    private boolean extractBundledExe() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) return false;
        try {
            File target = getManagedBinaryPath();
            target.getParentFile().mkdirs();
            try (InputStream in = getClass().getResourceAsStream("/bin/yt-dlp.exe")) {
                if (in == null) {
                    System.err.println("Bundled yt-dlp.exe not found in JAR");
                    return false;
                }
                try (java.io.FileOutputStream out = new java.io.FileOutputStream(target)) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = in.read(buf)) >= 0) {
                        out.write(buf, 0, read);
                    }
                }
            }
            System.out.println("Extracted bundled yt-dlp.exe to " + target);
            return tryPath(target.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to extract bundled yt-dlp: " + e.getMessage());
            return false;
        }
    }

    /** Get the app-managed binary path (AppData on Windows, ~/.local/bin on others). */
    private File getManagedBinaryPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String exeName = os.contains("win") ? "yt-dlp.exe" : "yt-dlp";
        if (os.contains("win")) {
            return new File(System.getenv("APPDATA"), "CatLoad\\bin\\" + exeName);
        } else {
            return new File(System.getProperty("user.home"), ".local/bin/" + exeName);
        }
    }

    /** Download yt-dlp binary from GitHub to the given target file.
     *  Uses OkHttp3 for reliable TLS, proxy, and redirect handling. */
    private boolean downloadBinary(String url, File target) {
        try {
            System.out.println("Downloading yt-dlp from " + url);
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .build();
            Request request = new Request.Builder().url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("HTTP " + response.code() + " for " + url);
                    return false;
                }
                try (InputStream in = response.body().byteStream();
                     FileOutputStream out = new FileOutputStream(target)) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = in.read(buf)) >= 0) {
                        out.write(buf, 0, read);
                    }
                }
                System.out.println("Downloaded yt-dlp to " + target);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Download failed (" + url + "): " + e.getMessage());
            return false;
        }
    }

    /** Download yt-dlp binary from GitHub to the app-managed directory. */
    private String installToLocalBin() {
        File target = getManagedBinaryPath();
        target.getParentFile().mkdirs();
        String os = System.getProperty("os.name").toLowerCase();
        String url = GITHUB_DL + (os.contains("win") ? ".exe"
                : os.contains("mac") ? "_macos" : "_linux");

        if (!downloadBinary(url, target)) return null;
        if (!os.contains("win")) target.setExecutable(true, false);
        if (tryPath(target.getAbsolutePath())) {
            return target.getAbsolutePath();
        }
        return null;
    }

    private boolean runCommand(List<String> cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            Thread drainer = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    while (r.readLine() != null) {}
                } catch (Exception ignored) {}
            });
            drainer.setDaemon(true);
            drainer.start();
            if (!p.waitFor(60, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return false;
            }
            drainer.join(2000);
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public CompletableFuture<Download> download(Download download, File downloadDir,
                                                  BiConsumer<Download, Double> progressCallback) {
        String lastFilePath = null;
        try {
            download.setStatus(DownloadStatus.DOWNLOADING);
            progressCallback.accept(download, 0.0);

            DownloadFormat fmt = download.getFormat();
            String customLabel = download.getCustomFormatLabel();
            String suffix = customLabel != null ? " [" + customLabel + "]" : fmt.getFileSuffix();
            File effectiveDir = download.getOutputDirOverride() != null
                ? new File(download.getOutputDirOverride())
                : downloadDir;
            String prefix = download.getTitlePrefix() != null ? download.getTitlePrefix() : "";
            String outputTmpl = new File(effectiveDir, prefix + "%(title)s" + suffix + ".%(ext)s").getAbsolutePath();

            List<String> cmd = new ArrayList<>();
            cmd.add(ytDlpPath);
            cmd.add("-f");
            String customVid = download.getCustomVideoFormatId();
            String customAud = download.getCustomAudioFormatId();
            if (customVid != null && customAud != null) {
                cmd.add(customVid + "+" + customAud);
            } else if (customVid != null) {
                cmd.add(customVid);
            } else if (customAud != null) {
                cmd.add(customAud);
            } else {
                cmd.add(fmt.toYtDlpFormat());
            }
            cmd.add("-o");
            cmd.add(outputTmpl);
            cmd.add("--no-playlist");
            cmd.add("--merge-output-format");
            cmd.add("mp4");
            cmd.add("--progress");
            cmd.add("--newline");
            cmd.add("--no-warnings");
            if (cookiesPath != null && !cookiesPath.isEmpty()) {
                cmd.add("--cookies");
                cmd.add(cookiesPath);
            }
            if (ffmpegLocation != null) {
                cmd.add("--ffmpeg-location");
                cmd.add(ffmpegLocation);
            }
            if (customVid == null && customAud == null
                    && fmt.needsAudioConversion() && fmt.getAudioFormat() != null) {
                cmd.add("-x");
                cmd.add("--audio-format");
                cmd.add(fmt.getAudioFormat());
            }
            cmd.add(download.getUrl());

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.environment().put("PYTHONUNBUFFERED", "1");
            pb.environment().put("FORCE_COLOR", "0");

            pb.redirectErrorStream(true);
            Process process = pb.start();
            activeProcesses.put(download.getId(), process);
            downloadDirs.put(download.getId(), downloadDir);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int fileCount = 0;
                while ((line = reader.readLine()) != null) {
                    if (cancelledIds.containsKey(download.getId())) break;
                    if (line.contains("[download]") && line.contains("%")) {
                        parseProgress(line, download, progressCallback);
                    } else if (line.contains("Destination:")) {
                        String dest = line.substring(line.indexOf("Destination:") + 12).trim();
                        lastFilePath = dest;
                        download.setTitle(new File(dest).getName());
                        fileCount++;
                        download.setPhase(fileCount == 1 ? (fmt.isAudio() ? "Audio" : "Video")
                                : fileCount == 2 ? "Audio" : null);
                    } else if (line.startsWith("[Merger]")) {
                        download.setPhase("Merging");
                        int startIdx = line.indexOf('"');
                        int endIdx = line.lastIndexOf('"');
                        if (startIdx >= 0 && endIdx > startIdx) {
                            lastFilePath = line.substring(startIdx + 1, endIdx);
                        }
                    }
                }

                int exitCode = process.waitFor();
                if (cancelledIds.remove(download.getId()) != null) {
                    download.setStatus(DownloadStatus.CANCELLED);
                    deletePartFile(lastFilePath);
                } else if (exitCode == 0) {
                    download.setStatus(DownloadStatus.COMPLETED);
                    download.setProgress(100);
                    download.setCompletedAt(LocalDateTime.now());
                    String filePath = lastFilePath;
                    if (filePath == null || !new File(filePath).exists()) {
                        if (effectiveDir.exists()) {
                            File found = findDownloadedFile(effectiveDir, prefix, suffix);
                            if (found != null) {
                                filePath = found.getAbsolutePath();
                            }
                        }
                    }
                    if (filePath != null) {
                        download.setFilePath(filePath);
                        download.setFileName(new File(filePath).getName());
                    }
                    progressCallback.accept(download, 100.0);
                } else {
                    download.setStatus(DownloadStatus.FAILED);
                    download.setErrorMessage("yt-dlp exited with code " + exitCode);
                }
            }
        } catch (Exception e) {
            if (cancelledIds.remove(download.getId()) == null) {
                download.setStatus(DownloadStatus.FAILED);
                download.setErrorMessage(e.getMessage());
            } else {
                download.setStatus(DownloadStatus.CANCELLED);
                deletePartFile(lastFilePath);
            }
        } finally {
            activeProcesses.remove(download.getId());
            downloadDirs.remove(download.getId());
        }
        return CompletableFuture.completedFuture(download);
    }

    private void parseProgress(String line, Download download, BiConsumer<Download, Double> cb) {
        if (cancelledIds.containsKey(download.getId())) return;
        try {
            int pctIdx = line.indexOf('%');
            if (pctIdx < 0) return;
            String num = line.substring(0, pctIdx);
            int sp = num.lastIndexOf(' ');
            if (sp >= 0) num = num.substring(sp).trim();
            double progress = Double.parseDouble(num);
            download.setProgress(progress);
            cb.accept(download, progress);
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse progress from: " + line);
        }
    }

    private static File findDownloadedFile(File dir, String prefix, String suffix) {
        File[] files = dir.listFiles((d, name) -> {
            int dot = name.lastIndexOf('.');
            if (dot < 0) return false;
            String base = name.substring(0, dot);
            return base.startsWith(prefix) && base.endsWith(suffix);
        });
        if (files == null || files.length == 0) return null;
        File newest = files[0];
        for (int i = 1; i < files.length; i++) {
            if (files[i].lastModified() > newest.lastModified()) newest = files[i];
        }
        return newest;
    }

    private static void deletePartFile(String filePath) {
        if (filePath == null) return;
        File f = new File(filePath + ".part");
        if (f.exists()) f.delete();
    }

    public void cancel(String downloadId) {
        cancelledIds.put(downloadId, Boolean.TRUE);
        Process p = activeProcesses.remove(downloadId);
        if (p != null && p.isAlive()) {
            p.destroyForcibly();
        }
        File dir = downloadDirs.remove(downloadId);
        if (dir != null && dir.exists()) {
            long recent = System.currentTimeMillis() - 10000;
            File[] parts = dir.listFiles((d, name) -> name.endsWith(".part"));
            if (parts != null) {
                for (File f : parts) {
                    if (f.lastModified() > recent) f.delete();
                }
            }
        }
    }

    public String fetchTitle(String url) {
        try {
            List<String> cmd = new ArrayList<>(List.of(ytDlpPath, "--get-title", "--no-warnings"));
            if (cookiesPath != null && !cookiesPath.isEmpty()) {
                cmd.add("--cookies");
                cmd.add(cookiesPath);
            }
            cmd.add(url);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String title = r.readLine();
                return (title != null && !title.isEmpty()) ? title : null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public String fetchThumbnailUrl(String url) {
        try {
            List<String> cmd = new ArrayList<>(List.of(ytDlpPath, "--get-thumbnail", "--no-warnings"));
            if (cookiesPath != null && !cookiesPath.isEmpty()) {
                cmd.add("--cookies");
                cmd.add(cookiesPath);
            }
            cmd.add(url);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String thumbnailUrl = r.readLine();
                return (thumbnailUrl != null && !thumbnailUrl.isEmpty()) ? thumbnailUrl.trim() : null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isAvailable() { return available; }

    public void setCookiesPath(String path) { this.cookiesPath = path; }
    public void setFfmpegLocation(String path) { this.ffmpegLocation = path; }

    public CompletableFuture<PlaylistData> fetchPlaylistEntries(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> cmd = new ArrayList<>(List.of(ytDlpPath, "--flat-playlist", "--yes-playlist", "-J", "--no-warnings"));
                if (cookiesPath != null && !cookiesPath.isEmpty()) {
                    cmd.add("--cookies");
                    cmd.add(cookiesPath);
                }
                cmd.add(url);

                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                Process p = pb.start();

                String json;
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line).append("\n");
                    json = sb.toString();
                }

                boolean exited = p.waitFor(30, TimeUnit.SECONDS);
                if (!exited) {
                    p.destroyForcibly();
                    System.err.println("yt-dlp --flat-playlist timed out for URL: " + url);
                    return new PlaylistData(null, List.of());
                }
                if (p.exitValue() != 0) {
                    System.err.println("yt-dlp --flat-playlist -J failed with exit " + p.exitValue());
                    return new PlaylistData(null, List.of());
                }

                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);

                String playlistTitle = root.has("title") ? root.get("title").asText() : null;

                com.fasterxml.jackson.databind.JsonNode entries = root.get("entries");
                if (entries == null || !entries.isArray() || entries.isEmpty()) {
                    return new PlaylistData(playlistTitle, List.of());
                }

                List<PlaylistEntry> result = new ArrayList<>();
                for (com.fasterxml.jackson.databind.JsonNode entry : entries) {
                    String id = entry.has("id") ? entry.get("id").asText() : null;
                    String title = entry.has("title") ? entry.get("title").asText() : "Unknown";
                    String entryUrl = entry.has("url") ? entry.get("url").asText() : null;

                    if (entryUrl == null && id != null) {
                        entryUrl = "https://www.youtube.com/watch?v=" + id;
                    }
                    if (entryUrl == null) continue;

                    Long duration = entry.has("duration") && !entry.get("duration").isNull()
                            ? entry.get("duration").asLong() : null;

                    result.add(new PlaylistEntry(id, entryUrl, title, duration));
                }
                return new PlaylistData(playlistTitle, result);
            } catch (Exception e) {
                System.err.println("Failed to fetch playlist entries: " + e.getMessage());
                return new PlaylistData(null, List.of());
            }
        });
    }

    public CompletableFuture<List<StreamInfo>> fetchFormats(String url) {
        return fetchFormats(url, -1);
    }

    public CompletableFuture<List<StreamInfo>> fetchFormats(String url, int playlistItems) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> cmd = new ArrayList<>(List.of(ytDlpPath, "-J", "--no-warnings"));
                if (playlistItems > 0) {
                    cmd.add("--playlist-items");
                    cmd.add(String.valueOf(playlistItems));
                }
                if (cookiesPath != null && !cookiesPath.isEmpty()) {
                    cmd.add("--cookies");
                    cmd.add(cookiesPath);
                }
                cmd.add(url);
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                String json;
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line).append("\n");
                    json = sb.toString();
                }
                int exit = p.waitFor();
                if (exit != 0) {
                    System.err.println("yt-dlp -J failed with exit " + exit);
                    return List.of();
                }
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);
                com.fasterxml.jackson.databind.JsonNode formats;
                if (playlistItems > 0) {
                    com.fasterxml.jackson.databind.JsonNode entries = root.get("entries");
                    if (entries != null && entries.isArray() && entries.size() > 0) {
                        formats = entries.get(0).get("formats");
                    } else {
                        formats = root.get("formats");
                    }
                } else {
                    formats = root.get("formats");
                }
                if (formats == null || !formats.isArray()) return List.of();
                List<StreamInfo> all = new ArrayList<>();
                for (com.fasterxml.jackson.databind.JsonNode f : formats) {
                    StreamInfo si = mapper.treeToValue(f, StreamInfo.class);
                    if (si != null) all.add(si);
                }
                return all;
            } catch (Exception e) {
                System.err.println("Failed to fetch formats: " + e.getMessage());
                return List.of();
            }
        });
    }
}
