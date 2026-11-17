package gregtech.engine.machine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;
import org.jspecify.annotations.Nullable;

/**
 * Compact face table for the orthogonally baked machine models.
 *
 * <p>Rows live in {@code assets/gt6ce/machine_models.tsv}; the client bake reads them instead of
 * shipping one static model JSON per machine, per lit/running state and per facing. The six face
 * columns are fixed to {@code DOWN, UP, NORTH, SOUTH, WEST, EAST}, the same numbers the vanilla
 * direction enum uses, so a row is a cube's worth of textures rather than a JSON tree.
 *
 * <p>This class stays free of Minecraft types: it only resolves texture paths to strings.
 */
public final class OrthogonalMachineModels {
    private static final String RESOURCE = "/assets/gt6ce/machine_models.tsv";
    private static final String LEGACY_TEXTURES = "/assets/gt6ce/machine_model_legacy_textures.tsv";
    private static final String TEXTURE_NAMESPACE = "gt6ce:";
    private static final String MACHINE_TEXTURE_DIRECTORY = "block/machines/";
    /** {@code modelPath, solidBase, particle, base x 6, overlay x 6}. */
    private static final int COLUMNS = 15;

    private static final int BASE_COLUMN = 3;
    private static final int OVERLAY_COLUMN = 9;
    /** A face the machine does not draw, and a particle the row leaves empty. */
    private static final String ABSENT = "-";

    private static final Map<String, Entry> MODELS = load(RESOURCE);
    private static final Map<String, Entry> LEGACY = load(LEGACY_TEXTURES);

    private OrthogonalMachineModels() {}

    /** Every row, keyed by model path; empty when the table has not been generated yet. */
    public static Map<String, Entry> all() {
        return MODELS;
    }

    public static @Nullable Entry get(String modelPath) {
        return modelPath == null ? null : MODELS.get(modelPath);
    }

    public static boolean contains(String modelPath) {
        return modelPath != null && MODELS.containsKey(modelPath);
    }

    /** Rows name machine textures relative to {@code gt6ce:block/machines/}; other packs name their own. */
    public static String fullTexturePath(String specTexture) {
        return specTexture.indexOf(':') >= 0
                ? specTexture
                : TEXTURE_NAMESPACE + MACHINE_TEXTURE_DIRECTORY + specTexture;
    }

    /**
     * Selects old face paths provided by the same or a higher priority pack. The probe returns
     * a nonnegative pack position for a visible texture and a negative value for an absent one.
     */
    public static Entry resolveLegacyTextures(Entry model, ToIntFunction<String> packPriority) {
        Entry legacy = LEGACY.get(model.modelPath());
        if (legacy == null) return model;

        String particle = preferredTexture(model.particle(), legacy.particle(), packPriority);
        String[] base = null;
        String[] overlay = null;
        for (int face = 0; face < 6; face++) {
            String currentBase = model.baseFace(face);
            String preferredBase = preferredTexture(currentBase, legacy.baseFace(face), packPriority);
            if (!Objects.equals(preferredBase, currentBase)) {
                if (base == null)
                    base = model.base() == null ? new String[6] : model.base().clone();
                base[face] = preferredBase;
            }
            String currentOverlay = model.overlayFace(face);
            String preferredOverlay = preferredTexture(currentOverlay, legacy.overlayFace(face), packPriority);
            if (!Objects.equals(preferredOverlay, currentOverlay)) {
                if (overlay == null)
                    overlay = model.overlay() == null
                            ? new String[6]
                            : model.overlay().clone();
                overlay[face] = preferredOverlay;
            }
        }
        if (Objects.equals(particle, model.particle()) && base == null && overlay == null) return model;
        return new Entry(
                model.modelPath(),
                base == null ? model.solidBase() : legacy.solidBase(),
                particle,
                base == null ? model.base() : base,
                overlay == null ? model.overlay() : overlay);
    }

