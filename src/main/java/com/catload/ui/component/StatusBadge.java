package com.catload.ui.component;

import com.catload.model.DownloadStatus;
import javafx.scene.control.Label;

public class StatusBadge extends Label {

    public StatusBadge(DownloadStatus status) {
        getStyleClass().add("status-badge");
        apply(status);
    }

    public final void apply(DownloadStatus status) {
        setText(switch (status) {
            case QUEUED -> "Queued"; case DOWNLOADING -> "Downloading";
            case COMPLETED -> "Completed"; case FAILED -> "Failed";
            case CANCELLED -> "Cancelled";
        });
        getStyleClass().removeIf(c -> c.startsWith("status-badge-"));
        getStyleClass().add(switch (status) {
            case DOWNLOADING -> "status-badge-downloading";
            case COMPLETED -> "status-badge-completed";
            case FAILED, CANCELLED -> "status-badge-failed";
            case QUEUED -> "status-badge-queued";
        });
    }
}
