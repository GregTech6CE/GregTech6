package gregtech.engine.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.jspecify.annotations.Nullable;

/**
 * Item group definitions served as virtual {@code remi:group} resources.
 *
 * <p>Each row of {@link #RESOURCE_PATH} describes one group: either a regular expression over item IDs
 * or an explicit list of tags and items, plus an optional shared exclusion list.
 */
public final class StackGroupCatalog {
    public static final String RESOURCE_PATH = "/assets/gt6ce/stack_groups.tsv";

    private static final List<String> EXCLUSIONS_STD =
            List.of("#minecraft:planks", "#gt6ce:wooden_planks", "item:gt6ce:planks_treated");
    private static final List<String> EXCLUSIONS_STD_PLATE = List.of(
            "#minecraft:planks",
            "#gt6ce:wooden_planks",
            "item:gt6ce:planks_treated",
            "item:minecraft:glass_pane",
            "item:minecraft:paper",
            "item:gt6ce:plate_paper");

    private static volatile @Nullable Map<String, Entry> cache;

    private StackGroupCatalog() {}

    /** How a group selects its members. */
    public enum Kind {
        REGEX,
        TAG,
        ITEMS
    }

    /**
     * One group row.
     *
     * <p>{@code contents} is empty for {@link Kind#REGEX} groups, which match {@code payload} instead.
     */
    public record Entry(
            String stem,
            int priority,
            String name,
            Kind kind,
            String payload,
            String exclusionsCode,
            List<String> contents,
            List<String> exclusions) {}

    public static Map<String, Entry> parse(InputStream stream) throws IOException {
        Map<String, Entry> entries = new LinkedHashMap<>();
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] parts = line.split("\t", -1);
                if (parts.length != 6) {
                    throw new IOException("Malformed stack group row at line " + lineNo + ": expected 6 columns");
                }
                Entry entry = createEntry(parts, lineNo);
                if (entries.putIfAbsent(entry.stem(), entry) != null) {
                    throw new IOException("Duplicate stack group stem at line " + lineNo + ": " + entry.stem());
                }
            }
        }
        return Collections.unmodifiableMap(entries);
    }

    /** Returns every group keyed by stem, loading {@link #RESOURCE_PATH} on first use. */
    public static Map<String, Entry> entries() {
        Map<String, Entry> current = cache;
        if (current != null) return current;
        synchronized (StackGroupCatalog.class) {
            current = cache;
            if (current != null) return current;
            try (InputStream stream = StackGroupCatalog.class.getResourceAsStream(RESOURCE_PATH)) {
                if (stream == null) {
                    throw new IOException("Missing stack group resource: " + RESOURCE_PATH);
                }
                current = parse(stream);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to load " + RESOURCE_PATH, e);
            }
            cache = current;
            return current;
        }
    }

    public static @Nullable Entry entry(String stem) {
        return stem == null ? null : entries().get(stem);
    }

    /** Renders {@code entry} as a compact {@code remi:group} JSON document. */
    public static String renderJson(Entry entry) {
        StringBuilder sb = new StringBuilder(160);
        sb.append("{\"type\":\"remi:group\",\"name\":");
        JsonEscaper.appendQuoted(sb, entry.name());
        sb.append(",\"priority\":").append(entry.priority());
        if (entry.kind() == Kind.REGEX) {
            sb.append(",\"regex\":");
            JsonEscaper.appendQuoted(sb, entry.payload());
        } else {
            sb.append(",\"contents\":");
            appendArray(sb, entry.contents());
        }
        if (!entry.exclusions().isEmpty()) {
            sb.append(",\"exclusions\":");
            appendArray(sb, entry.exclusions());
        }
        return sb.append('}').toString();
    }

    /** Renders every group into resource paths relative to the pack's asset namespace. */
    public static Map<String, byte[]> buildAllResources() {
        Map<String, Entry> all = entries();
        Map<String, byte[]> resources = new LinkedHashMap<>(all.size() * 2);
        for (Entry entry : all.values()) {
            resources.put(
                    "stack_groups/material_forms/" + entry.stem() + ".json",
                    renderJson(entry).getBytes(StandardCharsets.UTF_8));
        }
        return Collections.unmodifiableMap(resources);
    }

    private static Entry createEntry(String[] parts, int lineNo) throws IOException {
        String stem = parts[0];
        if (stem.isEmpty()) {
            throw new IOException("Empty stack group stem at line " + lineNo);
        }
        int priority;
        try {
            priority = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid stack group priority at line " + lineNo + ": " + parts[1], e);
        }
        String payload = parts[4];
        Kind kind;
        List<String> contents;
        switch (parts[3].toLowerCase(Locale.ROOT)) {
            case "regex" -> {
                if (payload.isEmpty()) {
                    throw new IOException("Missing regex payload at line " + lineNo + ": " + stem);
                }
                try {
                    Pattern.compile(payload);
                } catch (PatternSyntaxException e) {
                    throw new IOException("Invalid regex payload at line " + lineNo + ": " + payload, e);
                }
                kind = Kind.REGEX;
                contents = List.of();
            }
            case "tag" -> {
                if (!payload.startsWith("#")) {
                    throw new IOException("Tag payload must start with '#' at line " + lineNo + ": " + payload);
                }
                kind = Kind.TAG;
                contents = List.of(payload);
            }
            case "items" -> {
                kind = Kind.ITEMS;
                contents = splitItems(payload, stem, lineNo);
            }
            default -> throw new IOException("Unknown stack group kind at line " + lineNo + ": " + parts[3]);
        }
        String exclusionsCode = parts[5];
        List<String> exclusions = switch (exclusionsCode) {
            case "" -> List.of();
            case "STD" -> EXCLUSIONS_STD;
            case "STD_PLATE" -> EXCLUSIONS_STD_PLATE;
            default ->
                throw new IOException("Unknown stack group exclusions code at line " + lineNo + ": " + exclusionsCode);
        };
        return new Entry(stem, priority, parts[2], kind, payload, exclusionsCode, contents, exclusions);
    }

    private static List<String> splitItems(String payload, String stem, int lineNo) throws IOException {
        if (payload.isEmpty()) {
            throw new IOException("Missing item payload at line " + lineNo + ": " + stem);
        }
        String[] parts = payload.split(";", -1);
        List<String> items = new ArrayList<>(parts.length);
        for (String part : parts) {
            String item = part.trim();
            if (item.isEmpty()) {
                throw new IOException("Blank item in payload at line " + lineNo + ": " + stem);
            }
            items.add(item);
        }
        return List.copyOf(items);
    }

    private static void appendArray(StringBuilder sb, List<String> values) {
        sb.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(',');
            JsonEscaper.appendQuoted(sb, values.get(i));
        }
        sb.append(']');
    }
}
