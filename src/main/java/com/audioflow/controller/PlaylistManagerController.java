package com.audioflow.controller;

import com.audioflow.model.Playlist;
import com.audioflow.model.Song;
import com.audioflow.service.PlaylistService;
import com.audioflow.util.DragDropHandler;
import com.audioflow.util.ValidationUtils;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controlador para el Gestor de Playlists.
 * Permite crear, editar y gestionar playlists con drag & drop.
 */
public class PlaylistManagerController implements Initializable {

    // ========== ELEMENTOS DE UI ==========
    @FXML
    private VBox rootPane;
    @FXML
    private JFXListView<Playlist> playlistListView;
    @FXML
    private JFXListView<Song> songsListView;
    @FXML
    private VBox dropZone;
    @FXML
    private Label selectedPlaylistName;
    @FXML
    private Label songCountLabel;
    @FXML
    private JFXButton playAllBtn;
    @FXML
    private JFXButton newPlaylistBtn;
    @FXML
    private StackPane dialogOverlay;
    @FXML
    private JFXTextField playlistNameField;
    @FXML
    private JFXButton createBtn;
    @FXML
    private Label errorLabel;

    // ========== SERVICIOS Y DATOS ==========
    private final PlaylistService playlistService = new PlaylistService();
    private final ObservableList<Playlist> playlists = FXCollections.observableArrayList();
    private Playlist selectedPlaylist;

    // Callbacks
    private Consumer<Song> onPlaySong;
    private Consumer<Playlist> onPlayPlaylist;

    // Drag & Drop interno
    private int draggedIndex = -1;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("✓ Inicializando PlaylistManagerController...");

        setupPlaylistListView();
        setupSongsListView();
        setupDropZone();
        setupDialog();
        setupContextMenus();

