# 🎵 AudioFlow - Reproductor de Música Desktop

Reproductor de música moderno con JavaFX, JFoenix (Material Design), e Ikonli (iconos).

---

## 📋 Requisitos para Ejecutar

| Requisito | Versión |
|-----------|---------|
| Java JDK | 17+ |
| Maven | 3.8+ |

```bash
git clone https://github.com/tu-usuario/AudioFlow.git
cd AudioFlow
mvn javafx:run
```

---

## 👥 División por Pantallas

| Desarrollador | Pantallas Asignadas |
|---------------|---------------------|
| **BRICKZON** | 1. Dashboard (Principal) + 4. Mini Player |
| **ANGEL** | 2. Now Playing (Inmersiva) + 3. Gestor de Listas |

> Cada desarrollador hace **TODO** de sus pantallas: UI (FXML/CSS) + Controlador + Servicios necesarios.

---

## 🏗️ Estructura de Archivos

```
src/main/java/com/audioflow/
├── App.java                              # [COMPARTIDO]
│
├── controller/
│   ├── MainController.java               # [BRICKZON] Dashboard
│   ├── MiniPlayerController.java         # [BRICKZON] Widget flotante
│   ├── NowPlayingController.java         # [ANGEL] Vista inmersiva
│   └── PlaylistManagerController.java    # [ANGEL] Gestor de listas
│
├── model/
│   ├── Song.java                         # [COMPARTIDO] Ya existe
│   ├── Playlist.java                     # [COMPARTIDO] Ya existe
│   └── Rating.java                       # [ANGEL] Sistema de estrellas
│
├── service/
│   ├── AudioService.java                 # [COMPARTIDO] Ya existe
│   ├── AudioAnalyzerService.java         # [ANGEL] Espectro de audio
│   ├── PlaylistService.java              # [ANGEL] Persistencia
│   └── KeyboardService.java              # [ANGEL] Atajos de teclado
│
├── component/
│   ├── VolumeSlider.java                 # [BRICKZON] Slider popup
│   ├── StarRating.java                   # [ANGEL] 5 estrellas
│   └── AudioVisualizer.java              # [ANGEL] Visualizador
│
└── util/
    ├── DragDropHandler.java              # [COMPARTIDO] Ya existe
    └── ValidationUtils.java              # [ANGEL] Validación

src/main/resources/com/audioflow/
├── views/
│   ├── main-view.fxml                    # [BRICKZON]
│   ├── mini-player-view.fxml             # [BRICKZON]
│   ├── now-playing-view.fxml             # [ANGEL]
│   └── playlist-manager-view.fxml        # [ANGEL]
│
└── styles/
    ├── application.css                   # [BRICKZON]
    ├── mini-player.css                   # [BRICKZON]
    └── now-playing.css                   # [ANGEL]
```

---

## 🔷 BRICKZON: Dashboard + Mini Player

### Pantalla 1: Dashboard (Principal)

**Archivos:**
- `controller/MainController.java` (modificar existente)
- `views/main-view.fxml` (modificar existente)
- `styles/application.css` (modificar existente)
- `component/VolumeSlider.java` (crear)

**Funcionalidades a implementar:**

| Funcionalidad | Evento Técnico |
|---------------|----------------|
| Drag & Drop externo (carpetas) | `OnDragOver`, `OnDragDropped` |
| Panel se ilumina al recibir archivos | Cambio de estilo en drag |
| Play/Pause con transición de icono | `OnAction` del botón |
| Seek Bar (arrastrar punto) | `OnMousePressed`, `OnMouseReleased` |
| Volumen baja al arrastrar, vuelve al soltar | Lógica en eventos del slider |
| Control de Volumen con hover | `OnMouseEntered`, `OnMouseExited` + animación |
| Slider vertical aparece con animación | `FadeTransition`, `TranslateTransition` |
| Búsqueda/Filtrado en tiempo real | `OnKeyReleased` en TextField |
| Efectos hover en lista de canciones | `OnMouseEntered`, `OnMouseExited` |
| Botón "Minimizar a Mini Player" | `OnAction` → abrir MiniPlayer |

---

### Pantalla 4: Mini Player (Widget)

**Archivos:**
- `controller/MiniPlayerController.java` (crear)
- `views/mini-player-view.fxml` (crear)
- `styles/mini-player.css` (crear)

**Funcionalidades a implementar:**

| Funcionalidad | Evento Técnico |
|---------------|----------------|
| Ventana siempre encima | `stage.setAlwaysOnTop(true)` |
| Solo botones: Play, Pause, Siguiente | Controles mínimos |
| Animación suave al aparecer | `FadeTransition` al abrir |
| Sincronizar estado con ventana principal | Compartir `AudioService` |
| Interceptar cierre de ventana | `OnCloseRequest` |
| Preguntar "¿Minimizar a bandeja?" | Diálogo de confirmación |

---

## 🔶 ANGEL: Now Playing + Gestor de Listas

### Pantalla 2: Now Playing (Vista Inmersiva)

**Archivos:**
- `controller/NowPlayingController.java` (crear)
- `views/now-playing-view.fxml` (crear)
- `styles/now-playing.css` (crear)
- `component/AudioVisualizer.java` (crear)
- `component/StarRating.java` (crear)
- `service/AudioAnalyzerService.java` (crear)
- `service/KeyboardService.java` (crear)
- `model/Rating.java` (crear)

