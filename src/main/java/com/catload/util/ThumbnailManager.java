package com.catload.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ThumbnailManager {

    private static final String THUMBNAILS_DIR = "thumbnails";
    private final File cacheDir;
    private final OkHttpClient httpClient;

    public ThumbnailManager(File appDataDir) {
        this.cacheDir = new File(appDataDir, THUMBNAILS_DIR);
        this.cacheDir.mkdirs();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .build();
    }

    /**
     * Download a thumbnail from the given URL and save it locally.
     * Returns a future with the local file path, or null on failure.
     */
    public CompletableFuture<String> fetchAndCache(String thumbnailUrl, String downloadId) {
        return CompletableFuture.supplyAsync(() -> {
            File outputFile = new File(cacheDir, downloadId + ".jpg");
            try {
                Request request = new Request.Builder()
                        .url(thumbnailUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        return null;
                    }
                    try (InputStream in = response.body().byteStream();
                         FileOutputStream out = new FileOutputStream(outputFile)) {
                        byte[] buf = new byte[8192];
                        int read;
                        while ((read = in.read(buf)) >= 0) {
                            out.write(buf, 0, read);
                        }
                    }
                }
                if (outputFile.exists() && outputFile.length() > 0) {
                    return outputFile.getAbsolutePath();
                }
                outputFile.delete();
                return null;
            } catch (IOException e) {
                System.err.println("Thumbnail download failed: " + e.getMessage());
                outputFile.delete();
                return null;
            }
        });
    }

}
