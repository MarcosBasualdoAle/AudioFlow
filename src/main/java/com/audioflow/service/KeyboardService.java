package com.audioflow.service;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Servicio para manejar atajos de teclado globales.
 * Registra listeners en la Scene para capturar teclas.
 */
public class KeyboardService {

    /**
     * Tipos de acciones de teclado disponibles
     */
    public enum KeyAction {
        PLAY_PAUSE, // Espacio
        NEXT_TRACK, // Flecha derecha
        PREVIOUS_TRACK, // Flecha izquierda
        VOLUME_UP, // Flecha arriba
        VOLUME_DOWN, // Flecha abajo
        MUTE, // M
        ESCAPE, // Escape
        SEARCH // Ctrl+F
    }

    private final Map<KeyAction, Runnable> actionHandlers = new HashMap<>();
    private Scene registeredScene;
    private Consumer<KeyEvent> keyEventHandler;

    public KeyboardService() {
        // Inicialización
    }

    /**
     * Registra los atajos de teclado en una Scene.
     * Debe llamarse después de que la Scene esté creada.
     */
    public void registerShortcuts(Scene scene) {
        if (scene == null) {
            System.err.println("KeyboardService: Scene es null");
            return;
        }

        // Si ya hay una scene registrada, remover el handler anterior
        if (registeredScene != null && keyEventHandler != null) {
            registeredScene.removeEventHandler(KeyEvent.KEY_PRESSED, keyEventHandler::accept);
        }

        registeredScene = scene;

        keyEventHandler = this::handleKeyPress;
        scene.addEventHandler(KeyEvent.KEY_PRESSED, keyEventHandler::accept);

        System.out.println("✓ KeyboardService: Atajos de teclado registrados");
    }

    /**
     * Desregistra los atajos de teclado
     */
    public void unregisterShortcuts() {
        if (registeredScene != null && keyEventHandler != null) {
            registeredScene.removeEventHandler(KeyEvent.KEY_PRESSED, keyEventHandler::accept);
            registeredScene = null;
            keyEventHandler = null;
            System.out.println("✓ KeyboardService: Atajos de teclado desregistrados");
        }
    }

    /**
     * Procesa un evento de teclado
     */
    private void handleKeyPress(KeyEvent event) {
        // Ignorar si el foco está en un campo de texto
        if (event.getTarget() instanceof javafx.scene.control.TextInputControl) {
            return;
        }

        KeyAction action = mapKeyToAction(event);

        if (action != null) {
            Runnable handler = actionHandlers.get(action);
            if (handler != null) {
                event.consume(); // Prevenir propagación
                handler.run();
            }
        }
    }

    /**
     * Mapea un evento de teclado a una acción
     */
    private KeyAction mapKeyToAction(KeyEvent event) {
        KeyCode code = event.getCode();
        boolean ctrl = event.isControlDown();

        // Ctrl+F para búsqueda
        if (ctrl && code == KeyCode.F) {
            return KeyAction.SEARCH;
        }

        // Teclas individuales
        return switch (code) {
            case SPACE -> KeyAction.PLAY_PAUSE;
            case RIGHT -> KeyAction.NEXT_TRACK;
            case LEFT -> KeyAction.PREVIOUS_TRACK;
            case UP -> KeyAction.VOLUME_UP;
            case DOWN -> KeyAction.VOLUME_DOWN;
            case M -> KeyAction.MUTE;
            case ESCAPE -> KeyAction.ESCAPE;
            default -> null;
        };
    }

    // ========== REGISTRO DE HANDLERS ==========

    /**
     * Registra un handler para una acción específica
     */
    public void setOnAction(KeyAction action, Runnable handler) {
        actionHandlers.put(action, handler);
    }

    /**
     * Remueve el handler de una acción
     */
    public void removeAction(KeyAction action) {
        actionHandlers.remove(action);
    }

    /**
     * Registra todos los handlers de una vez
     */
    public void setHandlers(
            Runnable onPlayPause,
            Runnable onNext,
            Runnable onPrevious,
            Runnable onEscape) {
        if (onPlayPause != null)
            actionHandlers.put(KeyAction.PLAY_PAUSE, onPlayPause);
        if (onNext != null)
            actionHandlers.put(KeyAction.NEXT_TRACK, onNext);
        if (onPrevious != null)
            actionHandlers.put(KeyAction.PREVIOUS_TRACK, onPrevious);
        if (onEscape != null)
            actionHandlers.put(KeyAction.ESCAPE, onEscape);
    }

    /**
     * Registra handlers para control de volumen
     */
    public void setVolumeHandlers(Runnable onVolumeUp, Runnable onVolumeDown, Runnable onMute) {
        if (onVolumeUp != null)
            actionHandlers.put(KeyAction.VOLUME_UP, onVolumeUp);
        if (onVolumeDown != null)
            actionHandlers.put(KeyAction.VOLUME_DOWN, onVolumeDown);
        if (onMute != null)
            actionHandlers.put(KeyAction.MUTE, onMute);
    }

    /**
     * Limpia todos los handlers
     */
    public void clearAllHandlers() {
        actionHandlers.clear();
    }
}
