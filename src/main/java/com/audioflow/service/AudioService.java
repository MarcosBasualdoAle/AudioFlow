package com.audioflow.service;

import com.audioflow.model.Song;
import javafx.beans.property.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;

/**
 * Servicio que encapsula la lógica de reproducción de audio.
 * Usa MediaPlayer de JavaFX para reproducir archivos MP3/WAV.
 */
public class AudioService {

    private MediaPlayer mediaPlayer;
    private Song currentSong;

    // Propiedades observables para binding con la UI
    private final ObjectProperty<Duration> currentTime = new SimpleObjectProperty<>(Duration.ZERO);
    private final ObjectProperty<Duration> totalDuration = new SimpleObjectProperty<>(Duration.ZERO);
    private final DoubleProperty volume = new SimpleDoubleProperty(0.7);
    private final BooleanProperty playing = new SimpleBooleanProperty(false);
    private final BooleanProperty muted = new SimpleBooleanProperty(false);
    private final DoubleProperty progress = new SimpleDoubleProperty(0.0);

    // Listeners para eventos
    private Runnable onEndOfMedia;
    private Runnable onReady;

    public AudioService() {
        // Inicialización
    }

    /**
     * Carga y prepara una canción para reproducir
     */
    public void loadSong(Song song) {
        // Detener reproductor actual si existe
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        currentSong = song;

        try {
            // Crear Media desde el archivo
            File file = new File(song.getFilePath());
            if (!file.exists()) {
                System.err.println("Archivo no encontrado: " + song.getFilePath());
                return;
            }

            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            // Configurar bindings
            mediaPlayer.volumeProperty().bind(volume);
            mediaPlayer.muteProperty().bind(muted);

            // Listeners de tiempo
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                currentTime.set(newTime);
                if (totalDuration.get().toMillis() > 0) {
                    progress.set(newTime.toMillis() / totalDuration.get().toMillis());
                }
            });

            // Cuando el media está listo
            mediaPlayer.setOnReady(() -> {
                totalDuration.set(media.getDuration());
                song.setDuration(media.getDuration());
                if (onReady != null)
                    onReady.run();
                System.out.println("✓ Canción cargada: " + song.getTitle() + " (" + song.getFormattedDuration() + ")");
            });

            // Cuando termina la canción
            mediaPlayer.setOnEndOfMedia(() -> {
                playing.set(false);
                if (onEndOfMedia != null)
                    onEndOfMedia.run();
            });

            // Listener de estado
            mediaPlayer.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                playing.set(newStatus == MediaPlayer.Status.PLAYING);
            });

        } catch (Exception e) {
            System.err.println("Error al cargar la canción: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Inicia o reanuda la reproducción
     */
    public void play() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
            if (currentSong != null) {
                currentSong.setPlaying(true);
            }
        }
    }

    /**
     * Pausa la reproducción
     */
    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            if (currentSong != null) {
                currentSong.setPlaying(false);
            }
        }
    }

    /**
     * Alterna entre play y pause
     */
    public void togglePlayPause() {
        if (isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    /**
     * Detiene la reproducción completamente
     */
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            if (currentSong != null) {
                currentSong.setPlaying(false);
            }
        }
    }

    /**
     * Salta a una posición específica
     */
    public void seekTo(Duration position) {
        if (mediaPlayer != null) {
            mediaPlayer.seek(position);
        }
    }

    /**
     * Salta a un porcentaje de la canción (0.0 a 1.0)
     */
    public void seekToPercent(double percent) {
        if (mediaPlayer != null && totalDuration.get() != null) {
            Duration seekTime = totalDuration.get().multiply(percent);
            mediaPlayer.seek(seekTime);
        }
    }

    // ========== GETTERS DE PROPIEDADES ==========

    public Duration getCurrentTime() {
        return currentTime.get();
    }

    public ObjectProperty<Duration> currentTimeProperty() {
        return currentTime;
    }

    public Duration getTotalDuration() {
        return totalDuration.get();
    }

    public ObjectProperty<Duration> totalDurationProperty() {
        return totalDuration;
    }

    public double getVolume() {
        return volume.get();
    }

    public void setVolume(double value) {
        volume.set(Math.max(0, Math.min(1, value)));
    }

    public DoubleProperty volumeProperty() {
        return volume;
    }

    public boolean isPlaying() {
        return playing.get();
    }

    public BooleanProperty playingProperty() {
        return playing;
    }

    public boolean isMuted() {
        return muted.get();
    }

    public void setMuted(boolean value) {
        muted.set(value);
    }

    public BooleanProperty mutedProperty() {
        return muted;
    }

    public double getProgress() {
        return progress.get();
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    // ========== SETTERS DE CALLBACKS ==========

    public void setOnEndOfMedia(Runnable callback) {
        this.onEndOfMedia = callback;
    }

    public void setOnReady(Runnable callback) {
        this.onReady = callback;
    }

    /**
     * Formatea el tiempo actual como mm:ss
     */
    public String getFormattedCurrentTime() {
        return formatDuration(currentTime.get());
    }

    /**
     * Formatea la duración total como mm:ss
     */
    public String getFormattedTotalDuration() {
        return formatDuration(totalDuration.get());
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isUnknown())
            return "0:00";
        int totalSeconds = (int) duration.toSeconds();
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Libera recursos
     */
    public void dispose() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }
}
