package com.catload;

import com.catload.controller.MainController;
import com.catload.model.AppConfig;
import com.catload.repository.PreferencesRepository;
import com.catload.ui.theme.ThemeManager;
import com.catload.ui.view.MainView;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.io.File;

public class CatLoadApp extends Application {

    private ThemeManager themeManager;
    private MainController controller;

    @Override
    public void start(Stage stage) {
        themeManager = new ThemeManager();

        MainView mainView = new MainView(themeManager);

        // Cap default size to fit within screen bounds (fixes title bar off-screen on 720p)
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double sceneW = Math.min(1200, bounds.getWidth() * 0.95);
        double sceneH = Math.min(740, bounds.getHeight() * 0.92);

        Scene scene = new Scene(mainView, sceneW, sceneH);
        themeManager.setScene(scene);

        // Load saved theme
        File dataDir = new File(System.getProperty("user.home"), ".catload");
        dataDir.mkdirs();
        PreferencesRepository prefsRepo = new PreferencesRepository(dataDir);
        AppConfig config = prefsRepo.getConfig();
        themeManager.setPreferencesRepository(prefsRepo);
        themeManager.apply(config.isDarkTheme() ? ThemeManager.Theme.DARK : ThemeManager.Theme.LIGHT);

        // Wire the controller to views
        controller = new MainController(
                mainView,
                mainView.getHomeView(),
                mainView.getDownloadsView(),
                mainView.getSettingsView()
        );

        stage.setTitle("CatLoad");
        try {
            var iconStream = getClass().getResourceAsStream("/icons/tabby.png");
            if (iconStream != null) {
                stage.getIcons().add(new javafx.scene.image.Image(iconStream));
            } else {
                System.err.println("Icon not found at /icons/tabby.png");
            }
        } catch (Exception e) {
            System.err.println("Failed to load app icon: " + e.getMessage());
        }
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
