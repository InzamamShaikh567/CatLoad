package com.catload.model;

import java.util.List;

public record PlaylistData(String title, List<PlaylistEntry> entries) {}
