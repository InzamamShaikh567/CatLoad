package com.catload.ui.view;

import java.awt.Desktop;
import java.net.URI;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

public class SettingsView extends VBox {

    private final TextField downloadDirField;
    private final Slider concurrentSlider;
    private final Label concurrentLabel;
    private final CheckBox autoStartCheck;
    private final Label engineVersionLabel;
    private final Button engineUpdateBtn;
    private final Label engineStatusLabel;
    private final Label ffmpegStatusLabel;
    private final Button ffmpegInstallBtn;
    private final Label ffmpegProgressLabel;
    private final Button cookiesImportBtn;
    private final Label cookiesPathLabel;
    private final Button clearCookiesBtn;

    public SettingsView() {
        setPadding(new Insets(24));
        setSpacing(20);
        setMaxWidth(600);

        Label header = new Label("Settings");
        header.getStyleClass().add("label-title");

        // --- Downloads Section ---
        VBox downloadsSection = new VBox(16);
        downloadsSection.getStyleClass().add("glass-panel");

        Label dirLabel = new Label("Download Directory");
        dirLabel.getStyleClass().add("label-subtitle");

        this.downloadDirField = new TextField();
        downloadDirField.getStyleClass().add("url-field");
        downloadDirField.setMaxWidth(Double.MAX_VALUE);

        Button browseBtn = new Button("Browse...");
        browseBtn.getStyleClass().add("btn-secondary");
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Choose Download Directory");
            var dir = dc.showDialog(getScene().getWindow());
            if (dir != null) downloadDirField.setText(dir.getAbsolutePath());
        });

        Label concurrentTitle = new Label("Concurrent Downloads");
        concurrentTitle.getStyleClass().add("label-subtitle");

        this.concurrentSlider = new Slider(1, 10, 3);
        concurrentSlider.setShowTickLabels(true);
        concurrentSlider.setShowTickMarks(true);
        concurrentSlider.setMajorTickUnit(1);
        concurrentSlider.setSnapToTicks(true);

        this.concurrentLabel = new Label("3 downloads at once");
        concurrentLabel.getStyleClass().add("label-subtitle");
        concurrentSlider.valueProperty().addListener((obs, old, val) ->
                concurrentLabel.setText(val.intValue() + " downloads at once"));

        this.autoStartCheck = new CheckBox("Auto-start downloads when added");
        autoStartCheck.setSelected(true);

        downloadsSection.getChildren().addAll(
                dirLabel, downloadDirField, browseBtn,
                concurrentTitle, concurrentSlider, concurrentLabel,
                autoStartCheck
        );

        // --- Engine Section ---
        VBox engineSection = new VBox(12);
        engineSection.getStyleClass().add("glass-panel");

        Label engineHeader = new Label("Download engine");
        engineHeader.getStyleClass().add("label-title");

        Label engineDesc = new Label(
                "yt-dlp powers downloads from almost all sites. Keep it updated so new site changes keep working.");
        engineDesc.getStyleClass().add("label-subtitle");
        engineDesc.setWrapText(true);

        this.engineVersionLabel = new Label("Version: initializing…");
        engineVersionLabel.getStyleClass().add("label-subtitle");

        this.engineUpdateBtn = new Button("Update engine (yt-dlp)");
        engineUpdateBtn.getStyleClass().add("btn-secondary");

        this.engineStatusLabel = new Label();
        engineStatusLabel.getStyleClass().add("label-subtitle");
        engineStatusLabel.setVisible(false);

        engineSection.getChildren().addAll(
                engineHeader, engineDesc, engineVersionLabel, engineUpdateBtn, engineStatusLabel
        );

        // --- FFmpeg Section ---
        VBox ffmpegSection = new VBox(12);
        ffmpegSection.getStyleClass().add("glass-panel");

        Label ffmpegHeader = new Label("FFmpeg");
        ffmpegHeader.getStyleClass().add("label-title");

        Label ffmpegDesc = new Label(
                "FFmpeg merges video and audio streams. " +
                "CatLoad can download it automatically if missing.");
        ffmpegDesc.getStyleClass().add("label-subtitle");
        ffmpegDesc.setWrapText(true);

        this.ffmpegStatusLabel = new Label("Checking...");
        ffmpegStatusLabel.getStyleClass().add("label-subtitle");

        this.ffmpegInstallBtn = new Button("Install ffmpeg");
        ffmpegInstallBtn.getStyleClass().add("btn-secondary");

        this.ffmpegProgressLabel = new Label();
        ffmpegProgressLabel.getStyleClass().add("label-subtitle");
        ffmpegProgressLabel.setVisible(false);
        ffmpegProgressLabel.setManaged(false);

        ffmpegSection.getChildren().addAll(
                ffmpegHeader, ffmpegDesc, ffmpegStatusLabel, ffmpegInstallBtn, ffmpegProgressLabel
        );

        // --- Cookies Section ---
        VBox cookiesSection = new VBox(12);
        cookiesSection.getStyleClass().add("glass-panel");

        Label cookiesHeader = new Label("Cookies");
        cookiesHeader.getStyleClass().add("label-title");

        Label cookiesDesc = new Label(
                "Import a cookies.txt file to download restricted or account-linked videos.");
        cookiesDesc.getStyleClass().add("label-subtitle");
        cookiesDesc.setWrapText(true);

        Label cookiesHelpLink = new Label("How to get your Cookies.txt?");
        cookiesHelpLink.getStyleClass().add("label-subtitle");
        cookiesHelpLink.setCursor(javafx.scene.Cursor.HAND);
        cookiesHelpLink.setOnMouseClicked(e -> openUrl("https://github.com/InzamamShaikh567/CatLoad/blob/main/README.md#cookie-support"));

        this.cookiesImportBtn = new Button("Import cookies.txt...");
        cookiesImportBtn.getStyleClass().add("btn-secondary");

        HBox pathRow = new HBox(8);
        pathRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        this.cookiesPathLabel = new Label("No cookies file set");
        cookiesPathLabel.getStyleClass().add("label-subtitle");

        this.clearCookiesBtn = new Button("Clear");
        clearCookiesBtn.getStyleClass().add("btn-danger");
        clearCookiesBtn.setVisible(false);
        clearCookiesBtn.setManaged(false);

        pathRow.getChildren().addAll(cookiesPathLabel, clearCookiesBtn);

        cookiesSection.getChildren().addAll(
                cookiesHeader, cookiesDesc, cookiesHelpLink, cookiesImportBtn, pathRow
        );

        // --- About Section ---
        VBox aboutSection = new VBox(12);
        aboutSection.getStyleClass().add("glass-panel");

        Label aboutHeader = new Label("About");
        aboutHeader.getStyleClass().add("label-title");

        Label aboutText = new Label("CatLoad v1.0.0\nA cross-platform desktop media downloader built with JavaFX 21, powered by yt-dlp and FFmpeg.");
        aboutText.getStyleClass().add("label-subtitle");
        aboutText.setWrapText(true);

        Button sourceBtn = new Button("Source code");
        sourceBtn.getStyleClass().add("btn-secondary");
        sourceBtn.setOnAction(e -> openUrl("https://github.com/InzamamShaikh567/CatLoad"));

        Button licenseBtn = new Button("License");
        licenseBtn.getStyleClass().add("btn-secondary");
        licenseBtn.setOnAction(e -> openUrl("https://github.com/InzamamShaikh567/CatLoad/blob/main/LICENSE"));

        HBox aboutButtons = new HBox(8, sourceBtn, licenseBtn);

        aboutSection.getChildren().addAll(aboutHeader, aboutText, aboutButtons);

        // --- Support Section ---
        VBox supportSection = new VBox(12);
        supportSection.getStyleClass().add("glass-panel");

        Label supportHeader = new Label("Love CatLoad?");
        supportHeader.getStyleClass().add("label-title");

        Label supportText = new Label("If CatLoad has been useful to you, consider supporting its development.");
        supportText.getStyleClass().add("label-subtitle");
        supportText.setWrapText(true);

        Button donateBtn = new Button("❤ Support CatLoad");
        donateBtn.getStyleClass().add("btn-danger");
        donateBtn.setOnAction(e -> openUrl("https://paypal.me/GaffarAliShaikh"));

        supportSection.getChildren().addAll(supportHeader, supportText, donateBtn);

        VBox content = new VBox(20);
        content.getChildren().addAll(
                downloadsSection,
                engineSection,
                ffmpegSection,
                cookiesSection,
                aboutSection,
                supportSection
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(header, scrollPane);
    }

    public TextField getDownloadDirField() { return downloadDirField; }
    public void setDownloadDirectory(String path) { downloadDirField.setText(path); }
    public Slider getConcurrentSlider() { return concurrentSlider; }
    public CheckBox getAutoStartCheck() { return autoStartCheck; }
    public Label getEngineVersionLabel() { return engineVersionLabel; }
    public Button getEngineUpdateBtn() { return engineUpdateBtn; }
    public Label getEngineStatusLabel() { return engineStatusLabel; }
    public Button getCookiesImportBtn() { return cookiesImportBtn; }
    public Label getCookiesPathLabel() { return cookiesPathLabel; }
    public Button getClearCookiesBtn() { return clearCookiesBtn; }

    public Label getFfmpegStatusLabel() { return ffmpegStatusLabel; }
    public Button getFfmpegInstallBtn() { return ffmpegInstallBtn; }
    public Label getFfmpegProgressLabel() { return ffmpegProgressLabel; }

    public void updateFfmpegStatus(boolean available, String path, String version) {
        if (available && path != null) {
            String ver = version != null ? version : "?";
            ffmpegStatusLabel.setText("Version " + ver + " — " + path);
            ffmpegInstallBtn.setText("Reinstall ffmpeg");
        } else {
            ffmpegStatusLabel.setText("Not installed");
            ffmpegInstallBtn.setText("Install ffmpeg");
        }
    }

    public void setCookiesPath(String path) {
        if (path != null && !path.isEmpty()) {
            cookiesPathLabel.setText(path);
            clearCookiesBtn.setVisible(true);
            clearCookiesBtn.setManaged(true);
        } else {
            cookiesPathLabel.setText("No cookies file set");
            clearCookiesBtn.setVisible(false);
            clearCookiesBtn.setManaged(false);
        }
    }

    private void openUrl(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            System.err.println("Failed to open URL: " + url + " — " + ex.getMessage());
        }
    }
}