**Funcionalidades a implementar:**

| Funcionalidad | Evento Técnico |
|---------------|----------------|
| Carátula grande centrada | Layout FXML |
| Visualizador de audio (barras/ondas) | `MediaPlayer.setAudioSpectrumListener()` |
| Barras se mueven al ritmo | Canvas + `AudioAnalyzerService` |
| Sistema de 5 estrellas | `OnMouseEntered` (preview), `OnMouseClicked` (guardar) |
| Estrellas se iluminan con hover | Cambio de estilo en hover |
| Transición desde Dashboard | `FadeTransition` al cambiar vista |
| Abrir al clic en carátula pequeña | `OnMouseClicked` en carátula del dashboard |
| Atajos de teclado | `OnKeyPressed` en escena |
| Flecha Derecha → Siguiente | `KeyboardService` |
| Flecha Izquierda → Anterior | `KeyboardService` |
| Espacio → Pausa | `KeyboardService` |
| Icono translúcido de feedback | Aparece 0.5s y desaparece |
| Evento cuando canción termina | `OnEndOfMedia` → decidir siguiente acción |
| Lógica Shuffle/Repeat | Botones de estado + lógica en `OnEndOfMedia` |

---

### Pantalla 3: Gestor de Listas (Modo Edición)

**Archivos:**
- `controller/PlaylistManagerController.java` (crear)
- `views/playlist-manager-view.fxml` (crear)
- `service/PlaylistService.java` (crear)
- `util/ValidationUtils.java` (crear)

**Funcionalidades a implementar:**

| Funcionalidad | Evento Técnico |
|---------------|----------------|
| Drag & Drop interno (reordenar) | `OnDragDetected`, `OnDragOver`, `OnDragDropped` |
| Canciones se deslizan para hacer hueco | Animación `TranslateTransition` |
| Botón "Crear Nueva Playlist" | `OnAction` → abrir diálogo |
| Diálogo con campo de nombre | FXML de diálogo modal |
| Validación: nombre vacío → borde rojo | Listener en TextField |
| Validación: caracteres inválidos (/ \ : * ?) | `ValidationUtils` |
| Botón Guardar deshabilitado si inválido | `disableProperty().bind()` |
| Menú contextual (clic derecho) | `OnContextMenuRequested` |
| Opciones: Eliminar, Ver info, Editar etiquetas | Items del ContextMenu |
| Persistir playlists en JSON | `PlaylistService` guarda en `%APPDATA%` |

---

## 🔄 Diagrama de Dependencias

```
              ┌──────────────────────────────────────────┐
              │         FASE 1 (YA COMPLETADA)           │
              │  App.java, Song, Playlist, AudioService  │
              │      MainController, DragDropHandler     │
              └─────────────────┬────────────────────────┘
                                │
         ┌──────────────────────┴──────────────────────┐
         │                                              │
         ▼                                              ▼
┌─────────────────────┐                    ┌─────────────────────┐
│     BRICKZON        │                    │       ANGEL         │
├─────────────────────┤                    ├─────────────────────┤
│ • Dashboard mejorado│                    │ • Now Playing       │
│   - VolumeSlider    │                    │   - AudioVisualizer │
│   - Búsqueda        │                    │   - StarRating      │
│   - Drag&Drop mejor │                    │   - AudioAnalyzer   │
│                     │                    │   - KeyboardService │
│ • Mini Player       │                    │                     │
│   - Always on top   │◄──────────────────►│ • Gestor de Listas  │
│   - Sincronización  │ (AudioService      │   - Drag&Drop int.  │
│   - OnCloseRequest  │  compartido)       │   - Validación      │
│                     │                    │   - PlaylistService │
└─────────────────────┘                    └─────────────────────┘
```

> **Única dependencia cruzada**: Ambos usan `AudioService` para controlar la reproducción.

---

## 🔀 Ramas de Git

```bash
main
├── develop
├── feature/brickzon-dashboard    # Dashboard + VolumeSlider + Búsqueda
├── feature/brickzon-miniplayer   # Mini Player completo
├── feature/angel-nowplaying      # Now Playing + Visualizador + Estrellas
└── feature/angel-playlist        # Gestor de Listas + Validación
```

---

## ✅ Checklist de Entrega

### BRICKZON
- [ ] Dashboard con Drag & Drop externo mejorado (iluminación)
- [ ] Control de volumen con slider popup animado
- [ ] Seek bar con volumen que baja al arrastrar
- [ ] Búsqueda/filtrado en tiempo real
- [ ] Botón minimizar a Mini Player
- [ ] Mini Player flotante (always on top)
- [ ] Interceptar cierre con diálogo

### ANGEL
- [ ] Now Playing con carátula grande
- [ ] Visualizador de audio que se mueve
- [ ] Sistema de 5 estrellas funcional
- [ ] Atajos de teclado (flechas + espacio)
- [ ] Feedback visual (icono translúcido)
- [ ] Gestor de listas con Drag & Drop interno
- [ ] Validación de formularios
- [ ] Menú contextual (clic derecho)
- [ ] Persistencia de playlists
- [ ] Lógica OnEndOfMedia con Shuffle/Repeat

---

## 🚀 Comandos

```bash
mvn javafx:run          # Ejecutar
mvn clean javafx:run    # Limpiar y ejecutar
mvn compile             # Solo compilar
```