        loadPlaylists();
    }

    // ========== CONFIGURACIÓN ==========

    private void setupPlaylistListView() {
        playlistListView.setItems(playlists);

        // Personalizar celdas
        playlistListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Playlist playlist, boolean empty) {
                super.updateItem(playlist, empty);
                if (empty || playlist == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(playlist.getName() + " (" + playlist.size() + ")");
                }
            }
        });

        // Selección
        playlistListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> selectPlaylist(newVal));
    }

    private void setupSongsListView() {
        // Personalizar celdas con soporte drag
        songsListView.setCellFactory(lv -> {
            ListCell<Song> cell = new ListCell<>() {
                @Override
                protected void updateItem(Song song, boolean empty) {
                    super.updateItem(song, empty);
                    if (empty || song == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(song.getTitle() + " - " + song.getArtist());
                    }
                }
            };

            // Drag & Drop interno para reordenar
            cell.setOnDragDetected(event -> {
                if (cell.getItem() != null) {
                    draggedIndex = cell.getIndex();
                    Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(String.valueOf(cell.getIndex()));
                    db.setContent(content);
                    event.consume();
                }
            });

            cell.setOnDragOver(event -> {
                if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
                event.consume();
            });

            cell.setOnDragEntered(event -> {
                if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                    cell.setStyle("-fx-background-color: #333333;");
                }
            });

            cell.setOnDragExited(event -> {
                cell.setStyle("");
            });

            cell.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;

                if (db.hasString() && selectedPlaylist != null) {
                    int dropIndex = cell.getIndex();
                    if (dropIndex >= 0 && draggedIndex >= 0 && dropIndex != draggedIndex) {
                        // Reordenar
                        Song draggedSong = selectedPlaylist.getSongs().remove(draggedIndex);
                        selectedPlaylist.getSongs().add(dropIndex, draggedSong);
                        saveCurrentPlaylist();
                        success = true;
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            });

            cell.setOnDragDone(event -> {
                draggedIndex = -1;
                event.consume();
            });

            // Doble clic para reproducir
            cell.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && cell.getItem() != null && onPlaySong != null) {
                    onPlaySong.accept(cell.getItem());
                }
            });

            return cell;
        });
    }

    private void setupDropZone() {
        if (dropZone == null)
            return;

        // Configurar zona de drop para archivos externos
        dropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragEntered(event -> {
            if (event.getDragboard().hasFiles()) {
                dropZone.setStyle("-fx-background-color: rgba(29, 185, 84, 0.2); -fx-border-color: #1DB954;");
            }
        });

        dropZone.setOnDragExited(event -> {
            dropZone.setStyle("");
        });

        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasFiles() && selectedPlaylist != null) {
                java.util.List<Song> songs = DragDropHandler.processFiles(db.getFiles());
                for (Song song : songs) {
                    selectedPlaylist.addSong(song);
                }
                updateSongsList();
                saveCurrentPlaylist();
                success = true;
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void setupDialog() {
        // Validación en tiempo real del nombre
        if (playlistNameField != null) {
            playlistNameField.textProperty().addListener((obs, oldVal, newVal) -> {
                validatePlaylistName(newVal);
            });
        }

        // Cerrar diálogo al hacer clic fuera
        if (dialogOverlay != null) {
            dialogOverlay.setOnMouseClicked(event -> {
                if (event.getTarget() == dialogOverlay) {
                    handleCancelDialog();
                }
            });
        }
    }

    private void setupContextMenus() {
        // Menú contextual para playlists
        ContextMenu playlistContextMenu = new ContextMenu();
        MenuItem renameItem = new MenuItem("Renombrar");
        renameItem.setOnAction(e -> handleRenamePlaylist());
        MenuItem deleteItem = new MenuItem("Eliminar");
        deleteItem.setOnAction(e -> handleDeletePlaylist());
        playlistContextMenu.getItems().addAll(renameItem, deleteItem);
        playlistListView.setContextMenu(playlistContextMenu);

        // Menú contextual para canciones
        ContextMenu songContextMenu = new ContextMenu();
        MenuItem playItem = new MenuItem("Reproducir");
        playItem.setOnAction(e -> {
            Song selected = songsListView.getSelectionModel().getSelectedItem();
            if (selected != null && onPlaySong != null) {
                onPlaySong.accept(selected);
            }
        });
        MenuItem removeItem = new MenuItem("Quitar de playlist");
        removeItem.setOnAction(e -> handleRemoveSong());
        MenuItem infoItem = new MenuItem("Ver información");
        infoItem.setOnAction(e -> handleShowSongInfo());
        songContextMenu.getItems().addAll(playItem, new SeparatorMenuItem(), removeItem, infoItem);
        songsListView.setContextMenu(songContextMenu);
    }

    // ========== ACCIONES ==========

    @FXML
    private void handleNewPlaylist() {
        showDialog();
    }

    @FXML
    private void handleCreatePlaylist() {
        String name = playlistNameField.getText().trim();

        if (!validatePlaylistName(name)) {
            return;
        }

        // Verificar si ya existe
        if (playlistService.playlistExists(name)) {
            showError("Ya existe una playlist con ese nombre");
            return;
        }

        // Crear y guardar
        Playlist newPlaylist = new Playlist(name);
        playlists.add(newPlaylist);

        try {
            playlistService.savePlaylist(newPlaylist);
            hideDialog();
            playlistListView.getSelectionModel().select(newPlaylist);
        } catch (Exception e) {
            showError("Error al guardar: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancelDialog() {
        hideDialog();
    }

    @FXML
    private void handlePlayAll() {
        if (selectedPlaylist != null && onPlayPlaylist != null) {
            onPlayPlaylist.accept(selectedPlaylist);
        }
    }

    private void handleRenamePlaylist() {
        Playlist selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("Renombrar Playlist");
        dialog.setHeaderText(null);
        dialog.setContentText("Nuevo nombre:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (ValidationUtils.isValidPlaylistName(name)) {
                selected.setName(name);
                playlistListView.refresh();
                saveCurrentPlaylist();
            }
        });
    }

    private void handleDeletePlaylist() {
        Playlist selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Eliminar Playlist");
        alert.setHeaderText(null);
        alert.setContentText("¿Eliminar \"" + selected.getName() + "\"?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                playlistService.deletePlaylist(selected.getName());
                playlists.remove(selected);
                selectedPlaylist = null;
                updatePlaylistInfo();
            } catch (Exception e) {
                System.err.println("Error al eliminar: " + e.getMessage());
            }
        }
    }

    private void handleRemoveSong() {
        Song selected = songsListView.getSelectionModel().getSelectedItem();
        if (selected != null && selectedPlaylist != null) {
            selectedPlaylist.removeSong(selected);
            updateSongsList();
            saveCurrentPlaylist();
        }
    }

    private void handleShowSongInfo() {
        Song selected = songsListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Información de la canción");
            alert.setHeaderText(selected.getTitle());
            alert.setContentText(
                    "Artista: " + selected.getArtist() + "\n" +
                            "Álbum: " + selected.getAlbum() + "\n" +
                            "Duración: " + selected.getFormattedDuration() + "\n" +
                            "Archivo: " + selected.getFilePath());
            alert.showAndWait();
        }
    }

    // ========== MÉTODOS AUXILIARES ==========

    private void loadPlaylists() {
        playlists.clear();
        playlists.addAll(playlistService.loadPlaylists());

        if (!playlists.isEmpty()) {
            playlistListView.getSelectionModel().selectFirst();
        }
    }

    private void selectPlaylist(Playlist playlist) {
        selectedPlaylist = playlist;
        updatePlaylistInfo();
        updateSongsList();
    }

    private void updatePlaylistInfo() {
        if (selectedPlaylist != null) {
            selectedPlaylistName.setText(selectedPlaylist.getName());
            songCountLabel.setText(selectedPlaylist.size() + " canciones");
            playAllBtn.setDisable(selectedPlaylist.isEmpty());
        } else {
            selectedPlaylistName.setText("Selecciona una playlist");
            songCountLabel.setText("0 canciones");
            playAllBtn.setDisable(true);
        }
    }

    private void updateSongsList() {
        if (selectedPlaylist != null) {
            songsListView.setItems(selectedPlaylist.getSongs());
        } else {
            songsListView.setItems(FXCollections.emptyObservableList());
        }
    }

    private void saveCurrentPlaylist() {
        if (selectedPlaylist != null) {
            try {
                playlistService.savePlaylist(selectedPlaylist);
            } catch (Exception e) {
                System.err.println("Error guardando playlist: " + e.getMessage());
            }
        }
    }

    private void showDialog() {
        playlistNameField.clear();
        errorLabel.setVisible(false);
        dialogOverlay.setVisible(true);
        dialogOverlay.setManaged(true);
        Platform.runLater(() -> playlistNameField.requestFocus());
    }

    private void hideDialog() {
        dialogOverlay.setVisible(false);
        dialogOverlay.setManaged(false);
        playlistNameField.clear();
        errorLabel.setVisible(false);
    }

    private boolean validatePlaylistName(String name) {
        Optional<String> error = ValidationUtils.validatePlaylistName(name);

        if (error.isPresent()) {
            showError(error.get());
            createBtn.setDisable(true);
            return false;
        } else {
            errorLabel.setVisible(false);
            createBtn.setDisable(false);
            return true;
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        playlistNameField.setStyle("-fx-border-color: #ff4444;");
    }

    // ========== SETTERS PARA CALLBACKS ==========

    public void setOnPlaySong(Consumer<Song> callback) {
        this.onPlaySong = callback;
    }

    public void setOnPlayPlaylist(Consumer<Playlist> callback) {
        this.onPlayPlaylist = callback;
    }
}
