package com.catload.service;

import com.catload.model.AppConfig;
import com.catload.model.Download;
import com.catload.model.DownloadStatus;
import com.catload.model.PlaylistData;
import com.catload.model.PlaylistEntry;
import com.catload.model.StreamInfo;
import com.catload.network.HttpClient;
import com.catload.repository.DownloadRepository;
import com.catload.util.FileUtils;
import com.catload.util.ThumbnailManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadService {

    private final YtDlpEngine ytDlp;
    private final FfmpegManager ffmpegManager;
    private final ExtractorRegistry extractorRegistry;
    private final HttpClient httpClient;
    private final DownloadRepository repository;
    private final ThumbnailManager thumbnailManager;
    private final ExecutorService executor;
    private final Object throttleLock = new Object();
    private final ConcurrentHashMap<String, Boolean> cancelledIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, okhttp3.Call> activeFallbackCalls = new ConcurrentHashMap<>();
    private int maxConcurrent;
    private int activeDownloads;

    public DownloadService(DownloadRepository repository, AppConfig config, File dataDir) {
        this.ytDlp = new YtDlpEngine();
        this.ffmpegManager = new FfmpegManager();
        this.httpClient = new HttpClient();
        this.extractorRegistry = new ExtractorRegistry(httpClient);
        this.repository = repository;
        this.thumbnailManager = new ThumbnailManager(dataDir);
        this.maxConcurrent = config.getConcurrentDownloads();
        this.activeDownloads = 0;
        this.executor = Executors.newCachedThreadPool();
    }

    public void startDownload(Download download, File downloadDir) {
        repository.add(download);
        scheduleDownloadTasks(download, downloadDir);
    }

    private void scheduleDownloadTasks(Download download, File downloadDir) {
        executor.submit(() -> {
            synchronized (throttleLock) {
                while (activeDownloads >= maxConcurrent) {
                    try {
                        throttleLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        failDownload(download, "Download interrupted");
                        return;
                    }
                }
                activeDownloads++;
            }

            if (download.getStatus() == DownloadStatus.CANCELLED) {
                releasePermit();
                return;
            }

            try {
                if (!ytDlp.isAvailable()) {
                    download.setStatus(DownloadStatus.FAILED);
                    download.setErrorMessage("yt-dlp not available");
                    javafx.application.Platform.runLater(() -> repository.update(download));
                    releasePermit();
                    return;
                }

                ytDlp.download(download, downloadDir, (dl, progress) -> {
                    Download snapshot = dl.copy();
                    javafx.application.Platform.runLater(() -> repository.update(snapshot));
                }).whenComplete((result, ex) -> {
                    cancelledIds.remove(download.getId());
                    if (result.getStatus() == DownloadStatus.CANCELLED) {
                        releasePermit();
                    } else if (ex != null) {
                        download.setStatus(DownloadStatus.FAILED);
                        download.setErrorMessage("Download error: " + ex.getMessage());
                        javafx.application.Platform.runLater(() -> repository.update(download));
                        releasePermit();
                    } else if (result.getStatus() == DownloadStatus.COMPLETED) {
                        javafx.application.Platform.runLater(() -> repository.update(result));
                        releasePermit();
                        fetchThumbnailAsync(download);
                    } else {
                        fallbackDownload(download, downloadDir);
                    }
                });
            } catch (Exception e) {
                System.out.println("Download error, will retry: " + e.getMessage());
                retryOrFail(download, downloadDir, "Unexpected error: " + e.getMessage());
            }
        });
    }

    private void releasePermit() {
        synchronized (throttleLock) {
            activeDownloads--;
            throttleLock.notifyAll();
        }
    }

    private void fallbackDownload(Download download, File downloadDir) {
        String dlId = download.getId();
        if (cancelledIds.containsKey(dlId)) {
            handleFallbackCancelled(download);
            return;
        }
        try {
            download.setStatus(DownloadStatus.DOWNLOADING);
            javafx.application.Platform.runLater(() -> repository.update(download));

            var result = extractorRegistry.extract(download.getUrl(), download.getPlatform());
            if (result.isSuccess() && result.mediaUrl() != null) {
                String mediaUrl = result.mediaUrl();
                if (result.title() != null) download.setTitle(result.title());

                String fileName = FileUtils.sanitizeFileName(
                        download.getTitle() != null ? download.getTitle() : "download");
                String ext = FileUtils.getFileExtension(mediaUrl);
                if (ext.isEmpty()) ext = ".mp4";
                String prefix = download.getTitlePrefix() != null ? download.getTitlePrefix() : "";
                File effectiveDir = download.getOutputDirOverride() != null
                    ? new File(download.getOutputDirOverride())
                    : downloadDir;
                effectiveDir.mkdirs();
                File outputFile = new File(effectiveDir, prefix + fileName + ext);

                okhttp3.Call call = httpClient.getClient()
                        .newCall(new okhttp3.Request.Builder().url(mediaUrl).build());
                activeFallbackCalls.put(dlId, call);
                try (okhttp3.Response response = call.execute()) {
                    if (response.body() != null) {
                        Files.copy(response.body().byteStream(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } finally {
                    activeFallbackCalls.remove(dlId);
                }

                if (cancelledIds.containsKey(dlId)) {
                    handleFallbackCancelled(download);
                    return;
                }

                if (outputFile.exists()) {
                    download.setFilePath(outputFile.getAbsolutePath());
                    download.setFileName(outputFile.getName());
                    download.setFileSize(outputFile.length());
                }
                download.setStatus(DownloadStatus.COMPLETED);
                download.setProgress(100);
            } else {
                retryOrFail(download, downloadDir, result.error() != null ? result.error() : "All download methods failed");
                return;
            }
        } catch (Exception e) {
            if (cancelledIds.containsKey(dlId)) {
                handleFallbackCancelled(download);
                return;
            }
            retryOrFail(download, downloadDir, "Fallback error: " + e.getMessage());
            return;
        }
        javafx.application.Platform.runLater(() -> repository.update(download));
        releasePermit();
    }

    private void handleFallbackCancelled(Download download) {
        cancelledIds.remove(download.getId());
        download.setStatus(DownloadStatus.CANCELLED);
        javafx.application.Platform.runLater(() -> repository.update(download));
        releasePermit();
    }

    private void failDownload(Download download, String msg) {
        download.setStatus(DownloadStatus.FAILED);
        download.setErrorMessage(msg);
        javafx.application.Platform.runLater(() -> repository.update(download));
    }

    private void retryOrFail(Download download, File downloadDir, String errorMsg) {
        if (cancelledIds.containsKey(download.getId())) {
            handleFallbackCancelled(download);
            return;
        }
        if (download.getRetryCount() < 2) {
            download.setRetryCount(download.getRetryCount() + 1);
            download.setStatus(DownloadStatus.QUEUED);
            download.setProgress(0);
            download.setErrorMessage(null);
            javafx.application.Platform.runLater(() -> repository.update(download));
            System.out.println("Retrying download (" + download.getRetryCount() + "/2): " + download.getUrl());
            releasePermit();
            scheduleDownloadTasks(download, downloadDir);
        } else {
            download.setStatus(DownloadStatus.FAILED);
            download.setErrorMessage(errorMsg);
            javafx.application.Platform.runLater(() -> repository.update(download));
            releasePermit();
        }
    }

    public void cancelDownload(Download download) {
        Download copy = download.copy();
        copy.setStatus(DownloadStatus.CANCELLED);
        cancelledIds.put(copy.getId(), Boolean.TRUE);
        okhttp3.Call call = activeFallbackCalls.remove(copy.getId());
        if (call != null) call.cancel();
        ytDlp.cancel(copy.getId());
        repository.update(copy);
    }

    public void retryDownload(Download download, File downloadDir) {
        download.setStatus(DownloadStatus.QUEUED);
        download.setProgress(0);
        download.setErrorMessage(null);
        javafx.application.Platform.runLater(() -> repository.update(download));
        scheduleDownloadTasks(download, downloadDir);
    }

    private void fetchThumbnailAsync(Download download) {
        if (download.getThumbnailPath() != null) return;
        executor.submit(() -> {
            String thumbnailUrl = ytDlp.fetchThumbnailUrl(download.getUrl());
            if (thumbnailUrl != null) {
                download.setThumbnailUrl(thumbnailUrl);
                thumbnailManager.fetchAndCache(thumbnailUrl, download.getId())
                        .whenComplete((localPath, ex) -> {
                            if (localPath != null) {
                                download.setThumbnailPath(localPath);
                                javafx.application.Platform.runLater(() -> repository.update(download));
                            }
                        });
            }
        });
    }

    public void updateConcurrencyLimit(int limit) {
        synchronized (throttleLock) {
            maxConcurrent = limit;
            throttleLock.notifyAll();
        }
    }

    public boolean initializeYtDlp() {
        if (ytDlp.checkAvailable()) return true;
        System.out.println("yt-dlp not found. Installing...");
        boolean ok = ytDlp.install();
        if (ok && ytDlp.isAvailable()) {
            executor.submit(() -> {
                ytDlp.selfUpdate();
                System.out.println("Background yt-dlp update check complete");
            });
        }
        return ok;
    }

    public boolean initializeFfmpeg() {
        if (ffmpegManager.checkAvailable()) {
            syncFfmpegPath();
            return true;
        }
        return false;
    }

    public boolean installFfmpeg(java.util.function.Consumer<String> onProgress) {
        boolean ok = ffmpegManager.install(onProgress);
        if (ok) syncFfmpegPath();
        return ok;
    }

    public String getFfmpegPath() { return ffmpegManager.getFfmpegPath(); }
    public String getFfmpegVersion() { return ffmpegManager.getVersion(); }

    private void syncFfmpegPath() {
        String dir = ffmpegManager.getFfmpegDir();
        if (dir != null) ytDlp.setFfmpegLocation(dir);
    }

    public String getYtDlpVersion() { return ytDlp.getVersion(); }

    public String updateYtDlp() { return ytDlp.update(); }

    public void setCookiesPath(String path) { ytDlp.setCookiesPath(path); }

    public CompletableFuture<List<StreamInfo>> fetchFormats(String url) {
        return ytDlp.fetchFormats(url);
    }

    public CompletableFuture<List<StreamInfo>> fetchFormats(String url, int playlistItems) {
        return ytDlp.fetchFormats(url, playlistItems);
    }

    public CompletableFuture<PlaylistData> fetchPlaylistEntries(String url) {
        return ytDlp.fetchPlaylistEntries(url);
    }
}
