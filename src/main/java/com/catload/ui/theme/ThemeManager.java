package com.catload.ui.theme;

import com.catload.model.AppConfig;
import com.catload.repository.PreferencesRepository;
import javafx.scene.Scene;
import java.util.Objects;
import java.util.function.Consumer;

public class ThemeManager {

    public enum Theme {
        LIGHT,
        DARK
    }

    private static final String LIGHT_CSS = "/css/light-theme.css";
    private static final String DARK_CSS = "/css/dark-theme.css";

    private Scene scene;
    private Theme currentTheme = Theme.DARK;
    private PreferencesRepository prefsRepo;
    private Consumer<Theme> onToggle;

    public void setScene(Scene scene) {
        this.scene = Objects.requireNonNull(scene);
    }

    public void setPreferencesRepository(PreferencesRepository prefsRepo) {
        this.prefsRepo = prefsRepo;
    }

    public void setOnToggle(Consumer<Theme> onToggle) {
        this.onToggle = onToggle;
    }

    public void apply(Theme theme) {
        if (scene == null) return;
        currentTheme = theme;
        scene.getStylesheets().clear();
        String css = switch (theme) {
            case LIGHT -> LIGHT_CSS;
            case DARK -> DARK_CSS;
        };
        String url = getClass().getResource(css).toExternalForm();
        scene.getStylesheets().add(url);
    }

    public void toggle() {
        Theme next = currentTheme == Theme.LIGHT ? Theme.DARK : Theme.LIGHT;
        apply(next);
        if (prefsRepo != null) {
            AppConfig cfg = prefsRepo.getConfig();
            cfg.setDarkTheme(next == Theme.DARK);
            prefsRepo.saveConfig(cfg);
        }
        if (onToggle != null) onToggle.accept(next);
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public boolean isDark() {
        return currentTheme == Theme.DARK;
    }
}
