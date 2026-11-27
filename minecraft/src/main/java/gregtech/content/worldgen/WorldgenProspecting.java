package gregtech.content.worldgen;

import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.content.worldgen.GTOreVeins.HeightRange;
import gregtech.content.worldgen.OreSourceCatalog.Kind;
import gregtech.content.worldgen.OreVeinLayout.Center;
import gregtech.content.worldgen.OreVeinLayout.RegionalPlan;
import gregtech.content.worldgen.WorldgenLocate.Area;
import gregtech.content.worldgen.WorldgenLocate.Target;
import gregtech.content.worldgen.dungeon.GTDungeonLayout;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

/** Samples the installed generation plans without reading or generating chunks. */
final class WorldgenProspecting {
    private WorldgenProspecting() {}

    static BlockPos find(ServerLevel level, OreGenerationSummary summary, Target target, Area area, Set<Long> checked) {
        String dim = level.dimension().location().toString();
        long seed = level.getSeed();
        if (target.kind() == WorldgenLocate.Kind.LAYER) return sample(level, summary, target, area, checked);
        if (target.kind() == WorldgenLocate.Kind.BEDROCK_ORE)
            return bedrock(level, dim, summary, target, area, checked);
        var center = vein(level, dim, summary, target, area, checked);
        for (var source : target.sources()) {
            if (source.kind() == Kind.COLTAN_FIELD) {
                var p = MineralWorldgenRules.coltanCentre(seed);
                var band = summary.specialDeposits().get(source.kind());
                center = closer(center, new BlockPos(p.x(), (band.min() + band.max()) / 2, p.z()), area, checked);
            }
        }
        return center != null ? center : sample(level, summary, target, area, checked);
    }

    private static BlockPos bedrock(
            ServerLevel level, String dim, OreGenerationSummary summary, Target target, Area area, Set<Long> checked) {
        long seed = level.getSeed();
        var table = summary.bedrock().get(dim);
        var names = new ArrayList<String>();
        BlockPos best = null;
        boolean dungeon = false;
        for (var source : target.sources()) {
            if (source.kind() == Kind.BEDROCK && table != null) names.add(source.name());
            if (source.kind() == Kind.DUNGEON_BEDROCK) dungeon = true;
            if (source.kind() == Kind.COLTAN_BEDROCK) {
                var p = MineralWorldgenRules.coltanCentre(seed);
                int floor = summary.specialDeposits().get(source.kind()).min();
                best = closer(best, new BlockPos((p.x() & ~15) + 8, floor, (p.z() & ~15) + 8), area, checked);
            }
        }
        if (!names.isEmpty()) best = naturalBedrock(seed, dim, summary, area, checked, table, names, best);
        return dungeon ? dungeonMine(level, target, area, checked, best) : best;
    }

