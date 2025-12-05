package com.audioflow.util;

import com.audioflow.model.Song;
import javafx.scene.Node;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Utilidad para manejar Drag & Drop de archivos de audio.
 * Permite arrastrar archivos MP3/WAV desde el explorador de Windows.
 */
public class DragDropHandler {

    // Extensiones de audio soportadas
    private static final String[] SUPPORTED_EXTENSIONS = { ".mp3", ".wav", ".m4a", ".aac", ".flac" };

    /**
     * Configura un nodo para aceptar drag & drop de archivos de audio
     * 
     * @param target         Nodo que recibirá los archivos
     * @param onFilesDropped Callback que se ejecuta con la lista de canciones
     *                       creadas
     */
    public static void setupDropZone(Node target, Consumer<List<Song>> onFilesDropped) {

        // Cuando algo entra al área
        target.setOnDragOver(event -> {
            if (event.getGestureSource() != target && hasAudioFiles(event.getDragboard())) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        // Cuando el drag entra al área (para efectos visuales)
        target.setOnDragEntered(event -> {
            if (hasAudioFiles(event.getDragboard())) {
                target.setStyle("-fx-border-color: #1DB954; -fx-border-width: 3; -fx-border-style: dashed;");
            }
            event.consume();
        });

        // Cuando el drag sale del área
        target.setOnDragExited(event -> {
            target.setStyle("");
            event.consume();
        });

        // Cuando se sueltan los archivos
        target.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasFiles()) {
                List<Song> songs = processDroppedFiles(db.getFiles());
                if (!songs.isEmpty()) {
                    onFilesDropped.accept(songs);
                    success = true;
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

    /**
     * Verifica si el dragboard contiene archivos de audio
     */
    private static boolean hasAudioFiles(Dragboard db) {
        if (db.hasFiles()) {
            for (File file : db.getFiles()) {
                if (isAudioFile(file)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Verifica si un archivo es de audio soportado
     */
    public static boolean isAudioFile(File file) {
        if (file == null || !file.isFile())
            return false;
        String name = file.getName().toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Procesa los archivos soltados y crea objetos Song
     */
    private static List<Song> processDroppedFiles(List<File> files) {
        List<Song> songs = new ArrayList<>();

        for (File file : files) {
            if (file.isDirectory()) {
                // Si es un directorio, buscar archivos de audio dentro
                songs.addAll(scanDirectory(file));
            } else if (isAudioFile(file)) {
                songs.add(createSongFromFile(file));
            }
        }

        return songs;
    }

    /**
     * Escanea un directorio en busca de archivos de audio
     */
    private static List<Song> scanDirectory(File directory) {
        List<Song> songs = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    songs.addAll(scanDirectory(file));
                } else if (isAudioFile(file)) {
                    songs.add(createSongFromFile(file));
                }
            }
        }

        return songs;
    }

    /**
     * Crea un objeto Song desde un archivo
     */
    public static Song createSongFromFile(File file) {
        Song song = new Song(file.getAbsolutePath());

        // El título se extrae del nombre del archivo en el constructor
        // Aquí podríamos agregar extracción de metadatos ID3 en el futuro

        System.out.println("✓ Archivo agregado: " + song.getTitle());
        return song;
    }

    /**
     * Obtiene las extensiones soportadas como texto
     */
    public static String getSupportedExtensionsText() {
        return String.join(", ", SUPPORTED_EXTENSIONS);
    }
}
