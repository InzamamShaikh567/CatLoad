package com.catload.repository;

import com.catload.model.Download;
import com.catload.model.DownloadStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DownloadRepository {

    private final File dataFile;
    private final ObjectMapper mapper;
    private final ObservableList<Download> downloads;

    public DownloadRepository(File dataDir) {
        this.dataFile = new File(dataDir, "downloads.json");
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.downloads = FXCollections.observableArrayList();
        load();
    }

    public ObservableList<Download> getAll() { return downloads; }

    public void add(Download download) {
        downloads.add(download.copy());
        save();
    }

    public void update(Download download) {
        int i = indexOf(download.getId());
        if (i < 0) return;
        // Block stale progress callbacks from overwriting a terminal CANCELLED status.
        // Allow other transitions (e.g., FAILED → QUEUED for retry from non-CANCELLED state).
        if (downloads.get(i).getStatus() == DownloadStatus.CANCELLED
                && download.getStatus() != DownloadStatus.CANCELLED) return;
        downloads.set(i, download.copy());
        save();
    }

    public void remove(String id) {
        deleteThumbnailFiles(downloads.stream().filter(d -> d.getId().equals(id)).toList());
        downloads.removeIf(d -> d.getId().equals(id));
        save();
    }

    public void clearCompleted() {
        var completed = downloads.stream()
                .filter(d -> d.getStatus() == com.catload.model.DownloadStatus.COMPLETED)
                .toList();
        deleteThumbnailFiles(completed);
        downloads.removeIf(d -> d.getStatus() == com.catload.model.DownloadStatus.COMPLETED);
        save();
    }

    public void clearAll() {
        deleteThumbnailFiles(new ArrayList<>(downloads));
        downloads.clear();
        save();
    }

    private void deleteThumbnailFiles(List<Download> items) {
        File thumbDir = new File(dataFile.getParentFile(), "thumbnails");
        if (!thumbDir.exists()) return;
        for (Download d : items) {
            File thumb = new File(thumbDir, d.getId() + ".jpg");
            if (thumb.exists()) thumb.delete();
        }
    }

    private int indexOf(String id) {
        for (int i = 0; i < downloads.size(); i++)
            if (downloads.get(i).getId().equals(id)) return i;
        return -1;
    }

    private void load() {
        if (!dataFile.exists()) return;
        try {
            List<Download> list = mapper.readValue(dataFile, new TypeReference<List<Download>>() {});
            if (list != null) downloads.setAll(list);
        } catch (IOException e) {
            System.err.println("Failed to load downloads: " + e.getMessage());
        }
    }

    private void save() {
        try {
            dataFile.getParentFile().mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(dataFile, new ArrayList<>(downloads));
        } catch (IOException e) {
            System.err.println("Failed to save downloads: " + e.getMessage());
        }
    }
}
