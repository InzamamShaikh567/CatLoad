package com.catload.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.UUID;

public class Download {

    private String id;
    private String url;
    private Platform platform;
    private DownloadFormat format;
    private DownloadStatus status;
    private String title;
    private double progress;
    private String fileName;
    private String filePath;
    private long fileSize;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String phase;
    private String customVideoFormatId;
    private String customAudioFormatId;
    private String thumbnailUrl;
    private String thumbnailPath;
    private String outputDirOverride;
    private String titlePrefix;
    private String customFormatLabel;
    private int retryCount;

    public Download() {}

    public Download(String url) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.url = url;
        this.platform = Platform.fromUrl(url);
        this.format = DownloadFormat.BEST;
        this.status = DownloadStatus.QUEUED;
        this.progress = 0;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }
    public DownloadFormat getFormat() { return format; }
    public void setFormat(DownloadFormat format) { this.format = format; }
    public DownloadStatus getStatus() { return status; }
    public void setStatus(DownloadStatus status) { this.status = status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }
public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    @JsonIgnore public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public String getCustomVideoFormatId() { return customVideoFormatId; }
    public void setCustomVideoFormatId(String v) { this.customVideoFormatId = v; }
    public String getCustomAudioFormatId() { return customAudioFormatId; }
    public void setCustomAudioFormatId(String v) { this.customAudioFormatId = v; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getThumbnailPath() { return thumbnailPath; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getOutputDirOverride() { return outputDirOverride; }
    public void setOutputDirOverride(String outputDirOverride) { this.outputDirOverride = outputDirOverride; }
    public String getTitlePrefix() { return titlePrefix; }
    public void setTitlePrefix(String titlePrefix) { this.titlePrefix = titlePrefix; }
    public String getCustomFormatLabel() { return customFormatLabel; }
    public void setCustomFormatLabel(String customFormatLabel) { this.customFormatLabel = customFormatLabel; }
    @JsonIgnore public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public Download copy() {
        Download d = new Download();
        d.id = this.id; d.url = this.url; d.platform = this.platform;
        d.format = this.format; d.status = this.status; d.title = this.title;
        d.progress = this.progress; d.fileName = this.fileName; d.filePath = this.filePath;
        d.fileSize = this.fileSize; d.errorMessage = this.errorMessage; d.phase = this.phase;
        d.customVideoFormatId = this.customVideoFormatId; d.customAudioFormatId = this.customAudioFormatId;
        d.createdAt = this.createdAt; d.completedAt = this.completedAt;
        d.thumbnailUrl = this.thumbnailUrl; d.thumbnailPath = this.thumbnailPath;
        d.outputDirOverride = this.outputDirOverride; d.titlePrefix = this.titlePrefix;
        d.customFormatLabel = this.customFormatLabel; d.retryCount = this.retryCount;
        return d;
    }
}
