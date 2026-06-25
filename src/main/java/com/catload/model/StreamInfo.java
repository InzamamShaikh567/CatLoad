package com.catload.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamInfo {

    @JsonProperty("format_id")
    private String formatId;

    @JsonProperty("format_note")
    private String formatNote;

    private String ext;
    private Integer width;
    private Integer height;
    private String vcodec;
    private String acodec;
    private Double abr;
    private Double fps;
    @JsonProperty("filesize")
    private Long filesize;
    @JsonProperty("filesize_approx")
    private Long filesizeApprox;

    public StreamInfo() {}

    public boolean isVideoStream() {
        return vcodec != null && !"none".equals(vcodec);
    }

    public boolean isAudioStream() {
        return acodec != null && !"none".equals(acodec);
    }

    public boolean isAudioOnly() {
        return isAudioStream() && !isVideoStream();
    }

    public String getDisplayLabel() {
        StringBuilder sb = new StringBuilder();
        if (isVideoStream()) {
            sb.append(formatNote != null ? formatNote : (height != null ? height + "p" : "?"));
            String codec = humanizeCodec(vcodec);
            if (codec != null) sb.append("  · ").append(codec);
            if (fps != null && fps > 0) sb.append("  · ").append((int)Math.round(fps)).append("fps");
            sb.append("  · ").append(ext);
            appendFileSize(sb);
            sb.append("  [").append(formatId).append("]");
        } else {
            if (abr != null && abr > 0) {
                sb.append((int)Math.round(abr)).append("kbps");
            } else {
                sb.append(formatNote != null ? formatNote : "audio");
            }
            String codec = humanizeCodec(acodec);
            if (codec != null) sb.append("  · ").append(codec);
            sb.append("  · ").append(ext);
            appendFileSize(sb);
            sb.append("  [").append(formatId).append("]");
        }
        return sb.toString();
    }

    private static String humanizeCodec(String codec) {
        if (codec == null || codec.equals("none")) return null;
        if (codec.startsWith("avc1") || codec.equals("h264")) return "H.264";
        if (codec.startsWith("hev1") || codec.startsWith("hvc1") || codec.equals("hevc") || codec.equals("h265")) return "H.265";
        if (codec.startsWith("vp09") || codec.equals("vp9")) return "VP9";
        if (codec.startsWith("av01") || codec.equals("av1")) return "AV1";
        if (codec.startsWith("mp4a") || codec.equals("aac")) return "AAC";
        if (codec.equals("opus")) return "Opus";
        if (codec.equals("mp3")) return "MP3";
        if (codec.equals("flac")) return "FLAC";
        if (codec.startsWith("vorbis") || codec.equals("vorbis")) return "Vorbis";
        if (codec.startsWith("wav")) return "WAV";
        return codec;
    }

    private void appendFileSize(StringBuilder sb) {
        long size = filesize != null ? filesize : (filesizeApprox != null ? filesizeApprox : 0);
        if (size > 0) {
            if (size > 1_000_000_000) {
                sb.append(" · ").append(String.format("%.1f", size / 1_000_000_000.0)).append("GB");
            } else if (size > 1_000_000) {
                sb.append(" · ").append(String.format("%.0f", size / 1_000_000.0)).append("MB");
            } else {
                sb.append(" · ").append(String.format("%.0f", size / 1_000.0)).append("KB");
            }
        }
    }

    @Override
    public String toString() {
        return getDisplayLabel();
    }

    public String getFormatId() { return formatId; }
    public String getFormatNote() { return formatNote; }
    public String getExt() { return ext; }
    public Integer getWidth() { return width; }
    public Integer getHeight() { return height; }
    public String getVcodec() { return vcodec; }
    public String getAcodec() { return acodec; }
    public Double getAbr() { return abr; }
    public Double getFps() { return fps; }
    public Long getFilesize() { return filesize; }
    public Long getFilesizeApprox() { return filesizeApprox; }
}
