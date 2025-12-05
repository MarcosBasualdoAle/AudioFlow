package com.audioflow.controller;

import com.audioflow.model.Song;
import com.audioflow.service.AudioService;
import com.jfoenix.controls.JFXButton;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controlador del Mini Player flotante.
 * Widget compacto que muestra controles básicos de reproducción.
 * Sincronizado con la ventana principal a través del AudioService compartido.
 * 
 * @author Brickzon - Mini Player implementation
 */
public class MiniPlayerController implements Initializable {

    // ========== ELEMENTOS DE UI ==========

    @FXML
    private StackPane rootPane;

    @FXML
    private StackPane albumArtContainer;

    @FXML
    private ImageView albumArtView;

    @FXML
    private FontIcon albumArtPlaceholder;

    @FXML
    private Label songTitleLabel;

    @FXML
    private Label artistLabel;

    @FXML
    private JFXButton favoriteBtn;

    @FXML
    private FontIcon favoriteIcon;

    @FXML
    private JFXButton prevBtn;

    @FXML
    private JFXButton playPauseBtn;

    @FXML
    private FontIcon playPauseIcon;

    @FXML
    private JFXButton nextBtn;

    @FXML
    private HBox progressContainer;

    @FXML
    private StackPane progressBar;

    @FXML
    private StackPane progressFill;

    @FXML
    private JFXButton closeBtn;

    // ========== REFERENCIAS ==========

    private AudioService audioService;
    private MainController mainController;
    private boolean isFavorite = false;

    // Variables para arrastre de ventana
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("✓ Inicializando MiniPlayerController...");

        // Obtener el AudioService compartido
        audioService = MainController.getAudioService();

        // Configurar listeners
        setupAudioServiceListeners();

        // Configurar arrastre de ventana
        setupWindowDrag();

