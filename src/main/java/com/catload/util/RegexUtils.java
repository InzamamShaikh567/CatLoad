package com.catload.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtils {

    public static String extractMetaTag(String html, String property) {
        String e = Pattern.quote(property);
        Pattern p = Pattern.compile(
                "<meta[^>]+(?:property|name)=\"" + e + "\"[^>]+content=\"([^\"]+)\"",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(html);
        if (m.find()) return m.group(1);

        p = Pattern.compile(
                "<meta[^>]+content=\"([^\"]+)\"[^>]+(?:property|name)=\"" + e + "\"",
                Pattern.CASE_INSENSITIVE);
        m = p.matcher(html);
        if (m.find()) return m.group(1);

        return null;
    }

    public static String extractOgVideo(String html) {
        String url = extractMetaTag(html, "og:video");
        if (url == null) url = extractMetaTag(html, "og:video:url");
        if (url == null) url = extractMetaTag(html, "twitter:player:stream");
        if (url == null) url = extractMetaTag(html, "twitter:player");
        return url;
    }

    public static String extractOgImage(String html) {
        String url = extractMetaTag(html, "og:image");
        if (url == null) url = extractMetaTag(html, "twitter:image");
        return url;
    }
}
