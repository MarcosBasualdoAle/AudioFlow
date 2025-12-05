package com.audioflow.service;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.MediaPlayer;

/**
 * Servicio que analiza el espectro de audio para visualización.
 * Implementa AudioSpectrumListener para recibir datos del MediaPlayer.
 */
public class AudioAnalyzerService implements AudioSpectrumListener {

    // Número de bandas para el visualizador
    public static final int NUM_BANDS = 32;

    // Umbral mínimo para considerar una señal (en dB)
    public static final float THRESHOLD = -60.0f;

    // Factor de suavizado para transiciones (0.0 - 1.0)
    private static final float SMOOTHING_FACTOR = 0.3f;

    // Magnitudes normalizadas (0.0 - 1.0) para cada banda
    private final float[] magnitudes;
    private final float[] smoothedMagnitudes;

    // Propiedad observable para notificar cambios
    private final ObjectProperty<float[]> spectrumData;

    public AudioAnalyzerService() {
        this.magnitudes = new float[NUM_BANDS];
        this.smoothedMagnitudes = new float[NUM_BANDS];
        this.spectrumData = new SimpleObjectProperty<>(new float[NUM_BANDS]);
    }

    /**
     * Configura el MediaPlayer para análisis de espectro
     */
    public void attachToPlayer(MediaPlayer player) {
        if (player == null)
            return;

        player.setAudioSpectrumNumBands(NUM_BANDS);
        player.setAudioSpectrumInterval(0.05); // 50ms = 20 fps
        player.setAudioSpectrumThreshold((int) THRESHOLD);
        player.setAudioSpectrumListener(this);

        System.out.println("✓ AudioAnalyzerService: Conectado al MediaPlayer");
    }

    /**
     * Desconecta del MediaPlayer
     */
    public void detachFromPlayer(MediaPlayer player) {
        if (player != null) {
            player.setAudioSpectrumListener(null);
        }
    }

    /**
     * Callback del AudioSpectrumListener - llamado automáticamente por JavaFX
     */
    @Override
    public void spectrumDataUpdate(double timestamp, double duration,
            float[] rawMagnitudes, float[] phases) {

        // Procesar magnitudes
        int bands = Math.min(rawMagnitudes.length, NUM_BANDS);

        for (int i = 0; i < bands; i++) {
            // Normalizar: convertir de dB (negativo) a 0.0-1.0
            // -60dB -> 0.0, 0dB -> 1.0
            float normalized = (rawMagnitudes[i] - THRESHOLD) / (-THRESHOLD);
            normalized = Math.max(0.0f, Math.min(1.0f, normalized));

            magnitudes[i] = normalized;

            // Suavizado para animación más fluida
            smoothedMagnitudes[i] = smoothedMagnitudes[i] * (1 - SMOOTHING_FACTOR)
                    + normalized * SMOOTHING_FACTOR;
        }

        // Actualizar propiedad observable
        spectrumData.set(smoothedMagnitudes.clone());
    }

    // ========== GETTERS ==========

    /**
     * Obtiene las magnitudes suavizadas actuales
     */
    public float[] getMagnitudes() {
        return smoothedMagnitudes.clone();
    }

    /**
     * Obtiene la magnitud de una banda específica (0.0 - 1.0)
     */
    public float getMagnitude(int band) {
        if (band >= 0 && band < NUM_BANDS) {
            return smoothedMagnitudes[band];
        }
        return 0.0f;
    }

    /**
     * Propiedad observable del espectro
     */
    public ObjectProperty<float[]> spectrumDataProperty() {
        return spectrumData;
    }

    /**
     * Obtiene el promedio de todas las bandas (útil para efectos globales)
     */
    public float getAverageLevel() {
        float sum = 0;
        for (float mag : smoothedMagnitudes) {
            sum += mag;
        }
        return sum / NUM_BANDS;
    }

    /**
     * Obtiene el nivel de bajos (primeras 1/3 de las bandas)
     */
    public float getBassLevel() {
        float sum = 0;
        int bassCount = NUM_BANDS / 3;
        for (int i = 0; i < bassCount; i++) {
            sum += smoothedMagnitudes[i];
        }
        return sum / bassCount;
    }

    /**
     * Obtiene el nivel de medios (1/3 central de las bandas)
     */
    public float getMidLevel() {
        float sum = 0;
        int start = NUM_BANDS / 3;
        int end = 2 * NUM_BANDS / 3;
        for (int i = start; i < end; i++) {
            sum += smoothedMagnitudes[i];
        }
        return sum / (end - start);
    }

    /**
     * Obtiene el nivel de agudos (últimas 1/3 de las bandas)
     */
    public float getTrebleLevel() {
        float sum = 0;
        int start = 2 * NUM_BANDS / 3;
        for (int i = start; i < NUM_BANDS; i++) {
            sum += smoothedMagnitudes[i];
        }
        return sum / (NUM_BANDS - start);
    }

    /**
     * Resetea todas las magnitudes a cero
     */
    public void reset() {
        for (int i = 0; i < NUM_BANDS; i++) {
            magnitudes[i] = 0;
            smoothedMagnitudes[i] = 0;
        }
        spectrumData.set(new float[NUM_BANDS]);
    }
}
