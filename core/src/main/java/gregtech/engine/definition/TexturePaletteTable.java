package gregtech.engine.definition;

import gregtech.engine.mod.GregTechIdentity;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Palette-recoloring rules for sprites synthesized at runtime from shared template PNGs.
 *
 * <p>Palette keys and values use little-endian {@code ABGR} ({@code 0xAABBGGRR}), matching
 * {@code NativeImage.Format.RGBA} pixel integers without channel swizzling at draw time.
 */
public final class TexturePaletteTable {
    public static final String RESOURCE_PATH = "/assets/" + GregTechIdentity.MOD_ID + "/texture_palettes.tsv";

    private static volatile @Nullable TexturePaletteTable instance;

    private final Map<String, Entry> entries;
    private final Map<String, byte[]> mcmetaPresets;
    private final Map<String, byte[]> mcmetaByPath;

    private TexturePaletteTable(
            Map<String, Entry> entries, Map<String, byte[]> mcmetaPresets, Map<String, byte[]> mcmetaByPath) {
        this.entries = Collections.unmodifiableMap(entries);
        this.mcmetaPresets = Collections.unmodifiableMap(mcmetaPresets);
        this.mcmetaByPath = Collections.unmodifiableMap(mcmetaByPath);
    }

    public static TexturePaletteTable load() {
        TexturePaletteTable current = instance;
        if (current != null) return current;
        synchronized (TexturePaletteTable.class) {
            current = instance;
            if (current != null) return current;
            try (InputStream stream = TexturePaletteTable.class.getResourceAsStream(RESOURCE_PATH)) {
                if (stream == null) {
                    throw new IOException("Missing dynamic palette table resource: " + RESOURCE_PATH);
                }
                current = parse(stream);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to load " + RESOURCE_PATH, e);
            }
            instance = current;
            return current;
        }
    }

    public static TexturePaletteTable parse(InputStream stream) throws IOException {
        record PendingRow(
                int lineNo,
                String target,
                String template,
                @Nullable String mcmetaId,
                String pairsSpec) {}

        Map<String, byte[]> presets = new LinkedHashMap<>();
        List<PendingRow> rows = new ArrayList<>();
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\t", -1);
                String kind = parts[0];
                if ("mcmeta".equals(kind)) {
                    if (parts.length != 3) {
                        throw new IOException("Malformed mcmeta row at line " + lineNo);
                    }
                    presets.put(parts[1], parts[2].getBytes(StandardCharsets.UTF_8));
                } else if ("tex".equals(kind)) {
                    if (parts.length != 5) {
                        throw new IOException("Malformed tex row at line " + lineNo);
                    }
                    String mcmetaId = "-".equals(parts[3]) ? null : parts[3];
                    rows.add(new PendingRow(lineNo, parts[1], parts[2], mcmetaId, parts[4]));
                } else {
                    throw new IOException("Unknown row kind at line " + lineNo + ": " + kind);
                }
            }
        }
        Map<String, Entry> entries = new LinkedHashMap<>(rows.size() * 2);
        Map<String, byte[]> mcmetaByPath = new LinkedHashMap<>();
        for (PendingRow row : rows) {
            Entry entry = Entry.create(row.target(), row.template(), row.mcmetaId(), row.pairsSpec());
            if (entries.putIfAbsent(row.target(), entry) != null) {
                throw new IOException("Duplicate dynamic texture path at line " + row.lineNo() + ": " + row.target());
            }
            if (row.mcmetaId() != null) {
                byte[] preset = presets.get(row.mcmetaId());
                if (preset == null) {
                    throw new IOException("Unknown mcmeta preset at line " + row.lineNo() + ": " + row.mcmetaId());
                }
                mcmetaByPath.put(row.target() + ".mcmeta", preset);
            }
        }
        return new TexturePaletteTable(entries, presets, mcmetaByPath);
    }

    public boolean containsAssetPath(String assetPath) {
        return entries.containsKey(assetPath) || mcmetaByPath.containsKey(assetPath);
    }

    public @Nullable Entry entry(String assetPath) {
        return entries.get(assetPath);
    }

    public byte @Nullable [] mcmetaBytes(String assetPath) {
        return mcmetaByPath.get(assetPath);
    }

    public Collection<Entry> entries() {
        return entries.values();
    }

    public Map<String, byte[]> mcmetaByPath() {
        return mcmetaByPath;
    }

    /** One palette-mapped dynamic texture rule backed by a primitive open-addressed lookup table. */
    public static final class Entry {
        private final String assetPath;
        private final String templatePath;
        private final @Nullable String mcmetaId;
        private final boolean[] occupied;
        private final int[] keys;
        private final int[] values;
        private final int mask;
        private final int ruleCount;

        private Entry(
                String assetPath,
                String templatePath,
                @Nullable String mcmetaId,
                boolean[] occupied,
                int[] keys,
                int[] values,
                int ruleCount) {
            this.assetPath = assetPath;
            this.templatePath = templatePath;
            this.mcmetaId = mcmetaId;
            this.occupied = occupied;
            this.keys = keys;
            this.values = values;
            this.mask = occupied.length - 1;
            this.ruleCount = ruleCount;
        }

        static Entry create(String assetPath, String templatePath, @Nullable String mcmetaId, String pairsSpec)
                throws IOException {
            if ("-".equals(pairsSpec) || pairsSpec.isEmpty()) {
                return new Entry(assetPath, templatePath, mcmetaId, new boolean[1], new int[1], new int[1], 0);
            }
            String[] pairs = pairsSpec.split(",");
            Map<Integer, Integer> map = new HashMap<>(pairs.length * 2);
            for (String pair : pairs) {
                int sep = pair.indexOf(':');
                if (sep <= 0 || sep == pair.length() - 1) {
                    throw new IOException("Malformed palette pair for " + assetPath + ": " + pair);
                }
                int from;
                int to;
                try {
                    from = Integer.parseUnsignedInt(pair.substring(0, sep), 16);
                    to = Integer.parseUnsignedInt(pair.substring(sep + 1), 16);
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid hex color pair for " + assetPath + ": " + pair, e);
                }
                map.put(from, to);
            }
            int cap = Integer.highestOneBit(Math.max(4, map.size() * 3)) << 1;
            boolean[] occ = new boolean[cap];
            int[] k = new int[cap];
            int[] v = new int[cap];
            int mask = cap - 1;
            for (var e : map.entrySet()) {
                int key = e.getKey();
                int slot = mix(key) & mask;
                while (occ[slot]) {
                    slot = (slot + 1) & mask;
                }
                occ[slot] = true;
                k[slot] = key;
                v[slot] = e.getValue();
            }
            return new Entry(assetPath, templatePath, mcmetaId, occ, k, v, map.size());
        }

        private static int mix(int x) {
            x ^= (x >>> 16);
            x *= 0x7feb352d;
            x ^= (x >>> 15);
            return x;
        }

        public String assetPath() {
            return assetPath;
        }

        public String templatePath() {
            return templatePath;
        }

        public @Nullable String mcmetaId() {
            return mcmetaId;
        }

        public int ruleCount() {
            return ruleCount;
        }

        public int mapAbgr(int abgr) {
            // Unify all alpha == 0 pixels to canonical 0 (transparent).
            if ((abgr & 0xFF000000) == 0) return 0;
            if (ruleCount == 0) return abgr;
            int slot = mix(abgr) & mask;
            while (occupied[slot]) {
                if (keys[slot] == abgr) return values[slot];
                slot = (slot + 1) & mask;
            }
            return abgr;
        }
    }
}
