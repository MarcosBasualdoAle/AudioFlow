package com.audioflow.controller;

import com.audioflow.model.Playlist;
import com.audioflow.model.Song;
import com.audioflow.service.AudioService;
import com.audioflow.util.DragDropHandler;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXSlider;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controlador principal de la aplicación AudioFlow.
 * Maneja la UI principal, la lista de canciones y los controles de
 * reproducción.
 */
public class MainController implements Initializable {

    // ========== ELEMENTOS DE UI (inyectados desde FXML) ==========

    @FXML
    private StackPane rootPane;
    @FXML
    private VBox dropZone;
    @FXML
    private JFXListView<Song> songListView;
    @FXML
    private Label songTitleLabel;
    @FXML
    private Label artistLabel;
    @FXML
    private Label currentTimeLabel;
    @FXML
    private Label totalTimeLabel;
    @FXML
    private JFXSlider progressSlider;
    @FXML
    private JFXSlider volumeSlider;
    @FXML
    private JFXButton playPauseBtn;
    @FXML
    private JFXButton prevBtn;
    @FXML
    private JFXButton nextBtn;
    @FXML
    private JFXButton shuffleBtn;
    @FXML
    private FontIcon playPauseIcon;
    @FXML
    private FontIcon volumeIcon;
    @FXML
    private Label dropHintLabel;
    @FXML
    private VBox nowPlayingInfo;

    // ========== SERVICIOS Y DATOS ==========

    private final AudioService audioService = new AudioService();
    private final Playlist playlist = new Playlist("Mi Playlist");
    private boolean isUserDraggingSlider = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("✓ Inicializando MainController...");

        setupDragAndDrop();
        setupPlaylist();
        setupPlayerControls();
        setupVolumeControl();
        setupProgressSlider();
        setupAudioServiceCallbacks();

