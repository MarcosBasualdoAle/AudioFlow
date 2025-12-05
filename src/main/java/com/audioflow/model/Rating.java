package com.audioflow.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Modelo que representa una calificación de estrellas (0-5).
 * Usa propiedades JavaFX para binding reactivo con la UI.
 */
public class Rating {

    public static final int MIN_RATING = 0;
    public static final int MAX_RATING = 5;

    private final IntegerProperty value;

    /**
     * Constructor con valor inicial
     */
    public Rating(int initialValue) {
        this.value = new SimpleIntegerProperty(clamp(initialValue));
    }

    /**
     * Constructor por defecto (sin rating)
     */
    public Rating() {
        this(0);
    }

    // ========== GETTERS & SETTERS ==========

    public int getValue() {
        return value.get();
    }

    public void setValue(int newValue) {
        value.set(clamp(newValue));
    }

    public IntegerProperty valueProperty() {
        return value;
    }

    // ========== MÉTODOS ÚTILES ==========

    /**
     * Verifica si tiene calificación (> 0)
     */
    public boolean hasRating() {
        return value.get() > 0;
    }

    /**
     * Obtiene cantidad de estrellas llenas
     */
    public int getFilledStars() {
        return value.get();
    }

    /**
     * Obtiene cantidad de estrellas vacías
     */
    public int getEmptyStars() {
        return MAX_RATING - value.get();
    }

    /**
     * Resetea el rating a 0
     */
    public void clear() {
        value.set(0);
    }

    /**
     * Mantiene el valor dentro del rango válido
     */
    private int clamp(int val) {
        return Math.max(MIN_RATING, Math.min(MAX_RATING, val));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < getFilledStars(); i++)
            sb.append("★");
        for (int i = 0; i < getEmptyStars(); i++)
            sb.append("☆");
        return sb.toString();
    }
}
