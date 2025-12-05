package com.audioflow.util;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Utilidades para validación de datos en la aplicación.
 * Incluye validación de nombres de playlist y otros campos de texto.
 */
public final class ValidationUtils {

    // Caracteres no permitidos en nombres de archivo/playlist
    private static final Pattern INVALID_CHARS = Pattern.compile("[/\\\\:*?\"<>|]");

    // Longitud máxima para nombres de playlist
    private static final int MAX_PLAYLIST_NAME_LENGTH = 100;

    private ValidationUtils() {
        // Clase de utilidad - no instanciar
    }

    // ========== VALIDACIÓN DE PLAYLIST ==========

    /**
     * Valida un nombre de playlist.
     * 
     * @param name El nombre a validar
     * @return Optional vacío si es válido, o mensaje de error si no lo es
     */
    public static Optional<String> validatePlaylistName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.of("El nombre no puede estar vacío");
        }

        String trimmed = name.trim();

        if (trimmed.length() > MAX_PLAYLIST_NAME_LENGTH) {
            return Optional.of("El nombre no puede exceder " + MAX_PLAYLIST_NAME_LENGTH + " caracteres");
        }

        if (INVALID_CHARS.matcher(trimmed).find()) {
            return Optional.of("El nombre contiene caracteres no permitidos: / \\ : * ? \" < > |");
        }

        return Optional.empty(); // Válido
    }

    /**
     * Verifica si un nombre de playlist es válido
     */
    public static boolean isValidPlaylistName(String name) {
        return validatePlaylistName(name).isEmpty();
    }

    // ========== VALIDACIONES GENERALES ==========

    /**
     * Verifica si una cadena no está vacía (después de trim)
     */
    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * Verifica si una cadena está vacía o es null
     */
    public static boolean isEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    /**
     * Sanitiza un nombre de archivo removiendo caracteres inválidos
     */
    public static String sanitizeFileName(String name) {
        if (name == null)
            return "untitled";
        return INVALID_CHARS.matcher(name.trim()).replaceAll("_");
    }

    /**
     * Valida que un número esté en un rango
     */
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * Limita un valor a un rango (clamp)
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Limita un valor double a un rango (clamp)
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
