package com.catload.model;

public record PlaylistEntry(String id, String url, String title, Long durationSeconds) {
    public String getDurationFormatted() {
        if (durationSeconds == null || durationSeconds <= 0) return "--:--";
        long mins = durationSeconds / 60;
        long secs = durationSeconds % 60;
        return String.format("%d:%02d", mins, secs);
    }
}
