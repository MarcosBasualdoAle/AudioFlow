package com.audioflow.controller;

import com.audioflow.model.Playlist;
import com.audioflow.model.Song;
import com.audioflow.service.AudioService;
import com.audioflow.service.KeyboardService;
import com.audioflow.util.DragDropHandler;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXSlider;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador principal de la aplicación AudioFlow.
 * Maneja la UI principal con estilo Groove/Spotify.
 * 
 * @author Brickzon - Dashboard implementation
 */
public class MainController implements Initializable {

    // ========== ELEMENTOS DE UI (inyectados desde FXML) ==========

    // Root y contenedores principales
    @FXML
    private BorderPane rootPane;
    @FXML
    private VBox mainContent;

    // Header de Playlist
    @FXML
    private StackPane playlistCover;
    @FXML
    private Label playlistTitle;
    @FXML
    private Label playlistDescription;
    @FXML
    private Label songCountLabel;
    @FXML
    private Label totalDurationLabel;

    // Acciones
    @FXML
    private JFXButton playAllBtn;
    @FXML
    private FontIcon playAllIcon;
    @FXML
    private JFXButton shuffleBtn;
    @FXML
    private TextField searchField;

    // Lista de Canciones
    @FXML
    private JFXListView<Song> songListView;

    // Player Bar
    @FXML
    private JFXSlider progressSlider;
    @FXML
    private JFXSlider volumeSlider;
    @FXML
    private Label currentTimeLabel;
    @FXML
    private Label totalTimeLabel;
    @FXML
    private HBox nowPlayingInfo;
    @FXML
    private Label songTitleLabel;
    @FXML
    private Label artistLabel;
    @FXML
    private JFXButton playPauseBtn;
    @FXML
    private FontIcon playPauseIcon;
    @FXML
    private FontIcon volumeIcon;
    @FXML
    private HBox volumeContainer;
    @FXML
    private JFXButton minimizeBtn;
    @FXML
    private ImageView nowPlayingAlbumArt;
    @FXML
    private FontIcon nowPlayingPlaceholder;
    @FXML
    private StackPane nowPlayingCoverContainer;

    // Animación de giro para album art
    private RotateTransition albumArtRotation;

    // ========== SERVICIOS Y DATOS ==========

    private static AudioService audioService;
    private final Playlist playlist = new Playlist("Mi Biblioteca");
    private boolean isUserDraggingSlider = false;
    private FilteredList<Song> filteredSongs;
    private double savedVolumeBeforeSeek = 0.7;

    public static AudioService getAudioService() {
        if (audioService == null) {
            audioService = new AudioService();
        }
        return audioService;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("✓ Inicializando MainController (Estilo Groove)...");

        if (audioService == null) {
            audioService = new AudioService();
        }

        setupPlaylist();
        setupVolumeControl();
        setupProgressSlider();
        setupAudioServiceCallbacks();
        setupSearch();

        updatePlaylistStats();
        updateNowPlayingUI(null);
    }

    // ========== CONFIGURACIÓN INICIAL ==========