        // Actualizar UI con la canción actual (si hay una)
        updateNowPlayingUI(audioService.getCurrentSong());
        updatePlayPauseIcon(audioService.isPlaying());
    }

    /**
     * Configura el MainController de referencia para poder restaurar la ventana
     * principal.
     */
    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    // ========== CONFIGURACIÓN ==========

    private void setupAudioServiceListeners() {
        // Listener para estado de reproducción
        audioService.playingProperty().addListener((obs, wasPlaying, isPlaying) -> {
            Platform.runLater(() -> updatePlayPauseIcon(isPlaying));
        });

        // Listener para progreso
        audioService.progressProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> updateProgressBar(newVal.doubleValue()));
        });
    }

    private void setupWindowDrag() {
        if (rootPane != null) {
            rootPane.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });

            rootPane.setOnMouseDragged(event -> {
                Stage stage = (Stage) rootPane.getScene().getWindow();
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });
        }
    }

    // ========== ACCIONES DE BOTONES ==========

    @FXML
    private void handlePlayPause() {
        if (audioService.getCurrentSong() == null) {
            System.out.println("No hay canción cargada");
            return;
        }
        audioService.togglePlayPause();
    }

    @FXML
    private void handlePrevious() {
        if (mainController != null) {
            mainController.getPlaylist().previous();
            Song current = mainController.getPlaylist().getCurrentSong();
            if (current != null) {
                audioService.loadSong(current);
                audioService.setOnReady(() -> {
                    audioService.play();
                    Platform.runLater(() -> updateNowPlayingUI(current));
                });
            }
        }
    }

    @FXML
    private void handleNext() {
        if (mainController != null) {
            mainController.getPlaylist().next();
            Song current = mainController.getPlaylist().getCurrentSong();
            if (current != null) {
                audioService.loadSong(current);
                audioService.setOnReady(() -> {
                    audioService.play();
                    Platform.runLater(() -> updateNowPlayingUI(current));
                });
            }
        }
    }

    @FXML
    private void handleFavorite() {
        isFavorite = !isFavorite;
        if (favoriteIcon != null) {
            favoriteIcon.setIconLiteral(isFavorite ? "fas-heart" : "far-heart");
            if (isFavorite) {
                favoriteIcon.getStyleClass().add("favorite-active");
            } else {
                favoriteIcon.getStyleClass().remove("favorite-active");
            }
        }
        System.out.println(isFavorite ? "♥ Añadido a favoritos" : "♡ Eliminado de favoritos");
    }

    @FXML
    private void handleClose() {
        // Mostrar diálogo de confirmación
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cerrar Mini Player");
        alert.setHeaderText("¿Qué deseas hacer?");
        alert.setContentText("Puedes restaurar la ventana principal o minimizar a la bandeja del sistema.");

        ButtonType restoreButton = new ButtonType("Restaurar ventana principal");
        ButtonType minimizeButton = new ButtonType("Mantener en segundo plano");
        ButtonType cancelButton = ButtonType.CANCEL;

        alert.getButtonTypes().setAll(restoreButton, minimizeButton, cancelButton);

        // Aplicar estilo oscuro al diálogo
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/audioflow/styles/mini-player.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("mini-player-dialog");

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {
            if (result.get() == restoreButton) {
                restoreMainWindow();
            } else if (result.get() == minimizeButton) {
                // Mantener mini player abierto, no hacer nada
                System.out.println("Mini Player manteniéndose en segundo plano");
            }
            // Si es cancelar, no hacer nada
        }
    }

    private void restoreMainWindow() {
        // Cerrar el mini player con animación
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), rootPane);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            Stage miniStage = (Stage) rootPane.getScene().getWindow();
            miniStage.close();

            // Mostrar la ventana principal
            Stage mainStage = com.audioflow.App.getPrimaryStage();
            if (mainStage != null) {
                mainStage.show();
                mainStage.toFront();
            }
        });
        fadeOut.play();
    }

    // ========== MÉTODOS DE UI ==========

    private void updateNowPlayingUI(Song song) {
        if (song != null) {
            if (songTitleLabel != null) {
                songTitleLabel.setText(song.getTitle());
            }
            if (artistLabel != null) {
                artistLabel.setText(song.getArtist() + " • " + song.getAlbum());
            }

            // Actualizar album art
            if (albumArtView != null) {
                Image albumArt = song.getAlbumArt();
                if (albumArt != null) {
                    albumArtView.setImage(albumArt);
                    albumArtView.setVisible(true);
                    if (albumArtPlaceholder != null) {
                        albumArtPlaceholder.setVisible(false);
                    }

                    // Aplicar clip circular
                    Circle clip = new Circle(30, 30, 30);
                    albumArtView.setClip(clip);
                } else {
                    albumArtView.setImage(null);
                    albumArtView.setVisible(false);
                    if (albumArtPlaceholder != null) {
                        albumArtPlaceholder.setVisible(true);
                    }
                }
            }
        } else {
            if (songTitleLabel != null) {
                songTitleLabel.setText("Sin canción");
            }
            if (artistLabel != null) {
                artistLabel.setText("Selecciona algo para reproducir");
            }
            if (albumArtView != null) {
                albumArtView.setImage(null);
                albumArtView.setVisible(false);
            }
            if (albumArtPlaceholder != null) {
                albumArtPlaceholder.setVisible(true);
            }
        }
    }

    private void updatePlayPauseIcon(boolean isPlaying) {
        if (playPauseIcon != null) {
            playPauseIcon.setIconLiteral(isPlaying ? "fas-pause" : "fas-play");
        }
    }

    private void updateProgressBar(double progress) {
        if (progressFill != null && progressContainer != null) {
            double width = progressContainer.getWidth() * progress;
            progressFill.setPrefWidth(Math.max(0, width));
        }
    }

    /**
     * Actualiza la UI cuando cambia la canción desde otra parte de la aplicación.
     */
    public void refreshCurrentSong() {
        updateNowPlayingUI(audioService.getCurrentSong());
    }
}
