package gregtech.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

/** Merges the gt6ce language files contributed by the base mod and every installed addon. */
public final class GametestLang {
    private GametestLang() {}

    public static JsonObject load(String language) throws IOException {
        JsonObject merged = new JsonObject();
        Enumeration<URL> resources = GametestLang.class.getClassLoader()
                .getResources("assets/gt6ce/lang/" + language + ".json");
        while (resources.hasMoreElements()) {
            try (var stream = resources.nextElement().openStream()) {
                JsonObject part = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                for (var entry : part.entrySet()) merged.add(entry.getKey(), entry.getValue());
            }
        }
        if (merged.size() == 0) throw new IllegalStateException("missing language " + language);
        return merged;
    }
}
