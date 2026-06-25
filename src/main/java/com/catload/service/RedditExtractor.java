package com.catload.service;

import com.catload.network.HttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public class RedditExtractor {

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public RedditExtractor(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.mapper = new ObjectMapper();
    }

    public RedditResult extract(String url) {
        String jsonUrl = url.endsWith("/") ? url + ".json" : url + ".json";
        try {
            String json = httpClient.getString(jsonUrl);
            JsonNode root = mapper.readTree(json);
            if (root.isArray() && root.size() > 0) {
                JsonNode post = root.get(0).get("data").get("children").get(0).get("data");
                String title = post.has("title") ? post.get("title").asText() : null;
                String videoUrl = null;

                if (post.has("secure_media") && post.get("secure_media").has("reddit_video")) {
                    var rv = post.get("secure_media").get("reddit_video");
                    videoUrl = rv.has("fallback_url") ? rv.get("fallback_url").asText() : null;
                }
                if (videoUrl == null && post.has("url_overridden_by_dest"))
                    videoUrl = post.get("url_overridden_by_dest").asText();
                if (videoUrl == null && post.has("preview")) {
                    var imgs = post.get("preview").get("images");
                    if (imgs != null && imgs.size() > 0)
                        videoUrl = imgs.get(0).get("source").get("url").asText();
                }
                if (videoUrl != null) return new RedditResult(videoUrl, title, null);
                return new RedditResult(null, title, "No video in Reddit post");
            }
        } catch (IOException e) {
            return new RedditResult(null, null, "Reddit API error: " + e.getMessage());
        }
        return new RedditResult(null, null, "No Reddit data found");
    }

    public record RedditResult(String videoUrl, String title, String error) {
        public boolean isSuccess() { return videoUrl != null && error == null; }
    }
}
