package com.catload.service;

import com.catload.model.Platform;
import com.catload.network.HttpClient;

public class ExtractorRegistry {

    private final MetaScraper metaScraper;
    private final RedditExtractor redditExtractor;

    public ExtractorRegistry(HttpClient httpClient) {
        this.metaScraper = new MetaScraper(httpClient);
        this.redditExtractor = new RedditExtractor(httpClient);
    }

    public ExtractResult extract(String url, Platform platform) {
        return switch (platform) {
            case REDDIT -> fromReddit(url);
            default -> fromMeta(url);
        };
    }

    private ExtractResult fromReddit(String url) {
        var r = redditExtractor.extract(url);
        return new ExtractResult(r.videoUrl(), r.title(), r.error());
    }

    private ExtractResult fromMeta(String url) {
        var r = metaScraper.scrape(url);
        return new ExtractResult(r.mediaUrl(), r.title(), r.error());
    }

    public record ExtractResult(String mediaUrl, String title, String error) {
        public boolean isSuccess() { return mediaUrl != null && error == null; }
    }
}
