package com.catload.service;

import com.catload.network.HttpClient;
import com.catload.util.RegexUtils;
import java.io.IOException;

public class MetaScraper {

    private final HttpClient httpClient;

    public MetaScraper(HttpClient httpClient) { this.httpClient = httpClient; }

    public ScrapeResult scrape(String url) {
        String html;
        try { html = httpClient.getString(url); }
        catch (IOException e) { return new ScrapeResult(null, null, "Fetch failed: " + e.getMessage()); }

        String title = RegexUtils.extractMetaTag(html, "og:title");
        if (title == null) title = extractHtmlTitle(html);

        String videoUrl = RegexUtils.extractOgVideo(html);
        String imageUrl = RegexUtils.extractOgImage(html);

        if (videoUrl == null && imageUrl == null)
            return new ScrapeResult(null, null, "No media found in page meta tags");

        return new ScrapeResult(videoUrl != null ? videoUrl : imageUrl, title, null);
    }

    private String extractHtmlTitle(String html) {
        var p = java.util.regex.Pattern.compile("<title[^>]*>([^<]+)</title>", java.util.regex.Pattern.CASE_INSENSITIVE);
        var m = p.matcher(html);
        return m.find() ? m.group(1).trim() : null;
    }

    public record ScrapeResult(String mediaUrl, String title, String error) {
        public boolean isSuccess() { return mediaUrl != null && error == null; }
    }
}
