package gregtech.content.worldgen;

import static gregtech.content.worldgen.WorldgenHash.at;
import static gregtech.content.worldgen.WorldgenHash.mix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/** Seeded rock sequences with folded layers and dipping province boundaries. */
public final class StoneLayerField {
    public static final int SHALLOW_LAYER_MIN_Y = -7;
    public static final int CONTACT_DEPTH = 2;
    public static final int CORE_DEPTH = 3;
    private static final int REGION_SIZE = 384;
    private static final double REGION_WARP = 20.0;
    private static final int REGION_DATUM_Y = 32;
    private static final int MAX_REGION_DEPTH = 320;
    private static final double REGION_DIP = 84.0;
    private static final int BAND_THICKNESS = 12;
    private static final int DEEP_TRANSITION_Y = -22;
    private static final double FUEL_SEAM_THICKNESS = 1.5;
    private static final int LIGNITE_SEAM_OFFSET = 8;
    private static final List<GTStoneLayersGen.LayerDef> LAYERS = catalog();
    private static final int[] ROCKS = rockIndices();
    private static final int ANTHRACITE = layerForBlock("block_ore_anthracite");
    private static final int LIGNITE = layerForBlock("block_ore_lignite");
    private static final int DEEPSLATE = layerForBlock("minecraft:deepslate");
    private static final int TUFF = layerForBlock("minecraft:tuff");

    public enum Province {
        IGNEOUS,
        SEDIMENTARY,
        METAMORPHIC
    }

    private record Palette(int[] shallow, int[] deep) {}

    private static final Province[] PROVINCES = Province.values();

    private static final class Palettes {
        static final Palette[] BY_PROVINCE = palettes();
    }

    private final long seed;

    public StoneLayerField(long seed) {
        this.seed = seed;
    }

    public static List<GTStoneLayersGen.LayerDef> layers() {
        return LAYERS;
    }

    /**
     * Inclusive upper Y bound for scanning a chunk, extending one block above the highest filled
     * 16-block section so rocks resting directly on section boundaries (e.g. Y=63 or Y=143) are
     * still visited. Returns {@code minBuildHeight - 1} when the chunk has no filled sections.
     */
    public static int maxScanY(int minBuildHeight, int maxBuildHeight, int highestFilledSectionIndex) {
        if (highestFilledSectionIndex < 0) return minBuildHeight - 1;
        int sectionTopNext = minBuildHeight + (highestFilledSectionIndex + 1) * 16;
        return Math.min(maxBuildHeight - 1, sectionTopNext);
    }

    public Column column(int x, int z) {
        double fold = noise(seed ^ 0x85EBCA77L, x, z, 112);
        double stretch = noise(seed ^ 0x27D4EB2FL, x, z, 192);
        double shift = fold * 18.0 + noise(seed ^ 0xC2B2AE3DL, x, z, 40) * 5.0;
        return new Column(x + fold * REGION_WARP, z + stretch * REGION_WARP, shift, 1.0 + stretch * 0.12);
    }

    /**
     * Samples an inclusive height interval and enough padding to see contacts across all six faces.
     * The padding comes from the seed so neighboring chunks can generate in any order.
     */
    public Samples sample(int x, int z, int width, int depth, int minY, int maxY) {
        return new Samples(this, x, z, width, depth, minY, maxY);
    }

    public enum Face {
        ABOVE(0, 1, 0),
        BELOW(0, -1, 0),
        WEST(-1, 0, 0),
        EAST(1, 0, 0),
        NORTH(0, 0, -1),
        SOUTH(0, 0, 1);

        private final int x;
        private final int y;
        private final int z;

        Face(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public boolean vertical() {
            return y != 0;
        }
    }

    private static final Face[] FACES = Face.values();

    /** Measures the first different rock in each direction the first time the current position is queried. */
    public static final class Contacts {
        private final int[] opposing = new int[FACES.length];
        private final int[] distances = new int[FACES.length];
        private int nearest = CORE_DEPTH + 1;
        private Samples pending;
        private int index;

        private void measure() {
            if (pending != null) {
                pending.measureContacts(index, this);
                pending = null;
            }
        }

        /** Zero when no transition was found in this direction. */
        public int distance(Face face) {
            measure();
            return distances[face.ordinal()];
        }

        /** The rock across the transition when {@link #distance(Face)} is positive. */
        public int opposingLayer(Face face) {
            measure();
            return opposing[face.ordinal()];
        }

        /** Greater than {@link StoneLayerField#CORE_DEPTH} when no transition is nearby. */
        public int nearest() {
            measure();
            return nearest;
        }
    }