    private static BlockPos naturalBedrock(
            long seed,
            String dim,
            OreGenerationSummary summary,
            Area area,
            Set<Long> checked,
            OreGenerationSummary.BedrockTable table,
            List<String> names,
            BlockPos best) {
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

    /** Structure placement, room layout and ore selection can all be sampled without requesting a chunk. */
    private static BlockPos dungeonMine(ServerLevel level, Target target, Area area, Set<Long> checked, BlockPos best) {
        var state = level.getChunkSource().getGeneratorState();
        var biomeSource = level.getChunkSource().getGenerator().getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        var settings = GTDungeonLayout.settings();
        var origin = new ChunkPos(area.origin());
        int reach = area.radius() + settings.half();
        for (var holder : state.possibleStructureSets()) {
            var set = holder.value();
            var dungeons = set.structures().stream()
                    .map(entry -> entry.structure().value())
                    .filter(GTDungeonStructure.class::isInstance)
                    .toList();
            if (dungeons.isEmpty()) continue;
            var placement = set.placement();
            int step = placement instanceof GTDungeonPlacement
                    ? settings.period()
                    : placement instanceof RandomSpreadStructurePlacement spread ? spread.spacing() : 1;
            int rx = Math.floorDiv(origin.x, step), rz = Math.floorDiv(origin.z, step);
            for (var offset :
                    BlockPos.spiralAround(BlockPos.ZERO, (reach + step - 1) / step, Direction.EAST, Direction.SOUTH)) {
                int ring = Math.max(Math.abs(offset.getX()), Math.abs(offset.getZ()));
                long minDistance = Math.max(0, (ring - 1) * step - settings.half()) * 16L;
                if (best != null && minDistance * minDistance > horizontalDistance(best.getX(), best.getZ(), area))
                    break;
                int cx = (rx + offset.getX()) * step, cz = (rz + offset.getZ()) * step;
                var anchor = placement instanceof RandomSpreadStructurePlacement spread
                        ? spread.getPotentialStructureChunk(level.getSeed(), cx, cz)
                        : new ChunkPos(cx, cz);
                if (!GTDungeonLayout.passesAnchorClearance(anchor)) continue;
                long seed = GTDungeonLayout.seedFor(level.getSeed(), anchor);
                if (!GTDungeonLayout.passesProbability(seed) || !placement.isStructureChunk(state, anchor.x, anchor.z))
                    continue;
                var biome = biomeSource.getNoiseBiome(
                        anchor.getMiddleBlockX() >> 2,
                        level.getMinBuildHeight() >> 2,
                        anchor.getMiddleBlockZ() >> 2,
                        sampler);
                if (dungeons.stream().noneMatch(structure -> structure.biomes().contains(biome))) continue;
                var mine = GTDungeonGenerator.plannedBedrock(level, anchor, seed, target.material());
                if (mine != null) best = closer(best, mine, area, checked);
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
        var dimension = summary.dimensions().get(dim);
        var terrain = dimension.heights();
        var strata = dimension.strata() ? new StoneLayerField(level.getSeed()) : null;
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
                    ? center.body(range.intersect(terrain))
                    : VeinTerrain.region(level, site.plan(), terrain, site.regionX(), site.regionZ())
                            .body(level, level.getChunkSource().getGenerator(), center);
            if (body != null) best = veinOre(level, body, target, area, checked, strata, summary);
            if (best != null) break;
        }
        return best;
    }

    private static BlockPos veinOre(
            ServerLevel level,
            OreVeinLayout.Body body,
            Target target,
            Area area,
            Set<Long> checked,
            StoneLayerField strata,
            OreGenerationSummary summary) {
        var center = body.center();
        var anchor = new BlockPos(center.x(), body.centerY(), center.z());
        for (var pos : BlockPos.spiralAround(anchor, body.radius(), Direction.EAST, Direction.SOUTH)) {
            int x = pos.getX(), z = pos.getZ();
            if (!area.contains(x, z) || checked.contains(ChunkPos.asLong(x >> 4, z >> 4))) continue;
            var column = body.column(x, z);
            if (column == null) continue;
            NoiseColumn terrain = null;
            StoneLayerField.Samples layers = null;
            for (int y = body.minY(); y <= body.maxY(); y++) {
                var ore = column.sample(y);
                if (ore == null || ore.role().material(center.vein()).resolve() != target.material()) continue;
                if (terrain == null) terrain = terrain(level, x, z);
                if (strata != null && layers == null) layers = strata.sample(x, z, 1, 1, body.minY(), body.maxY());
                if (acceptsOre(level, layers, summary, target, terrain.getBlock(y), x, y, z))
                    return new BlockPos(x, y, z);
            }
        }
        return null;
    }

    private static boolean acceptsOre(
            ServerLevel level,
            StoneLayerField.Samples layers,
            OreGenerationSummary summary,
            Target target,
            BlockState ground,
            int x,
            int y,
            int z) {
        if (layers == null || !GTStoneLayerFeature.naturalLayerHost(ground))
            return GTOreBlockResolver.oreHost(ground) != null;
        int layer = layers.layerIndex(x, y, z);
        var background = GTStoneLayerFeature.layerHost(ground, layer);
        if (GTOreBlockResolver.oreHost(background) == null) return target.test(background);
        var contacts = new StoneLayerField.Contacts();
        layers.contacts(x, y, z, contacts);
        long seed = WorldgenHash.at(level.getSeed(), x, y, z);
        var ore = stratumOre(layer, y, contacts, seed, true, summary);
        if (ore != null) return ore.material() == target.material();
        if (contacts.nearest() != 1) return true;
        var gem = BoundaryGemOres.choose(seed, summary.boundaryGemDivisor());
        return gem == null || gem == target.material();
    }

    private static StratumOreRules.Ore stratumOre(
            int layer,
            int y,
            StoneLayerField.Contacts contacts,
            long seed,
            boolean host,
            OreGenerationSummary summary) {
        return StratumOreRules.place(
                layer,
                y,
                contacts,
                seed,
                (entry, small) -> entry.blockId().isEmpty()
                        ? host
                        : !summary.disabledStratumBlocks().contains(entry.blockId()));
    }

    private static NoiseColumn terrain(ServerLevel level, int x, int z) {
        var source = level.getChunkSource();
        return source.getGenerator().getBaseColumn(x, z, level, source.randomState());
    }

    private static void addSite(
            List<Site> sites, Center center, RegionalPlan plan, int rx, int rz, Set<String> types, Area area) {
        if (center != null
                && types.contains(center.vein().name())
                && (plan != null || area.contains(center.x(), center.z())))
            sites.add(new Site(center, plan, rx, rz, horizontalDistance(center.x(), center.z(), area)));
    }

    private static BlockPos sample(
            ServerLevel level, OreGenerationSummary summary, Target target, Area area, Set<Long> checked) {
        String dimension = level.dimension().location().toString();
        var active = summary.dimensions().get(dimension);
        if (active == null) return null;
        var strata = new Strata(level.getSeed(), active.heights(), summary, target);
        var smallOres = OreSourceCatalog.current().smallOres(dimension).stream()
                .filter(ore -> active.smallOres() && ore.material().resolve() == target.material())
                .toList();
        if (!strata.enabled() && smallOres.isEmpty()) return null;
        var center = new ChunkPos(area.origin());
        // Sample every nearby chunk, then spread the remaining columns across the requested radius.
        int farStep = Math.max(1, (area.radius() + 31) / 32);
        for (int pass = 0; pass < 2; pass++) {
            int step = pass == 0 ? 1 : farStep;
            int rings = pass == 0 ? Math.min(area.radius(), 8) : area.radius() / step;
            for (var offset : BlockPos.spiralAround(BlockPos.ZERO, rings, Direction.EAST, Direction.SOUTH)) {
                int dx = offset.getX() * step, dz = offset.getZ() * step;
                if (pass == 1 && Math.max(Math.abs(dx), Math.abs(dz)) <= 8) continue;
                int cx = center.x + dx, cz = center.z + dz;
                if (checked.contains(ChunkPos.asLong(cx, cz))) continue;
                int x = Math.max(area.minX(), Math.min(area.maxX(), (cx << 4) + 8));
                int z = Math.max(area.minZ(), Math.min(area.maxZ(), (cz << 4) + 8));
                if ((x >> 4) != cx || (z >> 4) != cz || !area.contains(x, z)) continue;
                BlockPos hit = strata.enabled()
                        ? strata.find(level, target, area.origin().getY(), x, z)
                        : null;
                if (hit != null) return hit;
                for (var ore : smallOres) {
                    var found = new BlockPos.MutableBlockPos();
                    if (ore.scatter(
                            level.getSeed(),
                            dimension,
                            cx << 4,
                            cz << 4,
                            active.heights(),
                            (bx, y, bz, material, small) -> {
                                if (!area.contains(bx, bz)) return false;
                                var layers = active.strata() ? strata.field.sample(bx, bz, 1, 1, y, y) : null;
                                if (!acceptsOre(
                                        level,
                                        layers,
                                        summary,
                                        target,
                                        terrain(level, bx, bz).getBlock(y),
                                        bx,
                                        y,
                                        bz)) return false;
                                found.set(bx, y, bz);
                                return true;
                            })) return found.immutable();
                }
            }
        }
        return null;
    }

    private static final class Strata {
        private final StoneLayerField field;
        private final HeightRange heights;
        private final boolean[] layers = new boolean[StoneLayerField.layers().size()];
        private final int[] column;
        private final OreGenerationSummary summary;
        private final boolean gems;

        Strata(long seed, HeightRange terrain, OreGenerationSummary summary, Target target) {
            this.summary = summary;
            field = new StoneLayerField(seed);
            int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
            boolean boundary = false;
            for (var source : target.sources()) {
                switch (source.kind()) {
                    case LAYER, CONTACT, SEAM, BOUNDARY_GEM -> {
                        low = Math.min(low, source.heights().min());
                        high = Math.max(high, source.heights().max());
                        boundary |= source.kind() == Kind.BOUNDARY_GEM;
                    }
                    default -> {}
                }
            }
            gems = boundary;
            heights = target.kind() == WorldgenLocate.Kind.LAYER
                    ? terrain
                    : terrain.intersect(new HeightRange(low, high));
            column = new int[heights.isEmpty() ? 0 : heights.max() - heights.min() + 1];
            if (!enabled()) return;
            for (int i = 0; i < layers.length; i++) {
                var stone = GTStoneLayerFeature.layerStone(i);
                if (target.kind() == WorldgenLocate.Kind.LAYER)
                    layers[i] = stone == null
                            ? target.blocks().contains(Blocks.STONE)
                                    || target.blocks().contains(Blocks.DEEPSLATE)
                            : target.test(stone);
                else
                    layers[i] = gems
                            || stone != null && target.test(stone)
                            || StratumOreRules.canProvide(i, target.material());
            }
        }

        boolean enabled() {
            return column.length > 0;
        }

        private BlockPos find(ServerLevel level, Target target, int originY, int x, int z) {
            field.column(x, z).fill(heights.min(), column, column.length);
            StoneLayerField.Samples samples = null;
            var contacts = new StoneLayerField.Contacts();
            NoiseColumn terrain = null;
            BlockPos best = null;
            for (int i = 0; i < column.length; i++) {
                int rock = column[i], y = heights.min() + i;
                if (!layers[rock] || best != null && Math.abs(y - originY) >= Math.abs(best.getY() - originY)) continue;
                BlockState stone = GTStoneLayerFeature.layerStone(rock);
                if (target.kind() == WorldgenLocate.Kind.ORE) {
                    if (samples == null) samples = field.sample(x, z, 1, 1, heights.min(), heights.max());
                    samples.contacts(x, y, z, contacts);
                    boolean host = stone == null || GTOreBlockResolver.stoneOf(stone) != null;
                    long seed = WorldgenHash.at(level.getSeed(), x, y, z);
                    var ore = stratumOre(rock, y, contacts, seed, host, summary);
                    if (ore != null) {
                        if (ore.material() != target.material()) continue;
                    } else if ((stone == null || !target.test(stone))
                            && !(gems
                                    && host
                                    && contacts.nearest() == 1
                                    && BoundaryGemOres.choose(seed, summary.boundaryGemDivisor()) == target.material()))
                        continue;
                }
                if (terrain == null) terrain = terrain(level, x, z);
                var ground = terrain.getBlock(y);
                if (!GTStoneLayerFeature.naturalLayerHost(ground)) continue;
                if (target.kind() == WorldgenLocate.Kind.LAYER && stone == null && !target.test(ground)) continue;
                best = new BlockPos(x, y, z);
            }
            return best;
        }
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
