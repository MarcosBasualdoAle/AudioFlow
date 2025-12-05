package com.audioflow.service;

import com.audioflow.model.Playlist;
import com.audioflow.model.Song;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Servicio para persistencia de playlists en formato JSON.
 * Guarda/carga playlists desde %APPDATA%/AudioFlow/
 */
public class PlaylistService {

    private static final String APP_FOLDER = "AudioFlow";
    private static final String PLAYLISTS_FILE = "playlists.json";

    private final Path storagePath;

    public PlaylistService() {
        // Obtener directorio de datos de la aplicación
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isEmpty()) {
            appData = System.getProperty("user.home");
        }

        this.storagePath = Paths.get(appData, APP_FOLDER);
        ensureDirectoryExists();
    }

    /**
     * Constructor para testing con path personalizado
     */
    public PlaylistService(Path customPath) {
        this.storagePath = customPath;
        ensureDirectoryExists();
    }

    // ========== OPERACIONES CRUD ==========

    /**
     * Guarda una playlist
     */
    public void savePlaylist(Playlist playlist) throws IOException {
        List<Map<String, Object>> allPlaylists = loadAllPlaylistsRaw();

        // Buscar si ya existe y actualizar, o agregar nueva
        boolean found = false;
        for (Map<String, Object> p : allPlaylists) {
            if (playlist.getName().equals(p.get("name"))) {
                p.put("songs", songsToList(playlist));
                found = true;
                break;
            }
        }

        if (!found) {
            Map<String, Object> newPlaylist = new LinkedHashMap<>();
            newPlaylist.put("name", playlist.getName());
            newPlaylist.put("songs", songsToList(playlist));
            allPlaylists.add(newPlaylist);
        }

        writePlaylistsFile(allPlaylists);
        System.out.println("✓ Playlist guardada: " + playlist.getName());
    }

    /**
     * Carga todas las playlists guardadas
     */
    public List<Playlist> loadPlaylists() {
        List<Playlist> result = new ArrayList<>();

        try {
            List<Map<String, Object>> raw = loadAllPlaylistsRaw();

            for (Map<String, Object> p : raw) {
                String name = (String) p.get("name");
                Playlist playlist = new Playlist(name);

                @SuppressWarnings("unchecked")
                List<Map<String, String>> songs = (List<Map<String, String>>) p.get("songs");
                if (songs != null) {
                    for (Map<String, String> songData : songs) {
                        Song song = new Song(
                                songData.getOrDefault("title", "Unknown"),
                                songData.getOrDefault("artist", "Unknown"),
                                songData.getOrDefault("album", "Unknown"),
                                javafx.util.Duration.ZERO,
                                songData.getOrDefault("filePath", ""));
                        playlist.addSong(song);
                    }
                }

                result.add(playlist);
            }
        } catch (Exception e) {
            System.err.println("Error cargando playlists: " + e.getMessage());
        }

        return result;
    }

    /**
     * Elimina una playlist por nombre
     */
    public boolean deletePlaylist(String name) throws IOException {
        List<Map<String, Object>> allPlaylists = loadAllPlaylistsRaw();
        boolean removed = allPlaylists.removeIf(p -> name.equals(p.get("name")));

        if (removed) {
            writePlaylistsFile(allPlaylists);
            System.out.println("✓ Playlist eliminada: " + name);
        }

        return removed;
    }

    /**
     * Verifica si existe una playlist con el nombre dado
     */
    public boolean playlistExists(String name) {
        return loadPlaylists().stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(name));
    }

    // ========== MÉTODOS AUXILIARES ==========

    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(storagePath);
        } catch (IOException e) {
            System.err.println("Error creando directorio: " + e.getMessage());
        }
    }

    private Path getPlaylistsFilePath() {
        return storagePath.resolve(PLAYLISTS_FILE);
    }

    private List<Map<String, Object>> loadAllPlaylistsRaw() {
        Path file = getPlaylistsFilePath();

        if (!Files.exists(file)) {
            return new ArrayList<>();
        }

        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            return parseJsonArray(content);
        } catch (IOException e) {
            System.err.println("Error leyendo archivo: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void writePlaylistsFile(List<Map<String, Object>> playlists) throws IOException {
        String json = toJsonArray(playlists);
        Files.writeString(getPlaylistsFilePath(), json, StandardCharsets.UTF_8);
    }

    private List<Map<String, String>> songsToList(Playlist playlist) {
        List<Map<String, String>> songs = new ArrayList<>();

        for (Song song : playlist.getSongs()) {
            Map<String, String> songData = new LinkedHashMap<>();
            songData.put("title", song.getTitle());
            songData.put("artist", song.getArtist());
            songData.put("album", song.getAlbum());
            songData.put("filePath", song.getFilePath());
            songs.add(songData);
        }

        return songs;
    }

    // ========== JSON SIMPLE (sin dependencias externas) ==========

    private String toJsonArray(List<Map<String, Object>> list) {
        StringBuilder sb = new StringBuilder("[\n");

        for (int i = 0; i < list.size(); i++) {
            sb.append("  ").append(toJsonObject(list.get(i)));
            if (i < list.size() - 1)
                sb.append(",");
            sb.append("\n");
        }

        sb.append("]");
        return sb.toString();
    }

    private String toJsonObject(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        int count = 0;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (count > 0)
                sb.append(", ");
            sb.append("\"").append(escapeJson(entry.getKey())).append("\": ");
            sb.append(valueToJson(entry.getValue()));
            count++;
        }

        sb.append("}");
        return sb.toString();
    }

    private String valueToJson(Object value) {
        if (value == null)
            return "null";
        if (value instanceof String)
            return "\"" + escapeJson((String) value) + "\"";
        if (value instanceof Number)
            return value.toString();
        if (value instanceof Boolean)
            return value.toString();
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append(valueToJson(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return toJsonObject(map);
        }
        return "\"" + escapeJson(value.toString()) + "\"";
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJsonArray(String json) {
        // Parser JSON simple para arrays de objetos
        List<Map<String, Object>> result = new ArrayList<>();
        json = json.trim();

        if (!json.startsWith("[") || !json.endsWith("]")) {
            return result;
        }

        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty())
            return result;

        // Dividir por objetos (simplificado)
        int depth = 0;
        int start = 0;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{')
                depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    String objStr = json.substring(start, i + 1).trim();
                    result.add(parseJsonObject(objStr));
                    start = i + 1;
                    // Skip comma
                    while (start < json.length()
                            && (json.charAt(start) == ',' || Character.isWhitespace(json.charAt(start)))) {
                        start++;
                    }
                    i = start - 1;
                }
            }
        }

        return result;
    }

    private Map<String, Object> parseJsonObject(String json) {
        Map<String, Object> result = new LinkedHashMap<>();
        json = json.trim();

        if (!json.startsWith("{") || !json.endsWith("}")) {
            return result;
        }

        json = json.substring(1, json.length() - 1).trim();

        // Simple key-value parsing
        int i = 0;
        while (i < json.length()) {
            // Find key
            int keyStart = json.indexOf('"', i);
            if (keyStart == -1)
                break;
            int keyEnd = json.indexOf('"', keyStart + 1);
            String key = json.substring(keyStart + 1, keyEnd);

            // Find colon
            int colonPos = json.indexOf(':', keyEnd);
            i = colonPos + 1;

            // Skip whitespace
            while (i < json.length() && Character.isWhitespace(json.charAt(i)))
                i++;

            // Parse value
            Object value;
            if (json.charAt(i) == '"') {
                int valueEnd = findStringEnd(json, i);
                value = unescapeJson(json.substring(i + 1, valueEnd));
                i = valueEnd + 1;
            } else if (json.charAt(i) == '[') {
                int arrayEnd = findArrayEnd(json, i);
                value = parseJsonArraySimple(json.substring(i, arrayEnd + 1));
                i = arrayEnd + 1;
            } else if (json.charAt(i) == '{') {
                int objEnd = findObjectEnd(json, i);
                value = parseJsonObject(json.substring(i, objEnd + 1));
                i = objEnd + 1;
            } else {
                int valueEnd = i;
                while (valueEnd < json.length() && json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') {
                    valueEnd++;
                }
                value = json.substring(i, valueEnd).trim();
                i = valueEnd;
            }

            result.put(key, value);

            // Skip comma
            while (i < json.length() && (json.charAt(i) == ',' || Character.isWhitespace(json.charAt(i))))
                i++;
        }

        return result;
    }

    private List<Object> parseJsonArraySimple(String json) {
        List<Object> result = new ArrayList<>();
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]"))
            return result;
        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty())
            return result;

        int i = 0;
        while (i < json.length()) {
            while (i < json.length() && Character.isWhitespace(json.charAt(i)))
                i++;
            if (i >= json.length())
                break;

            if (json.charAt(i) == '{') {
                int end = findObjectEnd(json, i);
                result.add(parseJsonObject(json.substring(i, end + 1)));
                i = end + 1;
            } else if (json.charAt(i) == '"') {
                int end = findStringEnd(json, i);
                result.add(unescapeJson(json.substring(i + 1, end)));
                i = end + 1;
            }

            while (i < json.length() && (json.charAt(i) == ',' || Character.isWhitespace(json.charAt(i))))
                i++;
        }

        return result;
    }

    private int findStringEnd(String json, int start) {
        int i = start + 1;
        while (i < json.length()) {
            if (json.charAt(i) == '"' && json.charAt(i - 1) != '\\')
                return i;
            i++;
        }
        return json.length() - 1;
    }

    private int findObjectEnd(String json, int start) {
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == '{')
                depth++;
            else if (json.charAt(i) == '}') {
                depth--;
                if (depth == 0)
                    return i;
            }
        }
        return json.length() - 1;
    }

    private int findArrayEnd(String json, int start) {
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == '[')
                depth++;
            else if (json.charAt(i) == ']') {
                depth--;
                if (depth == 0)
                    return i;
            }
        }
        return json.length() - 1;
    }

    private String unescapeJson(String s) {
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}