    public static final class Samples {
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int height;
        private final int row;
        private final int[] layers;
        private final int[] steps = new int[FACES.length];

        private Samples(StoneLayerField field, int x, int z, int width, int depth, int bottom, int top) {
            minX = x - CORE_DEPTH;
            minY = bottom - CORE_DEPTH;
            minZ = z - CORE_DEPTH;
            height = top - bottom + 1 + CORE_DEPTH * 2;
            int paddedDepth = depth + CORE_DEPTH * 2;
            row = paddedDepth * height;
            layers = new int[(width + CORE_DEPTH * 2) * row];
            var scores = new RegionScores();
            for (int dx = 0; dx < width + CORE_DEPTH * 2; dx++) {
                for (int dz = 0; dz < paddedDepth; dz++) {
                    field.column(minX + dx, minZ + dz).fill(minY, layers, dx * row + dz * height, height, scores);
                }
            }
            for (Face face : FACES) steps[face.ordinal()] = face.x * row + face.y + face.z * height;
        }

        public int layerIndex(int x, int y, int z) {
            return layers[index(x, y, z)];
        }

        /** Reuses the result for this position. Skipped positions require no neighbor reads. */
        public void contacts(int x, int y, int z, Contacts result) {
            result.pending = this;
            result.index = index(x, y, z);
        }

        private void measureContacts(int at, Contacts result) {
            int rock = ROCKS[layers[at]];
            result.nearest = CORE_DEPTH + 1;
            for (int face = 0; face < steps.length; face++) {
                int step = steps[face];
                result.distances[face] = 0;
                for (int d = 1; d <= CORE_DEPTH; d++) {
                    int other = layers[at + step * d];
                    if (rock == ROCKS[other]) continue;
                    result.opposing[face] = other;
                    result.distances[face] = d;
                    result.nearest = Math.min(result.nearest, d);
                    break;
                }
            }
        }

        private int index(int x, int y, int z) {
            return (x - minX) * row + (z - minZ) * height + y - minY;
        }
    }

    public final class Column {
        private final double x;
        private final double z;
        private final double shift;
        private final double stretch;

        private Column(double x, double z, double shift, double stretch) {
            this.x = x;
            this.z = z;
            this.shift = shift;
            this.stretch = stretch;
        }

        /** The ordinary or deep layer at Y. Both game generation and offline tools use this selection. */
        public int layerIndex(int y) {
            long region = regionAt(y, null);
            Province province = provinceOf(region);
            double height = warpedHeight(y);
            int layer = layerInBand(region, province, bandAt(height), y);
            return withFuelSeam(height, layer, fuelHorizon(region, province));
        }

        /** Fill the requested range, reusing the selection while the column stays inside one layer. */
        public void fill(int firstY, int[] samples, int length) {
            fill(firstY, samples, 0, length, new RegionScores());
        }

        private void fill(int firstY, int[] samples, int offset, int length, RegionScores scores) {
            if (length <= 0) return;
            long region = regionAt(firstY, scores);
            long lastRegion = scores.closest(firstY + length - 1);
            int from = 0;
            while (from < length) {
                int to = region == lastRegion ? length : firstDifferentRegion(firstY, from, length - 1, region, scores);
                Province province = provinceOf(region);
                fillRegion(region, province, firstY + from, samples, offset + from, to - from);
                from = to;
                if (from < length) region = scores.closest(firstY + from);
            }
        }

        private void fillRegion(long region, Province province, int firstY, int[] samples, int offset, int length) {
            int end = firstY + length;
            int y = firstY;
            while (y < end) {
                int band = bandAt(warpedHeight(y));
                int next = Math.min(end, firstYInBand(band + 1));
                if (y < SHALLOW_LAYER_MIN_Y) next = Math.min(next, SHALLOW_LAYER_MIN_Y);
                Arrays.fill(
                        samples,
                        offset + (y - firstY),
                        offset + (next - firstY),
                        layerInBand(region, province, band, y));
                y = next;
            }
            if (province == Province.SEDIMENTARY) {
                int horizon = fuelHorizon(region, province);
                fillSeam(samples, offset, firstY, end, horizon, ANTHRACITE);
                fillSeam(samples, offset, firstY, end, horizon + LIGNITE_SEAM_OFFSET, LIGNITE);
            }
        }

        private void fillSeam(int[] samples, int offset, int firstY, int end, int horizon, int layer) {
            int from = Math.max(firstY, firstYAtHeight(horizon));
            int to = Math.min(end, firstYAtHeight(horizon + FUEL_SEAM_THICKNESS));
            if (from < to) Arrays.fill(samples, offset + (from - firstY), offset + (to - firstY), layer);
        }

