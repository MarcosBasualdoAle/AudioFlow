package com.audioflow.component;

import com.audioflow.model.Rating;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Componente visual de calificación por estrellas (5 estrellas).
 * Soporta hover preview y click para confirmar rating.
 */
public class StarRating extends HBox {

    private static final int STAR_COUNT = 5;
    private static final String STAR_FILLED_ICON = "fas-star";
    private static final String STAR_EMPTY_ICON = "far-star";
    private static final String STAR_FILLED_COLOR = "#FFD700"; // Gold
    private static final String STAR_EMPTY_COLOR = "#727272"; // Gray
    private static final String STAR_HOVER_COLOR = "#FFA500"; // Orange
    private static final int ICON_SIZE = 20;

    private final FontIcon[] stars = new FontIcon[STAR_COUNT];
    private final IntegerProperty rating = new SimpleIntegerProperty(0);
    private int hoverRating = 0;
    private boolean isHovering = false;
    private boolean readOnly = false;

    public StarRating() {
        this(0);
    }

    public StarRating(int initialRating) {
        initializeComponent();
        setRating(initialRating);
    }

    private void initializeComponent() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(4);
        setCursor(Cursor.HAND);

        // Crear las 5 estrellas
        for (int i = 0; i < STAR_COUNT; i++) {
            FontIcon star = new FontIcon(STAR_EMPTY_ICON);
            star.setIconSize(ICON_SIZE);
            star.setIconColor(javafx.scene.paint.Color.web(STAR_EMPTY_COLOR));
            star.setMouseTransparent(true); // Hacer que las estrellas ignoren eventos de mouse
            stars[i] = star;
            getChildren().add(star);
        }

        // Eventos de mouse en el CONTENEDOR (no en cada estrella)
        setOnMouseMoved(e -> {
            if (!readOnly) {
                isHovering = true;
                // Calcular qué estrella basado en posición X
                double starWidth = getWidth() / STAR_COUNT;
                int starIndex = (int) Math.ceil(e.getX() / starWidth);
                hoverRating = Math.max(1, Math.min(STAR_COUNT, starIndex));
                updateStarsDisplay();
            }
        });

        setOnMouseExited(e -> {
            if (!readOnly) {
                isHovering = false;
                hoverRating = 0;
                updateStarsDisplay();
            }
        });

        setOnMouseClicked(e -> {
            if (!readOnly) {
                // Calcular qué estrella basado en posición X
                double starWidth = getWidth() / STAR_COUNT;
                int starIndex = (int) Math.ceil(e.getX() / starWidth);
                int clickedRating = Math.max(1, Math.min(STAR_COUNT, starIndex));

                // Si se hace clic en el rating actual, resetear a 0
                if (rating.get() == clickedRating) {
                    setRating(0);
                } else {
                    setRating(clickedRating);
                }
            }
        });
    }

    /**
     * Actualiza la visualización de las estrellas
     */
    private void updateStarsDisplay() {
        int displayRating = isHovering ? hoverRating : rating.get();

        for (int i = 0; i < STAR_COUNT; i++) {
            FontIcon star = stars[i];
            boolean filled = (i + 1) <= displayRating;

            star.setIconLiteral(filled ? STAR_FILLED_ICON : STAR_EMPTY_ICON);

            if (filled) {
                String color = isHovering ? STAR_HOVER_COLOR : STAR_FILLED_COLOR;
                star.setIconColor(javafx.scene.paint.Color.web(color));
            } else {
                star.setIconColor(javafx.scene.paint.Color.web(STAR_EMPTY_COLOR));
            }
        }
    }

    // ========== GETTERS & SETTERS ==========

    public int getRating() {
        return rating.get();
    }

    public void setRating(int value) {
        rating.set(Math.max(0, Math.min(STAR_COUNT, value)));
        updateStarsDisplay();
    }

    public IntegerProperty ratingProperty() {
        return rating;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        setCursor(readOnly ? Cursor.DEFAULT : Cursor.HAND);
    }

    /**
     * Aplica rating desde un modelo Rating
     */
    public void setFromModel(Rating ratingModel) {
        setRating(ratingModel.getValue());
    }

    /**
     * Obtiene el rating como modelo Rating
     */
    public Rating toModel() {
        return new Rating(rating.get());
    }

    /**
     * Configura el tamaño de los iconos
     */
    public void setIconSize(int size) {
        for (FontIcon star : stars) {
            star.setIconSize(size);
        }
    }

    /**
     * Configura el espaciado entre estrellas
     */
    public void setStarSpacing(double spacing) {
        setSpacing(spacing);
    }
}
