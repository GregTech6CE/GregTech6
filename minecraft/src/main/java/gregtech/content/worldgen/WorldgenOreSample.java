package gregtech.content.worldgen;

import gregtech.api.material.GTMaterial;
import gregtech.content.block.DenseOreBlock;
import gregtech.content.block.OreBlock;
import gregtech.content.blockentity.RockBlockEntity;
import gregtech.engine.material.GTMaterialRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelResource;

/** Counts GT ores in loaded chunks for the in-game summary and the offline comparison tool. */
final class WorldgenOreSample {
    private final Map<String, MaterialCounts> materials = new TreeMap<>();
    private final List<ChunkPos> chunks = new ArrayList<>();

    void add(LevelChunk chunk) {
        int sample = chunks.size();
        chunks.add(chunk.getPos());
        var sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            var section = sections[i];
            if (section.hasOnlyAir() || !section.maybeHas(WorldgenOreSample::isOre)) continue;
            int baseY = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        GTMaterial material = GTOreBlockResolver.oreMaterialOf(state.getBlock());
                        if (material != null)
                            materials
                                    .computeIfAbsent(material.getName(), key -> new MaterialCounts())
                                    .add(state, baseY + y, sample);
                    }
                }
            }
        }
    }

    int chunks() {
        return chunks.size();
    }

    int ores() {
        return materials.values().stream()
                .mapToInt(counts -> counts.total() - counts.bedrock())
                .sum();
    }

    int bedrockOres() {
        return materials.values().stream().mapToInt(MaterialCounts::bedrock).sum();
    }

    Path write(ServerLevel level, ChunkPos center, int radius) throws IOException {
        Path save = level.getServer().getWorldPath(LevelResource.ROOT);
        Path reports = Files.createDirectories(save.resolve("gt6ce-worldgen"));
        Path report = Files.createTempFile(reports, "ore-sample-", ".csv");
        try (var writer = Files.newBufferedWriter(report)) {
            writer.write("dimension,center_chunk_x,center_chunk_z,radius_chunks,loaded_chunks,requested_chunks,"
                    + "material,normal,poor,bedrock_full,bedrock_poor,dense,other_gt_ore,total,"
                    + "chunks_with_ore,blocks_per_loaded_chunk,chunk_presence_percent,min_y,max_y,"
                    + "nearest_ore_chunk_mean,nearest_ore_chunk_p95,nearest_ore_chunk_p99,nearest_ore_chunk_max,"
                    + "chunks_with_normal_ore,nearest_normal_chunk_mean,nearest_normal_chunk_p95,"
                    + "nearest_normal_chunk_p99,nearest_normal_chunk_max\n");
            int diameter = radius * 2 + 1;
            String region = "%s,%d,%d,%d,%d,%d,"
                    .formatted(level.dimension().location(), center.x, center.z, radius, chunks(), diameter * diameter);
            for (var entry : materials.entrySet()) {
                var counts = entry.getValue();
                writer.write(region);
                writer.write(String.format(
                        Locale.ROOT,
                        "\"%s\",%d,%d,%d,%d,%d,%d,%d,%d,%.4f,%.2f,%d,%d",
                        entry.getKey().replace("\"", "\"\""),
                        counts.blocks.normal,
                        counts.blocks.poor,
                        counts.blocks.bedrockFull,
                        counts.blocks.bedrockPoor,
                        counts.blocks.dense,
                        counts.blocks.other,
                        counts.total(),
                        counts.oreChunks.size(),
                        counts.total() / (double) chunks(),
                        100.0 * counts.oreChunks.size() / chunks(),
                        counts.minY,
                        counts.maxY));
                writer.write(nearestDistances(counts.oreChunks));
                writer.write("," + counts.normalChunks.size());
                writer.write(nearestDistances(counts.normalChunks));
                writer.newLine();
            }
        }
        return save.relativize(report);
    }

    /** Samples live chunks on the server thread. The writer only sees counts and save metadata. */
    static final class Export {
        private final Path save;
        private final String dimension;
        private final long seed;
        final ChunkPos origin;
        final int side;
        private final List<Map<String, OreCounts>> chunks;
        private final Map<String, Long> rocks = new TreeMap<>();

        Export(ServerLevel level, ChunkPos origin, int side) {
            save = level.getServer().getWorldPath(LevelResource.ROOT);
            dimension = level.dimension().location().toString();
            seed = level.getSeed();
            this.origin = origin;
            this.side = side;
            chunks = new ArrayList<>(side * side);
        }

        int size() {
            return chunks.size();
        }

        ChunkPos position(int index) {
            return new ChunkPos(origin.x + index / side, origin.z + index % side);
        }

        void add(LevelChunk chunk) {
            var ores = new TreeMap<String, OreCounts>();
            for (var section : chunk.getSections()) {
                if (!section.hasOnlyAir())
                    section.getStates().count((state, amount) -> countBlocks(state, amount, ores, rocks));
            }
            for (var pos : chunk.getBlockEntitiesPos()) {
                if (!(chunk.getBlockEntity(pos) instanceof RockBlockEntity rock) || !rock.hasRawOre()) continue;
                GTMaterial material = GTMaterialRegistry.get(rock.getMaterial()).resolve();
                if (material.isValid()) ores.computeIfAbsent(material.getName(), key -> new OreCounts()).rawPebbles++;
            }
            chunks.add(ores);
        }

        Path write() throws IOException {
            Path reports = Files.createDirectories(save.resolve("gt6ce-worldgen"));
            Path report = Files.createTempDirectory(reports, "chunk-sample-");
            try (var writer = Files.newBufferedWriter(report.resolve("ores.csv"))) {
                writer.write("chunk_x,chunk_z,material,normal,poor,bedrock,raw_pebbles\n");
                for (int i = 0; i < chunks.size(); i++) {
                    ChunkPos position = position(i);
                    for (var entry : chunks.get(i).entrySet()) {
                        var counts = entry.getValue();
                        writer.write(String.format(
                                Locale.ROOT,
                                "%d,%d,%s,%d,%d,%d,%d%n",
                                position.x,
                                position.z,
                                entry.getKey(),
                                counts.full(),
                                counts.poor,
                                counts.bedrock(),
                                counts.rawPebbles));
                    }
                }
            }
            try (var writer = Files.newBufferedWriter(report.resolve("rocks.csv"))) {
                writer.write("block,volume\n");
                for (var entry : rocks.entrySet()) writer.write(entry.getKey() + "," + entry.getValue() + "\n");
            }
            var manifest = new Properties();
            manifest.setProperty("seed", Long.toString(seed));
            manifest.setProperty("dimension", dimension);
            manifest.setProperty("origin_x", Integer.toString(origin.x));
            manifest.setProperty("origin_z", Integer.toString(origin.z));
            manifest.setProperty("side", Integer.toString(side));
            Path unpublished = report.resolve("sample.properties.tmp");
            try (var writer = Files.newBufferedWriter(unpublished)) {
                manifest.store(writer, "GT6CE sample of loaded chunks");
            }
            Files.move(unpublished, report.resolve("sample.properties"));
            return save.relativize(report);
        }
    }

    private static void countBlocks(
            BlockState state, int amount, Map<String, OreCounts> ores, Map<String, Long> rocks) {
        if (amount == 0) return;
        Block block = state.getBlock();
        GTMaterial material = GTOreBlockResolver.oreMaterialOf(block);
        if (material != null) {
            ores.computeIfAbsent(material.resolve().getName(), key -> new OreCounts())
                    .add(state, amount);
        } else if (GTOreBlockResolver.isBaseRock(state)) {
            rocks.merge(BuiltInRegistries.BLOCK.getKey(block).toString(), (long) amount, Long::sum);
        }
    }

    private String nearestDistances(List<Integer> oreChunks) {
        if (oreChunks.isEmpty()) return ",,,,";
        var distances = new double[chunks()];
        for (int i = 0; i < distances.length; i++) {
            var origin = chunks.get(i);
            long nearest = Long.MAX_VALUE;
            for (int sample : oreChunks) {
                var target = chunks.get(sample);
                nearest =
                        Math.min(nearest, MineralWorldgenRules.distanceSquared(origin.x, origin.z, target.x, target.z));
                if (nearest == 0) break;
            }
            distances[i] = Math.sqrt(nearest);
        }
        Arrays.sort(distances);
        return String.format(
                Locale.ROOT,
                ",%.4f,%.4f,%.4f,%.4f",
                Arrays.stream(distances).average().orElseThrow(),
                percentile(distances, 0.95),
                percentile(distances, 0.99),
                distances[distances.length - 1]);
    }

    private static double percentile(double[] ascending, double fraction) {
        return ascending[(int) Math.ceil(fraction * ascending.length) - 1];
    }

    private static boolean isOre(BlockState state) {
        return GTOreBlockResolver.oreMaterialOf(state.getBlock()) != null;
    }

    private static final class OreCounts {
        private int normal, poor, bedrockFull, bedrockPoor, dense, other, rawPebbles;

        /** Counts the palette entry and reports whether it is a full, non-bedrock ore. */
        private boolean add(BlockState state, int amount) {
            if (state.getBlock() instanceof OreBlock ore) {
                if (OreBlock.isBedrockOre(state)) {
                    if (ore.isSmall()) bedrockPoor += amount;
                    else bedrockFull += amount;
                    return false;
                }
                if (ore.isSmall()) {
                    poor += amount;
                    return false;
                }
                normal += amount;
            } else if (state.getBlock() instanceof DenseOreBlock) {
                dense += amount;
            } else {
                other += amount;
            }
            return true;
        }

        private int full() {
            return normal + dense + other;
        }

        private int bedrock() {
            return bedrockFull + bedrockPoor;
        }
    }

    private static final class MaterialCounts {
        private final OreCounts blocks = new OreCounts();
        private int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        private final List<Integer> oreChunks = new ArrayList<>();
        private final List<Integer> normalChunks = new ArrayList<>();
        private int lastChunk = -1;
        private int lastNormalChunk = -1;

        private void add(BlockState state, int y, int chunk) {
            if (blocks.add(state, 1)) addNormalChunk(chunk);
            if (lastChunk != chunk) {
                oreChunks.add(chunk);
                lastChunk = chunk;
            }
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        private void addNormalChunk(int chunk) {
            if (lastNormalChunk == chunk) return;
            normalChunks.add(chunk);
            lastNormalChunk = chunk;
        }

        private int total() {
            return blocks.full() + blocks.poor + bedrock();
        }

        private int bedrock() {
            return blocks.bedrock();
        }
    }
}
