package com.audioflow.util;

import com.audioflow.model.Song;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilidad para manejar Drag & Drop de archivos de audio.
 * Extrae metadatos ID3 (título, artista, álbum, carátula) de archivos MP3.
 * 
 * @author Brickzon
 */
public class DragDropHandler {

    // Extensiones de audio soportadas
    private static final String[] SUPPORTED_EXTENSIONS = { ".mp3", ".wav", ".m4a", ".aac", ".flac" };

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
     * Crea un objeto Song desde un archivo, extrayendo metadatos ID3
     */
    public static Song createSongFromFile(File file) {
        String filePath = file.getAbsolutePath();
        String title = extractTitleFromFilename(file.getName());
        String artist = "Artista Desconocido";
        String album = "Álbum Desconocido";
        Image albumArt = null;

        // Intentar extraer metadatos ID3 si es MP3
        if (file.getName().toLowerCase().endsWith(".mp3")) {
            try {
                Mp3File mp3File = new Mp3File(file);

                if (mp3File.hasId3v2Tag()) {
                    ID3v2 id3v2Tag = mp3File.getId3v2Tag();

                    // Título
                    if (id3v2Tag.getTitle() != null && !id3v2Tag.getTitle().isBlank()) {
                        title = id3v2Tag.getTitle();
                    }

                    // Artista
                    if (id3v2Tag.getArtist() != null && !id3v2Tag.getArtist().isBlank()) {
                        artist = id3v2Tag.getArtist();
                    }

                    // Álbum
                    if (id3v2Tag.getAlbum() != null && !id3v2Tag.getAlbum().isBlank()) {
                        album = id3v2Tag.getAlbum();
                    }

                    // Carátula (Album Art)
                    byte[] imageData = id3v2Tag.getAlbumImage();
                    if (imageData != null && imageData.length > 0) {
                        try {
                            albumArt = new Image(new ByteArrayInputStream(imageData));
                            System.out.println("  ✓ Carátula extraída");
                        } catch (Exception imgEx) {
                            System.out.println("  ⚠ No se pudo cargar la carátula");
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("  ⚠ No se pudieron leer metadatos ID3: " + e.getMessage());
            }
        }

        Song song = new Song(title, artist, album, null, filePath, albumArt);
        System.out.println("✓ Agregado: " + title + " - " + artist);
        return song;
    }

    /**
     * Extrae un título limpio del nombre del archivo
     */
    private static String extractTitleFromFilename(String filename) {
        // Quitar extensión
        int dotIndex = filename.lastIndexOf('.');
        String name = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;

        // Limpiar caracteres comunes en nombres de archivo
        name = name.replace("_", " ").replace("-", " - ");

        // Quitar números de pista al inicio (ej: "01 - ", "01. ", "1 - ")
        name = name.replaceFirst("^\\d{1,2}[.\\-\\s]+", "");

        return name.trim();
    }

    /**
     * Procesa una lista de archivos/directorios
     */
    public static List<Song> processFiles(List<File> files) {
        List<Song> songs = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory()) {
                songs.addAll(scanDirectory(file));
            } else if (isAudioFile(file)) {
                songs.add(createSongFromFile(file));
            }
        }
        return songs;
    }

    /**
     * Escanea un directorio recursivamente
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
     * Obtiene las extensiones soportadas
     */
    public static String getSupportedExtensionsText() {
        return String.join(", ", SUPPORTED_EXTENSIONS);
    }
}
