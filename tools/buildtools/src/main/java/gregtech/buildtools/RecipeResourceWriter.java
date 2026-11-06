package gregtech.buildtools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Materializes the native 1.21.1 tree from the shared 1.20.1 source tree.
 *
 * <p>The output directory is rebuilt from scratch so a removed or renamed source can never leave
 * an old resource behind, and a target that also exists in the version node's own resources is a
 * build failure rather than an implicit overwrite.</p>
 */
public final class RecipeResourceWriter {
    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private RecipeResourceWriter() {}

    public record Summary(int recipes, int tags, int excluded) {}

    public static Summary write(Path sourceData, Path outputData, Set<String> excluded, Path existingData, Path report)
            throws IOException {
        if (!Files.isDirectory(sourceData)) throw new IOException("Missing source data root " + sourceData);
        deleteTree(outputData);
        Files.createDirectories(outputData);
        var seenExclusions = new LinkedHashSet<String>();
        var written = new TreeSet<String>();
        int recipes = 0;
        int tags = 0;

        for (Path namespace : sortedChildren(sourceData)) {
            if (!Files.isDirectory(namespace)) continue;
            String ns = namespace.getFileName().toString();
            Path recipeSource = namespace.resolve("recipes");
            if (Files.isDirectory(recipeSource)) {
                for (Path file : sortedJson(recipeSource)) {
                    String rel = relative(recipeSource, file);
                    String dataRel = ns + "/recipes/" + rel;
                    if (excluded.contains(dataRel)) {
                        seenExclusions.add(dataRel);
                        continue;
                    }
                    var target = outputData.resolve(ns).resolve("recipe").resolve(rel);
                    rejectExisting(outputData, existingData, target, "recipe");
                    var source = parse(file);
                    var converted = RecipeResourceConverter.convertRecipe(dataRel, source.getAsJsonObject());
                    writeFile(target, GSON.toJson(converted) + "\n");
                    written.add(relative(outputData, target));
                    recipes++;
                }
            }
            Path tagSource = namespace.resolve("tags").resolve("items");
            if (Files.isDirectory(tagSource)) {
                for (Path file : sortedJson(tagSource)) {
                    String rel = relative(tagSource, file);
                    var target = outputData
                            .resolve(ns)
                            .resolve("tags")
                            .resolve("item")
                            .resolve(rel);
                    rejectExisting(outputData, existingData, target, "tag");
                    var source = parse(file);
                    RecipeResourceConverter.convertTag(ns + "/tags/items/" + rel, source.getAsJsonObject());
                    // Tag documents are copied verbatim: their values already use the native form.
                    writeFile(target, Files.readString(file, StandardCharsets.UTF_8));
                    written.add(relative(outputData, target));
                    tags++;
                }
            }
        }

        var stale = new TreeSet<>(excluded);
        stale.removeAll(seenExclusions);
        if (!stale.isEmpty()) throw new IOException("Exclusions name missing sources: " + stale);

        if (report != null) {
            Files.createDirectories(report.getParent());
            var lines = new ArrayList<>(written);
            lines.add(0, "# generated native resources: " + written.size());
            Files.write(report, lines, StandardCharsets.UTF_8);
        }
        return new Summary(recipes, tags, seenExclusions.size());
    }

    private static void rejectExisting(Path outputData, Path existingData, Path target, String kind)
            throws IOException {
        if (existingData == null) return;
        Path existing = existingData.resolve(relative(outputData, target));
        if (Files.exists(existing))
            throw new IOException("Duplicate " + kind + " target: " + target + " also exists as " + existing);
    }

    private static com.google.gson.JsonElement parse(Path file) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == '﻿') text = text.substring(1);
        try {
            return JsonParser.parseString(text);
        } catch (RuntimeException failure) {
            throw new IOException(file + ": " + failure.getMessage(), failure);
        }
    }

    private static void writeFile(Path target, String content) throws IOException {
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    private static List<Path> sortedChildren(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.sorted().toList();
        }
    }

    private static List<Path> sortedJson(Path dir) throws IOException {
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    private static String relative(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }

    /** Parses one exclusion file: blank lines and {@code #} comments are ignored. */
    public static Set<String> readExclusions(Path file) throws IOException {
        var out = new TreeSet<String>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            out.add(trimmed);
        }
        return out;
    }
}
