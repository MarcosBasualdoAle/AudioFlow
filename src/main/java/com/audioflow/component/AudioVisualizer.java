package com.audioflow.component;

import com.audioflow.service.AudioAnalyzerService;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

/**
 * Componente visual que muestra barras de espectro de audio animadas.
 * Se conecta con AudioAnalyzerService para obtener datos del espectro.
 */
public class AudioVisualizer extends Canvas {

    // Configuración visual
    private static final Color BAR_COLOR_LOW = Color.web("#1DB954"); // Verde (bajos)
    private static final Color BAR_COLOR_MID = Color.web("#1DB954"); // Verde (medios)
    private static final Color BAR_COLOR_HIGH = Color.web("#1ED760"); // Verde claro (agudos)
    private static final Color BACKGROUND_COLOR = Color.TRANSPARENT;

    private static final double BAR_GAP_RATIO = 0.3; // Espacio entre barras (30%)
    private static final double MIN_BAR_HEIGHT = 3; // Altura mínima de barra
    private static final double CORNER_RADIUS = 2; // Radio de esquinas

    private AudioAnalyzerService analyzerService;
    private AnimationTimer animationTimer;
    private boolean isRunning = false;

    // Configuración personalizable
    private int numBars = 32;
    private boolean useGradient = true;
    private boolean mirrorMode = false;
    private double decayRate = 0.85; // Velocidad de caída

    // Estado de las barras (para animación suave)
    private double[] displayHeights;

    public AudioVisualizer() {
        this(300, 100);
    }

    public AudioVisualizer(double width, double height) {
        super(width, height);
        this.displayHeights = new double[numBars];
        initializeTimer();
    }

    /**
     * Conecta el visualizador con el servicio de análisis
     */
    public void setAnalyzerService(AudioAnalyzerService service) {
        this.analyzerService = service;
    }

    /**
     * Inicia la animación
     */
    public void start() {
        if (!isRunning) {
            isRunning = true;
            animationTimer.start();
            System.out.println("✓ AudioVisualizer: Animación iniciada");
        }
    }

    /**
     * Detiene la animación
     */
    public void stop() {
        if (isRunning) {
            isRunning = false;
            animationTimer.stop();
            clearCanvas();
            System.out.println("✓ AudioVisualizer: Animación detenida");
        }
    }

    /**
     * Inicializa el timer de animación
     */
    private void initializeTimer() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                render();
            }
        };
    }

    /**
     * Renderiza el frame actual
     */
    private void render() {
        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();

        // Limpiar canvas
        gc.setFill(BACKGROUND_COLOR);
        gc.clearRect(0, 0, width, height);

        // Obtener datos del espectro
        float[] magnitudes;
        if (analyzerService != null) {
            magnitudes = analyzerService.getMagnitudes();
        } else {
            // Datos de demostración cuando no hay servicio conectado
            magnitudes = generateDemoData();
        }

        // Calcular dimensiones
        double totalBarWidth = width / numBars;
        double barWidth = totalBarWidth * (1 - BAR_GAP_RATIO);
        double gap = totalBarWidth * BAR_GAP_RATIO;

        // Dibujar barras
        for (int i = 0; i < numBars; i++) {
            // Obtener magnitud (con interpolación si hay diferente cantidad de bandas)
            float magnitude;
            if (magnitudes.length == numBars) {
                magnitude = magnitudes[i];
            } else {
                int sourceIndex = (int) ((float) i / numBars * magnitudes.length);
                magnitude = magnitudes[Math.min(sourceIndex, magnitudes.length - 1)];
            }

            // Calcular altura objetivo
            double targetHeight = Math.max(MIN_BAR_HEIGHT, magnitude * height);

            // Suavizar transición (decay)
            if (targetHeight > displayHeights[i]) {
                displayHeights[i] = targetHeight; // Subida inmediata
            } else {
                displayHeights[i] = displayHeights[i] * decayRate + targetHeight * (1 - decayRate);
            }

            double barHeight = displayHeights[i];
            double x = i * totalBarWidth + gap / 2;
            double y = height - barHeight;

            // Elegir color según posición (bajos, medios, agudos)
            if (useGradient) {
                LinearGradient gradient = new LinearGradient(
                        0, height, 0, 0, false, CycleMethod.NO_CYCLE,
                        new Stop(0, BAR_COLOR_LOW),
                        new Stop(0.5, BAR_COLOR_MID),
                        new Stop(1, BAR_COLOR_HIGH));
                gc.setFill(gradient);
            } else {
                double position = (double) i / numBars;
                if (position < 0.33) {
                    gc.setFill(BAR_COLOR_LOW);
                } else if (position < 0.66) {
                    gc.setFill(BAR_COLOR_MID);
                } else {
                    gc.setFill(BAR_COLOR_HIGH);
                }
            }

            // Dibujar barra con esquinas redondeadas
            gc.fillRoundRect(x, y, barWidth, barHeight, CORNER_RADIUS, CORNER_RADIUS);

            // Modo espejo (opcional)
            if (mirrorMode) {
                gc.fillRoundRect(x, 0, barWidth, barHeight, CORNER_RADIUS, CORNER_RADIUS);
            }
        }
    }

    /**
     * Genera datos de demostración para cuando no hay audio
     */
    private float[] generateDemoData() {
        float[] demo = new float[numBars];
        double time = System.currentTimeMillis() / 1000.0;

        for (int i = 0; i < numBars; i++) {
            // Onda sinusoidal suave
            demo[i] = (float) (0.3 + 0.2 * Math.sin(time * 2 + i * 0.3));
        }

        return demo;
    }

    /**
     * Limpia el canvas
     */
    private void clearCanvas() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        // Resetear alturas
        for (int i = 0; i < displayHeights.length; i++) {
            displayHeights[i] = 0;
        }
    }

    // ========== CONFIGURACIÓN ==========

    public void setNumBars(int numBars) {
        this.numBars = numBars;
        this.displayHeights = new double[numBars];
    }

    public void setUseGradient(boolean useGradient) {
        this.useGradient = useGradient;
    }

    public void setMirrorMode(boolean mirrorMode) {
        this.mirrorMode = mirrorMode;
    }

    public void setDecayRate(double rate) {
        this.decayRate = Math.max(0.5, Math.min(0.99, rate));
    }

    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double prefWidth(double height) {
        return getWidth();
    }

    @Override
    public double prefHeight(double width) {
        return getHeight();
    }

    /**
     * Redimensiona el canvas
     */
    public void resize(double width, double height) {
        setWidth(width);
        setHeight(height);
    }
}
