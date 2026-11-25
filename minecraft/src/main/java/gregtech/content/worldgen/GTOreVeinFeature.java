package gregtech.content.worldgen;

import gregtech.GregTechConfig;
import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.api.material.GTMaterial;
import gregtech.content.block.OreBlock;
import gregtech.content.worldgen.GTOreVeins.HeightRange;
import gregtech.content.worldgen.GTOreVeins.OreVein;
import gregtech.content.worldgen.OreVeinLayout.RegionalPlan;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
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
        boolean indicators = GregTechConfig.Worldgen.SURFACE_ROCKS.enabled();
        return visitPlans(level, context.chunkGenerator(), chunkPos, body -> placeBody(level, chunk, body, indicators));
    }

    /** Visits every planned body whose bounds touch the chunk. Returns whether any visitor call succeeds. */
    static boolean visitPlans(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            ChunkPos chunkPos,
            Predicate<OreVeinLayout.Body> visitor) {
        String dimension = level.getLevel().dimension().location().toString();
        var catalog = OreSourceCatalog.current();
        var veins = catalog.veins(dimension);
        if (veins.isEmpty()) return false;
        var terrain = OreGenerationSummary.terrainHeights(level, chunkGenerator);
        boolean placed = OreVeinDeclaration.END.equals(dimension)
                && visitWeighted(
                        level.getSeed(),
                        chunkPos,
                        dimension,
                        veins,
                        GTOreVeins.selectionWeight(dimension, veins),
                        terrain,
                        visitor);
        for (var plan : catalog.regionalPlans(dimension)) {
            placed |= visitRegions(level, chunkPos, chunkGenerator, dimension, plan, terrain, visitor);
        }
        return placed;
    }

    private static boolean visitWeighted(
            long seed,
            ChunkPos chunkPos,
            String dimension,
            List<OreVein> veins,
            int weight,
            HeightRange terrain,
            Predicate<OreVeinLayout.Body> visitor) {
        int cellSize = OreVeinLayout.CELL_SIZE;
        int radius = OreVeinLayout.MAX_RADIUS;
        boolean placed = false;
        for (int cellX = Math.floorDiv(chunkPos.getMinBlockX() - radius, cellSize);
                cellX <= Math.floorDiv(chunkPos.getMaxBlockX() + radius, cellSize);
                cellX++) {
            for (int cellZ = Math.floorDiv(chunkPos.getMinBlockZ() - radius, cellSize);
                    cellZ <= Math.floorDiv(chunkPos.getMaxBlockZ() + radius, cellSize);
                    cellZ++) {
                var center = OreVeinLayout.weightedCenter(seed, cellX, cellZ, veins, weight);
                if (center == null) continue;
                var range = center.vein().heights(dimension);
                var body = center.body(range.min(), range.max(), terrain.min(), terrain.max());
                if (body != null && touches(body, chunkPos)) placed |= visitor.test(body);
            }
        }
        return placed;
    }

    private static boolean visitRegions(
            WorldGenLevel level,
            ChunkPos pos,
            ChunkGenerator generator,
            String dimension,
            RegionalPlan plan,
            HeightRange bounds,
            Predicate<OreVeinLayout.Body> visitor) {
        int side = plan.cells() * OreVeinLayout.CELL_SIZE;
        int radius = OreVeinLayout.MAX_RADIUS;
        boolean placed = false;
        for (int x = Math.floorDiv(pos.getMinBlockX() - radius, side);
                x <= Math.floorDiv(pos.getMaxBlockX() + radius, side);
                x++) {
            for (int z = Math.floorDiv(pos.getMinBlockZ() - radius, side);
                    z <= Math.floorDiv(pos.getMaxBlockZ() + radius, side);
                    z++) {
                for (var body : VeinTerrain.candidates(level, generator, dimension, plan, bounds, x, z, pos)) {
                    if (touches(body, pos)) placed |= visitor.test(body);
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

    private static boolean placeBody(
            WorldGenLevel level, ChunkAccess chunk, OreVeinLayout.Body body, boolean indicators) {
        var center = body.center();
        ChunkPos pos = chunk.getPos();
        int r = body.radius();
        int minX = Math.max(pos.getMinBlockX(), center.x() - r);
        int minZ = Math.max(pos.getMinBlockZ(), center.z() - r);
        int maxX = Math.min(pos.getMaxBlockX(), center.x() + r);
        int maxZ = Math.min(pos.getMaxBlockZ(), center.z() + r);
        var cursor = new BlockPos.MutableBlockPos();
        GTMaterial primary = center.vein().top().resolve();
        boolean regional = center.guaranteed();
        boolean centerChunk = regional && pos.x == (center.x() >> 4) && pos.z == (center.z() >> 4);
        Map<GTMaterial, BlockPos> cores = centerChunk ? new HashMap<>(4) : null;
        boolean indicator = false;
        boolean placed = false;
        // Overworld-like deposits may be truncated by terrain or caves. Retry within this chunk's legal hosts.
        for (int pass = 0; pass < (regional ? 2 : 1); pass++) {
            boolean shift = pass == 1;
            if (shift && placed) break;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    var column = body.column(x, z);
                    if (column == null) continue;
                    int offset = shift ? recoveryOffset(chunk, body, column, cursor.set(x, 0, z)) : 0;
                    if (offset == Integer.MIN_VALUE) continue;
                    GTMaterial lastOre = placeColumn(level, chunk, column, cursor.set(x, 0, z), offset, primary, cores);
                    if (lastOre == null) continue;
                    placed = true;
                    if (indicators && !centerChunk && (regional && !indicator || indicatorRoll(center, x, z))) {
                        indicator |= GTRockPlacement.placeVeinMarker(level, chunk, x, z, lastOre);
                    }
                }
            }
        }
        if (cores != null) {
            // The valuable minerals can be in any layer of a mixed deposit, including the dispersed role.
            var vein = center.vein();
            var handled = new HashSet<GTMaterial>(4);
            for (GTMaterial role : List.of(vein.top(), vein.between(), vein.bottom(), vein.spread())) {
                GTMaterial material = role.resolve();
                if (!handled.add(material)) continue;
                BlockPos core = cores.get(material);
                if (core == null) {
                    core = recoverCore(level, chunk, body, material, cursor);
                    placed |= core != null;
                }
                if (indicators && core != null) placeNearbyMarker(level, chunk, core, material);
            }
        }
        return placed;
    }

    private static void placeNearbyMarker(
            WorldGenLevel level, ChunkAccess chunk, BlockPos anchor, GTMaterial material) {
        // Search neighboring columns without writing into another chunk.
        for (BlockPos candidate : BlockPos.spiralAround(anchor, 15, Direction.EAST, Direction.SOUTH)) {
            if (inChunk(chunk.getPos(), candidate)
                    && GTRockPlacement.placeVeinMarker(level, chunk, candidate.getX(), candidate.getZ(), material))
                return;
        }
    }

    private static boolean indicatorRoll(OreVeinLayout.Center center, int x, int z) {
        return WorldgenHash.roll(WorldgenHash.at(center.seed() ^ 0xC13FA9A902A6328FL, x, z), 128) == 0;
    }

    private static GTMaterial placeColumn(
            WorldGenLevel level,
            ChunkAccess chunk,
            OreVeinLayout.Column column,
            BlockPos.MutableBlockPos cursor,
            int offset,
            GTMaterial primary,
            Map<GTMaterial, BlockPos> cores) {
        var body = column.body();
        var vein = body.center().vein();
        GTMaterial lastOre = null;
        int minY = Math.max(body.minY(), body.placementMinY() - offset);
        int maxY = Math.min(body.maxY(), body.placementMaxY() - offset);
        for (int y = minY; y <= maxY; y++) {
            var sample = column.sample(y);
            if (sample == null) continue;
            GTMaterial material = sample.role().material(vein).resolve();
            cursor.setY(y + offset);
            var host = chunk.getBlockState(cursor);
            if (!placeOre(level, chunk, cursor, host, material, sample.small())) continue;
            if (cores != null
                    && (!sample.small() || host.getBlock() instanceof OreBlock ore && !ore.isSmall())
                    && !cores.containsKey(material)) {
                cores.put(material, cursor.immutable());
            }
            if (lastOre != primary) lastOre = material;
        }
        return lastOre;
    }

    private static int recoveryOffset(
            ChunkAccess chunk, OreVeinLayout.Body body, OreVeinLayout.Column column, BlockPos.MutableBlockPos cursor) {
        int surface = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, cursor.getX() & 15, cursor.getZ() & 15);
        int bestSampleY = Integer.MIN_VALUE;
        for (int distance = 0; distance <= body.halfHeight(); distance++) {
            if (column.sample(body.centerY() - distance) != null) {
                bestSampleY = body.centerY() - distance;
                break;
            }
            if (distance == 0 || column.sample(body.centerY() + distance) == null) continue;
            bestSampleY = body.centerY() + distance;
            break;
        }
        if (bestSampleY == Integer.MIN_VALUE) return Integer.MIN_VALUE;
        int maxY = Math.min(surface, body.placementMaxY());
        int minY = body.placementMinY();
        if (maxY < minY) return Integer.MIN_VALUE;
        int anchor = Math.max(minY, Math.min(maxY, body.centerY()));
        // Look around the scheduled center first, then towards deeper natural rock.
        for (int radius = 0; radius <= Math.max(maxY - anchor, anchor - minY); radius++) {
            if (anchor - radius >= minY
                    && GTOreBlockResolver.oreHost(chunk.getBlockState(cursor.setY(anchor - radius))) != null)
                return anchor - radius - bestSampleY;
            if (radius > 0
                    && anchor + radius <= maxY
                    && GTOreBlockResolver.oreHost(chunk.getBlockState(cursor.setY(anchor + radius))) != null)
                return anchor + radius - bestSampleY;
        }
        return Integer.MIN_VALUE;
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

    private static BlockPos recoverCore(
            WorldGenLevel level,
            ChunkAccess chunk,
            OreVeinLayout.Body body,
            GTMaterial material,
            BlockPos.MutableBlockPos cursor) {
        var center = body.center();
        var anchor = new BlockPos(center.x(), body.centerY(), center.z());
        for (BlockPos column : BlockPos.spiralAround(anchor, 15, Direction.EAST, Direction.SOUTH)) {
            if (!inChunk(chunk.getPos(), column)) continue;
            var core = recoverCoreInColumn(level, chunk, body, material, cursor.set(column));
            if (core != null) {
                growCore(level, chunk, body, material, core);
                return core;
            }
        }
        return null;
    }

    private static BlockPos recoverCoreInColumn(
            WorldGenLevel level,
            ChunkAccess chunk,
            OreVeinLayout.Body body,
            GTMaterial material,
            BlockPos.MutableBlockPos cursor) {
        int surface = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, cursor.getX() & 15, cursor.getZ() & 15);
        int min = body.placementMinY();
        int max = Math.min(surface, body.placementMaxY());
        if (max < min) return null;
        int start = Math.max(min, Math.min(max, body.centerY()));
        for (int depth = 0; depth <= Math.max(start - min, max - start); depth++) {
            int low = start - depth;
            if (low >= min && placeOre(level, chunk, cursor.setY(low), chunk.getBlockState(cursor), material, false))
                return cursor.immutable();
            int high = start + depth;
            if (depth > 0
                    && high <= max
                    && placeOre(level, chunk, cursor.setY(high), chunk.getBlockState(cursor), material, false))
                return cursor.immutable();
        }
        return null;
    }

    private static boolean inChunk(ChunkPos chunk, BlockPos pos) {
        return chunk.x == (pos.getX() >> 4) && chunk.z == (pos.getZ() >> 4);
    }

    private static void growCore(
            WorldGenLevel level, ChunkAccess chunk, OreVeinLayout.Body body, GTMaterial material, BlockPos core) {
        for (var side : Direction.values()) {
            BlockPos next = core.relative(side);
            if (!inChunk(chunk.getPos(), next)
                    || next.getY() < body.placementMinY()
                    || next.getY() > body.placementMaxY()) continue;
            placeOre(level, chunk, next, chunk.getBlockState(next), material, false);
        }
    }
}
