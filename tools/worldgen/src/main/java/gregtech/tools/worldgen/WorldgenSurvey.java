package gregtech.tools.worldgen;

import static gregtech.tools.worldgen.OreSurveyChunk.DIMENSION;
import static gregtech.tools.worldgen.WorldgenSampler.BOTTOM;
import static gregtech.tools.worldgen.WorldgenSampler.TOP;

import gregtech.api.material.GTMaterial;
import gregtech.content.worldgen.GTOreVeins;
import gregtech.content.worldgen.GTOreVeins.HeightRange;
import gregtech.content.worldgen.OreSourceCatalog;
import gregtech.content.worldgen.OreVeinLayout;
import gregtech.content.worldgen.OreVeinLayout.Body;
import gregtech.content.worldgen.StoneLayerField;
import gregtech.content.worldgen.StratumOreRules;
import gregtech.engine.material.GTMaterialRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Mineral and layer statistics for a synthetic Overworld rock volume. */
public final class WorldgenSurvey {
    private static final OreSourceCatalog.Geology GEOLOGY = OreSourceCatalog.Geology.WITHOUT_VANILLA_ORES;
    private static final int TILE_CHUNKS = 4;

    private WorldgenSurvey() {}

    public static void main(String[] args) throws IOException {
        if (args.length < 5 || args.length > 6)
            throw new IllegalArgumentException(
                    "Expected: seed report-directory firstChunkX firstChunkZ side [surfaceY]");
        long seed = Long.parseLong(args[0]);
        var area = new Area(
                Integer.parseInt(args[2]),
                Integer.parseInt(args[3]),
                Integer.parseInt(args[4]),
                args.length == 6 ? Integer.parseInt(args[5]) : 143);
        Path directory = Files.createDirectories(Path.of(args[1]));
        survey(directory, seed, area);
    }

    private record Area(int x, int z, int side, int top) {
        private Area {
            if (side < 1 || side > 64 || top <= BOTTOM || top > TOP)
                throw new IllegalArgumentException("Side must be 1..64 chunks and surfaceY must be -63..319");
            Math.multiplyExact(Math.addExact(x, side), 16);
            Math.multiplyExact(Math.addExact(z, side), 16);
            Math.multiplyExact(x, 16);
            Math.multiplyExact(z, 16);
        }

        int size() {
            return side * side;
        }

        int chunkIndex(int dx, int dz) {
            return dx * side + dz;
        }

        int margin() {
            return side / 4;
        }
    }

    private static final class Mineral {
        long normal, poor, seams, bedrock;
        final BitSet mineable = new BitSet();
        final BitSet full = new BitSet();
        final BitSet basal = new BitSet();

        void add(int chunk, byte form) {
            switch (form) {
                case OreSurveyChunk.NORMAL -> normal++;
                case OreSurveyChunk.POOR -> poor++;
                case OreSurveyChunk.SEAM -> seams++;
                case OreSurveyChunk.BEDROCK, OreSurveyChunk.BEDROCK_SMALL -> {
                    bedrock++;
                    basal.set(chunk);
                    return;
                }
                default -> throw new IllegalArgumentException("Unknown ore form: " + form);
            }
            mineable.set(chunk);
            if (form != OreSurveyChunk.POOR) full.set(chunk);
        }
    }

