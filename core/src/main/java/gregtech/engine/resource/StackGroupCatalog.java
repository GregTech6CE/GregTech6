package gregtech.engine.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.jspecify.annotations.Nullable;

/**
 * Item group definitions served as virtual {@code remi:group} resources.
 *
 * <p>Groups come from three sources: {@link #TAG_GROUP_STEMS} names one tag group per material prefix,
 * {@link #ITEMS_RESOURCE_PATH} lists explicit item members and {@link #REGEX_RESOURCE_PATH} matches item IDs
 * by regular expression. Every source may declare an optional shared exclusion list.
 */
public final class StackGroupCatalog {
    public static final String ITEMS_RESOURCE_PATH = "/assets/gt6ce/stack_groups_items.tsv";
    public static final String REGEX_RESOURCE_PATH = "/assets/gt6ce/stack_groups_regex.tsv";

    private static final String EXCLUSIONS_CODE_STD = "STD";
    private static final String EXCLUSIONS_CODE_STD_PLATE = "STD_PLATE";

    private static final List<String> EXCLUSIONS_STD =
            List.of("#minecraft:planks", "#gt6ce:wooden_planks", "item:gt6ce:planks_treated");
    private static final List<String> EXCLUSIONS_STD_PLATE = List.of(
            "#minecraft:planks",
            "#gt6ce:wooden_planks",
            "item:gt6ce:planks_treated",
            "item:minecraft:glass_pane",
            "item:minecraft:paper",
            "item:gt6ce:plate_paper");

    /** Stem of the only tag group that also excludes plain sheet-like items. */
    private static final String PLATE_STEM = "plate";

    private static final int TAG_PRIORITY = 100;
    private static final int COLUMN_COUNT = 5;

    /** Tag groups in ascending stem order; every stem maps to the {@code #gt6ce:<stem>} tag. */
    private static final List<String> TAG_GROUP_STEMS = List.of(
            "arrow_gt_plastic",
            "arrow_gt_wood",
            "billet",
            "block_dust",
            "block_gem",
            "block_ingot",
            "block_plate",
            "block_plate_gem",
            "block_raw",
            "block_solid",
            "bolt",
            "boule_gt",
            "bullet_gt_large",
            "bullet_gt_medium",
            "bullet_gt_small",
            "casing_machine",
            "casing_machine_dense",
            "casing_machine_double",
            "casing_machine_quadruple",
            "chain",
            "chunk_gt",
            "crushed",
            "crushed_centrifuged",
            "crushed_centrifuged_tiny",
            "crushed_purified",
            "crushed_purified_tiny",
            "crushed_tiny",
            "dust",
            "dust_div72",
            "dust_small",
            "dust_tiny",
            "foil",
            "gear_gt",
            "gear_gt_small",
            "gem",
            "gem_chipped",
            "gem_exquisite",
            "gem_flawed",
            "gem_flawless",
            "gem_legendary",
            "ingot",
            "ingot_double",
            "ingot_quadruple",
            "ingot_quintuple",
            "ingot_triple",
            "item_casing",
            "lens",
            "minecart_wheels",
            "nugget",
            "ore",
            "ore_raw",
            "ore_small",
            "plate",
            "plate_curved",
            "plate_dense",
            "plate_double",
            "plate_gem",
            "plate_gem_tiny",
            "plate_quadruple",
            "plate_quintuple",
            "plate_tiny",
            "plate_triple",
            "rail_gt",
            "ring",
            "rock_gt",
            "rotor",
            "round",
            "scrap_gt",
            "screw",
            "spring",
            "spring_small",
            "stick",
            "stick_long",
            "tool_head_arrow",
            "tool_head_axe",
            "tool_head_axe_double",
            "tool_head_builderwand",
            "tool_head_buzz_saw",
            "tool_head_chainsaw",
            "tool_head_chisel",
            "tool_head_construction_pickaxe",
            "tool_head_drill",
            "tool_head_file",
            "tool_head_hammer",
            "tool_head_hoe",
            "tool_head_pickaxe",
            "tool_head_pickaxe_gem",
            "tool_head_plow",
            "tool_head_raw_arrow",
            "tool_head_raw_axe",
            "tool_head_raw_axe_double",
            "tool_head_raw_chisel",
            "tool_head_raw_hoe",
            "tool_head_raw_pickaxe",
            "tool_head_raw_plow",
            "tool_head_raw_saw",
            "tool_head_raw_sense",
            "tool_head_raw_shovel",
            "tool_head_raw_spade",
            "tool_head_raw_sword",
            "tool_head_raw_universal_spade",
            "tool_head_saw",
            "tool_head_screwdriver",
            "tool_head_sense",
            "tool_head_shovel",
            "tool_head_spade",
            "tool_head_sword",
            "tool_head_universal_spade",
            "tool_head_wrench",
            "unit",
            "wire_fine");

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

    /**
     * Parses {@link #ITEMS_RESOURCE_PATH} rows: {@code stem, priority, name, items, exclusions}.
     *
     * <p>{@code stream} is closed before this method returns.
     */
    public static Map<String, Entry> parseItems(InputStream stream) throws IOException {
        return parseRows(stream, Kind.ITEMS, StackGroupCatalog::readItems);
    }

    /**
     * Parses {@link #REGEX_RESOURCE_PATH} rows: {@code stem, priority, name, regex, exclusions}.
     *
     * <p>{@code stream} is closed before this method returns.
     */
    public static Map<String, Entry> parseRegex(InputStream stream) throws IOException {
        return parseRows(stream, Kind.REGEX, StackGroupCatalog::readRegex);
    }

