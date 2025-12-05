package com.audioflow.model;

import javafx.beans.property.*;
import javafx.scene.image.Image;
import javafx.util.Duration;

/**
 * Modelo que representa una canción en la aplicación.
 * Usa propiedades JavaFX para binding reactivo con la UI.
 */
public class Song {

    private final StringProperty title;
    private final StringProperty artist;
    private final StringProperty album;
    private final ObjectProperty<Duration> duration;
    private final StringProperty filePath;
    private final ObjectProperty<Image> albumArt;
    private final BooleanProperty playing;
    private final IntegerProperty rating; // 0-5 estrellas

    /**
     * Constructor completo
     */
    public Song(String title, String artist, String album, Duration duration, String filePath) {
        this.title = new SimpleStringProperty(title);
        this.artist = new SimpleStringProperty(artist);
        this.album = new SimpleStringProperty(album);
        this.duration = new SimpleObjectProperty<>(duration);
        this.filePath = new SimpleStringProperty(filePath);
        this.albumArt = new SimpleObjectProperty<>();
        this.playing = new SimpleBooleanProperty(false);
        this.rating = new SimpleIntegerProperty(0);
    }

    /**
     * Constructor con carátula de álbum
     */
    public Song(String title, String artist, String album, Duration duration, String filePath, Image albumArt) {
        this(title, artist, album, duration, filePath);
        this.albumArt.set(albumArt);
    }

    /**
     * Constructor simplificado (útil para drag & drop)
     */
    public Song(String filePath) {
        this("Canción Desconocida", "Artista Desconocido", "Álbum Desconocido", Duration.ZERO, filePath);
        // Extraer nombre del archivo como título temporal
        String fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
        fileName = fileName.substring(0, fileName.lastIndexOf("."));
        this.title.set(fileName);
    }

    // ========== GETTERS ==========

    public String getTitle() {
        return title.get();
    }

    public String getArtist() {
        return artist.get();
    }

    public String getAlbum() {
        return album.get();
    }

    public Duration getDuration() {
        return duration.get();
    }

    public String getFilePath() {
        return filePath.get();
    }

    public Image getAlbumArt() {
        return albumArt.get();
    }

    public boolean isPlaying() {
        return playing.get();
    }

    public int getRating() {
        return rating.get();
    }

    // ========== SETTERS ==========

    public void setTitle(String value) {
        title.set(value);
    }

    public void setArtist(String value) {
        artist.set(value);
    }

    public void setAlbum(String value) {
        album.set(value);
    }

    public void setDuration(Duration value) {
        duration.set(value);
    }

    public void setFilePath(String value) {
        filePath.set(value);
    }

    public void setAlbumArt(Image value) {
        albumArt.set(value);
    }

    public void setPlaying(boolean value) {
        playing.set(value);
    }

    public void setRating(int value) {
        rating.set(Math.max(0, Math.min(5, value))); // Limitar a 0-5
    }

    // ========== PROPIEDADES (para binding) ==========

    public StringProperty titleProperty() {
        return title;
    }

    public StringProperty artistProperty() {
        return artist;
    }

    public StringProperty albumProperty() {
        return album;
    }

    public ObjectProperty<Duration> durationProperty() {
        return duration;
    }

    public StringProperty filePathProperty() {
        return filePath;
    }

    public ObjectProperty<Image> albumArtProperty() {
        return albumArt;
    }

    public BooleanProperty playingProperty() {
        return playing;
    }

    public IntegerProperty ratingProperty() {
        return rating;
    }

    /**
     * Formatea la duración como mm:ss
     */
    public String getFormattedDuration() {
        if (duration.get() == null)
            return "0:00";
        int totalSeconds = (int) duration.get().toSeconds();
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    public String toString() {
        return String.format("%s - %s", getArtist(), getTitle());
    }
}