    private static void survey(Path directory, long seed, Area area) throws IOException {
        var catalog = WorldgenSampler.materialCatalog();
        var ores = GTMaterialRegistry.sortedMaterials().stream()
                .map(GTMaterial::resolve)
                .distinct()
                .filter(material -> !sources(catalog, material).isEmpty())
                .sorted(java.util.Comparator.comparing(GTMaterial::getName))
                .toList();
        Map<GTMaterial, Mineral> minerals = new HashMap<>();
        ores.forEach(material -> minerals.put(material, new Mineral()));
        var layers = StoneLayerField.layers();
        var seams = layers.stream()
                .map(layer -> layer.isOreSeam() ? GTMaterialRegistry.get(layer.material()) : null)
                .toArray(GTMaterial[]::new);
        var volumes = new LayerVolumes(layers.size(), BOTTOM + 1, area.top());
        var field = new StoneLayerField(seed);
        var chunk = new OreSurveyChunk(seed, area.top(), seams);
        var smallOres = catalog.smallOres(DIMENSION);
        var bedrock = catalog.bedrockOres(DIMENSION, GEOLOGY, new HeightRange(BOTTOM, area.top()), source -> true);
        long start = System.nanoTime();
        var bodies = veinBodies(catalog, seed, area);
        for (int tileX = 0; tileX < area.side(); tileX += TILE_CHUNKS) {
            for (int tileZ = 0; tileZ < area.side(); tileZ += TILE_CHUNKS) {
                int width = Math.min(area.side() - tileX, TILE_CHUNKS);
                int depth = Math.min(area.side() - tileZ, TILE_CHUNKS);
                var rocks = field.sample(
                        (area.x() + tileX) << 4,
                        (area.z() + tileZ) << 4,
                        width << 4,
                        depth << 4,
                        BOTTOM + 1,
                        area.top());
                for (int dx = tileX; dx < tileX + width; dx++) {
                    for (int dz = tileZ; dz < tileZ + depth; dz++) {
                        int index = area.chunkIndex(dx, dz);
                        chunk.generate(
                                area.x() + dx, area.z() + dz, rocks, volumes, bodies.get(index), smallOres, bedrock);
                        for (int block = 0; block < chunk.minerals.length; block++) {
                            var material = chunk.minerals[block];
                            if (material != null) minerals.get(material).add(index, chunk.forms[block]);
                        }
                    }
                }
            }
        }
        double ms = (System.nanoTime() - start) / 1_000_000.0;
        writeMinerals(directory, catalog, ores, minerals, area);
        writeLayers(directory, volumes, seams);
        writeLayerHosts(directory, volumes);
        Files.writeString(
                directory.resolve("survey.properties"),
                String.format(
                        Locale.ROOT,
                        "source=synthetic_overworld%nseed=%d%norigin_x=%d%norigin_z=%d%nside=%d%n"
                                + "bottom_y=%d%nsurface_y=%d%ndistance_margin=%d%nchunks=%d%nsampling_ms=%.3f%nms_per_chunk=%.4f%n",
                        seed,
                        area.x(),
                        area.z(),
                        area.side(),
                        BOTTOM,
                        area.top(),
                        area.margin(),
                        area.size(),
                        ms,
                        ms / area.size()));
        System.out.printf(
                Locale.ROOT,
                "Simulated %,d chunks and %d minerals in %.0f ms (%.3f ms/chunk)%n",
                area.size(),
                minerals.size(),
                ms,
                ms / area.size());
    }

    private static List<String> sources(OreSourceCatalog catalog, GTMaterial material) {
        return catalog.sources(material).stream()
                .filter(source -> source.dimension().equals(DIMENSION)
                        && source.enabled(GEOLOGY)
                        && !source.blockId().startsWith("minecraft:infested_"))
                .map(source -> source.kind().name().toLowerCase(Locale.ROOT))
                .distinct()
                .sorted()
                .toList();
    }

    /** Counts hosts before ore rolls, without changing or advancing generation's random sequence. */
    static final class LayerVolumes {
        final long[] totals;
        private final int bottom;
        private final long[][] hosts;
        private final long[][] interiors;

        LayerVolumes(int layers, int bottom, int top) {
            this.bottom = bottom;
            totals = new long[layers];
            hosts = new long[layers][top - bottom + 1];
            interiors = new long[layers][top - bottom + 1];
        }

        void record(int layer, int y, int contactDistance) {
            totals[layer]++;
            hosts[layer][y - bottom]++;
            if (contactDistance > StoneLayerField.CORE_DEPTH) interiors[layer][y - bottom]++;
        }

