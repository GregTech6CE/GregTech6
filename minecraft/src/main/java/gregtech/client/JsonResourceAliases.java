package gregtech.client;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Expands release-only JSON templates without redirecting user resource-pack overrides. */
final class JsonResourceAliases {
    private static final Map<String, byte[]> RESOURCES = load();

    private JsonResourceAliases() {}

    static Map<String, byte[]> resources() {
        return RESOURCES;
    }

    static boolean contains(String absolutePath) {
        return RESOURCES.containsKey(absolutePath) || JsonResourceAliases.class.getResource(absolutePath) != null;
    }

    private static Map<String, byte[]> load() {
        try (var input = JsonResourceAliases.class.getResourceAsStream("/META-INF/gt6ce/model-json-aliases.json")) {
            // Development classpaths keep the complete static resource tree.
            if (input == null) return Map.of();
            var index = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            Map<String, byte[]> result = new HashMap<>();
            if (index.get("version").getAsInt() != 1) throw new IOException("Unsupported model template version");
            for (var groupElement : index.getAsJsonArray("groups")) {
                var group = groupElement.getAsJsonObject();
                var segments = group.getAsJsonArray("segments");
                for (var variantElement : group.getAsJsonArray("variants")) {
                    var variant = variantElement.getAsJsonArray();
                    var parameters = variant.get(0).getAsJsonArray();
                    if (segments.size() != parameters.size() + 1) throw new IOException("Invalid template arity");
                    var json = new StringBuilder(segments.get(0).getAsString());
                    for (int i = 0; i < parameters.size(); i++) {
                        json.append(new JsonPrimitive(parameters.get(i).getAsString()));
                        json.append(segments.get(i + 1).getAsString());
                    }
                    byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
                    for (var pathElement : variant.get(1).getAsJsonArray()) {
                        String path = checkedPath(pathElement.getAsString());
                        if (result.put(path, bytes) != null) throw new IOException("Duplicate generated JSON: " + path);
                    }
                }
            }
            return Map.copyOf(result);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot load shared model JSON", exception);
        }
    }

    private static String checkedPath(String path) throws IOException {
        if (!(path.startsWith("assets/gt6ce/models/") || path.startsWith("assets/gt6ce/blockstates/"))
                || !path.endsWith(".json")
                || path.contains("..")
                || path.contains("\\")) {
            throw new IOException("Invalid JSON alias path: " + path);
        }
        return "/" + path;
    }
}