        private double warpedHeight(int y) {
            return y * stretch + shift;
        }

        private int firstYInBand(int band) {
            int y = (int) Math.ceil((band * (double) BAND_THICKNESS - shift) / stretch);
            // Match the point sampler on either side of a floating-point layer boundary.
            if (bandAt(warpedHeight(y - 1)) >= band) return y - 1;
            return bandAt(warpedHeight(y)) >= band ? y : y + 1;
        }

        private int firstYAtHeight(double height) {
            int y = (int) Math.ceil((height - shift) / stretch);
            if (warpedHeight(y - 1) >= height) return y - 1;
            return warpedHeight(y) >= height ? y : y + 1;
        }

        private long regionAt(int y, RegionScores scores) {
            int cellX = (int) Math.floor(x / REGION_SIZE);
            int cellZ = (int) Math.floor(z / REGION_SIZE);
            int depth = regionDepth(y);
            double nearest = Double.POSITIVE_INFINITY;
            long chosen = 0;
            int index = 0;
            // With sites in the middle half of each cell, an omitted site starts at least
            // 7/16 * REGION_SIZE^2 farther in squared distance. The capped dip cannot close that gap.
            for (int cx = cellX - 1; cx <= cellX + 1; cx++) {
                for (int cz = cellZ - 1; cz <= cellZ + 1; cz++) {
                    long site = at(seed ^ 0xA24BAED4963EE407L, cx, cz);
                    double dx = cx * (double) REGION_SIZE + REGION_SIZE * (0.25 + unit(site) * 0.5) - x;
                    double dz = cz * (double) REGION_SIZE + REGION_SIZE * (0.25 + unit(mix(site)) * 0.5) - z;
                    double distance = dx * dx + dz * dz;
                    double dip = ((site & 0xFFFFL) * (2.0 / 65535.0) - 1.0) * REGION_DIP;
                    if (scores != null) scores.set(index, site, distance, dip);
                    index++;
                    double score = distance + depth * dip;
                    if (score < nearest) {
                        nearest = score;
                        chosen = site;
                    }
                }
            }
            return chosen;
        }
    }

    private static int regionDepth(int y) {
        return Math.max(REGION_DATUM_Y - MAX_REGION_DEPTH, Math.min(y, REGION_DATUM_Y + MAX_REGION_DEPTH))
                - REGION_DATUM_Y;
    }

    private static int firstDifferentRegion(int firstY, int from, int end, long region, RegionScores scores) {
        // Each site's score is affine in depth, so once another site wins, this one cannot win again.
        int low = from + 1;
        while (low < end) {
            int middle = low + (end - low) / 2;
            if (scores.closest(firstY + middle) == region) low = middle + 1;
            else end = middle;
        }
        return low;
    }

    private static final class RegionScores {
        private final long[] sites = new long[9];
        private final double[] distances = new double[9];
        private final double[] dips = new double[9];

        void set(int index, long site, double distance, double dip) {
            sites[index] = site;
            distances[index] = distance;
            dips[index] = dip;
        }

        long closest(int y) {
            int depth = regionDepth(y);
            double nearest = Double.POSITIVE_INFINITY;
            long chosen = 0;
            for (int index = 0; index < sites.length; index++) {
                double score = distances[index] + depth * dips[index];
                if (score < nearest) {
                    nearest = score;
                    chosen = sites[index];
                }
            }
            return chosen;
        }
    }

    private static Province provinceOf(long region) {
        return PROVINCES[WorldgenHash.roll(region, PROVINCES.length)];
    }

    private static int bandAt(double height) {
        return (int) Math.floor(height / BAND_THICKNESS);
    }

    private static int fuelHorizon(long region, Province province) {
        return province == Province.SEDIMENTARY ? 18 + WorldgenHash.roll(mix(region ^ 0x35D0F1AB7CA2DE4FL), 6) : 0;
    }

    private static int withFuelSeam(double height, int ordinary, int horizon) {
        if (horizon == 0) return ordinary;
        // Thin interbeds follow the same warp as the surrounding layers.
        if (height >= horizon && height < horizon + FUEL_SEAM_THICKNESS) return ANTHRACITE;
        int ligniteHorizon = horizon + LIGNITE_SEAM_OFFSET;
        return height >= ligniteHorizon && height < ligniteHorizon + FUEL_SEAM_THICKNESS ? LIGNITE : ordinary;
    }

