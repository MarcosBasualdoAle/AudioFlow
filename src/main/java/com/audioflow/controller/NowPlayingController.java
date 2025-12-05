package com.audioflow.controller;

import com.audioflow.component.AudioVisualizer;
import com.audioflow.component.StarRating;
import com.audioflow.model.Song;
import com.audioflow.service.AudioAnalyzerService;
import com.audioflow.service.AudioService;
import com.audioflow.service.KeyboardService;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSlider;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controlador para la vista inmersiva "Now Playing".
 * Muestra carátula grande, visualizador de audio, rating y controles.
 */
public class NowPlayingController implements Initializable {

    // ========== ELEMENTOS DE UI ==========
    @FXML
    private StackPane rootPane;
    @FXML
    private VBox backgroundGradient;
    @FXML
    private ImageView albumArtView;
    @FXML
    private VBox albumPlaceholder;
    @FXML
    private AudioVisualizer audioVisualizer;
    @FXML
    private Label songTitleLabel;
    @FXML
    private Label artistLabel;
    @FXML
    private StarRating starRating;
    @FXML
    private JFXSlider progressSlider;
    @FXML
    private JFXSlider volumeSlider;
    @FXML
    private Label currentTimeLabel;
    @FXML
    private Label totalTimeLabel;
    @FXML
    private JFXButton playPauseBtn;
    @FXML
    private JFXButton shuffleBtn;
    @FXML
    private JFXButton repeatBtn;
    @FXML
    private FontIcon playPauseIcon;
    @FXML
    private FontIcon shuffleIcon;
    @FXML
    private FontIcon repeatIcon;
    @FXML
    private FontIcon volumeIcon;
    @FXML
    private JFXButton closeBtn;
    @FXML
    private StackPane feedbackOverlay;
    @FXML
    private FontIcon feedbackIcon;

    // ========== SERVICIOS ==========
    private AudioService audioService;
    private AudioAnalyzerService analyzerService;
    private KeyboardService keyboardService;

    // ========== ESTADO ==========
    private boolean isUserDraggingSlider = false;
    private boolean shuffleEnabled = false;
    private RepeatMode repeatMode = RepeatMode.OFF;
    private Runnable onCloseCallback;
    private Runnable onPreviousCallback;
    private Runnable onNextCallback;
    private Song currentSong; // Canción actual para persistir rating

    public enum RepeatMode {
        OFF, ALL, ONE
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("✓ Inicializando NowPlayingController...");

        setupProgressSlider();
        setupVolumeControl();
        setupVisualizer();
        setupFeedbackOverlay();

        updateSongInfo(null);
    }

    /**
     * Configura el controlador con los servicios compartidos
     */
    public void setup(AudioService audioService, KeyboardService keyboardService) {
        this.audioService = audioService;
        this.keyboardService = keyboardService;
        this.analyzerService = new AudioAnalyzerService();

        bindToAudioService();
        setupKeyboardShortcuts();

        // Conectar visualizador con datos reales del MediaPlayer
        if (audioVisualizer != null) {
            // Registrar spectrum listener para datos reales
            audioService.setAudioSpectrumListener(
                    (double timestamp, double duration, float[] magnitudes, float[] phases) -> {
                        audioVisualizer.updateSpectrum(timestamp, duration, magnitudes, phases);
                    });
        }
    }

