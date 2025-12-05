module com.audioflow {
    // Módulos oficiales de JavaFX necesarios
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.graphics;

    // Librerías de Terceros (JFoenix e Iconos)
    requires com.jfoenix;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires mp3agic;

    // Abrir paquetes a JavaFX para reflexión (necesario para FXML)
    opens com.audioflow to javafx.fxml;
    opens com.audioflow.controller to javafx.fxml;
    opens com.audioflow.model to javafx.base;
    opens com.audioflow.component to javafx.fxml;

    // Exportar paquetes principales
    exports com.audioflow;
    exports com.audioflow.controller;
    exports com.audioflow.model;
    exports com.audioflow.service;
    exports com.audioflow.util;
    exports com.audioflow.component;
}