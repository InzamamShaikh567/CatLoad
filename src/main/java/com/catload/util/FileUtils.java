package com.catload.util;

public class FileUtils {

    public static String sanitizeFileName(String name) {
        if (name == null) return "download";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", " ").trim();
    }

    public static String getFileExtension(String url) {
        if (url == null) return "";
        int q = url.indexOf('?');
        if (q > 0) url = url.substring(0, q);
        int dot = url.lastIndexOf('.');
        return dot < 0 ? "" : url.substring(dot);
    }
}
