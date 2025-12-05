package com.audioflow.component;

import com.jfoenix.controls.JFXSlider;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Componente personalizado de control de volumen con slider vertical popup.
 * Muestra un slider que aparece con animación al hacer hover.
 * 
 * @author Brickzon
 */
public class VolumeSlider extends VBox {

    private final JFXSlider slider;
    private final Label volumeLabel;
    private final FontIcon volumeIcon;

    // Animaciones
    private FadeTransition fadeIn;
    private FadeTransition fadeOut;
    private TranslateTransition slideIn;
    private TranslateTransition slideOut;

    // Estado
    private boolean isVisible = false;
    private double savedVolume = 0.7;

    public VolumeSlider() {
        // Configuración del contenedor
        setAlignment(Pos.CENTER);
        setSpacing(5);
        getStyleClass().add("volume-slider-popup");

        // Slider vertical
        slider = new JFXSlider(0, 100, 70);
        slider.setOrientation(javafx.geometry.Orientation.VERTICAL);
        slider.setPrefHeight(100);
        slider.setPrefWidth(30);
        slider.getStyleClass().add("volume-slider-vertical");

        // Label para mostrar porcentaje
        volumeLabel = new Label("70%");
        volumeLabel.getStyleClass().add("volume-label");

        // Icono de volumen
        volumeIcon = new FontIcon("fas-volume-up");
        volumeIcon.setIconSize(16);
        volumeIcon.getStyleClass().add("volume-popup-icon");

        // Añadir elementos
        getChildren().addAll(volumeLabel, slider, volumeIcon);

        // Listener para actualizar label e icono
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateVolumeLabel(newVal.doubleValue());
            updateVolumeIcon(newVal.doubleValue());
        });

        // Inicializar animaciones
        setupAnimations();

        // Ocultar inicialmente
        setOpacity(0);
        setVisible(false);
    }

    /**
     * Configura las animaciones de entrada/salida
     */
    private void setupAnimations() {
        // Fade In
        fadeIn = new FadeTransition(Duration.millis(200), this);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        // Fade Out
        fadeOut = new FadeTransition(Duration.millis(200), this);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> setVisible(false));

        // Slide In (de abajo hacia arriba)
        slideIn = new TranslateTransition(Duration.millis(200), this);
        slideIn.setFromY(10);
        slideIn.setToY(0);

        // Slide Out
        slideOut = new TranslateTransition(Duration.millis(200), this);
        slideOut.setFromY(0);
        slideOut.setToY(10);
    }

    /**
     * Muestra el slider con animación
     */
    public void show() {
        if (!isVisible) {
            isVisible = true;
            setVisible(true);
            fadeIn.playFromStart();
            slideIn.playFromStart();
        }
    }

    /**
     * Oculta el slider con animación
     */
    public void hide() {
        if (isVisible) {
            isVisible = false;
            fadeOut.playFromStart();
            slideOut.playFromStart();
        }
    }

    /**
     * Obtiene el valor actual del slider (0-100)
     */
    public double getValue() {
        return slider.getValue();
    }

    /**
     * Establece el valor del slider (0-100)
     */
    public void setValue(double value) {
        slider.setValue(value);
    }

    /**
     * Obtiene la propiedad de valor para binding
     */
    public javafx.beans.property.DoubleProperty valueProperty() {
        return slider.valueProperty();
    }

    /**
     * Guarda el volumen actual (para atenuación durante seek)
     */
    public void saveVolume() {
        savedVolume = slider.getValue();
    }

    /**
     * Restaura el volumen guardado
     */
    public void restoreVolume() {
        slider.setValue(savedVolume);
    }

    /**
     * Reduce el volumen temporalmente (para seek bar)
     */
    public void attenuateVolume() {
        slider.setValue(savedVolume * 0.3); // Reduce al 30%
    }

    /**
     * Actualiza el label con el porcentaje
     */
    private void updateVolumeLabel(double value) {
        volumeLabel.setText(String.format("%.0f%%", value));
    }

    /**
     * Actualiza el icono según el nivel de volumen
     */
    private void updateVolumeIcon(double value) {
        if (value == 0) {
            volumeIcon.setIconLiteral("fas-volume-mute");
        } else if (value < 50) {
            volumeIcon.setIconLiteral("fas-volume-down");
        } else {
            volumeIcon.setIconLiteral("fas-volume-up");
        }
    }

    /**
     * Verifica si el popup está visible
     */
    public boolean isPopupVisible() {
        return isVisible;
    }
}
