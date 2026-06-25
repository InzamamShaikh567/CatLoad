package com.catload.model;

import java.util.Locale;

public enum Platform {
    YOUTUBE, YOUTUBE_SHORTS, TIKTOK, INSTAGRAM, TWITTER, X,
    REDDIT, FACEBOOK, PINTEREST, TELEGRAM, TWITCH, VIMEO,
    DAILYMOTION, SOUNDCLOUD, BANDCAMP, VK, OK, RUTUBE, YANDEX,
    PATREON, LINKEDIN, TUMBLR, FLICKR, IMGUR,
    IMAGE_HOST, DIRECT_VIDEO, DIRECT_AUDIO, UNKNOWN;

    public static Platform fromUrl(String url) {
        if (url == null || url.isBlank()) return UNKNOWN;
        String host = url.toLowerCase(Locale.ROOT);

        if (host.contains("youtube.com") || host.contains("youtu.be"))
            return host.contains("shorts") ? YOUTUBE_SHORTS : YOUTUBE;
        if (host.contains("tiktok.com")) return TIKTOK;
        if (host.contains("instagram.com")) return INSTAGRAM;
        if (host.contains("twitter.com")) return TWITTER;
        if (host.contains("x.com")) return X;
        if (host.contains("reddit.com")) return REDDIT;
        if (host.contains("facebook.com") || host.contains("fb.com")) return FACEBOOK;
        if (host.contains("pinterest.com")) return PINTEREST;
        if (host.contains("telegram.org") || host.contains("t.me")) return TELEGRAM;
        if (host.contains("twitch.tv")) return TWITCH;
        if (host.contains("vimeo.com")) return VIMEO;
        if (host.contains("dailymotion.com")) return DAILYMOTION;
        if (host.contains("soundcloud.com")) return SOUNDCLOUD;
        if (host.contains("bandcamp.com")) return BANDCAMP;
        if (host.contains("vk.com")) return VK;
        if (host.contains("ok.ru")) return OK;
        if (host.contains("rutube.ru")) return RUTUBE;
        if (host.contains("yandex")) return YANDEX;
        if (host.contains("patreon.com")) return PATREON;
        if (host.contains("linkedin.com")) return LINKEDIN;
        if (host.contains("tumblr.com")) return TUMBLR;
        if (host.contains("flickr.com")) return FLICKR;
        if (host.contains("imgur.com")) return IMGUR;

        if (url.matches("(?i).+\\.(jpg|jpeg|png|gif|webp|bmp)(\\?.*)?$")) return IMAGE_HOST;
        if (url.matches("(?i).+\\.(mp4|webm|mov|avi|mkv)(\\?.*)?$")) return DIRECT_VIDEO;
        if (url.matches("(?i).+\\.(mp3|m4a|wav|flac|ogg|aac)(\\?.*)?$")) return DIRECT_AUDIO;

        return UNKNOWN;
    }

    public String getDisplayName() {
        return switch (this) {
            case YOUTUBE -> "YouTube"; case YOUTUBE_SHORTS -> "YouTube Shorts";
            case TIKTOK -> "TikTok"; case INSTAGRAM -> "Instagram";
            case TWITTER -> "Twitter"; case X -> "X (Twitter)";
            case REDDIT -> "Reddit"; case FACEBOOK -> "Facebook";
            case PINTEREST -> "Pinterest"; case TELEGRAM -> "Telegram";
            case TWITCH -> "Twitch"; case VIMEO -> "Vimeo";
            case DAILYMOTION -> "Dailymotion"; case SOUNDCLOUD -> "SoundCloud";
            case BANDCAMP -> "Bandcamp"; case VK -> "VK";
            case OK -> "OK"; case RUTUBE -> "Rutube";
            case YANDEX -> "Yandex"; case PATREON -> "Patreon";
            case LINKEDIN -> "LinkedIn"; case TUMBLR -> "Tumblr";
            case FLICKR -> "Flickr"; case IMGUR -> "Imgur";
            case IMAGE_HOST -> "Image"; case DIRECT_VIDEO -> "Direct Video";
            case DIRECT_AUDIO -> "Direct Audio"; case UNKNOWN -> "Unknown";
        };
    }
}