    private static @Nullable String preferredTexture(
            @Nullable String current, @Nullable String legacy, ToIntFunction<String> packPriority) {
        if (legacy == null) return current;
        int legacyPriority = packPriority.applyAsInt(legacy);
        return legacyPriority >= 0 && (current == null || legacyPriority >= packPriority.applyAsInt(current))
                ? legacy
                : current;
    }

    /**
     * One table row. {@code base} carries the tinted shell faces, {@code overlay} the untinted
     * detail pass; an entry may hold either pass alone but never neither.
     */
    public record Entry(
            String modelPath,
            boolean solidBase,
            @Nullable String particle,
            @Nullable String[] base,
            @Nullable String[] overlay) {
        public Entry {
            base = normalize(base);
            overlay = normalize(overlay);
        }

        /** Texture of one base face, or null when the shell does not draw it. */
        public @Nullable String baseFace(int face) {
            return face(base, face);
        }

        /** Texture of one overlay face, or null when the machine has no detail on it. */
        public @Nullable String overlayFace(int face) {
            return face(overlay, face);
        }

        public boolean hasOverlay() {
            return overlay != null;
        }
    }

    /** Parses a whole table, skipping blank lines and {@code #} comments. */
    public static List<Entry> parse(Reader reader) throws IOException {
        return parse(reader, RESOURCE);
    }

    private static List<Entry> parse(Reader reader, String resource) throws IOException {
        BufferedReader lines = reader instanceof BufferedReader buffered ? buffered : new BufferedReader(reader);
        List<Entry> entries = new ArrayList<>();
        String line;
        int number = 0;
        while ((line = lines.readLine()) != null) {
            number++;
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            String[] columns = line.split("\t", -1);
            if (columns.length != COLUMNS)
                throw new IllegalStateException(
                        "Expected " + COLUMNS + " columns on line " + number + " of " + resource);
            String modelPath = columns[0].strip();
            if (modelPath.isEmpty())
                throw new IllegalStateException("Empty model path on line " + number + " of " + resource);
            entries.add(new Entry(
                    modelPath,
                    columns[1].strip().equals("1"),
                    absent(columns[2]),
                    faces(columns, BASE_COLUMN),
                    faces(columns, OVERLAY_COLUMN)));
        }
        return List.copyOf(entries);
    }

    private static Map<String, Entry> load(String resource) {
        // The table ships separately from this class; an empty map lets missing rows be reported
        // without aborting startup, rather than leaving deleted static block models in place.
        try (InputStream stream = OrthogonalMachineModels.class.getResourceAsStream(resource)) {
            if (stream == null) return Map.of();
            Map<String, Entry> models = new LinkedHashMap<>();
            for (Entry entry : parse(new InputStreamReader(stream, StandardCharsets.UTF_8), resource)) {
                if (models.put(entry.modelPath(), entry) != null)
                    throw new IllegalStateException("Duplicate model " + entry.modelPath() + " in " + resource);
            }
            return Collections.unmodifiableMap(models);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + resource, e);
        }
    }

    private static @Nullable String[] faces(String[] columns, int start) {
        String[] faces = new String[6];
        boolean any = false;
        for (int face = 0; face < faces.length; face++) {
            faces[face] = absent(columns[start + face]);
            any |= faces[face] != null;
        }
        return any ? faces : null;
    }

    /** Fixed-length face array; callers index it by direction id, so it never stays ragged. */
    private static @Nullable String[] normalize(@Nullable String[] faces) {
        if (faces == null) return null;
        String[] copy = new String[6];
        for (int face = 0; face < copy.length; face++) copy[face] = face < faces.length ? absent(faces[face]) : null;
        return copy;
    }

    private static @Nullable String absent(@Nullable String value) {
        if (value == null) return null;
        String stripped = value.strip();
        return stripped.isEmpty() || stripped.equals(ABSENT) ? null : stripped;
    }

    private static @Nullable String face(@Nullable String[] faces, int face) {
        return faces == null || face < 0 || face >= faces.length ? null : faces[face];
    }
}
