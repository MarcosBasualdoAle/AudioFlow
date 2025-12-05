package com.audioflow.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Collections;

/**
 * Modelo que representa una playlist (lista de reproducción).
 * Contiene una colección observable de canciones.
 */
public class Playlist {

    private String name;
    private final ObservableList<Song> songs;
    private int currentIndex;

    public Playlist(String name) {
        this.name = name;
        this.songs = FXCollections.observableArrayList();
        this.currentIndex = -1;
    }

    // ========== GESTIÓN DE CANCIONES ==========

    /**
     * Agrega una canción a la playlist
     */
    public void addSong(Song song) {
        songs.add(song);
        if (currentIndex == -1) {
            currentIndex = 0;
        }
    }

    /**
     * Elimina una canción de la playlist
     */
    public void removeSong(Song song) {
        int index = songs.indexOf(song);
        songs.remove(song);

        // Ajustar índice si es necesario
        if (index <= currentIndex && currentIndex > 0) {
            currentIndex--;
        }
    }

    /**
     * Limpia toda la playlist
     */
    public void clear() {
        songs.clear();
        currentIndex = -1;
    }

    /**
     * Mezcla aleatoriamente las canciones
     */
    public void shuffle() {
        if (songs.size() > 1) {
            Song current = getCurrentSong();
            Collections.shuffle(songs);
            // Mover la canción actual al inicio
            if (current != null) {
                songs.remove(current);
                songs.add(0, current);
                currentIndex = 0;
            }
        }
    }

    // ========== NAVEGACIÓN ==========

    /**
     * Obtiene la canción actual
     */
    public Song getCurrentSong() {
        if (currentIndex >= 0 && currentIndex < songs.size()) {
            return songs.get(currentIndex);
        }
        return null;
    }

    /**
     * Avanza a la siguiente canción
     * 
     * @return true si se pudo avanzar
     */
    public boolean next() {
        if (currentIndex < songs.size() - 1) {
            currentIndex++;
            return true;
        }
        return false;
    }

    /**
     * Retrocede a la canción anterior
     * 
     * @return true si se pudo retroceder
     */
    public boolean previous() {
        if (currentIndex > 0) {
            currentIndex--;
            return true;
        }
        return false;
    }

    /**
     * Salta a un índice específico
     */
    public void goToIndex(int index) {
        if (index >= 0 && index < songs.size()) {
            currentIndex = index;
        }
    }

    // ========== GETTERS ==========

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ObservableList<Song> getSongs() {
        return songs;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int size() {
        return songs.size();
    }

    public boolean isEmpty() {
        return songs.isEmpty();
    }

    /**
     * Verifica si hay una canción siguiente
     */
    public boolean hasNext() {
        return currentIndex < songs.size() - 1;
    }

    /**
     * Verifica si hay una canción anterior
     */
    public boolean hasPrevious() {
        return currentIndex > 0;
    }
}
