package gregtech.content.worldgen;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.content.worldgen.GTOreVeins.HeightRange;
import gregtech.content.worldgen.OreVeinLayout.Body;
import gregtech.content.worldgen.OreVeinLayout.Center;
import gregtech.content.worldgen.OreVeinLayout.RegionalPlan;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** Fits each guaranteed vein to an uninterrupted band of host rock in its assigned region. */
final class VeinTerrain {
    private record Region(RegionalPlan plan, HeightRange heights, int x, int z) {}

    // The cached values contain only plans, rock intervals and coordinates. Worlds own their noise and generator.
    private static final Cache<ServerLevel, Cache<Region, VeinTerrain>> LEVELS =
            CacheBuilder.newBuilder().weakKeys().build();

    private final Region region;
    private final StoneLayerField strata;
    private final List<Center> centers;
    private final Map<Center, Body> bodies = new HashMap<>();
    private final Map<Long, List<HeightRange>> columns = new HashMap<>();

    private VeinTerrain(Region region, ServerLevel level) {
        this.region = region;
        centers = region.plan().centers(level.getSeed(), region.x(), region.z());
        var active = OreGenerationSummary.ordinarySources(
                level, level.getChunkSource().getGenerator());
        strata = active != null && active.strata() ? new StoneLayerField(level.getSeed()) : null;
    }

    static VeinTerrain region(WorldGenLevel level, RegionalPlan plan, HeightRange bounds, int x, int z) {
        var cache = LEVELS.asMap()
                .computeIfAbsent(
                        level.getLevel(),
                        ignored -> CacheBuilder.newBuilder().maximumSize(64).build());
        return cache.asMap()
                .computeIfAbsent(new Region(plan, bounds, x, z), key -> new VeinTerrain(key, level.getLevel()));
    }

    List<Center> centers() {
        return centers;
    }

    synchronized Body body(WorldGenLevel level, ChunkGenerator generator, Center center) {
        if (bodies.containsKey(center)) return bodies.get(center);
        String dimension = level.getLevel().dimension().location().toString();
        var range = center.vein().heights(dimension).intersect(region.heights());
        Body body = center.guaranteed() ? fit(level, generator, center, range, false) : center.body(range);
        if (body == null && center.guaranteed()) {
            var reserve = GTOreVeins.reserveHeights(dimension, center.vein().name(), region.heights());
            if (reserve.isPresent()) body = fit(level, generator, center, reserve.get(), true);
        }
        bodies.put(center, body);
        return body;
    }

    private Body fit(WorldGenLevel level, ChunkGenerator generator, Center center, HeightRange range, boolean reserve) {
        if (range.isEmpty()) return null;
        int side = region.plan().cells() * OreVeinLayout.CELL_SIZE;
        int minX = region.x() * side, minZ = region.z() * side;
        int endX = minX + side, endZ = minZ + side;
        if (center.vein().subregional()) {
            int half = region.plan().cells() / 2 * OreVeinLayout.CELL_SIZE;
            if (center.x() < minX + half) endX = minX + half;
            else minX += half;
            if (center.z() < minZ + half) endZ = minZ + half;
            else minZ += half;
        }

        boolean thinBand = range.max() - range.min() < 2;
        Body best = at(level, generator, center, center.x(), center.z(), range, reserve);
        // Three solid blocks can hold the mixed core. Still allow thinner bands on floating islands or in flat worlds.
        if (best != null && (thinBand || best.halfHeight() > 0)) return best;
        int spacing = OreVeinDeclaration.END.equals(
                        level.getLevel().dimension().location().toString())
                ? 16
                : OreVeinLayout.CELL_SIZE / 2;
        int firstX = Math.floorDiv(center.x() - minX, spacing) * spacing + minX + spacing / 2;
        int firstZ = Math.floorDiv(center.z() - minZ, spacing) * spacing + minZ + spacing / 2;
        int rings = Math.max(endX - minX, endZ - minZ) / spacing;
        for (var offset : BlockPos.spiralAround(BlockPos.ZERO, rings, Direction.EAST, Direction.SOUTH)) {
            int x = firstX + offset.getX() * spacing, z = firstZ + offset.getZ() * spacing;
            if (x < minX || x >= endX || z < minZ || z >= endZ || x == center.x() && z == center.z()) continue;
            Body candidate = at(level, generator, center, x, z, range, reserve);
            if (candidate == null) continue;
            if (thinBand || candidate.halfHeight() > 0) return candidate;
            if (best == null) best = candidate;
        }
        return best;
    }

    private Body at(
            WorldGenLevel level,
            ChunkGenerator generator,
            Center center,
            int x,
            int z,
            HeightRange range,
            boolean reserve) {
        var bands = columns.computeIfAbsent(ChunkPos.asLong(x, z), key -> rockBands(level, generator, x, z));
        return center.fit(x, z, range, bands, reserve);
    }

    private List<HeightRange> rockBands(WorldGenLevel level, ChunkGenerator generator, int x, int z) {
        var column = generator.getBaseColumn(
                x, z, level, level.getLevel().getChunkSource().randomState());
        var bands = new ArrayList<HeightRange>();
        int start = region.heights().min();
        int top = region.heights().max();
        int bottom = start;
        int[] layers = null;
        if (strata != null) {
            layers = new int[top - bottom + 1];
            strata.column(x, z).fill(bottom, layers, layers.length);
        }
        for (int y = bottom; y <= top; y++) {
            var host = column.getBlock(y);
            if (layers != null) host = GTStoneLayerFeature.layerHost(host, layers[y - bottom]);
            if (GTOreBlockResolver.oreHost(host) != null) continue;
            if (start < y) bands.add(new HeightRange(start, y - 1));
            start = y + 1;
        }
        if (start <= top) bands.add(new HeightRange(start, top));
        return bands;
    }
}