    private static int layerInBand(long region, Province province, int band, int y) {
        Palette palette = Palettes.BY_PROVINCE[province.ordinal()];
        int[] choices = band * BAND_THICKNESS < DEEP_TRANSITION_Y ? palette.deep() : palette.shallow();
        long roll = mix(region ^ (band * 0x9E3779B97F4A7C15L));
        int index = choices[WorldgenHash.roll(roll, choices.length)];
        return y < SHALLOW_LAYER_MIN_Y && LAYERS.get(index).noDeep() ? DEEPSLATE : index;
    }

    private static double noise(long seed, int x, int z, int scale) {
        int gx = Math.floorDiv(x, scale);
        int gz = Math.floorDiv(z, scale);
        double fx = smooth(Math.floorMod(x, scale) / (double) scale);
        double fz = smooth(Math.floorMod(z, scale) / (double) scale);
        double near = lerp(unit(at(seed, gx, gz)), unit(at(seed, gx + 1, gz)), fx);
        double far = lerp(unit(at(seed, gx, gz + 1)), unit(at(seed, gx + 1, gz + 1)), fx);
        return 2.0 * lerp(near, far, fz) - 1.0;
    }

    private static double smooth(double value) {
        return value * value * (3.0 - 2.0 * value);
    }

    private static double lerp(double start, double end, double weight) {
        return start + (end - start) * weight;
    }

    private static double unit(long value) {
        return (value >>> 11) * 0x1.0p-53;
    }

    private static List<GTStoneLayersGen.LayerDef> catalog() {
        var result = new ArrayList<>(GTStoneLayersGen.LAYERS);
        result.addAll(GTStoneLayersGen.DEEP_LAYERS);
        return List.copyOf(result);
    }

    private static int[] rockIndices() {
        var ids = new HashMap<String, Integer>();
        int[] rows = new int[LAYERS.size()];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = ids.computeIfAbsent(LAYERS.get(i).material(), material -> ids.size());
        }
        return rows;
    }

    private static int layerForBlock(String blockId) {
        for (int index = 0; index < LAYERS.size(); index++) {
            if (blockId.equals(LAYERS.get(index).blockId())) return index;
        }
        throw new IllegalStateException("Missing stone layer " + blockId);
    }

    private static Palette[] palettes() {
        var palettes = new Palette[PROVINCES.length];
        int regularLayers = LAYERS.size() - GTStoneLayersGen.DEEP_LAYERS.size();
        for (Province province : PROVINCES) {
            var shallow = new ArrayList<Integer>();
            var deep = new ArrayList<Integer>();
            for (int i = 0; i < regularLayers; i++) {
                var layer = LAYERS.get(i);
                if (i == ANTHRACITE || i == LIGNITE || !matches(layer, province)) continue;
                for (int n = 0; n < layer.weight(); n++) {
                    shallow.add(i);
                    if (deepRock(layer)) deep.add(i);
                }
            }
            deep.add(DEEPSLATE);
            deep.add(DEEPSLATE);
            deep.add(TUFF);
            palettes[province.ordinal()] = new Palette(
                    shallow.stream().mapToInt(Integer::intValue).toArray(),
                    deep.stream().mapToInt(Integer::intValue).toArray());
        }
        return palettes;
    }

    private static boolean matches(GTStoneLayersGen.LayerDef layer, Province province) {
        if (layer.isOreSeam()) return province == Province.SEDIMENTARY;
        if (layer.stoneType() == null) {
            return switch (layer.surfaceMaterial()) {
                case "Chert" -> province == Province.SEDIMENTARY;
                case "Pumice", "Dacite" -> province == Province.IGNEOUS;
                default -> true;
            };
        }
        return switch (layer.stoneType()) {
            case "BASALT",
                    "KOMATIITE",
                    "KIMBERLITE",
                    "ANDESITE",
                    "DIORITE",
                    "GRANITE",
                    "GRANITE_BLACK",
                    "GRANITE_RED" -> province == Province.IGNEOUS;
            case "LIMESTONE", "SHALE" -> province == Province.SEDIMENTARY;
            case "MARBLE", "GREENSCHIST", "BLUESCHIST", "SLATE", "QUARTZITE" -> province == Province.METAMORPHIC;
            default -> true;
        };
    }

    private static boolean deepRock(GTStoneLayersGen.LayerDef layer) {
        if (layer.noDeep()) return false;
        if (layer.stoneType() == null) return !layer.ores().isEmpty();
        return switch (layer.stoneType()) {
            case "LIMESTONE", "SHALE" -> false;
            default -> true;
        };
    }
}
