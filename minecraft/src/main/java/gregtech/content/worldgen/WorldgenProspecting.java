package gregtech.content.worldgen;

import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.content.worldgen.GTOreVeins.HeightRange;
import gregtech.content.worldgen.OreSourceCatalog.Kind;
import gregtech.content.worldgen.OreVeinLayout.Center;
import gregtech.content.worldgen.OreVeinLayout.RegionalPlan;
import gregtech.content.worldgen.WorldgenLocate.Area;
import gregtech.content.worldgen.WorldgenLocate.Target;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;

/** Samples the installed generation plans without reading or generating chunks. */
final class WorldgenProspecting {
    private WorldgenProspecting() {}

    static BlockPos find(ServerLevel level, OreGenerationSummary summary, Target target, Area area, Set<Long> checked) {
        String dim = level.dimension().location().toString();
        long seed = level.getSeed();
        if (target.kind() == WorldgenLocate.Kind.LAYER)
            return layer(level, summary.dimensions().get(dim).heights(), target, area, checked);
        if (target.kind() == WorldgenLocate.Kind.BEDROCK_ORE) return bedrock(seed, dim, summary, target, area, checked);
        var center = vein(level, dim, summary, target, area, checked);
        for (var source : target.sources()) {
            if (source.kind() == Kind.COLTAN_FIELD) {
                var p = MineralWorldgenRules.coltanCentre(seed);
                var band = summary.specialDeposits().get(source.kind());
                center = closer(center, new BlockPos(p.x(), (band.min() + band.max()) / 2, p.z()), area, checked);
            }
        }
        return center;
    }

    private static BlockPos bedrock(
            long seed, String dim, OreGenerationSummary summary, Target target, Area area, Set<Long> checked) {
        var table = summary.bedrock().get(dim);
        var names = new ArrayList<String>();
        BlockPos best = null;
        for (var source : target.sources()) {
            if (source.kind() == Kind.BEDROCK && table != null) names.add(source.name());
            if (source.kind() == Kind.COLTAN_BEDROCK) {
                var p = MineralWorldgenRules.coltanCentre(seed);
                int floor = summary.specialDeposits().get(source.kind()).min();
                best = closer(best, new BlockPos((p.x() & ~15) + 8, floor, (p.z() & ~15) + 8), area, checked);
            }
        }
        if (names.isEmpty()) return best;
        long salted = MineralWorldgenRules.bedrockSeed(seed, dim);
        var origin = new ChunkPos(area.origin());
        for (var offset : BlockPos.spiralAround(BlockPos.ZERO, area.radius(), Direction.EAST, Direction.SOUTH)) {
            int ring = Math.max(Math.abs(offset.getX()), Math.abs(offset.getZ()));
            int lowerBound = Math.max(0, ring * 16 - 8);
            if (best != null && horizontalDistance(best.getX(), best.getZ(), area) < (long) lowerBound * lowerBound)
                break;
            int cx = origin.x + offset.getX(), cz = origin.z + offset.getZ();
            if (checked.contains(ChunkPos.asLong(cx, cz))) continue;
            for (String name : names) {
                if (MineralWorldgenRules.bedrockHit(
                        salted, cx, cz, name, table.divisors().get(name), summary.bedrockDensity())) {
                    best = closer(best, new BlockPos((cx << 4) + 8, table.floor(), (cz << 4) + 8), area, checked);
                    break;
                }
            }
        }
        return best;
    }

    private record Site(Center center, RegionalPlan plan, int regionX, int regionZ, long distance) {}

