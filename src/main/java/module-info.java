module com.catload {
    requires javafx.controls;
    requires okhttp3;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires java.desktop;

    opens com.catload.model to com.fasterxml.jackson.databind;

    exports com.catload;
    exports com.catload.controller;
    exports com.catload.model;
    exports com.catload.service;
    exports com.catload.repository;
    exports com.catload.ui.view;
    exports com.catload.ui.component;
    exports com.catload.ui.theme;
}
