package com.catload.controller;

import com.catload.model.AppConfig;
import com.catload.model.Download;
import com.catload.model.DownloadFormat;
import com.catload.model.DownloadStatus;
import com.catload.model.Platform;
import com.catload.model.PlaylistData;
import com.catload.model.PlaylistEntry;
import com.catload.model.StreamInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import com.catload.repository.DownloadRepository;
import com.catload.repository.PreferencesRepository;
import com.catload.service.DownloadService;
import com.catload.ui.component.DownloadRow;
import com.catload.ui.component.PlaylistPickerDialog;
import com.catload.ui.view.DownloadsView;
import com.catload.ui.view.HomeView;
import com.catload.util.FileUtils;
import com.catload.ui.view.MainView;
import com.catload.ui.view.SettingsView;

import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class MainController {

    private final File dataDir;
    private Stage ffmpegOverlay;
    private Label ffmpegOverlayLabel;
    private final DownloadRepository downloadRepo;
    private final DownloadService downloadService;
    private final PreferencesRepository prefsRepo;
    private final HomeView homeView;
    private final DownloadsView downloadsView;
    private final MainView mainView;
    private final AppConfig config;

    public MainController(MainView mainView, HomeView homeView, DownloadsView downloadsView, SettingsView settingsView) {
        this.mainView = mainView;
        this.dataDir = new File(System.getProperty("user.home"), ".catload");
        this.dataDir.mkdirs();

        this.prefsRepo = new PreferencesRepository(dataDir);
        this.config = prefsRepo.getConfig();
        this.downloadRepo = new DownloadRepository(dataDir);
        this.downloadService = new DownloadService(downloadRepo, config, dataDir);
        this.homeView = homeView;
        this.downloadsView = downloadsView;

        wireHomeView();
        wireDownloadsView();
        wireSettingsView(settingsView);
        wireRepositoryListener();
        updateBadges();

        javafx.application.Platform.runLater(() -> {
            boolean ytOk = downloadService.initializeYtDlp();
            String ver = downloadService.getYtDlpVersion();
            settingsView.getEngineVersionLabel().setText(
                    ytOk ? "Version: " + (ver != null ? ver : "?") : "yt-dlp not available");
            if (!ytOk) {
                System.err.println("Warning: yt-dlp not available. Some downloads may fail.");
            }

            // Check ffmpeg
            boolean ffOk = downloadService.initializeFfmpeg();
            settingsView.updateFfmpegStatus(ffOk, downloadService.getFfmpegPath(), downloadService.getFfmpegVersion());
            if (!ffOk) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("FFmpeg not found");
                alert.setHeaderText("Install FFmpeg?");
                alert.setContentText("FFmpeg is needed to merge video and audio streams. "
                        + "Download and install it now? (~50 MB)");
                ButtonType installBtn = new ButtonType("Install");
                ButtonType laterBtn = new ButtonType("Not now", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(installBtn, laterBtn);
                alert.showAndWait().ifPresent(response -> {
                    if (response == installBtn) {
                        showFfmpegOverlay();
                        new Thread(() -> {
                            boolean installed = downloadService.installFfmpeg(msg ->
                                    javafx.application.Platform.runLater(() -> {
                                        if (ffmpegOverlayLabel != null)
                                            ffmpegOverlayLabel.setText(msg);
                                    }));
                            javafx.application.Platform.runLater(() -> {
                                hideFfmpegOverlay();
                                if (installed) {
                                    settingsView.updateFfmpegStatus(true,
                                            downloadService.getFfmpegPath(),
                                            downloadService.getFfmpegVersion());
                                } else {
                                    Alert err = new Alert(Alert.AlertType.ERROR);
                                    err.setTitle("FFmpeg installation failed");
                                    err.setHeaderText("Could not install FFmpeg");
                                    err.setContentText("Check your internet connection and try again from Settings > FFmpeg.");
                                    err.show();
                                }
                            });
                        }).start();
                    }
                });
            }
        });
    }

    private void wireHomeView() {
        homeView.getUrlField().textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.isBlank()) {
                Platform p = Platform.fromUrl(val);
                homeView.getPlatformLabel().setText("Detected: " + p.getDisplayName());
                homeView.getPlatformLabel().setVisible(p != Platform.UNKNOWN);
            } else {
                homeView.getPlatformLabel().setVisible(false);
            }
        });
        homeView.getDownloadBtn().setOnAction(e -> startDownload());
        homeView.getUrlField().setOnAction(e -> startDownload());

        // Custom download toggle
        homeView.getCustomToggle().selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (!isSelected) {
                homeView.setCustomMode(false);
                return;
            }
            String url = homeView.getUrlField().getText();
            if (url == null || url.isBlank()) {
                homeView.getCustomToggle().setSelected(false);
                return;
            }
            homeView.setCustomMode(true);
            homeView.setStreamsLoading(true);

            int playlistItems = isPlaylistUrl(url) ? 1 : -1;
            downloadService.fetchFormats(url, playlistItems).thenAccept(streams -> {
                javafx.application.Platform.runLater(() -> {
                    if (!homeView.isCustomMode()) return;
                    populateCustomStreams(streams);
                });
            });
        });
    }

    private void wireSettingsView(SettingsView settingsView) {
        // Set initial UI values from config BEFORE registering listeners
        // so listener callbacks don't trigger redundant saves on startup
        settingsView.getConcurrentSlider().setValue(config.getConcurrentDownloads());
        settingsView.getAutoStartCheck().setSelected(config.isAutoStartDownloads());
        settingsView.setDownloadDirectory(config.getDownloadDirectory());

        settingsView.getConcurrentSlider().valueProperty().addListener((obs, old, val) -> {
            config.setConcurrentDownloads(val.intValue());
            downloadService.updateConcurrencyLimit(val.intValue());
            prefsRepo.saveConfig(config);
        });
        settingsView.getAutoStartCheck().selectedProperty().addListener((obs, old, val) -> {
            config.setAutoStartDownloads(val);
            prefsRepo.saveConfig(config);
        });
        settingsView.getDownloadDirField().textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.isBlank() && !val.equals(old)) {
                config.setDownloadDirectory(val);
                prefsRepo.saveConfig(config);
            }
        });
        settingsView.getEngineUpdateBtn().setOnAction(e -> {
            Button btn = settingsView.getEngineUpdateBtn();
            Label statusLabel = settingsView.getEngineStatusLabel();
            btn.setDisable(true);
            btn.setText("Updating…");
            statusLabel.setVisible(false);
            new Thread(() -> {
                String result = downloadService.updateYtDlp();
                javafx.application.Platform.runLater(() -> {
                    String ver = downloadService.getYtDlpVersion();
                    settingsView.getEngineVersionLabel().setText(
                            "Version: " + (ver != null ? ver : "?"));
                    btn.setDisable(false);
                    btn.setText("Update engine (yt-dlp)");
                    statusLabel.setText(result);
                    statusLabel.setVisible(true);
                });
            }).start();
        });

        // FFmpeg install button
        settingsView.getFfmpegInstallBtn().setOnAction(e -> {
            Button btn = settingsView.getFfmpegInstallBtn();
            Label progLabel = settingsView.getFfmpegProgressLabel();
            btn.setDisable(true);
            btn.setText("Installing...");
            progLabel.setVisible(true);
            progLabel.setManaged(true);
            progLabel.setText("Starting...");
            new Thread(() -> {
                boolean ok = downloadService.installFfmpeg(msg ->
                        javafx.application.Platform.runLater(() -> progLabel.setText(msg)));
                javafx.application.Platform.runLater(() -> {
                    settingsView.updateFfmpegStatus(ok, downloadService.getFfmpegPath(), downloadService.getFfmpegVersion());
                    btn.setDisable(false);
                    progLabel.setText(ok ? "Installation complete" : "Installation failed");
                });
            }).start();
        });

        // Cookies import
        String savedPath = config.getCookiesPath();
        if (savedPath != null && !savedPath.isEmpty()) {
            settingsView.setCookiesPath(savedPath);
            downloadService.setCookiesPath(savedPath);
        }
        settingsView.getCookiesImportBtn().setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Import cookies.txt");
            fc.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Netscape cookies", "*.txt"));
            javafx.stage.Window win = settingsView.getScene().getWindow();
            java.io.File f = fc.showOpenDialog(win);
            if (f != null && f.exists()) {
                String path = f.getAbsolutePath();
                config.setCookiesPath(path);
                prefsRepo.saveConfig(config);
                downloadService.setCookiesPath(path);
                settingsView.setCookiesPath(path);
            }
        });
        settingsView.getClearCookiesBtn().setOnAction(e -> {
            config.setCookiesPath(null);
            prefsRepo.saveConfig(config);
            downloadService.setCookiesPath(null);
            settingsView.setCookiesPath(null);
        });
    }

    private static boolean isPlaylistUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains("list=")
                || lower.contains("/playlist/")
                || lower.contains("&playlist=")
                || lower.contains("?playlist=");
    }

    private void startDownload() {
        String url = homeView.getUrlField().getText();
        if (url == null || url.isBlank()) return;

        DownloadFormat fmt = DownloadFormat.fromDisplayName(homeView.getFormatCombo().getValue());

        // Disable button to prevent double-submission
        homeView.getDownloadBtn().setDisable(true);

        if (!isPlaylistUrl(url)) {
            startSingleDownload(url, fmt);
            homeView.getDownloadBtn().setDisable(false);
            return;
        }

        // Show loading feedback for playlist detection
        Label statusLabel = homeView.getPlaylistStatusLabel();
        statusLabel.setText("Checking for playlist entries...");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);

        downloadService.fetchPlaylistEntries(url)
            .exceptionally(ex -> {
                System.err.println("Failed to fetch playlist entries: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            })
            .thenAccept(data -> {
                javafx.application.Platform.runLater(() -> {
                    homeView.getDownloadBtn().setDisable(false);

                    if (data == null || data.entries() == null) {
                        statusLabel.setText("Failed to detect playlist. Check console for details.");
                        statusLabel.setVisible(true);
                        statusLabel.setManaged(true);
                        new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.ERROR,
                            "Failed to detect playlist entries for this URL.\n\nTry downloading as a single video instead.",
                            javafx.scene.control.ButtonType.OK
                        ).show();
                        return;
                    }
                    statusLabel.setVisible(false);
                    statusLabel.setManaged(false);

                    if (!data.entries().isEmpty()) {
                        showPlaylistPicker(data.entries(), fmt, url, data.title());
                    } else {
                        statusLabel.setText("No playlist entries found. Downloading as single video.");
                        statusLabel.setVisible(true);
                        statusLabel.setManaged(true);
                        startSingleDownload(url, fmt);
                    }
                });
            });
    }

    private void startSingleDownload(String url, DownloadFormat fmt) {
        Download dl = new Download(url);
        dl.setFormat(fmt);

        applyCustomStreams(dl);

        File dir = new File(config.getDownloadDirectory());
        dir.mkdirs();
        downloadService.startDownload(dl, dir);
        homeView.getUrlField().clear();
        if (homeView.isCustomMode()) {
            homeView.getCustomToggle().setSelected(false);
            homeView.setCustomMode(false);
        }
    }

    private void applyCustomStreams(Download dl) {
        if (!homeView.isCustomMode()) return;
        StreamInfo video = homeView.getVideoStreamCombo().getValue();
        StreamInfo audio = homeView.getAudioStreamCombo().getValue();
        if (video != null) {
            dl.setCustomVideoFormatId(video.getFormatId());
            dl.setCustomFormatLabel(video.getFormatNote() != null ? video.getFormatNote() : (video.getHeight() != null ? video.getHeight() + "p" : "?"));
        }
        if (audio != null) dl.setCustomAudioFormatId(audio.getFormatId());
        if (video == null && audio != null) dl.setCustomFormatLabel("audio");
    }

    private void populateCustomStreams(List<StreamInfo> streams) {
        homeView.setStreamsLoading(false);
        homeView.getFetchStatusLabel().setVisible(false);
        homeView.getFetchStatusLabel().setManaged(false);

        List<StreamInfo> video = new ArrayList<>();
        List<StreamInfo> audio = new ArrayList<>();
        for (StreamInfo si : streams) {
            if (si.isVideoStream()) video.add(si);
            if (si.isAudioOnly()) audio.add(si);
        }
        if (video.isEmpty() && audio.isEmpty()) {
            homeView.getFetchStatusLabel()
                    .setText("No streams found. Try a different URL.");
            homeView.getFetchStatusLabel().setVisible(true);
            homeView.getFetchStatusLabel().setManaged(true);
            return;
        }
        Collections.reverse(video);
        Collections.reverse(audio);
        homeView.getVideoStreamCombo().getItems().setAll(video);
        homeView.getAudioStreamCombo().getItems().setAll(audio);
    }

    private void showPlaylistPicker(List<PlaylistEntry> entries, DownloadFormat fmt, String originalUrl, String playlistTitle) {
        PlaylistPickerDialog dialog = new PlaylistPickerDialog(entries, originalUrl, playlistTitle);
        dialog.initOwner(mainView.getScene().getWindow());

        // Apply the app's stylesheets so the dialog matches the theme
        Scene scene = mainView.getScene();
        if (scene != null) {
            dialog.getDialogPane().getStylesheets().addAll(scene.getStylesheets());
            dialog.getDialogPane().getStyleClass().addAll(scene.getRoot().getStyleClass());
        }

        // Store custom format IDs if custom mode is active
        StreamInfo playlistVideo = homeView.isCustomMode() ? homeView.getVideoStreamCombo().getValue() : null;
        StreamInfo playlistAudio = homeView.isCustomMode() ? homeView.getAudioStreamCombo().getValue() : null;
        String customVid = playlistVideo != null ? playlistVideo.getFormatId() : null;
        String customAud = playlistAudio != null ? playlistAudio.getFormatId() : null;
        String customLabel = playlistVideo != null
            ? (playlistVideo.getFormatNote() != null ? playlistVideo.getFormatNote() : (playlistVideo.getHeight() != null ? playlistVideo.getHeight() + "p" : "?"))
            : (playlistAudio != null ? "audio" : null);

        Optional<PlaylistPickerDialog.PlaylistPickerResult> result = dialog.showAndWait();
        result.ifPresent(picker -> {
            File baseDir = new File(config.getDownloadDirectory());
            File targetDir = baseDir;

            if (picker.saveToFolder() && picker.playlistTitle() != null && !picker.playlistTitle().isBlank()) {
                String safeName = FileUtils.sanitizeFileName(picker.playlistTitle());
                targetDir = new File(baseDir, safeName);
            }
            targetDir.mkdirs();

            for (String entryUrl : picker.selectedUrls()) {
                Download dl = new Download(entryUrl);
                dl.setFormat(fmt);
                if (customVid != null) dl.setCustomVideoFormatId(customVid);
                if (customAud != null) dl.setCustomAudioFormatId(customAud);
                if (customLabel != null) dl.setCustomFormatLabel(customLabel);

                if (picker.addPrefix()) {
                    int orderNum = picker.urlToOrderIndex().getOrDefault(entryUrl, 0);
                    dl.setOutputDirOverride(targetDir.getAbsolutePath());
                    int padWidth = Math.max(2, String.valueOf(entries.size()).length());
                    dl.setTitlePrefix(String.format("%0" + padWidth + "d. ", orderNum));
                }

                downloadService.startDownload(dl, targetDir);
            }

            homeView.getUrlField().clear();
            if (homeView.isCustomMode()) {
                homeView.getCustomToggle().setSelected(false);
                homeView.setCustomMode(false);
            }
        });
    }

    private void wireDownloadsView() {
        for (Download d : downloadRepo.getAll()) addDownloadRow(d);
        downloadsView.getClearCompletedBtn().setOnAction(e ->
                downloadRepo.clearCompleted());
        downloadsView.getClearAllBtn().setOnAction(e ->
                downloadRepo.clearAll());
    }

    private void wireRepositoryListener() {
        downloadRepo.getAll().addListener((ListChangeListener<Download>) change -> {
            while (change.next()) {
                if (change.wasReplaced()) {
                    List<? extends Download> removed = change.getRemoved();
                    List<? extends Download> added = change.getAddedSubList();
                    for (int i = 0; i < Math.min(removed.size(), added.size()); i++) {
                        Download old = removed.get(i);
                        Download updated = added.get(i);
                        if (old.getStatus() != updated.getStatus()) {
                            VBox oldTab = tabForStatus(old.getStatus());
                            VBox newTab = tabForStatus(updated.getStatus());
                            moveRowBetweenTabs(oldTab, newTab, updated.getId());
                        }
                        updateExistingRows(updated);
                    }
                } else {
                    if (change.wasAdded()) {
                        for (Download d : change.getAddedSubList())
                            if (!isRowPresentInStatusTab(d)) addDownloadRow(d);
                    }
                    if (change.wasRemoved()) {
                        for (Download d : change.getRemoved()) removeDownloadRow(d);
                    }
                }
            }
            updateBadges();
        });
    }

    private void updateBadges() {
        mainView.updateDownloadBadges(
                countStatus(DownloadStatus.DOWNLOADING),
                countStatus(DownloadStatus.COMPLETED),
                countStatus(DownloadStatus.FAILED),
                countStatus(DownloadStatus.QUEUED)
        );
    }

    private int countStatus(DownloadStatus status) {
        return (int) downloadRepo.getAll().stream()
                .filter(d -> d.getStatus() == status)
                .count();
    }

    private VBox tabForStatus(DownloadStatus status) {
        return switch (status) {
            case QUEUED -> downloadsView.getQueueList();
            case DOWNLOADING -> downloadsView.getDownloadingList();
            case COMPLETED -> downloadsView.getCompletedList();
            case FAILED, CANCELLED -> downloadsView.getFailedList();
        };
    }

    private boolean isRowPresentInStatusTab(Download d) {
        VBox target = tabForStatus(d.getStatus());
        return target.getChildren().stream().anyMatch(n ->
                n instanceof DownloadRow && ((DownloadRow) n).getDownload().getId().equals(d.getId()));
    }

    private void addDownloadRow(Download d) {
        VBox statusTab = tabForStatus(d.getStatus());

        DownloadRow statusRow = new DownloadRow(d);
        statusRow.getActionBtn().setOnAction(e -> handleRowAction(statusRow));
        addToList(statusTab, statusRow);

        DownloadRow allRow = new DownloadRow(d);
        allRow.getActionBtn().setOnAction(e -> handleRowAction(allRow));
        addToList(downloadsView.getAllList(), allRow);
    }

    private void addToList(VBox list, DownloadRow row) {
        if (list.getChildren().size() == 1 && list.getChildren().get(0) instanceof Label)
            list.getChildren().clear();
        list.getChildren().add(0, row);
    }

    private void removeDownloadRow(Download d) {
        String id = d.getId();
        for (Node n : downloadsView.getAllList().getChildren()) {
            if (n instanceof DownloadRow && ((DownloadRow) n).getDownload().getId().equals(id)) {
                downloadsView.getAllList().getChildren().remove(n);
                break;
            }
        }
        for (VBox tab : List.of(downloadsView.getQueueList(), downloadsView.getDownloadingList(),
                downloadsView.getCompletedList(), downloadsView.getFailedList())) {
            for (Node n : tab.getChildren()) {
                if (n instanceof DownloadRow && ((DownloadRow) n).getDownload().getId().equals(id)) {
                    tab.getChildren().remove(n);
                    return;
                }
            }
        }
    }

    private void moveRowBetweenTabs(VBox from, VBox to, String id) {
        for (Node n : from.getChildren()) {
            if (n instanceof DownloadRow && ((DownloadRow) n).getDownload().getId().equals(id)) {
                from.getChildren().remove(n);
                to.getChildren().add(0, n);
                return;
            }
        }
    }

    private void updateExistingRows(Download d) {
        VBox tab = tabForStatus(d.getStatus());
        for (Node n : tab.getChildren()) {
            if (n instanceof DownloadRow row && row.getDownload().getId().equals(d.getId())) {
                row.updateProgress(d);
                break;
            }
        }
        for (Node n : downloadsView.getAllList().getChildren()) {
            if (n instanceof DownloadRow row && row.getDownload().getId().equals(d.getId())) {
                row.updateProgress(d);
                break;
            }
        }
    }

    private void handleRowAction(DownloadRow row) {
        Download d = row.getDownload();
        switch (d.getStatus()) {
            case DOWNLOADING, QUEUED -> downloadService.cancelDownload(d);
            case FAILED -> {
                File dir = new File(config.getDownloadDirectory());
                downloadService.retryDownload(d, dir);
            }
        }
    }

    private void showFfmpegOverlay() {
        ffmpegOverlay = new Stage();
        ffmpegOverlay.initModality(Modality.WINDOW_MODAL);
        ffmpegOverlay.initOwner(mainView.getScene().getWindow());
        ffmpegOverlay.setTitle("Installing FFmpeg");

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new javafx.geometry.Insets(30));

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(50, 50);

        ffmpegOverlayLabel = new Label("Downloading ffmpeg...");

        content.getChildren().addAll(spinner, ffmpegOverlayLabel);
        ffmpegOverlay.setScene(new Scene(content, 320, 160));
        ffmpegOverlay.setResizable(false);
        ffmpegOverlay.show();
    }

    private void hideFfmpegOverlay() {
        if (ffmpegOverlay != null) {
            ffmpegOverlay.close();
            ffmpegOverlay = null;
        }
        ffmpegOverlayLabel = null;
    }
}
