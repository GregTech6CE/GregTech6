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
 * <p>Blockstate stems that are not listed here but follow a procedural family are rendered by
 * {@link ProceduralModels}; {@link #contains} and {@link #renderJson} cover both sources.
 */
public final class VirtualBlockstateCatalog {
    public static final String RESOURCE_PATH = "/assets/gt6ce/blockstate_definitions.tsv";

    private static final List<String> ALL_FACINGS = List.of("down", "up", "north", "south", "west", "east");
    private static final List<String> HORIZONTAL_FACINGS = List.of("north", "east", "south", "west");

    /** Facing order used where a definition table lists one model per direction. */
    private static final List<String> MODEL_ORDER = List.of("down", "east", "north", "south", "up", "west");

    private static final List<String> HORIZONTAL_MODEL_ORDER = List.of("east", "north", "south", "west");

    private static final List<String> SLAB_TYPES = List.of("bottom", "double", "top");

    /** Slab model of a horizontal facing; the downward facing is handled separately. */
    private static final Map<String, Integer> SLAB_FACING_MODEL =
            Map.of("up", 2, "east", 3, "north", 4, "south", 5, "west", 6);

    private static final List<String> RAIL_SHAPES = List.of(
            "ascending_east",
            "ascending_north",
            "ascending_south",
            "ascending_west",
            "east_west",
            "north_east",
            "north_south",
            "north_west",
            "south_east",
            "south_west");

    /** Rail shapes that only exist on plain tracks. */
    private static final Set<String> CURVED_RAIL_SHAPES =
            Set.of("north_east", "north_west", "south_east", "south_west");

    /** First solar panel model of a facing; each facing owns an inactive and an active variant. */
    private static final Map<String, Integer> SOLAR_MODEL_BASE =
            Map.of("down", 0, "up", 0, "east", 2, "north", 4, "south", 6, "west", 8);

    private static final List<String> COLORS = List.of(
            "white",
            "orange",
            "magenta",
            "light_blue",
            "yellow",
            "lime",
            "pink",
            "gray",
            "light_gray",
            "cyan",
            "purple",
            "blue",
            "brown",
            "green",
            "red",
            "black");

    private static final int[] QUARTER_TURNS = {0, 90, 180, 270};

    private static volatile @Nullable Map<String, Entry> cache;

    private VirtualBlockstateCatalog() {}

    /** One virtual blockstate row; {@code models} lists the models its variants reference by index. */
    public record Entry(String block, Kind kind, List<String> models) {}

    /** Model reference of one variant: an index into {@link Entry#models()} plus its rotation. */
    private record ModelRef(int model, int x, int y) {}

    /** One blockstate variant: its property key and the model references it applies in order. */
    private record Variant(String key, List<ModelRef> models) {}

    /** The repeated variant shapes; each declares how many models a row of that shape lists. */
    public enum Kind {
        SINGLETON(1),
        FACING_4_ROT(1),
        FACING_6_MODELS(6),
        FACING_4_MODELS(4),
        FACING_6_ROT(1),
        FACING_6_OPEN_2(2),
        FACING_6_OPEN_1(1),
        SCAFFOLD_DESIGN_4(4),
        FACING_6_LIT_1(1),
        AXIS_2(2),
        FACING_5_FLAT_DOWN(1),
        FACING_6_LIT_2(2),
        FACING_6_SENSOR(1),
        FACING_6_ACTIVE_2(2),
        RAIL_BOOSTER(6),
        WOOD_SLAB(7),
        BUSH_STAGES(28),
        RAIL_REGULAR(4),
        LEAVES(1),
        SAPLING(2),
        FACING_6_HEAT_EXCHANGER(4),
        BARS_MASK_16(16),
        FACING_5_FUNNEL(2),
        FLUID_LEVEL_16(2),
        FACING_6_ACTIVITY_4(4),
        COLOR_16_SINGLE(1),
        BOMBS_ARMED_SUNK_4(4),
        FACING_4_RESIN_2(2),
        ACTIVE_2(2),
        TWIGS_4(4),
        FACING_6_SOLAR_10(10),
        FACING_6_ACTIVE_LOADED_4(4);

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
                case FACING_6_SENSOR -> sensorRotations();
                case FACING_6_MODELS -> facingModelPerDirection();
                case FACING_4_MODELS -> horizontalModelPerDirection();
                case FACING_6_OPEN_1 -> openVariants(false);
                case FACING_6_OPEN_2 -> openVariants(true);
                case SCAFFOLD_DESIGN_4 -> scaffoldDesigns();
                case FACING_6_LIT_1 -> litVariants(false);
                case FACING_6_LIT_2 -> litVariants(true);
                case FACING_6_ACTIVE_2 -> activeVariants();
                case FACING_6_ACTIVITY_4 -> activityVariants();
                case FACING_6_ACTIVE_LOADED_4 -> activeLoadedVariants();
                case FACING_6_HEAT_EXCHANGER -> heatExchangerVariants();
                case FACING_6_SOLAR_10 -> solarPanelVariants();
                case FACING_4_RESIN_2 -> resinVariants();
                case BOMBS_ARMED_SUNK_4 -> bombVariants();
                case FACING_5_FLAT_DOWN -> flatDownVariants(0);
                case FACING_5_FUNNEL -> flatDownVariants(1);
                case AXIS_2 -> axisVariants();
                case RAIL_BOOSTER -> railBoosterVariants();
                case RAIL_REGULAR -> railVariants();
                case WOOD_SLAB -> woodSlabVariants();
                case BUSH_STAGES -> bushVariants();
                case LEAVES -> leafVariants();
                case SAPLING -> saplingVariants();
                case BARS_MASK_16 -> barVariants();
                case FLUID_LEVEL_16 -> fluidLevelVariants();
                case COLOR_16_SINGLE -> colorVariants();
                case ACTIVE_2 -> List.of(variant("active=false", ref(0)), variant("active=true", ref(1)));
                case TWIGS_4 -> twigVariants();
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

    /** Reports whether {@code stem} resolves to a virtual or procedural blockstate. */
    public static boolean contains(String stem) {
        return stem != null && (entries().containsKey(stem) || ProceduralModels.containsBlockstate(stem));
    }

    /** Renders {@code stem}, or returns {@code null} when no virtual or procedural blockstate matches. */
    public static @Nullable String renderJson(String stem) {
        Entry entry = stem == null ? null : entries().get(stem);
        if (entry != null) return renderEntryJson(entry);
        return ProceduralModels.renderBlockstateJson(stem);
    }

    /**
     * Emits every virtual and procedural blockstate to {@code sink}.
     *
     * <p>Identical rendered documents share one byte array, and a stem emitted twice fails the pack build.
     */
    public static void emitAll(BiConsumer<String, byte[]> sink) {
        Set<String> seen = new HashSet<>(2048);
        Map<String, byte[]> encoded = new HashMap<>();
        ProceduralModels.emitBlockstates((stem, bytes) -> {
            if (!seen.add(stem)) {
                throw new IllegalStateException("Duplicate virtual blockstate: " + stem);
            }
            sink.accept(stem, bytes);
        });
        for (Entry entry : entries().values()) {
            if (!seen.add(entry.block())) {
                throw new IllegalStateException("Duplicate virtual blockstate: " + entry.block());
            }
            String json = renderEntryJson(entry);
            sink.accept(entry.block(), encoded.computeIfAbsent(json, text -> text.getBytes(StandardCharsets.UTF_8)));
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
        StringBuilder out = new StringBuilder(320);
        out.append("{\"variants\":{");
        boolean first = true;
        for (Variant variant : entry.kind().variants()) {
            if (!first) out.append(',');
            first = false;
            JsonEscaper.appendQuoted(out, variant.key());
            out.append(':');
            appendModels(out, variant.models(), entry.models());
        }
        return out.append("}}").toString();
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

    private static Variant variant(String key, ModelRef... refs) {
        return new Variant(key, List.of(refs));
    }

    private static ModelRef ref(int model) {
        return new ModelRef(model, 0, 0);
    }

    private static ModelRef ref(int model, int x, int y) {
        return new ModelRef(model, x, y);
    }

    /** Rotation that turns a north-facing model onto {@code facing}. */
    private static ModelRef facingRotation(int model, String facing) {
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

    /** Sensor blocks sit a half turn further round than the plain facing rotation. */
    private static ModelRef sensorRotation(int model, String facing) {
        return switch (facing) {
            case "up" -> ref(model, 270, 180);
            case "down" -> ref(model, 90, 180);
            default -> facingRotation(model, facing);
        };
    }

    private static List<Variant> facingRotations(int model, List<String> facings) {
        List<Variant> variants = new ArrayList<>(facings.size());
        for (String facing : facings) {
            variants.add(variant("facing=" + facing, facingRotation(model, facing)));
        }
        return variants;
    }

    private static List<Variant> sensorRotations() {
        List<Variant> variants = new ArrayList<>(ALL_FACINGS.size());
        for (String facing : ALL_FACINGS) {
            variants.add(variant("facing=" + facing, sensorRotation(0, facing)));
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

    /** Scaffold designs 1 and 2 hang their hatch on the opposite side, so they turn a half turn further. */
    private static List<Variant> scaffoldDesigns() {
        List<Variant> variants = new ArrayList<>(16);
        for (int design = 0; design < 4; design++) {
            for (String facing : HORIZONTAL_FACINGS) {
                int yaw = HORIZONTAL_FACINGS.indexOf(facing) * 90;
                if (design == 1 || design == 2) yaw = (yaw + 180) % 360;
                variants.add(variant("design=" + design + ",facing=" + facing, ref(design, 0, yaw)));
            }
        }
        return variants;
    }

    private static List<Variant> openVariants(boolean distinctModels) {
        List<Variant> variants = new ArrayList<>(12);
        for (String facing : ALL_FACINGS) {
            for (boolean open : new boolean[] {false, true}) {
                variants.add(variant(
                        "facing=" + facing + ",open=" + open, facingRotation(distinctModels && open ? 1 : 0, facing)));
            }
        }
        return variants;
    }

    private static List<Variant> litVariants(boolean distinctModels) {
        List<Variant> variants = new ArrayList<>(12);
        for (String facing : ALL_FACINGS) {
            for (boolean lit : new boolean[] {false, true}) {
                variants.add(variant(
                        "facing=" + facing + ",lit=" + lit, facingRotation(distinctModels && lit ? 1 : 0, facing)));
            }
        }
        return variants;
    }

    private static List<Variant> activeVariants() {
        List<Variant> variants = new ArrayList<>(12);
        for (String facing : ALL_FACINGS) {
            for (boolean active : new boolean[] {false, true}) {
                variants.add(variant("facing=" + facing + ",active=" + active, facingRotation(active ? 1 : 0, facing)));
            }
        }
        return variants;
    }

    private static List<Variant> activityVariants() {
        List<Variant> variants = new ArrayList<>(24);
        for (String facing : ALL_FACINGS) {
            for (int activity = 0; activity < 4; activity++) {
                variants.add(variant("facing=" + facing + ",activity=" + activity, facingRotation(activity, facing)));
            }
        }
        return variants;
    }

    private static List<Variant> activeLoadedVariants() {
        List<Variant> variants = new ArrayList<>(24);
        for (String facing : ALL_FACINGS) {
            for (boolean active : new boolean[] {false, true}) {
                for (boolean loaded : new boolean[] {false, true}) {
                    int model = (active ? 2 : 0) + (loaded ? 1 : 0);
                    variants.add(variant(
                            "facing=" + facing + ",active=" + active + ",loaded=" + loaded,
                            facingRotation(model, facing)));
                }
            }
        }
        return variants;
    }

    /** Heat exchangers keep a separate model for the downward facing and do not tilt up or down. */
    private static List<Variant> heatExchangerVariants() {
        List<Variant> variants = new ArrayList<>(12);
        for (String facing : ALL_FACINGS) {
            for (boolean lit : new boolean[] {false, true}) {
                int model = facing.equals("down") ? (lit ? 1 : 0) : 2 + (lit ? 1 : 0);
                ModelRef reference =
                        facing.equals("down") || facing.equals("up") ? ref(model) : facingRotation(model, facing);
                variants.add(variant("facing=" + facing + ",lit=" + lit, reference));
            }
        }
        return variants;
    }

    private static List<Variant> solarPanelVariants() {
        List<Variant> variants = new ArrayList<>(12);
        for (String facing : ALL_FACINGS) {
            for (boolean active : new boolean[] {false, true}) {
                variants.add(variant(
                        "facing=" + facing + ",solar_active=" + active,
                        ref(SOLAR_MODEL_BASE.get(facing) + (active ? 1 : 0))));
            }
        }
        return variants;
    }

    private static List<Variant> resinVariants() {
        List<Variant> variants = new ArrayList<>(8);
        for (String facing : HORIZONTAL_FACINGS) {
            for (boolean resin : new boolean[] {false, true}) {
                variants.add(variant("facing=" + facing + ",resin=" + resin, facingRotation(resin ? 1 : 0, facing)));
            }
        }
        return variants;
    }

    private static List<Variant> bombVariants() {
        List<Variant> variants = new ArrayList<>(24);
        for (boolean armed : new boolean[] {false, true}) {
            for (String facing : ALL_FACINGS) {
                for (boolean sunk : new boolean[] {false, true}) {
                    int model = (armed ? 2 : 0) + (sunk ? 1 : 0);
                    variants.add(variant(
                            "armed=" + armed + ",facing=" + facing + ",sunk=" + sunk, facingRotation(model, facing)));
                }
            }
        }
        return variants;
    }

    /** Facings that carry a dedicated model; the downward facing keeps the flat model unrotated. */
    private static List<Variant> flatDownVariants(int horizontalModel) {
        List<Variant> variants = new ArrayList<>(5);
        for (String facing : HORIZONTAL_FACINGS) {
            variants.add(variant("facing=" + facing, facingRotation(horizontalModel, facing)));
        }
        variants.add(variant("facing=down", ref(0)));
        return variants;
    }

    private static List<Variant> axisVariants() {
        return List.of(variant("axis=y", ref(1)), variant("axis=z", ref(0, 90, 0)), variant("axis=x", ref(0, 90, 90)));
    }

    private static List<Variant> railVariants() {
        List<Variant> variants = new ArrayList<>(RAIL_SHAPES.size());
        for (String shape : RAIL_SHAPES) {
            variants.add(variant("shape=" + shape, railRef(shape, 0)));
        }
        return variants;
    }

    private static List<Variant> railBoosterVariants() {
        List<Variant> variants = new ArrayList<>(12);
        for (boolean powered : new boolean[] {false, true}) {
            for (String shape : RAIL_SHAPES) {
                if (CURVED_RAIL_SHAPES.contains(shape)) continue;
                variants.add(variant("powered=" + powered + ",shape=" + shape, railRef(shape, powered ? 3 : 0)));
            }
        }
        return variants;
    }

    /** Rail model and rotation for one shape; {@code base} selects the powered or unpowered model set. */
    private static ModelRef railRef(String shape, int base) {
        return switch (shape) {
            case "ascending_north" -> ref(base);
            case "ascending_east" -> ref(base, 0, 90);
            case "ascending_south" -> ref(base + 1);
            case "ascending_west" -> ref(base + 1, 0, 90);
            case "north_south" -> ref(base + 2);
            case "east_west" -> ref(base + 2, 0, 90);
            case "south_east" -> ref(base + 3);
            case "south_west" -> ref(base + 3, 0, 90);
            case "north_west" -> ref(base + 3, 0, 180);
            case "north_east" -> ref(base + 3, 0, 270);
            default -> throw new IllegalArgumentException("Unknown rail shape: " + shape);
        };
    }

    private static List<Variant> woodSlabVariants() {
        List<Variant> variants = new ArrayList<>(18);
        for (String facing : ALL_FACINGS) {
            for (String type : SLAB_TYPES) {
                variants.add(variant("facing=" + facing + ",type=" + type, ref(slabModel(facing, type))));
            }
        }
        return variants;
    }

    /**
     * Slab model for one facing and slab type: a double slab shows the planks model, and the top half
     * of a downward facing block shows the upward model.
     */
    private static int slabModel(String facing, String type) {
        if (type.equals("double")) return 1;
        if (facing.equals("down")) return type.equals("bottom") ? 0 : 2;
        return SLAB_FACING_MODEL.get(facing);
    }

    /** Bushes list seven models per growth stage: six support variants and the unsupported one. */
    private static List<Variant> bushVariants() {
        List<Variant> variants = new ArrayList<>(28);
        for (int stage = 0; stage < 4; stage++) {
            for (int support = 0; support < 7; support++) {
                variants.add(variant("stage=" + stage + ",support=" + support, ref(stage * 7 + support)));
            }
        }
        return variants;
    }

    private static List<Variant> leafVariants() {
        List<Variant> variants = new ArrayList<>(14);
        for (int distance = 1; distance <= 7; distance++) {
            for (boolean persistent : new boolean[] {false, true}) {
                variants.add(variant("distance=" + distance + ",persistent=" + persistent, ref(0)));
            }
        }
        return variants;
    }

    private static List<Variant> saplingVariants() {
        return List.of(variant("stage=0", ref(0), ref(1)), variant("stage=1", ref(0), ref(1)));
    }

    private static List<Variant> barVariants() {
        List<Variant> variants = new ArrayList<>(16);
        for (int mask = 0; mask < 16; mask++) {
            variants.add(variant("mask=" + mask, ref(mask)));
        }
        return variants;
    }

    private static List<Variant> fluidLevelVariants() {
        List<Variant> variants = new ArrayList<>(16);
        variants.add(variant("level=0", ref(0)));
        for (int level = 1; level < 16; level++) {
            variants.add(variant("level=" + level, ref(1)));
        }
        return variants;
    }

    private static List<Variant> colorVariants() {
        List<Variant> variants = new ArrayList<>(COLORS.size());
        for (String color : COLORS) {
            variants.add(variant("color=" + color, ref(0)));
        }
        return variants;
    }

    private static List<Variant> twigVariants() {
        List<ModelRef> refs = new ArrayList<>(16);
        for (int twig = 0; twig < 4; twig++) {
            for (int yaw : QUARTER_TURNS) {
                refs.add(ref(twig, 0, yaw));
            }
        }
        return List.of(new Variant("", refs));
    }
}
