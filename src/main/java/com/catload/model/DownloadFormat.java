package com.catload.model;

public enum DownloadFormat {
    BEST("Best Quality", false),
    Q1080("1080p", false),
    Q720("720p", false),
    Q480("480p", false),
    AUDIO_M4A("Audio M4A", true),
    AUDIO_MP3("Audio MP3", true);

    private final String displayName;
    private final boolean audio;

    DownloadFormat(String displayName, boolean audio) {
        this.displayName = displayName;
        this.audio = audio;
    }

    public String getDisplayName() { return displayName; }
    public boolean isAudio() { return audio; }

    public String toYtDlpFormat() {
        return switch (this) {
            case BEST -> "bv*+ba/b";
            case Q1080 -> "bv*[height<=1080]+ba/b";
            case Q720 -> "bv*[height<=720]+ba/b";
            case Q480 -> "bv*[height<=480]+ba/b";
            case AUDIO_M4A, AUDIO_MP3 -> "bestaudio/best";
        };
    }

    public boolean needsAudioConversion() {
        return this == AUDIO_M4A || this == AUDIO_MP3;
    }

    public String getAudioFormat() {
        return switch (this) {
            case AUDIO_M4A -> "m4a";
            case AUDIO_MP3 -> "mp3";
            default -> null;
        };
    }

    public String getFileSuffix() {
        return switch (this) {
            case BEST -> " [best]";
            case Q1080 -> " [1080p]";
            case Q720 -> " [720p]";
            case Q480 -> " [480p]";
            case AUDIO_M4A -> " [m4a]";
            case AUDIO_MP3 -> " [mp3]";
        };
    }

    public static DownloadFormat fromDisplayName(String name) {
        for (var f : values()) {
            if (f.displayName.equals(name)) return f;
        }
        return BEST;
    }
}
