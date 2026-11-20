package gregtech.engine.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.jspecify.annotations.Nullable;

/**
 * Virtual blockstate definitions served by the shared item model pack.
 *
 * <p>Rows of {@link #RESOURCE_PATH} name a block, one of the repeated {@link Kind} shapes and the
 * models its variants cycle through. A kind resolves its own property keys and rotations, so a row
 * carries no JSON and a whole family of blocks costs one line each.
 *
 * <p>Stems that are not listed here but follow a generated family are rendered by
 * {@link FamilyBlockstateGenerators}, and the procedural families by {@link ProceduralModels};
 * {@link #contains} and {@link #renderJson} cover all three sources.
 */
public final class VirtualBlockstateCatalog {
    public static final String RESOURCE_PATH = "/assets/gt6ce/blockstate_definitions.tsv";

    private static final List<String> ALL_FACINGS = List.of("down", "up", "north", "south", "west", "east");
    private static final List<String> HORIZONTAL_FACINGS = List.of("north", "east", "south", "west");

    /** Facing order used where a definition table lists one model per direction. */
    private static final List<String> MODEL_ORDER = List.of("down", "east", "north", "south", "up", "west");

    private static final List<String> HORIZONTAL_MODEL_ORDER = List.of("east", "north", "south", "west");

    private static volatile @Nullable Map<String, Entry> cache;

    private VirtualBlockstateCatalog() {}

    /** One virtual blockstate row; {@code models} lists the models its variants reference by index. */
    public record Entry(String block, Kind kind, List<String> models) {}

    /** Model reference of one variant: an index into {@link Entry#models()} plus its rotation. */
    record ModelRef(int model, int x, int y) {}

    /** One blockstate variant: its property key and the model references it applies in order. */
    record Variant(String key, List<ModelRef> models) {}

    /** The repeated variant shapes; each declares how many models a row of that shape lists. */
    public enum Kind {
        SINGLETON(1),
        FACING_4_ROT(1),
        FACING_6_MODELS(6),
        FACING_4_MODELS(4),
        FACING_6_ROT(1),
        AXIS_2(2);

        private final int modelCount;

        Kind(int modelCount) {
            this.modelCount = modelCount;
        }

        /** Number of models a row of this kind must list. */
        public int modelCount() {
            return modelCount;
        }

        /** Variant table of this kind, resolved against the row's model list. */
        List<Variant> variants() {
            return switch (this) {
                case SINGLETON -> List.of(variant("", ref(0)));
                case FACING_4_ROT -> facingRotations(0, HORIZONTAL_FACINGS);
                case FACING_6_ROT -> facingRotations(0, ALL_FACINGS);
                case FACING_6_MODELS -> facingModelPerDirection();
                case FACING_4_MODELS -> horizontalModelPerDirection();
                case AXIS_2 -> axisVariants();
            };
        }
    }

