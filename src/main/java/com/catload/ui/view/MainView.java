package com.catload.ui.view;

import com.catload.ui.theme.ThemeManager;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class MainView extends BorderPane {

    private final ThemeManager themeManager;
    private Button activeNav;
    private final BorderPane contentArea;
    private Label badgeDownloading;
    private Label badgeCompleted;
    private Label badgeFailed;
    private ToggleButton themeToggle;

    public MainView(ThemeManager themeManager) {
        this.themeManager = themeManager;

        this.homeView = new HomeView();
        this.downloadsView = new DownloadsView();
        this.settingsView = new SettingsView();

        VBox sidebar = createSidebar();
        HBox topBar = createTopBar();
        this.contentArea = new BorderPane();
        this.contentArea.getStyleClass().add("content-area");

        setLeft(sidebar);
        setTop(topBar);
        setCenter(contentArea);

        showHome();

        // Update toggle text when theme changes from outside
        themeManager.setOnToggle(t -> updateThemeToggleText());
    }

    public HomeView getHomeView() { return homeView; }
    public DownloadsView getDownloadsView() { return downloadsView; }
    public SettingsView getSettingsView() { return settingsView; }

    public void updateDownloadBadges(int downloading, int completed, int failed) {
        setBadge(badgeDownloading, downloading);
        setBadge(badgeCompleted, completed);
        setBadge(badgeFailed, failed);
    }

    private void setBadge(Label badge, int count) {
        if (count > 0) {
            badge.setText(String.valueOf(count));
            badge.setVisible(true);
            badge.setManaged(true);
        } else {
            badge.setVisible(false);
            badge.setManaged(false);
        }
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(200);
        sidebar.setMinWidth(200);

        Label appLabel = new Label("CatLoad");
        appLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 0 16 20 16; -fx-text-fill: #00A894;");

        Button homeBtn = createNavButton("Home");
        Button downloadsBtn = createDownloadsButton();
        Button settingsBtn = createNavButton("Settings");

        homeBtn.setOnAction(e -> { selectNav(homeBtn); showHome(); });
        downloadsBtn.setOnAction(e -> { selectNav(downloadsBtn); showDownloads(); });
        settingsBtn.setOnAction(e -> { selectNav(settingsBtn); showSettings(); });

        sidebar.getChildren().addAll(appLabel, homeBtn, downloadsBtn, settingsBtn);
        return sidebar;
    }

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

    private Button createDownloadsButton() {
        Button btn = new Button();
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);

        HBox content = new HBox(4);
        content.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label("Downloads");
        label.getStyleClass().add("nav-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        badgeDownloading = createBadgeLabel("nav-badge-downloading");
        badgeCompleted = createBadgeLabel("nav-badge-completed");
        badgeFailed = createBadgeLabel("nav-badge-failed");

        content.getChildren().addAll(label, spacer, badgeDownloading, badgeCompleted, badgeFailed);
        btn.setGraphic(content);
        return btn;
    }

    private Label createBadgeLabel(String styleClass) {
        Label badge = new Label("0");
        badge.getStyleClass().addAll("nav-badge", styleClass);
        badge.setVisible(false);
        badge.setManaged(false);
        return badge;
    }

    private void selectNav(Button btn) {
        if (activeNav != null) activeNav.getStyleClass().remove("active");
        activeNav = btn;
        activeNav.getStyleClass().add("active");
    }

    private HBox createTopBar() {
        HBox bar = new HBox();
        bar.getStyleClass().add("top-bar");

        Label title = new Label("CatLoad");
        title.getStyleClass().add("top-bar-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.themeToggle = new ToggleButton();
        themeToggle.getStyleClass().add("btn-secondary");
        themeToggle.setOnAction(e -> themeManager.toggle());
        themeToggle.setFocusTraversable(false);
        updateThemeToggleText();

        bar.getChildren().addAll(title, spacer, themeToggle);
        return bar;
    }

    private void updateThemeToggleText() {
        themeToggle.setText(themeManager.isDark() ? "Light" : "Dark");
    }

    private void animateContentSwitch(Node newView) {
        FadeTransition ft = new FadeTransition(Duration.millis(150), newView);
        ft.setFromValue(0.7);
        ft.setToValue(1.0);
        contentArea.setCenter(newView);
        ft.play();
    }

    private void showHome() {
        animateContentSwitch(homeView);
        updateTopBarTitle("Home");
    }

    private void showDownloads() {
        animateContentSwitch(downloadsView);
        updateTopBarTitle("Downloads");
    }

    private void showSettings() {
        animateContentSwitch(settingsView);
        updateTopBarTitle("Settings");
    }

    private void updateTopBarTitle(String title) {
        Label titleLabel = (Label) ((HBox) getTop()).getChildren().get(0);
        titleLabel.setText("CatLoad  ·  " + title);
    }

    private HomeView homeView;
    private DownloadsView downloadsView;
    private SettingsView settingsView;
}