        // Estado inicial
        updateNowPlayingUI(null);
    }

    // ========== CONFIGURACIÓN INICIAL ==========

    /**
     * Configura la zona de Drag & Drop
     */
    private void setupDragAndDrop() {
        DragDropHandler.setupDropZone(dropZone, songs -> {
            for (Song song : songs) {
                playlist.addSong(song);
            }
            updateDropZoneVisibility();

            // Si es la primera canción, prepararla
            if (playlist.size() == songs.size()) {
                loadCurrentSong();
            }
        });
    }

    /**
     * Configura la lista de canciones
     */
    private void setupPlaylist() {
        songListView.setItems(playlist.getSongs());

        // Doble clic para reproducir
        songListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Song selected = songListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    int index = playlist.getSongs().indexOf(selected);
                    playlist.goToIndex(index);
                    loadAndPlayCurrentSong();
                }
            }
        });

        // Personalizar cómo se muestran las canciones
        songListView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Song song, boolean empty) {
                super.updateItem(song, empty);
                if (empty || song == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(song.getTitle() + " - " + song.getArtist());
                    // Resaltar canción actual
                    if (song.isPlaying()) {
                        setStyle("-fx-text-fill: #1DB954; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    /**
     * Configura los botones de control de reproducción
     */
    private void setupPlayerControls() {
        // Los eventos de botones se configuran en el FXML con onAction
    }

    /**
     * Configura el control de volumen
     */
    private void setupVolumeControl() {
        if (volumeSlider != null) {
            volumeSlider.setValue(audioService.getVolume() * 100);
            volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                audioService.setVolume(newVal.doubleValue() / 100);
                updateVolumeIcon(newVal.doubleValue());
            });
        }
    }

    /**
     * Configura el slider de progreso
     */
    private void setupProgressSlider() {
        if (progressSlider != null) {
            // Detectar cuando el usuario arrastra el slider
            progressSlider.setOnMousePressed(e -> isUserDraggingSlider = true);
            progressSlider.setOnMouseReleased(e -> {
                isUserDraggingSlider = false;
                audioService.seekToPercent(progressSlider.getValue() / 100);
            });

            // Actualizar slider con el progreso de la canción
            audioService.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUserDraggingSlider) {
                    progressSlider.setValue(newVal.doubleValue() * 100);
                }
            });
        }
    }

    /**
     * Configura callbacks del servicio de audio
     */
    private void setupAudioServiceCallbacks() {
        // Actualizar tiempo actual
        audioService.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            Platform.runLater(() -> {
                if (currentTimeLabel != null) {
                    currentTimeLabel.setText(audioService.getFormattedCurrentTime());
                }
            });
        });

        // Cuando termina una canción, pasar a la siguiente
        audioService.setOnEndOfMedia(() -> {
            Platform.runLater(() -> {
                if (playlist.hasNext()) {
                    handleNext();
                } else {
                    System.out.println("✓ Playlist terminada");
                }
            });
        });

        // Cuando está listo el audio
        audioService.setOnReady(() -> {
            Platform.runLater(() -> {
                if (totalTimeLabel != null) {
                    totalTimeLabel.setText(audioService.getFormattedTotalDuration());
                }
            });
        });

        // Actualizar icono de play/pause
        audioService.playingProperty().addListener((obs, wasPlaying, isPlaying) -> {
            Platform.runLater(() -> updatePlayPauseIcon(isPlaying));
        });
    }

    // ========== ACCIONES DE BOTONES (llamadas desde FXML) ==========

    @FXML
    private void handlePlayPause() {
        if (playlist.isEmpty()) {
            System.out.println("No hay canciones en la playlist");
            return;
        }

        if (audioService.getCurrentSong() == null) {
            loadAndPlayCurrentSong();
        } else {
            audioService.togglePlayPause();
        }
    }

    @FXML
    private void handlePrevious() {
        if (playlist.hasPrevious()) {
            playlist.previous();
            loadAndPlayCurrentSong();
        }
    }

    @FXML
    private void handleNext() {
        if (playlist.hasNext()) {
            playlist.next();
            loadAndPlayCurrentSong();
        }
    }

    @FXML
    private void handleShuffle() {
        playlist.shuffle();
        songListView.refresh();
        System.out.println("✓ Playlist mezclada");
    }

    @FXML
    private void handleMute() {
        audioService.setMuted(!audioService.isMuted());
        updateVolumeIcon(audioService.isMuted() ? 0 : volumeSlider.getValue());
    }

    // ========== MÉTODOS AUXILIARES ==========

    /**
     * Carga la canción actual de la playlist
     */
    private void loadCurrentSong() {
        Song current = playlist.getCurrentSong();
        if (current != null) {
            audioService.loadSong(current);
            updateNowPlayingUI(current);
        }
    }

    /**
     * Carga y reproduce la canción actual
     */
    private void loadAndPlayCurrentSong() {
        // Marcar canción anterior como no playing
        playlist.getSongs().forEach(s -> s.setPlaying(false));

        Song current = playlist.getCurrentSong();
        if (current != null) {
            audioService.loadSong(current);
            audioService.setOnReady(() -> {
                audioService.play();
                Platform.runLater(() -> {
                    updateNowPlayingUI(current);
                    current.setPlaying(true);
                    songListView.refresh();
                });
            });
        }
    }

    /**
     * Actualiza la UI de "Reproduciendo ahora"
     */
    private void updateNowPlayingUI(Song song) {
        if (song != null) {
            if (songTitleLabel != null)
                songTitleLabel.setText(song.getTitle());
            if (artistLabel != null)
                artistLabel.setText(song.getArtist());
            if (nowPlayingInfo != null)
                nowPlayingInfo.setVisible(true);
        } else {
            if (songTitleLabel != null)
                songTitleLabel.setText("Sin canción");
            if (artistLabel != null)
                artistLabel.setText("Arrastra archivos MP3 para comenzar");
            if (nowPlayingInfo != null)
                nowPlayingInfo.setVisible(false);
        }
    }

    /**
     * Actualiza el icono de play/pause
     */
    private void updatePlayPauseIcon(boolean isPlaying) {
        if (playPauseIcon != null) {
            playPauseIcon.setIconLiteral(isPlaying ? "fas-pause" : "fas-play");
        }
    }

    /**
     * Actualiza el icono de volumen según el nivel
     */
    private void updateVolumeIcon(double volume) {
        if (volumeIcon != null) {
            if (volume == 0 || audioService.isMuted()) {
                volumeIcon.setIconLiteral("fas-volume-mute");
            } else if (volume < 50) {
                volumeIcon.setIconLiteral("fas-volume-down");
            } else {
                volumeIcon.setIconLiteral("fas-volume-up");
            }
        }
    }

    /**
     * Muestra/oculta el hint de drag & drop
     */
    private void updateDropZoneVisibility() {
        if (dropHintLabel != null) {
            dropHintLabel.setVisible(playlist.isEmpty());
        }
    }
}
