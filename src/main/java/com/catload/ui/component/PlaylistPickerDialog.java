package com.catload.ui.component;

import com.catload.model.PlaylistEntry;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PlaylistPickerDialog extends Dialog<PlaylistPickerDialog.PlaylistPickerResult> {

    public record PlaylistPickerResult(
        Set<String> selectedUrls,
        boolean addPrefix,
        boolean saveToFolder,
        String playlistTitle,
        Map<String, Integer> urlToOrderIndex
    ) {}

    private static final double DIALOG_SIZE_RATIO = 0.8;

    private final Map<CheckBox, String> checkboxToUrl = new LinkedHashMap<>();
    private final CheckBox selectAllCheckBox;
    private final Label selectionLabel;
    private final Button downloadBtn;
    private final int totalEntries;
    private final String playlistTitle;
    private final Map<String, Integer> urlToOrderIndex;
    private boolean updatingSelection;

    public PlaylistPickerDialog(List<PlaylistEntry> entries, String playlistUrl, String playlistTitle) {
        this.totalEntries = entries.size();
        this.playlistTitle = playlistTitle;

        // Build order index map: URL → 1-based playlist position
        Map<String, Integer> orderMap = new LinkedHashMap<>();
        for (int i = 0; i < entries.size(); i++) {
            orderMap.put(entries.get(i).url(), i + 1);
        }
        this.urlToOrderIndex = orderMap;

        setTitle("Playlist Detected");
        setResizable(true);

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("glass-panel");
        Label header = new Label(entries.size() + " videos found in playlist");
        header.getStyleClass().add("label-title");

        selectAllCheckBox = new CheckBox("Select All");
        selectAllCheckBox.setSelected(true);
        selectAllCheckBox.getStyleClass().add("label-subtitle");
        selectAllCheckBox.selectedProperty().addListener((obs, was, isNow) -> {
            if (updatingSelection) return;
            updatingSelection = true;
            for (CheckBox cb : checkboxToUrl.keySet()) {
                cb.setSelected(isNow);
            }
            updatingSelection = false;
            updateSelectionCount();
        });

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerRow.getChildren().addAll(selectAllCheckBox, spacer);

        VBox entryList = new VBox(4);
        for (PlaylistEntry entry : entries) {
            entryList.getChildren().add(createEntryRow(entry));
        }

        ScrollPane scrollPane = new ScrollPane(entryList);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        selectionLabel = new Label();
        selectionLabel.getStyleClass().add("label-subtitle");

        CheckBox prefixCheckBox = new CheckBox("Add Number Prefix");
        prefixCheckBox.getStyleClass().add("label-subtitle");

        CheckBox folderCheckBox = new CheckBox("Save to Playlist Folder");
        folderCheckBox.getStyleClass().add("label-subtitle");

        downloadBtn = new Button("Download Selected (" + entries.size() + ")");
        downloadBtn.getStyleClass().add("btn-primary");
        downloadBtn.setOnAction(e -> {
            Set<String> selected = checkboxToUrl.entrySet().stream()
                    .filter(e2 -> e2.getKey().isSelected())
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            PlaylistPickerResult result = new PlaylistPickerResult(
                selected,
                prefixCheckBox.isSelected(),
                folderCheckBox.isSelected(),
                playlistTitle,
                urlToOrderIndex
            );
            setResult(result);
            hideWindow();
        });

        updateSelectionCount();

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> {
            setResult(null);
            hideWindow();
        });

        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getChildren().addAll(selectionLabel, prefixCheckBox, folderCheckBox, footerSpacer, cancelBtn, downloadBtn);

        content.getChildren().addAll(header, headerRow, scrollPane, footer);
        content.setMaxWidth(Double.MAX_VALUE);
        content.setMaxHeight(Double.MAX_VALUE);
        getDialogPane().setContent(content);
        getDialogPane().setMaxWidth(Double.MAX_VALUE);
        getDialogPane().setMaxHeight(Double.MAX_VALUE);
        getDialogPane().setPadding(new Insets(0, 0, 0, 0));

        setOnShown(e -> {
            javafx.stage.Stage dialogStage = (javafx.stage.Stage) getDialogPane().getScene().getWindow();
            javafx.scene.Node buttonBar = getDialogPane().lookup(".button-bar");
            if (buttonBar != null) {
                buttonBar.setVisible(false);
                buttonBar.setManaged(false);
            }
            javafx.stage.Window ownerWindow = dialogStage.getOwner();
            if (ownerWindow != null) {
                double ownerW = ownerWindow.getWidth();
                double ownerH = ownerWindow.getHeight();
                double targetW = ownerW * DIALOG_SIZE_RATIO;
                double chromeH = dialogStage.getHeight() - dialogStage.getScene().getHeight();
                double dialogH = ownerH * 0.8;
                double targetContentH = dialogH - chromeH;
                getDialogPane().setMinWidth(targetW);
                getDialogPane().setPrefWidth(targetW);
                getDialogPane().setMinHeight(targetContentH);
                getDialogPane().setPrefHeight(targetContentH);
                dialogStage.setWidth(targetW);
                dialogStage.setHeight(dialogH);
                dialogStage.setX(ownerWindow.getX() + (ownerW - targetW) / 2);
                dialogStage.setY(ownerWindow.getY() + ownerH * 0.2 - 1);
            }
            dialogStage.addEventFilter(javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST, ev -> {
                ev.consume();
                setResult(null);
                dialogStage.hide();
            });
        });
    }

    private void hideWindow() {
        if (getDialogPane().getScene() != null && getDialogPane().getScene().getWindow() != null) {
            getDialogPane().getScene().getWindow().hide();
        }
    }

    private HBox createEntryRow(PlaylistEntry entry) {
        CheckBox cb = new CheckBox();
        cb.setSelected(true);
        checkboxToUrl.put(cb, entry.url());

        Label durationLabel = new Label(entry.getDurationFormatted());
        durationLabel.getStyleClass().add("label-subtitle");
        durationLabel.setPrefWidth(50);

        Label titleLabel = new Label(entry.title());
        titleLabel.getStyleClass().add("label-title");
        titleLabel.setWrapText(true);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.getStyleClass().addAll("card", "card-hover");
        row.getChildren().addAll(cb, durationLabel, titleLabel);

        cb.selectedProperty().addListener((obs, was, isNow) -> {
            if (updatingSelection) return;
            updatingSelection = true;
            updateSelectionCount();
            boolean allChecked = checkboxToUrl.keySet().stream().allMatch(CheckBox::isSelected);
            selectAllCheckBox.setSelected(allChecked);
            updatingSelection = false;
        });

        return row;
    }

    private void updateSelectionCount() {
        long count = checkboxToUrl.keySet().stream().filter(CheckBox::isSelected).count();
        selectionLabel.setText("Selected: " + count + " of " + totalEntries + " videos");
        downloadBtn.setDisable(count == 0);
        downloadBtn.setText("Download Selected" + (count < totalEntries ? " (" + count + ")" : ""));
    }
}