        long count(int layer, int minY, int maxY, boolean interior) {
            var heights = (interior ? interiors : hosts)[layer];
            long count = 0;
            for (int y = Math.max(0, minY - bottom); y <= Math.min(heights.length - 1, maxY - bottom); y++) {
                count += heights[y];
            }
            return count;
        }

        long eligible(int layer, List<StratumOreRules.Ore> ores) {
            long count = 0;
            var heights = hosts[layer];
            for (int offset = 0; offset < heights.length; offset++) {
                int y = bottom + offset;
                for (var ore : ores) {
                    if (y >= ore.minY() && y <= ore.maxY()) {
                        count += heights[offset];
                        break;
                    }
                }
            }
            return count;
        }
    }

    private static void writeLayerHosts(Path directory, LayerVolumes volumes) throws IOException {
        try (var out = Files.newBufferedWriter(directory.resolve("layer-ore-hosts.csv"))) {
            out.write(
                    "layer_index,rock,material,min_y,max_y,chance_div,layer_volume,eligible_host_blocks,interior_host_blocks\n");
            for (int layer = 0; layer < volumes.totals.length; layer++) {
                for (var ore : StratumOreRules.layerOres(layer)) {
                    if (!ore.blockId().isEmpty()) continue;
                    out.write(String.format(
                            Locale.ROOT,
                            "%d,%s,%s,%d,%d,%d,%d,%d,%d%n",
                            layer,
                            StoneLayerField.layers().get(layer).material(),
                            ore.material().getName(),
                            ore.minY(),
                            ore.maxY(),
                            ore.chanceDiv(),
                            volumes.totals[layer],
                            volumes.count(layer, ore.minY(), ore.maxY(), false),
                            volumes.count(layer, ore.minY(), ore.maxY(), true)));
                }
            }
        }
    }

    private static List<List<Body>> veinBodies(OreSourceCatalog catalog, long seed, Area area) {
        var chunks = new ArrayList<List<Body>>(area.size());
        for (int i = 0; i < area.size(); i++) chunks.add(new ArrayList<>());
        int reach = OreVeinLayout.MAX_RADIUS;
        var terrain = new HeightRange(BOTTOM + 1, area.top());
        var rock = List.of(terrain);
        for (var plan : catalog.regionalPlans(DIMENSION)) {
            int region = plan.cells() * OreVeinLayout.CELL_SIZE;
            int minX = (int) Math.floorDiv(16L * area.x() - reach, region);
            int minZ = (int) Math.floorDiv(16L * area.z() - reach, region);
            int maxX = (int) Math.floorDiv(16L * (area.x() + area.side()) - 1 + reach, region);
            int maxZ = (int) Math.floorDiv(16L * (area.z() + area.side()) - 1 + reach, region);
            for (int rx = minX; rx <= maxX; rx++) {
                for (int rz = minZ; rz <= maxZ; rz++) {
                    for (var center : plan.centers(seed, rx, rz)) {
                        var band = center.vein().heights(DIMENSION).intersect(terrain);
                        var body = center.guaranteed()
                                ? center.fit(center.x(), center.z(), band, rock, false)
                                : center.body(band);
                        if (body == null && center.guaranteed()) {
                            var reserve = GTOreVeins.reserveHeights(
                                    DIMENSION, center.vein().name(), terrain);
                            if (reserve.isPresent())
                                body = center.fit(center.x(), center.z(), reserve.get(), rock, true);
                        }
                        if (body != null) addBody(chunks, area, body);
                    }
                }
            }
        }
        return chunks;
    }