    private static BlockPos vein(
            ServerLevel level, String dim, OreGenerationSummary summary, Target target, Area area, Set<Long> checked) {
        var types = new HashSet<String>();
        for (var source : target.sources()) if (source.kind() == Kind.VEIN) types.add(source.name());
        if (types.isEmpty()) return null;
        var terrain = summary.dimensions().get(dim).heights();
        var catalog = OreSourceCatalog.current();
        var regions = catalog.regionalPlans(dim);
        var veins = catalog.veins(dim);
        long seed = level.getSeed();
        int weight = GTOreVeins.selectionWeight(dim, veins);
        var sites = new ArrayList<Site>();
        for (int i = 0; i <= regions.size(); i++) {
            var plan = i == regions.size() ? null : regions.get(i);
            if (plan == null && !OreVeinDeclaration.END.equals(dim)) continue;
            int side = OreVeinLayout.CELL_SIZE * (plan == null ? 1 : plan.cells());
            for (int rx = Math.floorDiv(area.minX(), side); rx <= Math.floorDiv(area.maxX(), side); rx++) {
                for (int rz = Math.floorDiv(area.minZ(), side); rz <= Math.floorDiv(area.maxZ(), side); rz++) {
                    if (plan == null) {
                        addSite(
                                sites,
                                OreVeinLayout.weightedCenter(seed, rx, rz, veins, weight),
                                null,
                                rx,
                                rz,
                                types,
                                area);
                    } else {
                        for (var center : plan.centers(seed, rx, rz)) addSite(sites, center, plan, rx, rz, types, area);
                    }
                }
            }
        }
        sites.sort(Comparator.comparingLong(Site::distance));
        BlockPos best = null;
        for (var site : sites) {
            var center = site.center();
            var range = center.vein().heights(dim);
            var body = site.plan() == null
                    ? center.body(range.min(), range.max(), terrain.min(), terrain.max())
                    : VeinTerrain.locate(
                            level,
                            level.getChunkSource().getGenerator(),
                            dim,
                            site.plan(),
                            terrain,
                            site.regionX(),
                            site.regionZ(),
                            center);
            if (body != null)
                best = closer(
                        best,
                        new BlockPos(
                                body.center().x(), body.centerY(), body.center().z()),
                        area,
                        checked);
            if (best != null) break;
        }
        return best;
    }

    private static void addSite(
            List<Site> sites, Center center, RegionalPlan plan, int rx, int rz, Set<String> types, Area area) {
        if (center != null
                && types.contains(center.vein().name())
                && (plan != null || area.contains(center.x(), center.z())))
            sites.add(new Site(center, plan, rx, rz, horizontalDistance(center.x(), center.z(), area)));
    }

    private static BlockPos layer(ServerLevel level, HeightRange heights, Target target, Area area, Set<Long> checked) {
        var field = new StoneLayerField(level.getSeed());
        boolean[] matches = new boolean[StoneLayerField.layers().size()];
        for (int i = 0; i < matches.length; i++) {
            var layer = GTStoneLayerFeature.layerStone(i);
            matches[i] = layer == null
                    ? target.blocks().contains(Blocks.STONE) || target.blocks().contains(Blocks.DEEPSLATE)
                    : target.test(layer);
        }
        var center = new ChunkPos(area.origin());
        int[] column = new int[heights.max() - heights.min() + 1];
        int farStep = Math.max(1, (area.radius() + 31) / 32);
        for (int pass = 0; pass < 2; pass++) {
            int step = pass == 0 ? 1 : farStep;
            int rings = pass == 0 ? Math.min(area.radius(), 8) : area.radius() / step;
            for (var offset : BlockPos.spiralAround(BlockPos.ZERO, rings, Direction.EAST, Direction.SOUTH)) {
                int dx = offset.getX() * step, dz = offset.getZ() * step;
                if (pass == 1 && Math.max(Math.abs(dx), Math.abs(dz)) <= 8) continue;
                int x = ((center.x + dx) << 4) + 8, z = ((center.z + dz) << 4) + 8;
                if (!area.contains(x, z) || checked.contains(ChunkPos.asLong(x >> 4, z >> 4))) continue;
                field.column(x, z).fill(heights.min(), column, column.length);
                NoiseColumn terrain = null;
                BlockPos best = null;
                for (int i = 0; i < column.length; i++) {
                    if (!matches[column[i]]) continue;
                    if (terrain == null)
                        terrain = level.getChunkSource()
                                .getGenerator()
                                .getBaseColumn(
                                        x, z, level, level.getChunkSource().randomState());
                    int y = heights.min() + i;
                    var ground = terrain.getBlock(y);
                    if (!GTStoneLayerFeature.naturalLayerHost(ground)) continue;
                    if (GTStoneLayerFeature.layerStone(column[i]) == null && !target.test(ground)) continue;
                    if (best == null
                            || Math.abs(y - area.origin().getY())
                                    < Math.abs(best.getY() - area.origin().getY())) best = new BlockPos(x, y, z);
                }
                if (best != null) return best;
            }
        }
        return null;
    }

    private static BlockPos closer(BlockPos best, BlockPos pos, Area area, Set<Long> checked) {
        if (!area.contains(pos.getX(), pos.getZ()) || checked.contains(new ChunkPos(pos).toLong())) return best;
        return best == null
                        || horizontalDistance(pos.getX(), pos.getZ(), area)
                                < horizontalDistance(best.getX(), best.getZ(), area)
                ? pos
                : best;
    }

    private static long horizontalDistance(int x, int z, Area area) {
        long dx = (long) x - area.origin().getX(), dz = (long) z - area.origin().getZ();
        return dx * dx + dz * dz;
    }
}