    public static Map<String, Entry> parse(InputStream stream) throws IOException {
        Map<String, Entry> entries = new LinkedHashMap<>();
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] parts = line.split("\t", -1);
                if (parts.length != 3) {
                    throw new IOException(
                            "Malformed blockstate definition row at line " + lineNo + ": expected 3 columns");
                }
                Entry entry = createEntry(parts, lineNo);
                if (entries.putIfAbsent(entry.block(), entry) != null) {
                    throw new IOException("Duplicate blockstate definition at line " + lineNo + ": " + entry.block());
                }
            }
        }
        return Collections.unmodifiableMap(entries);
    }

    /** Returns every virtual blockstate keyed by blockstate stem, loading {@link #RESOURCE_PATH} on first use. */
    public static Map<String, Entry> entries() {
        Map<String, Entry> current = cache;
        if (current != null) return current;
        synchronized (VirtualBlockstateCatalog.class) {
            current = cache;
            if (current != null) return current;
            try (InputStream stream = VirtualBlockstateCatalog.class.getResourceAsStream(RESOURCE_PATH)) {
                if (stream == null) {
                    throw new IOException("Missing blockstate definition resource: " + RESOURCE_PATH);
                }
                current = parse(stream);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to load " + RESOURCE_PATH, e);
            }
            cache = current;
            return current;
        }
    }

    /** Reports whether {@code stem} resolves to a table, family or procedural blockstate. */
    public static boolean contains(String stem) {
        return stem != null
                && (entries().containsKey(stem)
                        || FamilyBlockstateGenerators.contains(stem)
                        || ProceduralModels.containsBlockstate(stem));
    }

    /** Renders {@code stem}, or returns {@code null} when no table, family or procedural blockstate matches. */
    public static @Nullable String renderJson(String stem) {
        if (stem == null) return null;
        Entry entry = entries().get(stem);
        if (entry != null) return renderEntryJson(entry);
        String family = FamilyBlockstateGenerators.renderJson(stem);
        if (family != null) return family;
        return ProceduralModels.renderBlockstateJson(stem);
    }

    /**
     * Emits every table, family and procedural blockstate to {@code sink}.
     *
     * <p>Identical rendered documents share one byte array, and a stem emitted twice fails the pack build.
     */
    public static void emitAll(BiConsumer<String, byte[]> sink) {
        Set<String> seen = new HashSet<>(2048);
        Map<String, byte[]> encoded = new HashMap<>();
        BiConsumer<String, byte[]> guarded = (stem, bytes) -> {
            if (!seen.add(stem)) {
                throw new IllegalStateException("Duplicate virtual blockstate: " + stem);
            }
            sink.accept(stem, bytes);
        };
        ProceduralModels.emitBlockstates(guarded);
        FamilyBlockstateGenerators.emitAll(guarded);
        for (Entry entry : entries().values()) {
            guarded.accept(entry.block(), encodeUtf8(encoded, renderEntryJson(entry)));
        }
    }

    private static Entry createEntry(String[] parts, int lineNo) throws IOException {
        String block = parts[0];
        if (block.isEmpty()) {
            throw new IOException("Empty blockstate name at line " + lineNo);
        }
        Kind kind;
        try {
            kind = Kind.valueOf(parts[1]);
        } catch (IllegalArgumentException e) {
            throw new IOException("Unknown blockstate kind at line " + lineNo + ": " + parts[1], e);
        }
        List<String> models = List.of(parts[2].split(";", -1));
        if (models.size() != kind.modelCount()) {
            throw new IOException("Blockstate " + block + " at line " + lineNo + " needs " + kind.modelCount()
                    + " models for " + kind + " but lists " + models.size());
        }
        if (models.contains("")) {
            throw new IOException("Empty model reference at line " + lineNo + ": " + block);
        }
        return new Entry(block, kind, models);
    }

    private static String renderEntryJson(Entry entry) {
        return renderVariantsJson(entry.kind().variants(), entry.models());
    }

    /** Renders one blockstate document from a variant table and the models its references index. */
    static String renderVariantsJson(List<Variant> variants, List<String> models) {
        StringBuilder out = new StringBuilder(320);
        out.append("{\"variants\":{");
        boolean first = true;
        for (Variant variant : variants) {
            if (!first) out.append(',');
            first = false;
            JsonEscaper.appendQuoted(out, variant.key());
            out.append(':');
            appendModels(out, variant.models(), models);
        }
        return out.append("}}").toString();
    }

    /** Encodes {@code json} once per distinct document. */
    static byte[] encodeUtf8(Map<String, byte[]> cache, String json) {
        return cache.computeIfAbsent(json, text -> text.getBytes(StandardCharsets.UTF_8));
    }

    private static void appendModels(StringBuilder out, List<ModelRef> refs, List<String> models) {
        boolean multiple = refs.size() > 1;
        if (multiple) out.append('[');
        for (int index = 0; index < refs.size(); index++) {
            if (index > 0) out.append(',');
            appendModel(out, refs.get(index), models);
        }
        if (multiple) out.append(']');
    }

    private static void appendModel(StringBuilder out, ModelRef reference, List<String> models) {
        out.append("{\"model\":");
        JsonEscaper.appendQuoted(out, models.get(reference.model()));
        if (reference.x() != 0) out.append(",\"x\":").append(reference.x());
        if (reference.y() != 0) out.append(",\"y\":").append(reference.y());
        out.append('}');
    }

    // -------------------------------------------------------------- variant tables

    static Variant variant(String key, ModelRef... refs) {
        return new Variant(key, List.of(refs));
    }

    static ModelRef ref(int model) {
        return new ModelRef(model, 0, 0);
    }

    static ModelRef ref(int model, int x, int y) {
        return new ModelRef(model, x, y);
    }

    /** Rotation that turns a north-facing model onto {@code facing}. */
    static ModelRef facingRotation(int model, String facing) {
        return switch (facing) {
            case "north" -> ref(model);
            case "east" -> ref(model, 0, 90);
            case "south" -> ref(model, 0, 180);
            case "west" -> ref(model, 0, 270);
            case "up" -> ref(model, 270, 0);
            case "down" -> ref(model, 90, 0);
            default -> throw new IllegalArgumentException("Unknown facing: " + facing);
        };
    }

    private static List<Variant> facingRotations(int model, List<String> facings) {
        List<Variant> variants = new ArrayList<>(facings.size());
        for (String facing : facings) {
            variants.add(variant("facing=" + facing, facingRotation(model, facing)));
        }
        return variants;
    }

    private static List<Variant> facingModelPerDirection() {
        List<Variant> variants = new ArrayList<>(ALL_FACINGS.size());
        for (String facing : ALL_FACINGS) {
            variants.add(variant("facing=" + facing, ref(MODEL_ORDER.indexOf(facing))));
        }
        return variants;
    }

    private static List<Variant> horizontalModelPerDirection() {
        List<Variant> variants = new ArrayList<>(HORIZONTAL_FACINGS.size());
        for (String facing : HORIZONTAL_FACINGS) {
            variants.add(variant("facing=" + facing, ref(HORIZONTAL_MODEL_ORDER.indexOf(facing))));
        }
        return variants;
    }

    private static List<Variant> axisVariants() {
        return List.of(variant("axis=y", ref(1)), variant("axis=z", ref(0, 90, 0)), variant("axis=x", ref(0, 90, 90)));
    }

    /**
     * Facings that carry a dedicated model; the downward facing keeps the flat model unrotated.
     *
     * <p>Shared with the fluid attachment family, whose horizontal facings all point at the flat model.
     */
    static List<Variant> flatDownVariants(int horizontalModel) {
        List<Variant> variants = new ArrayList<>(5);
        for (String facing : HORIZONTAL_FACINGS) {
            variants.add(variant("facing=" + facing, facingRotation(horizontalModel, facing)));
        }
        variants.add(variant("facing=down", ref(0)));
        return variants;
    }
}