    private boolean hasAudioFiles(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            for (File file : event.getDragboard().getFiles()) {
                if (file.isDirectory() || DragDropHandler.isAudioFile(file)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void processDirectory(File directory, List<Song> songs) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    processDirectory(file, songs);
                } else if (DragDropHandler.isAudioFile(file)) {
                    songs.add(DragDropHandler.createSongFromFile(file));
                }
            }
        }
    }

    private void setupPlaylist() {
        filteredSongs = new FilteredList<>(playlist.getSongs(), p -> true);
        songListView.setItems(filteredSongs);

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

        // Estilo tabla para celdas
        songListView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            private final HBox container = new HBox();
            private final Label indexLabel = new Label();
            private final VBox textContainer = new VBox(2);
            private final Label titleLabel = new Label();
            private final Label artistLabel = new Label();
            private final Label albumLabel = new Label();
            private final Label durationLabel = new Label();
            private final FontIcon playIcon = new FontIcon("fas-play");
            private final StackPane indexStack = new StackPane();

            // Album art en celda
            private final StackPane artContainer = new StackPane();
            private final ImageView cellAlbumArt = new ImageView();
            private final FontIcon artPlaceholder = new FontIcon("fas-compact-disc");

            {
                indexLabel.getStyleClass().add("song-cell-index");
                titleLabel.getStyleClass().add("song-cell-title");
                artistLabel.getStyleClass().add("song-cell-artist");
                albumLabel.getStyleClass().add("song-cell-album");
                durationLabel.getStyleClass().add("song-cell-duration");
                playIcon.setIconSize(12);
                playIcon.getStyleClass().add("play-hover-icon");
                playIcon.setVisible(false);

                indexStack.getChildren().addAll(indexLabel, playIcon);
                indexStack.setMinWidth(50);
                indexStack.setAlignment(Pos.CENTER);

                // Configurar album art en celda
                cellAlbumArt.setFitWidth(40);
                cellAlbumArt.setFitHeight(40);
                cellAlbumArt.setPreserveRatio(true);
                artPlaceholder.setIconSize(18);
                artPlaceholder.getStyleClass().add("song-art-placeholder");
                artContainer.getChildren().addAll(cellAlbumArt, artPlaceholder);
                artContainer.getStyleClass().add("song-album-art-container");
                artContainer.setMinWidth(40);
                artContainer.setMinHeight(40);

                textContainer.getChildren().addAll(titleLabel, artistLabel);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                container.getChildren().addAll(indexStack, artContainer, textContainer, spacer, albumLabel,
                        durationLabel);
                container.setAlignment(Pos.CENTER_LEFT);
                container.setSpacing(12);
                container.getStyleClass().add("song-cell-container");
            }

            @Override
            protected void updateItem(Song song, boolean empty) {
                super.updateItem(song, empty);
                if (empty || song == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    int idx = getIndex() + 1;
                    indexLabel.setText(String.valueOf(idx));
                    titleLabel.setText(song.getTitle());
                    artistLabel.setText(song.getArtist());
                    albumLabel.setText(song.getAlbum());
                    durationLabel.setText(song.getFormattedDuration());

                    // Mostrar album art si existe
                    Image art = song.getAlbumArt();
                    if (art != null) {
                        cellAlbumArt.setImage(art);
                        cellAlbumArt.setVisible(true);
                        artPlaceholder.setVisible(false);
                    } else {
                        cellAlbumArt.setImage(null);
                        cellAlbumArt.setVisible(false);
                        artPlaceholder.setVisible(true);
                    }

                    if (song.isPlaying()) {
                        container.getStyleClass().add("song-cell-playing");
                        indexLabel.setVisible(false);
                        playIcon.setVisible(true);
                        playIcon.setIconLiteral("fas-volume-up");
                    } else {
                        container.getStyleClass().remove("song-cell-playing");
                        indexLabel.setVisible(true);
                        playIcon.setVisible(false);
                    }

                    setGraphic(container);
                }
            }
        });
    }

    private void setupVolumeControl() {
        if (volumeSlider != null) {
            volumeSlider.setValue(audioService.getVolume() * 100);
            volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                audioService.setVolume(newVal.doubleValue() / 100);
                updateVolumeIcon(newVal.doubleValue());
            });

            // Sincronizar cuando el volumen cambia desde otra parte (Now Playing, teclado)
            audioService.volumeProperty().addListener((obs, oldVal, newVal) -> {
                double sliderValue = newVal.doubleValue() * 100;
                if (Math.abs(volumeSlider.getValue() - sliderValue) > 0.5) {
                    Platform.runLater(() -> {
                        volumeSlider.setValue(sliderValue);
                        updateVolumeIcon(sliderValue);
                    });
                }
            });
        }
    }

    private void setupProgressSlider() {
        if (progressSlider != null) {
            progressSlider.setOnMousePressed(e -> {
                isUserDraggingSlider = true;
                savedVolumeBeforeSeek = volumeSlider.getValue();
                volumeSlider.setValue(savedVolumeBeforeSeek * 0.3);
            });

            progressSlider.setOnMouseReleased(e -> {
                isUserDraggingSlider = false;
                audioService.seekToPercent(progressSlider.getValue() / 100);
                volumeSlider.setValue(savedVolumeBeforeSeek);
            });

            audioService.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUserDraggingSlider) {
                    progressSlider.setValue(newVal.doubleValue() * 100);
                }
            });
        }
    }

    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> filterSongs(newVal));
        }
    }

    private void updateFilteredList() {
        if (filteredSongs != null && searchField != null) {
            filterSongs(searchField.getText());
        }
    }

    private void filterSongs(String searchText) {
        if (filteredSongs != null) {
            if (searchText == null || searchText.isEmpty()) {
                filteredSongs.setPredicate(song -> true);
            } else {
                String lowerCaseFilter = searchText.toLowerCase();
                filteredSongs.setPredicate(song -> song.getTitle().toLowerCase().contains(lowerCaseFilter) ||
                        song.getArtist().toLowerCase().contains(lowerCaseFilter) ||
                        song.getAlbum().toLowerCase().contains(lowerCaseFilter));
            }
        }
    }

    private void setupAudioServiceCallbacks() {
        audioService.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            Platform.runLater(() -> {
                if (currentTimeLabel != null) {
                    currentTimeLabel.setText(audioService.getFormattedCurrentTime());
                }
            });
        });

        audioService.setOnEndOfMedia(() -> {
            Platform.runLater(() -> {
                if (playlist.hasNext()) {
                    handleNext();
                }
            });
        });

        audioService.setOnReady(() -> {
            Platform.runLater(() -> {
                if (totalTimeLabel != null) {
                    totalTimeLabel.setText(audioService.getFormattedTotalDuration());
                }
            });
        });

        audioService.playingProperty().addListener((obs, wasPlaying, isPlaying) -> {
            Platform.runLater(() -> updatePlayPauseIcon(isPlaying));
        });
    }

    // ========== ACCIONES DE BOTONES ==========

    @FXML
    private void handlePlayPause() {
        if (playlist.isEmpty()) {
            System.out.println("No hay canciones");
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

    @FXML
    private void handleSearch() {
        filterSongs(searchField.getText());
    }

    @FXML
    private void handleHomeClick() {
        // Si estamos en otra vista, volver a la principal
        if (originalCenterContent != null && rootPane.getCenter() != originalCenterContent) {
            showMainView();
        }
        // Mostrar todas las canciones
        if (searchField != null) {
            searchField.clear();
        }
        filterSongs("");
    }

    @FXML
    private void handleAllSongsClick() {
        handleHomeClick();
    }

    @FXML
    private void handleVolumeHoverEnter() {
        if (volumeContainer != null) {
            volumeContainer.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 5;");
        }
    }

    @FXML
    private void handleVolumeHoverExit() {
        if (volumeContainer != null) {
            volumeContainer.setStyle("");
        }
    }

    @FXML
    private void handleMinimizeToMiniPlayer() {
        System.out.println("ℹ️ Mini Player - próxima fase");
    }

    // ========== DRAG & DROP GLOBAL (toda la app) ==========

    @FXML
    private void handleDragOver(DragEvent event) {
        if (event.getGestureSource() != rootPane && hasAudioFiles(event)) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    @FXML
    private void handleDragEntered(DragEvent event) {
        if (hasAudioFiles(event)) {
            // Aplicar glow al área principal
            if (mainContent != null) {
                mainContent.getStyleClass().add("main-content-drag-active");
            }
            System.out.println("✓ Drag detectado - Área iluminada");
        }
        event.consume();
    }

    @FXML
    private void handleDragExited(DragEvent event) {
        // Quitar glow
        if (mainContent != null) {
            mainContent.getStyleClass().remove("main-content-drag-active");
        }
        event.consume();
    }

    @FXML
    private void handleDragDropped(DragEvent event) {
        // Quitar glow
        if (mainContent != null) {
            mainContent.getStyleClass().remove("main-content-drag-active");
        }

        boolean success = false;
        if (event.getDragboard().hasFiles()) {
            List<Song> songs = new ArrayList<>();
            for (File file : event.getDragboard().getFiles()) {
                if (file.isDirectory()) {
                    processDirectory(file, songs);
                } else if (DragDropHandler.isAudioFile(file)) {
                    songs.add(DragDropHandler.createSongFromFile(file));
                }
            }

            if (!songs.isEmpty()) {
                for (Song song : songs) {
                    playlist.addSong(song);
                }
                updateFilteredList();
                updatePlaylistStats();

                if (playlist.size() == songs.size()) {
                    loadCurrentSong();
                }
                success = true;
                System.out.println("✓ " + songs.size() + " canciones agregadas");
            }
        }

        event.setDropCompleted(success);
        event.consume();
    }

    // ========== MÉTODOS AUXILIARES ==========

    private void loadCurrentSong() {
        Song current = playlist.getCurrentSong();
        if (current != null) {
            audioService.loadSong(current);
            updateNowPlayingUI(current);
        }
    }

    private void loadAndPlayCurrentSong() {
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
                    updatePlaylistStats();
                });
            });
        }
    }

    private void updateNowPlayingUI(Song song) {
        if (song != null) {
            if (songTitleLabel != null)
                songTitleLabel.setText(song.getTitle());
            if (artistLabel != null)
                artistLabel.setText(song.getArtist());
            if (nowPlayingInfo != null)
                nowPlayingInfo.setVisible(true);

            // Mostrar album art si existe
            if (nowPlayingAlbumArt != null) {
                Image albumArt = song.getAlbumArt();
                if (albumArt != null) {
                    nowPlayingAlbumArt.setImage(albumArt);
                    nowPlayingAlbumArt.setVisible(true);
                    if (nowPlayingPlaceholder != null) {
                        nowPlayingPlaceholder.setVisible(false);
                    }

                    // Aplicar clip circular
                    Circle clip = new Circle(25, 25, 25);
                    nowPlayingAlbumArt.setClip(clip);

                    // Iniciar animación de giro
                    startAlbumArtRotation();
                } else {
                    nowPlayingAlbumArt.setImage(null);
                    nowPlayingAlbumArt.setVisible(false);
                    if (nowPlayingPlaceholder != null) {
                        nowPlayingPlaceholder.setVisible(true);
                    }
                    stopAlbumArtRotation();
                }
            }
        } else {
            if (songTitleLabel != null)
                songTitleLabel.setText("Sin canción");
            if (artistLabel != null)
                artistLabel.setText("Selecciona algo");
            if (nowPlayingAlbumArt != null) {
                nowPlayingAlbumArt.setImage(null);
                nowPlayingAlbumArt.setVisible(false);
            }
            if (nowPlayingPlaceholder != null) {
                nowPlayingPlaceholder.setVisible(true);
            }
            stopAlbumArtRotation();
        }
    }

    private void startAlbumArtRotation() {
        if (albumArtRotation == null && nowPlayingCoverContainer != null) {
            albumArtRotation = new RotateTransition(Duration.seconds(8), nowPlayingCoverContainer);
            albumArtRotation.setByAngle(360);
            albumArtRotation.setCycleCount(Animation.INDEFINITE);
            albumArtRotation.setInterpolator(Interpolator.LINEAR);
        }
        if (albumArtRotation != null) {
            albumArtRotation.play();
        }
    }

    private void stopAlbumArtRotation() {
        if (albumArtRotation != null) {
            albumArtRotation.pause();
        }
    }

    private void updatePlaylistStats() {
        int count = playlist.size();
        if (songCountLabel != null) {
            songCountLabel.setText(count + (count == 1 ? " canción" : " canciones"));
        }

        // Calcular duración total
        double totalSeconds = playlist.getSongs().stream()
                .filter(s -> s.getDuration() != null)
                .mapToDouble(s -> s.getDuration().toSeconds())
                .sum();

        if (totalDurationLabel != null) {
            int mins = (int) (totalSeconds / 60);
            if (mins < 60) {
                totalDurationLabel.setText(mins + " min");
            } else {
                int hours = mins / 60;
                int remainMins = mins % 60;
                totalDurationLabel.setText(hours + " hr " + remainMins + " min");
            }
        }

        // Actualizar descripción
        if (playlistDescription != null) {
            if (count == 0) {
                playlistDescription.setText("Arrastra archivos de música para comenzar");
            } else {
                playlistDescription.setText("Tu colección de música personal");
            }
        }
    }

    private void updatePlayPauseIcon(boolean isPlaying) {
        if (playPauseIcon != null) {
            playPauseIcon.setIconLiteral(isPlaying ? "fas-pause" : "fas-play");
        }
        if (playAllIcon != null) {
            playAllIcon.setIconLiteral(isPlaying ? "fas-pause" : "fas-play");
        }
    }

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

    public Playlist getPlaylist() {
        return playlist;
    }

    // ========== NAVEGACIÓN DENTRO DE LA APLICACIÓN ==========

    // Almacenar referencia al contenido principal original
    private javafx.scene.Node originalCenterContent;
    private KeyboardService keyboardService;
    private NowPlayingController nowPlayingController;

    @FXML
    private void handlePlaylistsClick() {
        System.out.println("✓ Abriendo Gestor de Playlists...");
        try {
            // Guardar el contenido original si no lo hemos guardado
            if (originalCenterContent == null) {
                originalCenterContent = rootPane.getCenter();
            }

            // Cargar la vista del gestor de playlists
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/audioflow/views/playlist-manager-view.fxml"));
            Parent playlistManagerView = loader.load();

            // Obtener el controlador y configurar callbacks
            PlaylistManagerController controller = loader.getController();
            controller.setOnPlaySong(song -> {
                // Agregar canción a la playlist actual y reproducir
                if (!playlist.getSongs().contains(song)) {
                    playlist.addSong(song);
                }
                int index = playlist.getSongs().indexOf(song);
                playlist.goToIndex(index);
                loadAndPlayCurrentSong();
                // Volver a la vista principal
                showMainView();
            });
            controller.setOnPlayPlaylist(selectedPlaylist -> {
                // Agregar todas las canciones y reproducir
                for (Song song : selectedPlaylist.getSongs()) {
                    if (!playlist.getSongs().contains(song)) {
                        playlist.addSong(song);
                    }
                }
                updatePlaylistStats();
                if (!selectedPlaylist.isEmpty()) {
                    playlist.goToIndex(playlist.getSongs().indexOf(selectedPlaylist.getSongs().get(0)));
                    loadAndPlayCurrentSong();
                }
                // Volver a la vista principal
                showMainView();
            });

            // Reemplazar el contenido central
            rootPane.setCenter(playlistManagerView);

        } catch (IOException e) {
            System.err.println("Error abriendo Gestor de Playlists: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleOpenNowPlaying() {
        if (playlist.isEmpty()) {
            System.out.println("ℹ️ Agrega canciones primero");
            return;
        }

        System.out.println("✓ Abriendo vista Now Playing...");
        try {
            // Guardar el contenido original si no lo hemos guardado
            if (originalCenterContent == null) {
                originalCenterContent = rootPane.getCenter();
            }

            // Cargar la vista Now Playing
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/audioflow/views/now-playing-view.fxml"));
            Parent nowPlayingView = loader.load();

            // Obtener el controlador y configurar
            nowPlayingController = loader.getController();
            keyboardService = new KeyboardService();
            nowPlayingController.setup(audioService, keyboardService);
            nowPlayingController.updateSongInfo(playlist.getCurrentSong());

            // Callback para cerrar (volver a vista principal)
            nowPlayingController.setOnClose(this::showMainView);

            // Callbacks para navegación prev/next
            nowPlayingController.setOnPrevious(() -> {
                if (playlist.hasPrevious()) {
                    playlist.previous();
                    loadAndPlayCurrentSong();
                    nowPlayingController.updateSongInfo(playlist.getCurrentSong());
                }
            });

            nowPlayingController.setOnNext(() -> {
                if (playlist.hasNext()) {
                    playlist.next();
                    loadAndPlayCurrentSong();
                    nowPlayingController.updateSongInfo(playlist.getCurrentSong());
                }
            });

            // Reemplazar el contenido central
            rootPane.setCenter(nowPlayingView);

            // Configurar acciones de teclado en la escena actual
            Scene currentScene = rootPane.getScene();
            if (currentScene != null) {
                keyboardService.registerShortcuts(currentScene);
            }

            nowPlayingController.show();

        } catch (IOException e) {
            System.err.println("Error abriendo Now Playing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Vuelve a mostrar la vista principal
     */
    private void showMainView() {
        if (originalCenterContent != null) {
            rootPane.setCenter(originalCenterContent);
            System.out.println("✓ Volviendo a vista principal...");

            // Desregistrar atajos de teclado si estaban activos
            if (keyboardService != null) {
                keyboardService.unregisterShortcuts();
            }

            // Actualizar la lista de canciones
            songListView.refresh();
            updatePlaylistStats();
        }
    }
}
