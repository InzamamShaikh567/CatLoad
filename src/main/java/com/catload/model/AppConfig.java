package com.catload.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {

    private String downloadDirectory;
    private int concurrentDownloads = 3;
    private boolean autoStartDownloads = true;
    private boolean darkTheme = true;
    private String cookiesPath;

    public AppConfig() {
        this.downloadDirectory = System.getProperty("user.home") + "/Downloads/CatLoad";
    }

    public String getDownloadDirectory() { return downloadDirectory; }
    public void setDownloadDirectory(String downloadDirectory) { this.downloadDirectory = downloadDirectory; }
    public int getConcurrentDownloads() { return concurrentDownloads; }
    public void setConcurrentDownloads(int concurrentDownloads) { this.concurrentDownloads = concurrentDownloads; }
    public boolean isAutoStartDownloads() { return autoStartDownloads; }
    public void setAutoStartDownloads(boolean autoStartDownloads) { this.autoStartDownloads = autoStartDownloads; }
    public boolean isDarkTheme() { return darkTheme; }
    public void setDarkTheme(boolean darkTheme) { this.darkTheme = darkTheme; }
    public String getCookiesPath() { return cookiesPath; }
    public void setCookiesPath(String cookiesPath) { this.cookiesPath = cookiesPath; }
}
