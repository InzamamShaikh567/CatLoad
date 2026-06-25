package com.catload.repository;

import com.catload.model.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

public class PreferencesRepository {

    private final File configFile;
    private final ObjectMapper mapper;
    private AppConfig config;

    public PreferencesRepository(File dataDir) {
        this.configFile = new File(dataDir, "config.json");
        this.mapper = new ObjectMapper();
        load();
    }

    public AppConfig getConfig() { return config; }

    public void saveConfig(AppConfig config) {
        this.config = config;
        persist();
    }

    private void load() {
        if (!configFile.exists()) { config = new AppConfig(); persist(); return; }
        try {
            config = mapper.readValue(configFile, AppConfig.class);
        } catch (IOException e) {
            config = new AppConfig(); persist();
        }
    }

    private void persist() {
        try {
            configFile.getParentFile().mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(configFile, config);
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }
}
