package com.catload.ui.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DownloadsView extends BorderPane {

    private final TabPane tabPane;
    private final VBox allList;
    private final VBox queueList;
    private final VBox downloadingList;
    private final VBox completedList;
    private final VBox failedList;
    private final Button clearCompletedBtn;
    private final Button clearAllBtn;

    public DownloadsView() {
        setPadding(new Insets(24));

        this.allList = createPlaceholderList("No downloads yet");
        this.queueList = createPlaceholderList("No queued downloads");
        this.downloadingList = createPlaceholderList("No active downloads");
        this.completedList = createPlaceholderList("No completed downloads");
        this.failedList = createPlaceholderList("No failed downloads");

        this.tabPane = new TabPane();
        Tab allTab = new Tab("All", scrollWrap(allList));
        Tab queueTab = new Tab("Queue", scrollWrap(queueList));
        Tab downloadingTab = new Tab("Downloading", scrollWrap(downloadingList));
        Tab completedTab = new Tab("Completed", scrollWrap(completedList));
        Tab failedTab = new Tab("Failed", scrollWrap(failedList));
        allTab.setClosable(false);
        queueTab.setClosable(false);
        downloadingTab.setClosable(false);
        completedTab.setClosable(false);
        failedTab.setClosable(false);
        tabPane.getTabs().addAll(allTab, queueTab, downloadingTab, completedTab, failedTab);

        this.clearCompletedBtn = new Button("Clear Completed");
        clearCompletedBtn.getStyleClass().add("btn-secondary");

        this.clearAllBtn = new Button("Clear All");
        clearAllBtn.getStyleClass().add("btn-danger");

        HBox toolbar = new HBox(12, clearCompletedBtn, clearAllBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 12, 0));

        VBox topSection = new VBox(8, toolbar, tabPane);
        VBox.setVgrow(tabPane, javafx.scene.layout.Priority.ALWAYS);

        setCenter(topSection);
    }

    private static VBox createPlaceholderList(String text) {
        VBox list = new VBox(8);
        Label empty = new Label(text);
        empty.getStyleClass().add("label-subtitle");
        list.getChildren().add(empty);
        return list;
    }

    private static ScrollPane scrollWrap(VBox content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return sp;
    }

    public Button getClearCompletedBtn() { return clearCompletedBtn; }
    public Button getClearAllBtn() { return clearAllBtn; }
    public VBox getAllList() { return allList; }
    public VBox getQueueList() { return queueList; }
    public VBox getDownloadingList() { return downloadingList; }
    public VBox getCompletedList() { return completedList; }
    public VBox getFailedList() { return failedList; }
}