    /** Returns every group keyed by stem, loading both resources on first use. */
    public static Map<String, Entry> entries() {
        Map<String, Entry> current = cache;
        if (current != null) return current;
        synchronized (StackGroupCatalog.class) {
            current = cache;
            if (current != null) return current;
            try {
                current = loadAll();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to load stack group resources", e);
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

    /** Merges the generated tag groups with both table resources, rejecting duplicate stems. */
    private static Map<String, Entry> loadAll() throws IOException {
        Map<String, Entry> merged = new LinkedHashMap<>(512);
        for (String stem : TAG_GROUP_STEMS) {
            addUnique(merged, tagEntry(stem), "generated tag groups");
        }
        addAllUnique(merged, parseItems(openResource(ITEMS_RESOURCE_PATH)), ITEMS_RESOURCE_PATH);
        addAllUnique(merged, parseRegex(openResource(REGEX_RESOURCE_PATH)), REGEX_RESOURCE_PATH);
        return sortByStem(merged);
    }

    private static InputStream openResource(String path) throws IOException {
        InputStream stream = StackGroupCatalog.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IOException("Missing stack group resource: " + path);
        }
        return stream;
    }

    private static void addAllUnique(Map<String, Entry> target, Map<String, Entry> source, String sourceName)
            throws IOException {
        for (Entry entry : source.values()) {
            addUnique(target, entry, sourceName);
        }
    }

    private static void addUnique(Map<String, Entry> target, Entry entry, String sourceName) throws IOException {
        if (target.putIfAbsent(entry.stem(), entry) != null) {
            throw new IOException("Duplicate stack group stem in " + sourceName + ": " + entry.stem());
        }
    }

    /** Returns {@code entries} in ascending stem order so the emitted pack stays deterministic. */
    private static Map<String, Entry> sortByStem(Map<String, Entry> entries) {
        List<Entry> sorted = new ArrayList<>(entries.values());
        sorted.sort(Comparator.comparing(Entry::stem));
        Map<String, Entry> ordered = new LinkedHashMap<>(sorted.size() * 2);
        for (Entry entry : sorted) {
            ordered.put(entry.stem(), entry);
        }
        return Collections.unmodifiableMap(ordered);
    }

    /** Builds the tag group for {@code stem} without touching the material registries. */
    private static Entry tagEntry(String stem) {
        boolean plate = PLATE_STEM.equals(stem);
        String payload = "#gt6ce:" + stem;
        return new Entry(
                stem,
                TAG_PRIORITY,
                "itemGroup.gt6ce." + stem,
                Kind.TAG,
                payload,
                plate ? EXCLUSIONS_CODE_STD_PLATE : EXCLUSIONS_CODE_STD,
                List.of(payload),
                plate ? EXCLUSIONS_STD_PLATE : EXCLUSIONS_STD);
    }

    /** Reads five-column rows, delegating the group-specific payload column to {@code payloadReader}. */
    private static Map<String, Entry> parseRows(InputStream stream, Kind kind, PayloadReader payloadReader)
            throws IOException {
        Map<String, Entry> entries = new LinkedHashMap<>(512);
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] parts = line.split("\t", -1);
                if (parts.length != COLUMN_COUNT) {
                    throw new IOException(
                            "Malformed stack group row at line " + lineNo + ": expected " + COLUMN_COUNT + " columns");
                }
                Entry entry = createEntry(parts, lineNo, kind, payloadReader);
                if (entries.putIfAbsent(entry.stem(), entry) != null) {
                    throw new IOException("Duplicate stack group stem at line " + lineNo + ": " + entry.stem());
                }
            }
        }
        return Collections.unmodifiableMap(entries);
    }

    private static Entry createEntry(String[] parts, int lineNo, Kind kind, PayloadReader payloadReader)
            throws IOException {
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
        String payload = parts[3];
        List<String> contents = payloadReader.read(payload, stem, lineNo);
        String exclusionsCode = parts[4];
        List<String> exclusions = switch (exclusionsCode) {
            case "" -> List.of();
            case EXCLUSIONS_CODE_STD -> EXCLUSIONS_STD;
            case EXCLUSIONS_CODE_STD_PLATE -> EXCLUSIONS_STD_PLATE;
            default ->
                throw new IOException("Unknown stack group exclusions code at line " + lineNo + ": " + exclusionsCode);
        };
        return new Entry(stem, priority, parts[2], kind, payload, exclusionsCode, contents, exclusions);
    }

    /** Validates a regular expression payload; regex groups carry no {@code contents}. */
    private static List<String> readRegex(String payload, String stem, int lineNo) throws IOException {
        if (payload.isEmpty()) {
            throw new IOException("Missing regex payload at line " + lineNo + ": " + stem);
        }
        try {
            Pattern.compile(payload);
        } catch (PatternSyntaxException e) {
            throw new IOException("Invalid regex payload at line " + lineNo + ": " + payload, e);
        }
        return List.of();
    }

    /** Splits a semicolon-separated item payload into its members. */
    private static List<String> readItems(String payload, String stem, int lineNo) throws IOException {
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

    /** Turns one payload column into the group's member list. */
    @FunctionalInterface
    private interface PayloadReader {
        List<String> read(String payload, String stem, int lineNo) throws IOException;
    }
}
