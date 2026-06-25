package com.catload.ui.view;

import com.catload.model.StreamInfo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class HomeView extends BorderPane {

    private final TextField urlField;
    private final ComboBox<String> formatCombo;
    private final Button downloadBtn;
    private final Label platformLabel;
    private final ToggleButton customToggle;
    private final VBox customPanel;
    private final ComboBox<StreamInfo> videoStreamCombo;
    private final ComboBox<StreamInfo> audioStreamCombo;
    private final ProgressIndicator fetchSpinner;
    private final Label fetchStatusLabel;
    private final Label playlistStatusLabel;

    public HomeView() {
        setPadding(new Insets(40));

        VBox glassPanel = new VBox(20);
        glassPanel.getStyleClass().add("glass-panel");
        glassPanel.setMaxWidth(700);
        glassPanel.setAlignment(Pos.CENTER);
        glassPanel.setMaxHeight(Double.MAX_VALUE);

        Label header = new Label("Paste a URL to download");
        header.getStyleClass().add("label-title");

        Label subtitle = new Label("Supports YouTube, TikTok, Instagram, Twitter, Reddit, and many other sites");
        subtitle.getStyleClass().add("label-subtitle");
        subtitle.setWrapText(true);
        subtitle.setAlignment(Pos.CENTER);

        Label playlistSubtitle = new Label("Download individual videos or entire playlists");
        playlistSubtitle.getStyleClass().add("label-subtitle");
        playlistSubtitle.setWrapText(true);
        playlistSubtitle.setAlignment(Pos.CENTER);

        this.urlField = new TextField();
        urlField.setPromptText("https://www.youtube.com/watch?v=...");
        urlField.getStyleClass().add("url-field");
        urlField.setMaxWidth(Double.MAX_VALUE);

        this.platformLabel = new Label("");
        platformLabel.getStyleClass().add("label-subtitle");
        platformLabel.setVisible(false);

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER);

        this.formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll("Best Quality", "1080p", "720p", "480p", "Audio M4A", "Audio MP3");
        formatCombo.getSelectionModel().select(0);
        formatCombo.getStyleClass().add("combo-box");

        this.downloadBtn = new Button("Download");
        downloadBtn.getStyleClass().add("btn-primary");
        downloadBtn.setDefaultButton(true);

        controls.getChildren().addAll(formatCombo, downloadBtn);

        // --- Custom stream selection ---
        this.customToggle = new ToggleButton("Custom Download");
        customToggle.getStyleClass().add("btn-secondary");
        HBox.setHgrow(customToggle, Priority.ALWAYS);

        HBox customToggleRow = new HBox(customToggle);
        customToggleRow.setAlignment(Pos.CENTER);

        this.videoStreamCombo = createStreamCombo("Choose video quality");
        this.audioStreamCombo = createStreamCombo("Choose audio quality");

        this.fetchSpinner = new ProgressIndicator();
        fetchSpinner.setPrefSize(20, 20);
        fetchSpinner.setManaged(false);
        fetchSpinner.setVisible(false);

        this.fetchStatusLabel = new Label();
        fetchStatusLabel.getStyleClass().add("label-subtitle");
        fetchStatusLabel.setManaged(false);
        fetchStatusLabel.setVisible(false);

        this.playlistStatusLabel = new Label();
        playlistStatusLabel.getStyleClass().add("label-subtitle");
        playlistStatusLabel.setManaged(false);
        playlistStatusLabel.setVisible(false);

        Label videoLabel = new Label("Video Stream");
        videoLabel.getStyleClass().add("label-subtitle");
        Label audioLabel = new Label("Audio Stream");
        audioLabel.getStyleClass().add("label-subtitle");
        VBox videoBox = new VBox(4, videoLabel, videoStreamCombo);
        VBox audioBox = new VBox(4, audioLabel, audioStreamCombo);

        this.customPanel = new VBox(12);
        HBox spinnerRow = new HBox(8, fetchSpinner, fetchStatusLabel);
        spinnerRow.setAlignment(Pos.CENTER_LEFT);
        customPanel.getChildren().addAll(spinnerRow, videoBox, audioBox);
        customPanel.setManaged(false);
        customPanel.setVisible(false);
        videoStreamCombo.setDisable(true);
        audioStreamCombo.setDisable(true);
        // ---

        glassPanel.getChildren().addAll(header, subtitle, playlistSubtitle, urlField, platformLabel,
                playlistStatusLabel, controls, customToggleRow, customPanel);

        StackPane centerWrapper = new StackPane(glassPanel);
        centerWrapper.setAlignment(Pos.CENTER);
        ScrollPane scrollPane = new ScrollPane(centerWrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setCenter(scrollPane);
    }

    private static ComboBox<StreamInfo> createStreamCombo(String promptText) {
        ComboBox<StreamInfo> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setPromptText(promptText);
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(StreamInfo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayLabel());
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(StreamInfo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? promptText : item.getDisplayLabel());
            }
        });
        return combo;
    }

    public TextField getUrlField() { return urlField; }
    public ComboBox<String> getFormatCombo() { return formatCombo; }
    public Button getDownloadBtn() { return downloadBtn; }
    public Label getPlatformLabel() { return platformLabel; }
    public ToggleButton getCustomToggle() { return customToggle; }
    public ComboBox<StreamInfo> getVideoStreamCombo() { return videoStreamCombo; }
    public ComboBox<StreamInfo> getAudioStreamCombo() { return audioStreamCombo; }
    public ProgressIndicator getFetchSpinner() { return fetchSpinner; }
    public Label getFetchStatusLabel() { return fetchStatusLabel; }
    public Label getPlaylistStatusLabel() { return playlistStatusLabel; }

    public void setStreamsLoading(boolean loading) {
        videoStreamCombo.setDisable(loading);
        audioStreamCombo.setDisable(loading);
        fetchSpinner.setVisible(loading);
        fetchSpinner.setManaged(loading);
        if (loading) {
            fetchStatusLabel.setText("Fetching available streams...");
            fetchStatusLabel.setVisible(true);
            fetchStatusLabel.setManaged(true);
            videoStreamCombo.getItems().clear();
            videoStreamCombo.getSelectionModel().clearSelection();
            audioStreamCombo.getItems().clear();
            audioStreamCombo.getSelectionModel().clearSelection();
        } else {
            fetchStatusLabel.setVisible(false);
            fetchStatusLabel.setManaged(false);
        }
    }

    public void setCustomMode(boolean active) {
        customPanel.setManaged(active);
        customPanel.setVisible(active);
        customToggle.setText(active ? "▾ Simple" : "▸ Custom Download");
        if (!active) {
            videoStreamCombo.getItems().clear();
            videoStreamCombo.getSelectionModel().clearSelection();
            audioStreamCombo.getItems().clear();
            audioStreamCombo.getSelectionModel().clearSelection();
            fetchSpinner.setVisible(false);
            fetchSpinner.setManaged(false);
            fetchStatusLabel.setVisible(false);
            fetchStatusLabel.setManaged(false);
        }
    }

    public boolean isCustomMode() {
        return customToggle.isSelected();
    }
}
