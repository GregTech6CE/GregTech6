package gregtech.engine.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.jspecify.annotations.Nullable;

/**
 * Virtual model definitions served by the shared item model pack.
 *
 * <p>Rows of {@link #RESOURCE_PATH} describe models that are not shipped as JSON files on disk: a parent
 * model, an optional render type, an optional ambient occlusion opt-out and optional texture slots. Models
 * whose parent is {@code @scaffold1} are generated procedurally from their {@code rod} texture, so a row
 * that omits that texture is rejected while parsing.
 */
public final class VirtualModelCatalog {
    public static final String RESOURCE_PATH = "/assets/gt6ce/model_definitions.tsv";

    private static final String SCAFFOLD_PARENT = "@scaffold1";

    private static volatile @Nullable Map<String, Entry> cache;

    private VirtualModelCatalog() {}

    /** One virtual model row; {@code path} is a model path without the {@code .json} suffix. */
    public record Entry(
            String path, String parent, String renderType, boolean disableAo, Map<String, String> textures) {}

    public static Map<String, Entry> parse(InputStream stream) throws IOException {
        Map<String, Entry> entries = new LinkedHashMap<>();
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] parts = line.split("\t", -1);
                if (parts.length != 5) {
                    throw new IOException("Malformed model definition row at line " + lineNo + ": expected 5 columns");
                }
                Entry entry = createEntry(parts, lineNo);
                if (entries.putIfAbsent(entry.path(), entry) != null) {
                    throw new IOException("Duplicate model definition path at line " + lineNo + ": " + entry.path());
                }
            }
        }
        return Collections.unmodifiableMap(entries);
    }

    /** Returns every virtual model keyed by model path, loading {@link #RESOURCE_PATH} on first use. */
    public static Map<String, Entry> entries() {
        Map<String, Entry> current = cache;
        if (current != null) return current;
        synchronized (VirtualModelCatalog.class) {
            current = cache;
            if (current != null) return current;
            try (InputStream stream = VirtualModelCatalog.class.getResourceAsStream(RESOURCE_PATH)) {
                if (stream == null) {
                    throw new IOException("Missing model definition resource: " + RESOURCE_PATH);
                }
                current = parse(stream);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to load " + RESOURCE_PATH, e);
            }
            cache = current;
            return current;
        }
    }

    /** Reports whether {@code modelPath} resolves to a virtual or procedural model. */
    public static boolean contains(String modelPath) {
        return modelPath != null && (entries().containsKey(modelPath) || ProceduralModels.containsModel(modelPath));
    }

    /** Renders {@code modelPath}, or returns {@code null} when no virtual or procedural model matches. */
    public static @Nullable String renderJson(String modelPath, String compositeLoader) {
        Entry entry = modelPath == null ? null : entries().get(modelPath);
        if (entry != null) return renderEntryJson(entry, compositeLoader);
        return ProceduralModels.renderModelJson(modelPath, compositeLoader);
    }

    /**
     * Emits every virtual and procedural model to {@code sink}.
     *
     * <p>Identical rendered documents share one byte array, and a path emitted twice fails the pack build.
     */
    public static void emitAll(String compositeLoader, BiConsumer<String, byte[]> sink) {
        Set<String> seen = new HashSet<>(8192);
        Map<String, byte[]> encoded = new HashMap<>();
        ProceduralModels.emitModels(compositeLoader, (path, bytes) -> {
            if (!seen.add(path)) {
                throw new IllegalStateException("Duplicate virtual model: " + path);
            }
            sink.accept(path, bytes);
        });
        for (Entry entry : entries().values()) {
            String json = renderEntryJson(entry, compositeLoader);
            if (!seen.add(entry.path())) {
                throw new IllegalStateException("Duplicate virtual model: " + entry.path());
            }
            sink.accept(entry.path(), encoded.computeIfAbsent(json, text -> text.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private static Entry createEntry(String[] parts, int lineNo) throws IOException {
        String path = parts[0];
        if (path.isEmpty()) {
            throw new IOException("Empty model definition path at line " + lineNo);
        }
        String parent = parts[1];
        if (parent.isEmpty()) {
            throw new IOException("Empty model definition parent at line " + lineNo + ": " + path);
        }
        boolean disableAo = switch (parts[3]) {
            case "" -> false;
            case "0" -> true;
            default -> throw new IOException("Invalid ambient occlusion flag at line " + lineNo + ": " + parts[3]);
        };
        Map<String, String> textures = parseTextures(parts[4], path, lineNo);
        if (SCAFFOLD_PARENT.equals(parent) && !textures.containsKey("rod")) {
            throw new IOException("Scaffold model missing rod texture at line " + lineNo + ": " + path);
        }
        return new Entry(path, parent, parts[2], disableAo, textures);
    }

    private static Map<String, String> parseTextures(String spec, String path, int lineNo) throws IOException {
        if (spec.isEmpty()) return Map.of();
        String[] pairs = spec.split(";", -1);
        Map<String, String> textures = new LinkedHashMap<>(pairs.length * 2);
        for (String pair : pairs) {
            int sep = pair.indexOf('=');
            String key = sep < 0 ? "" : pair.substring(0, sep).trim();
            String value = sep < 0 ? "" : pair.substring(sep + 1).trim();
            if (key.isEmpty() || value.isEmpty()) {
                throw new IOException("Malformed texture entry at line " + lineNo + " for " + path + ": " + pair);
            }
            if (textures.putIfAbsent(key, value) != null) {
                throw new IOException("Duplicate texture key at line " + lineNo + " for " + path + ": " + key);
            }
        }
        return Collections.unmodifiableMap(textures);
    }

    private static String renderEntryJson(Entry entry, String compositeLoader) {
        if (SCAFFOLD_PARENT.equals(entry.parent())) {
            return ProceduralModels.scaffoldDesign1Json(
                    compositeLoader, entry.textures().get("rod"));
        }
        StringBuilder sb = new StringBuilder(192);
        sb.append('{');
        appendStringField(sb, "parent", entry.parent());
        appendStringField(sb, "render_type", entry.renderType());
        if (entry.disableAo()) {
            startField(sb, "ambientocclusion");
            sb.append("false");
        }
        appendTextures(sb, entry.textures());
        return sb.append('}').toString();
    }

    private static void appendStringField(StringBuilder sb, String key, String value) {
        if (value.isEmpty()) return;
        startField(sb, key);
        JsonEscaper.appendQuoted(sb, value);
    }

    private static void appendTextures(StringBuilder sb, Map<String, String> textures) {
        if (textures.isEmpty()) return;
        startField(sb, "textures");
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, String> texture : textures.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            JsonEscaper.appendQuoted(sb, texture.getKey());
            sb.append(':');
            JsonEscaper.appendQuoted(sb, texture.getValue());
        }
        sb.append('}');
    }

    /** Writes the separator and key of a JSON object field; the opening brace needs no separator. */
    private static void startField(StringBuilder sb, String key) {
        if (sb.charAt(sb.length() - 1) != '{') sb.append(',');
        sb.append('"').append(key).append("\":");
    }
}