    private static void addBody(List<List<Body>> chunks, Area area, Body body) {
        var center = body.center();
        int minX = Math.max(0, ((center.x() - body.radius()) >> 4) - area.x());
        int maxX = Math.min(area.side() - 1, ((center.x() + body.radius()) >> 4) - area.x());
        int minZ = Math.max(0, ((center.z() - body.radius()) >> 4) - area.z());
        int maxZ = Math.min(area.side() - 1, ((center.z() + body.radius()) >> 4) - area.z());
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++) chunks.get(area.chunkIndex(x, z)).add(body);
    }

    private static void writeMinerals(
            Path directory,
            OreSourceCatalog catalog,
            List<GTMaterial> ores,
            Map<GTMaterial, Mineral> minerals,
            Area area)
            throws IOException {
        try (var out = Files.newBufferedWriter(directory.resolve("mineral-distribution.csv"))) {
            out.write("material,sources,normal_blocks,poor_blocks,seam_blocks,bedrock_blocks,total_blocks,"
                    + "blocks_per_chunk,mineable_chunks,full_chunks,bedrock_chunks,mineable_chunk_percent");
            for (String kind : List.of("mineable", "full", "bedrock")) {
                for (String statistic : List.of("mean", "p95", "p99", "max", "edge_sensitive"))
                    out.write("," + kind + "_" + statistic);
            }
            out.newLine();
            for (var material : ores) {
                var ore = minerals.get(material);
                long total = ore.normal + ore.poor + ore.seams + ore.bedrock;
                out.write(String.format(
                        Locale.ROOT,
                        "%s,%s,%d,%d,%d,%d,%d,%.4f,%d,%d,%d,%.4f",
                        material.getName(),
                        String.join("|", sources(catalog, material)),
                        ore.normal,
                        ore.poor,
                        ore.seams,
                        ore.bedrock,
                        total,
                        total / (double) area.size(),
                        ore.mineable.cardinality(),
                        ore.full.cardinality(),
                        ore.basal.cardinality(),
                        100.0 * ore.mineable.cardinality() / area.size()));
                for (BitSet targets : List.of(ore.mineable, ore.full, ore.basal)) {
                    out.write(WorldgenComparison.distanceFields(
                            WorldgenComparison.distances(targets, area.side(), area.margin())));
                }
                out.newLine();
            }
        }
    }

    private static void writeLayers(Path directory, LayerVolumes volumes, GTMaterial[] seams) throws IOException {
        var shares = new TreeMap<String, long[]>();
        long total = 0, empty = 0, emptyAtHeight = 0;
        var layers = StoneLayerField.layers();
        for (int index = 0; index < volumes.totals.length; index++) {
            long count = volumes.totals[index];
            if (count == 0) continue;
            var values = shares.computeIfAbsent(layers.get(index).material(), ignored -> new long[3]);
            values[0] += count;
            total += count;
            if (seams[index] != null) continue;
            var ores = StratumOreRules.layerOres(index).stream()
                    .filter(ore ->
                            !disabledVanillaOre(ore.blockId()) && !ore.blockId().startsWith("minecraft:infested_"))
                    .toList();
            if (ores.isEmpty()) {
                values[1] += count;
                empty += count;
            }
            long unavailable = count - volumes.eligible(index, ores);
            values[2] += unavailable;
            emptyAtHeight += unavailable;
        }
        try (var out = Files.newBufferedWriter(directory.resolve("layer-distribution.csv"))) {
            out.write("material,layer_volume,volume_share_percent,empty_catalog_blocks,empty_at_height_blocks\n");
            for (var entry : shares.entrySet()) {
                var values = entry.getValue();
                out.write(String.format(
                        Locale.ROOT,
                        "%s,%d,%.4f,%d,%d%n",
                        entry.getKey(),
                        values[0],
                        100.0 * values[0] / total,
                        values[1],
                        values[2]));
            }
        }
        Files.writeString(
                directory.resolve("layer-totals.csv"),
                String.format(
                        Locale.ROOT,
                        "layer_volume,empty_catalog_blocks,empty_catalog_percent,empty_at_height_blocks,empty_at_height_percent%n"
                                + "%d,%d,%.4f,%d,%.4f%n",
                        total,
                        empty,
                        100.0 * empty / total,
                        emptyAtHeight,
                        100.0 * emptyAtHeight / total));
    }

    static boolean disabledVanillaOre(String id) {
        return id.startsWith("minecraft:") && id.endsWith("_ore");
    }
}