    /**
     * Enlaza propiedades con el AudioService
     */
    private void bindToAudioService() {
        if (audioService == null)
            return;

        // Actualizar tiempo actual
        audioService.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            Platform.runLater(() -> {
                if (currentTimeLabel != null) {
                    currentTimeLabel.setText(audioService.getFormattedCurrentTime());
                }
            });
        });

        // Actualizar progreso del slider
        audioService.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUserDraggingSlider && progressSlider != null) {
                progressSlider.setValue(newVal.doubleValue() * 100);
            }
        });

        // Actualizar icono play/pause
        audioService.playingProperty().addListener((obs, wasPlaying, isPlaying) -> {
            Platform.runLater(() -> {
                updatePlayPauseIcon(isPlaying);
                if (isPlaying) {
                    audioVisualizer.start();
                } else {
                    audioVisualizer.stop();
                }
            });
        });

        // Cuando el audio está listo
        audioService.setOnReady(() -> {
            Platform.runLater(() -> {
                if (totalTimeLabel != null) {
                    totalTimeLabel.setText(audioService.getFormattedTotalDuration());
                }
            });
        });

        // Sincronizar volumen
        if (volumeSlider != null) {
            volumeSlider.setValue(audioService.getVolume() * 100);
        }
    }

    /**
     * Configura atajos de teclado
     */
    private void setupKeyboardShortcuts() {
        if (keyboardService != null) {
            keyboardService.setHandlers(
                    this::handlePlayPause,
                    this::handleNext,
                    this::handlePrevious,
                    this::handleClose);

            // Agregar handlers de volumen
            keyboardService.setVolumeHandlers(
                    () -> changeVolume(5), // UP = +5%
                    () -> changeVolume(-5), // DOWN = -5%
                    this::handleMute); // M = mute toggle
        }
    }

    /**
     * Cambia el volumen en el porcentaje indicado
     */
    private void changeVolume(double delta) {
        if (audioService != null && volumeSlider != null) {
            double newValue = volumeSlider.getValue() + delta;
            newValue = Math.max(0, Math.min(100, newValue));
            volumeSlider.setValue(newValue);
        }
    }

    // ========== CONFIGURACIÓN DE UI ==========

    private void setupProgressSlider() {
        if (progressSlider != null) {
            progressSlider.setOnMousePressed(e -> isUserDraggingSlider = true);
            progressSlider.setOnMouseReleased(e -> {
                isUserDraggingSlider = false;
                if (audioService != null) {
                    audioService.seekToPercent(progressSlider.getValue() / 100);
                }
            });
            // Evitar que el slider capture las flechas LEFT/RIGHT (usadas para prev/next)
            progressSlider.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.RIGHT) {
                    e.consume(); // No dejar que el slider procese estas teclas
                }
            });
        }
    }

    private void setupVolumeControl() {
        if (volumeSlider != null) {
            volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (audioService != null) {
                    audioService.setVolume(newVal.doubleValue() / 100);
                    updateVolumeIcon(newVal.doubleValue());
                }
            });
            // Evitar que el slider capture las flechas LEFT/RIGHT (usadas para prev/next)
            volumeSlider.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.RIGHT) {
                    e.consume(); // No dejar que el slider procese estas teclas
                }
            });
        }
    }

    private void setupVisualizer() {
        if (audioVisualizer != null) {
            audioVisualizer.setNumBars(32);
            audioVisualizer.setUseGradient(true);
        }
    }

    private void setupFeedbackOverlay() {
        if (feedbackOverlay != null) {
            feedbackOverlay.setOpacity(0);
        }
    }

    // ========== ACCIONES DE BOTONES ==========

    @FXML
    private void handlePlayPause() {
        if (audioService != null) {
            audioService.togglePlayPause();
            showFeedback(audioService.isPlaying() ? "fas-pause" : "fas-play");
        }
    }

    @FXML
    private void handlePrevious() {
        showFeedback("fas-step-backward");
        if (onPreviousCallback != null) {
            onPreviousCallback.run();
        }
    }

    @FXML
    private void handleNext() {
        showFeedback("fas-step-forward");
        if (onNextCallback != null) {
            onNextCallback.run();
        }
    }

    @FXML
    private void handleShuffle() {
        shuffleEnabled = !shuffleEnabled;
        updateShuffleButton();
        showFeedback("fas-random");
    }

    @FXML
    private void handleRepeat() {
        // Ciclar entre modos: OFF -> ALL -> ONE -> OFF
        repeatMode = switch (repeatMode) {
            case OFF -> RepeatMode.ALL;
            case ALL -> RepeatMode.ONE;
            case ONE -> RepeatMode.OFF;
        };
        updateRepeatButton();
    }

    @FXML
    private void handleMute() {
        if (audioService != null) {
            audioService.setMuted(!audioService.isMuted());
            updateVolumeIcon(audioService.isMuted() ? 0 : volumeSlider.getValue());
        }
    }

    @FXML
    private void handleClose() {
        // Animación de fade out
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), rootPane);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
        });
        fadeOut.play();
    }

    // ========== MÉTODOS PÚBLICOS ==========

    /**
     * Actualiza la UI con la información de una canción
     */
    public void updateSongInfo(Song song) {
        this.currentSong = song;

        if (song != null) {
            if (songTitleLabel != null)
                songTitleLabel.setText(song.getTitle());
            if (artistLabel != null)
                artistLabel.setText(song.getArtist());

            // Actualizar carátula
            if (song.getAlbumArt() != null && albumArtView != null) {
                albumArtView.setImage(song.getAlbumArt());
                albumArtView.setVisible(true);
                if (albumPlaceholder != null)
                    albumPlaceholder.setVisible(false);
            } else {
                if (albumArtView != null)
                    albumArtView.setVisible(false);
                if (albumPlaceholder != null)
                    albumPlaceholder.setVisible(true);
            }

            // Conectar StarRating con la canción
            if (starRating != null) {
                starRating.setRating(song.getRating());
                starRating.ratingProperty().addListener((obs, oldVal, newVal) -> {
                    if (currentSong != null) {
                        currentSong.setRating(newVal.intValue());
                    }
                });
            }
        } else {
            if (songTitleLabel != null)
                songTitleLabel.setText("Sin canción");
            if (artistLabel != null)
                artistLabel.setText("Carga música para reproducir");
            if (albumArtView != null)
                albumArtView.setVisible(false);
            if (albumPlaceholder != null)
                albumPlaceholder.setVisible(true);
            if (starRating != null)
                starRating.setRating(0);
        }
    }

    /**
     * Establece callback para cuando se cierra la vista
     */
    public void setOnClose(Runnable callback) {
        this.onCloseCallback = callback;
    }

    /**
     * Establece callback para canción anterior
     */
    public void setOnPrevious(Runnable callback) {
        this.onPreviousCallback = callback;
    }

    /**
     * Establece callback para siguiente canción
     */
    public void setOnNext(Runnable callback) {
        this.onNextCallback = callback;
    }

    /**
     * Muestra la vista con animación
     */
    public void show() {
        rootPane.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), rootPane);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        // Sincronizar estado del botón play/pause con el reproductor
        if (audioService != null) {
            boolean isPlaying = audioService.isPlaying();
            updatePlayPauseIcon(isPlaying);

            // Iniciar/detener visualizador según estado
            if (isPlaying) {
                audioVisualizer.start();
            } else {
                audioVisualizer.stop(); // Mostrará barras vacías estáticas
            }
        }
    }

    // ========== MÉTODOS AUXILIARES ==========

    private void updatePlayPauseIcon(boolean isPlaying) {
        if (playPauseIcon != null) {
            playPauseIcon.setIconLiteral(isPlaying ? "fas-pause" : "fas-play");
        }
    }

    private void updateVolumeIcon(double volume) {
        if (volumeIcon != null) {
            if (volume == 0 || (audioService != null && audioService.isMuted())) {
                volumeIcon.setIconLiteral("fas-volume-mute");
            } else if (volume < 50) {
                volumeIcon.setIconLiteral("fas-volume-down");
            } else {
                volumeIcon.setIconLiteral("fas-volume-up");
            }
        }
    }

    private void updateShuffleButton() {
        if (shuffleBtn != null) {
            if (shuffleEnabled) {
                shuffleBtn.getStyleClass().add("active");
            } else {
                shuffleBtn.getStyleClass().remove("active");
            }
        }
    }

    private void updateRepeatButton() {
        if (repeatBtn != null && repeatIcon != null) {
            repeatBtn.getStyleClass().remove("active");

            switch (repeatMode) {
                case OFF -> repeatIcon.setIconLiteral("fas-redo");
                case ALL -> {
                    repeatIcon.setIconLiteral("fas-redo");
                    repeatBtn.getStyleClass().add("active");
                }
                case ONE -> {
                    repeatIcon.setIconLiteral("fas-redo-alt");
                    repeatBtn.getStyleClass().add("active");
                }
            }
        }
    }

    /**
     * Muestra feedback visual temporal (icono grande translúcido)
     */
    private void showFeedback(String iconLiteral) {
        if (feedbackOverlay == null || feedbackIcon == null)
            return;

        feedbackIcon.setIconLiteral(iconLiteral);

        // Fade in
        FadeTransition fadeIn = new FadeTransition(Duration.millis(100), feedbackOverlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        // Fade out después de 300ms
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), feedbackOverlay);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.millis(200));

        fadeIn.setOnFinished(e -> fadeOut.play());
        fadeIn.play();
    }

    public boolean isShuffleEnabled() {
        return shuffleEnabled;
    }

    public RepeatMode getRepeatMode() {
        return repeatMode;
    }
}
