package com.audioflow;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Clase principal de AudioFlow - Reproductor de Música
 * 
 * Esta clase inicializa la aplicación JavaFX, carga la vista principal
 * y aplica los estilos CSS modernos.
 */
public class App extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Cargar el archivo FXML principal
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/audioflow/views/main-view.fxml"));
        Parent root = loader.load();

        // Crear la escena con dimensiones iniciales
        Scene scene = new Scene(root, 1200, 800);

        // Aplicar CSS
        scene.getStylesheets().add(getClass().getResource("/com/audioflow/styles/application.css").toExternalForm());

        // Configurar el stage
        stage.setTitle("AudioFlow - Music Player");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        // Mostrar la ventana
        stage.show();

        System.out.println("✓ AudioFlow iniciado correctamente");
    }

    /**
     * Obtiene el stage principal (útil para diálogos modales)
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
