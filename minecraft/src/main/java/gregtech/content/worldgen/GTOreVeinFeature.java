package gregtech.content.worldgen;

import gregtech.GregTechConfig;
import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.api.material.GTMaterial;
import gregtech.content.block.OreBlock;
import gregtech.content.worldgen.GTOreVeins.HeightRange;
import gregtech.content.worldgen.GTOreVeins.OreVein;
import gregtech.content.worldgen.OreVeinLayout.RegionalPlan;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Consumes the portion of the planned mineral bodies belonging to this chunk. */
public final class GTOreVeinFeature extends Feature<NoneFeatureConfiguration> {
    public GTOreVeinFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!GregTechConfig.Worldgen.GEOLOGY.enabled()) return false;
        WorldGenLevel level = context.level();
        ChunkPos chunkPos = new ChunkPos(context.origin());
        ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z);
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        String dimension = level.getLevel().dimension().location().toString();
        var catalog = OreSourceCatalog.current();
        var veins = catalog.veins(dimension);
        if (veins.isEmpty()) return false;
        var terrain = OreGenerationSummary.terrainHeights(level, chunkGenerator);
        boolean placed =
                OreVeinDeclaration.END.equals(dimension) && placeWeighted(level, chunk, dimension, veins, terrain);
        for (var plan : catalog.regionalPlans(dimension)) {
            placed |= placeRegionVeins(level, chunk, chunkGenerator, plan, terrain);
        }
        return placed;
    }

    private static boolean placeWeighted(
            WorldGenLevel level, ChunkAccess chunk, String dimension, List<OreVein> veins, HeightRange terrain) {
        ChunkPos chunkPos = chunk.getPos();
        int weight = GTOreVeins.selectionWeight(dimension, veins);
        int cellSize = OreVeinLayout.CELL_SIZE;
        int radius = OreVeinLayout.MAX_RADIUS;
        boolean placed = false;
        for (int cellX = Math.floorDiv(chunkPos.getMinBlockX() - radius, cellSize);
                cellX <= Math.floorDiv(chunkPos.getMaxBlockX() + radius, cellSize);
                cellX++) {
            for (int cellZ = Math.floorDiv(chunkPos.getMinBlockZ() - radius, cellSize);
                    cellZ <= Math.floorDiv(chunkPos.getMaxBlockZ() + radius, cellSize);
                    cellZ++) {
                var center = OreVeinLayout.weightedCenter(level.getSeed(), cellX, cellZ, veins, weight);
                if (center == null) continue;
                var range = center.vein().heights(dimension);
                var body = center.body(range.intersect(terrain));
                if (body != null && touches(body, chunkPos)) placed |= placeBody(level, chunk, body);
            }
        }
        return placed;
    }

    private static boolean placeRegionVeins(
            WorldGenLevel level, ChunkAccess chunk, ChunkGenerator generator, RegionalPlan plan, HeightRange bounds) {
        ChunkPos pos = chunk.getPos();
        int side = plan.cells() * OreVeinLayout.CELL_SIZE;
        int radius = OreVeinLayout.MAX_RADIUS;
        boolean placed = false;
        for (int x = Math.floorDiv(pos.getMinBlockX() - radius, side);
                x <= Math.floorDiv(pos.getMaxBlockX() + radius, side);
                x++) {
            for (int z = Math.floorDiv(pos.getMinBlockZ() - radius, side);
                    z <= Math.floorDiv(pos.getMaxBlockZ() + radius, side);
                    z++) {
                var region = VeinTerrain.region(level, plan, bounds, x, z);
                for (var center : region.centers()) {
                    var body = region.body(level, generator, center);
                    if (body != null && touches(body, pos)) placed |= placeBody(level, chunk, body);
                }
            }
        }
        return placed;
    }

    private static boolean touches(OreVeinLayout.Body body, ChunkPos chunk) {
        var center = body.center();
        int radius = body.radius();
        return center.x() + radius >= chunk.getMinBlockX()
                && center.x() - radius <= chunk.getMaxBlockX()
                && center.z() + radius >= chunk.getMinBlockZ()
                && center.z() - radius <= chunk.getMaxBlockZ();
    }

    private static boolean placeBody(WorldGenLevel level, ChunkAccess chunk, OreVeinLayout.Body body) {
        var center = body.center();
        ChunkPos pos = chunk.getPos();
        int r = body.radius();
        int minX = Math.max(pos.getMinBlockX(), center.x() - r);
        int minZ = Math.max(pos.getMinBlockZ(), center.z() - r);
        int maxX = Math.min(pos.getMaxBlockX(), center.x() + r);
        int maxZ = Math.min(pos.getMaxBlockZ(), center.z() + r);
        var cursor = new BlockPos.MutableBlockPos();
        boolean centerChunk = center.guaranteed() && pos.x == (center.x() >> 4) && pos.z == (center.z() >> 4);
        var indicated = new HashSet<GTMaterial>();
        boolean placed = false;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                var column = body.column(x, z);
                if (column == null) continue;
                boolean mayMark = centerChunk
                        || center.guaranteed() && indicated.isEmpty()
                        || WorldgenHash.roll(WorldgenHash.at(center.seed() ^ 0xC13FA9A902A6328FL, x, z), 128) == 0;
                for (int y = body.minY(); y <= body.maxY(); y++) {
                    var sample = column.sample(y);
                    if (sample == null) continue;
                    GTMaterial material = sample.role().material(center.vein()).resolve();
                    cursor.set(x, y, z);
                    if (!placeOre(level, chunk, cursor, chunk.getBlockState(cursor), material, sample.small()))
                        continue;
                    placed = true;
                    if (mayMark && (!centerChunk || !sample.small()) && !indicated.contains(material)) {
                        mayMark = false;
                        if (GTRockPlacement.placeVeinMarker(level, chunk, x, z, material)) indicated.add(material);
                    }
                }
            }
        }
        return placed;
    }

    private static boolean placeOre(
            WorldGenLevel level, ChunkAccess chunk, BlockPos pos, BlockState host, GTMaterial material, boolean small) {
        GTOreBlockResolver.OrePlacement placement;
        if (host.getBlock() instanceof OreBlock ore) {
            if (ore.material().resolve() != material || OreBlock.isBedrockOre(host) || OreBlock.isBroken(host))
                return false;
            if (small || !ore.isSmall()) return true;
            placement = GTOreBlockResolver.resolve(OreBlock.stoneOf(host), material, false);
        } else {
            placement = GTOreBlockResolver.resolve(host, material, small);
        }
        return placement != null && GTOreBlockResolver.setInChunk(level, chunk, pos, placement.state());
    }
}
