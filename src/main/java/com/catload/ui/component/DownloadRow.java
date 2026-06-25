package com.catload.ui.component;

import com.catload.model.Download;
import com.catload.model.DownloadStatus;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

public class DownloadRow extends HBox {

    private static final double THUMB_WIDTH = 120;
    private static final double THUMB_HEIGHT = 68;

    private Download download;
    private final ProgressBar progressBar;
    private final Label pctLabel;
    private final Label phaseLabel;
    private final Label titleLabel;
    private final VBox progressBox;
    private final StatusBadge statusBadge;
    private final Button actionBtn;
    private final Button playBtn;
    private final Button folderBtn;
    private final StackPane thumbnailPane;
    private final Label placeholderLabel;
    private final ImageView thumbnailImage;

    public DownloadRow(Download download) {
        this.download = download;
        setPadding(new Insets(12, 16, 12, 16));
        setSpacing(12);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().addAll("card", "card-hover");

        // Entrance animation
        setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), this);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        // Thumbnail area
        this.thumbnailImage = new ImageView();
        thumbnailImage.setFitWidth(THUMB_WIDTH);
        thumbnailImage.setFitHeight(THUMB_HEIGHT);
        thumbnailImage.setPreserveRatio(true);
        thumbnailImage.setSmooth(true);

        this.placeholderLabel = new Label(download.getPlatform().getDisplayName());
        placeholderLabel.getStyleClass().addAll("label-subtitle", "thumbnail-placeholder");
        placeholderLabel.setMaxWidth(THUMB_WIDTH);
        placeholderLabel.setAlignment(Pos.CENTER);

        this.thumbnailPane = new StackPane(thumbnailImage, placeholderLabel);
        thumbnailPane.getStyleClass().add("thumbnail");
        thumbnailPane.setPrefSize(THUMB_WIDTH, THUMB_HEIGHT);
        thumbnailPane.setMinSize(THUMB_WIDTH, THUMB_HEIGHT);
        thumbnailPane.setMaxSize(THUMB_WIDTH, THUMB_HEIGHT);

        // Load thumbnail if already cached
        loadThumbnail(download);

        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        this.titleLabel = new Label(download.getTitle() != null ? download.getTitle() : download.getUrl());
        titleLabel.getStyleClass().add("label-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(350);
        titleLabel.setOnMouseClicked(e -> {
            if (download.getStatus() == DownloadStatus.COMPLETED && download.getFilePath() != null) {
                openFile();
            }
        });

        String formatLabel = download.getCustomFormatLabel() != null
                ? download.getCustomFormatLabel() : download.getFormat().getDisplayName();
        Label subLabel = new Label(download.getPlatform().getDisplayName()
                + " · " + formatLabel);
        subLabel.getStyleClass().add("label-subtitle");
        infoBox.getChildren().addAll(titleLabel, subLabel);

        this.progressBox = new VBox(4);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPrefWidth(180);
        this.phaseLabel = new Label();
        phaseLabel.getStyleClass().add("label-phase");
        phaseLabel.setManaged(false);
        phaseLabel.setVisible(false);
        this.progressBar = new ProgressBar(download.getStatus() == DownloadStatus.COMPLETED ? 1.0
                : download.getProgress() / 100.0);
        progressBar.setPrefWidth(180);
        this.pctLabel = new Label(download.getStatus() == DownloadStatus.COMPLETED ? "100%"
                : String.format("%.1f%%", download.getProgress()));
        pctLabel.getStyleClass().add("label-subtitle");
        progressBox.getChildren().addAll(phaseLabel, progressBar, pctLabel);

        VBox actBox = new VBox(8);
        actBox.setAlignment(Pos.CENTER);
        this.statusBadge = new StatusBadge(download.getStatus());
        this.actionBtn = new Button(switch (download.getStatus()) {
            case DOWNLOADING, QUEUED -> "Cancel";
            case FAILED -> "Retry";
            default -> "";
        });
        actionBtn.getStyleClass().addAll("btn-secondary");
        boolean needsAction = switch (download.getStatus()) {
            case DOWNLOADING, QUEUED, FAILED -> true;
            default -> false;
        };
        actionBtn.setManaged(needsAction);
        actionBtn.setVisible(needsAction);

        this.playBtn = new Button("Play");
        playBtn.getStyleClass().addAll("btn-secondary", "btn-small");
        playBtn.setOnAction(e -> openFile());

        this.folderBtn = new Button("Folder");
        folderBtn.getStyleClass().addAll("btn-secondary", "btn-small");
        folderBtn.setOnAction(e -> openFolder());

        boolean completed = download.getStatus() == DownloadStatus.COMPLETED;
        playBtn.setManaged(completed);
        playBtn.setVisible(completed);
        folderBtn.setManaged(completed);
        folderBtn.setVisible(completed);

        progressBox.setManaged(!completed);
        progressBox.setVisible(!completed);

        HBox completedActions = new HBox(4, playBtn, folderBtn);
        completedActions.setAlignment(Pos.CENTER);
        actBox.getChildren().addAll(statusBadge, actionBtn, completedActions);

        getChildren().addAll(thumbnailPane, infoBox, progressBox, actBox);

        if (completed) {
            titleLabel.setCursor(Cursor.HAND);
        }
    }

    private void loadThumbnail(Download dl) {
        String path = dl.getThumbnailPath();
        if (path != null && !path.isEmpty()) {
            File file = new File(path);
            if (file.exists() && file.length() > 0) {
                Image image = new Image(file.toURI().toString());
                thumbnailImage.setImage(image);
                thumbnailImage.setVisible(true);
                thumbnailImage.setManaged(true);
                placeholderLabel.setVisible(false);
                placeholderLabel.setManaged(false);
                thumbnailPane.getStyleClass().removeAll("thumbnail", "thumbnail-loaded");
                thumbnailPane.getStyleClass().add("thumbnail-loaded");
                return;
            }
        }
        thumbnailImage.setImage(null);
        thumbnailImage.setVisible(false);
        thumbnailImage.setManaged(false);
        placeholderLabel.setVisible(true);
        placeholderLabel.setManaged(true);
        thumbnailPane.getStyleClass().removeAll("thumbnail", "thumbnail-loaded");
        thumbnailPane.getStyleClass().add("thumbnail");
    }

    public void updateProgress(Download dl) {
        this.download = dl;

        // Reload thumbnail if path changed
        if (dl.getThumbnailPath() != null && thumbnailImage.getImage() == null) {
            loadThumbnail(dl);
        }

        titleLabel.setText(dl.getTitle() != null ? dl.getTitle() : dl.getUrl());

        progressBar.setProgress(dl.getProgress() / 100.0);
        pctLabel.setText(dl.getStatus() == DownloadStatus.COMPLETED ? "100%"
                : String.format("%.1f%%", dl.getProgress()));

        String phase = dl.getPhase();
        boolean showPhase = phase != null && !phase.isEmpty()
                && dl.getStatus() != DownloadStatus.COMPLETED;
        phaseLabel.setText(phase);
        phaseLabel.setManaged(showPhase);
        phaseLabel.setVisible(showPhase);

        boolean completed = dl.getStatus() == DownloadStatus.COMPLETED;
        boolean needsAction = switch (dl.getStatus()) {
            case DOWNLOADING, QUEUED, FAILED -> true;
            default -> false;
        };

        actionBtn.setText(switch (dl.getStatus()) {
            case DOWNLOADING, QUEUED -> "Cancel";
            case FAILED -> "Retry";
            default -> "";
        });
        actionBtn.setManaged(needsAction);
        actionBtn.setVisible(needsAction);

        playBtn.setManaged(completed);
        playBtn.setVisible(completed);
        folderBtn.setManaged(completed && dl.getFilePath() != null);
        folderBtn.setVisible(completed && dl.getFilePath() != null);

        progressBox.setManaged(!completed);
        progressBox.setVisible(!completed);

        titleLabel.setCursor(completed && dl.getFilePath() != null ? Cursor.HAND : Cursor.DEFAULT);

        statusBadge.apply(dl.getStatus());
    }

    private void openFile() {
        if (download.getFilePath() == null) return;
        File file = new File(download.getFilePath());
        if (!file.exists()) {
            System.err.println("File not found for Play: " + download.getFilePath());
            return;
        }
        try {
            Desktop.getDesktop().open(file);
        } catch (Exception e) {
            System.err.println("Failed to open file: " + download.getFilePath() + " — " + e.getMessage());
        }
    }

    private void openFolder() {
        if (download.getFilePath() == null) return;
        File file = new File(download.getFilePath());
        if (!file.exists()) {
            System.err.println("File not found: " + download.getFilePath());
            File parent = file.getParentFile();
            if (parent != null && parent.exists()) {
                try {
                    Desktop.getDesktop().browse(parent.toURI());
                } catch (IOException ex) {
                    System.err.println("Failed to open folder: " + ex.getMessage());
                }
            }
            return;
        }
        try {
            Desktop.getDesktop().browseFileDirectory(file);
        } catch (Exception e) {
            openFolderFallback(file);
        }
    }

    private void openFolderFallback(File file) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Use cmd.exe so quoting is handled properly for paths with spaces
                new ProcessBuilder("cmd", "/c",
                        "explorer /select,\"" + file.getAbsolutePath() + "\"").start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", "-R", file.getAbsolutePath()).start();
            } else {
                Desktop.getDesktop().browse(file.getParentFile().toURI());
            }
        } catch (Exception ex) {
            System.err.println("Failed to open folder: " + ex.getMessage());
        }
    }

    public Button getActionBtn() { return actionBtn; }
    public Button getPlayBtn() { return playBtn; }
    public Button getFolderBtn() { return folderBtn; }
    public Download getDownload() { return download; }
}
