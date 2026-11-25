package gregtech.content.worldgen;

import static gregtech.content.worldgen.GTOreVeins.SURFACE_MARGIN;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.content.worldgen.GTOreVeins.HeightRange;
import gregtech.content.worldgen.OreVeinLayout.Body;
import gregtech.content.worldgen.OreVeinLayout.Center;
import gregtech.content.worldgen.OreVeinLayout.RegionalPlan;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

/** Plans regional veins from undecorated terrain, independently of chunk generation order. */
final class VeinTerrain {
    private static final int LOCAL_SAMPLE_SPACING = 8;
    private static final int LOCAL_SEARCH_STEPS = 4;
    private static final int REGION_SAMPLE_SPACING = OreVeinLayout.CELL_SIZE / 2;
    private static final Cache<Region, PlannedRegion> REGIONS =
            CacheBuilder.newBuilder().maximumSize(128).build();

    private VeinTerrain() {}

    /** Releases terrain generators and noise state after the server's generation workers stop. */
    static void clear() {
        REGIONS.invalidateAll();
        REGIONS.cleanUp();
    }

    private record Region(
            long seed,
            String dimension,
            ChunkGenerator generator,
            RandomState noise,
            RegionalPlan plan,
            HeightRange bounds,
            int x,
            int z) {}

    /** Includes every body that can reach the chunk, in regional placement order. */
    static List<Body> candidates(
            WorldGenLevel level,
            ChunkGenerator generator,
            String dimension,
            RegionalPlan plan,
            HeightRange bounds,
            int x,
            int z,
            ChunkPos chunk) {
        var key = new Region(
                level.getSeed(),
                dimension,
                generator,
                level.getLevel().getChunkSource().randomState(),
                plan,
                bounds,
                x,
                z);
        return REGIONS.asMap().computeIfAbsent(key, PlannedRegion::new).forChunk(level, chunk);
    }

    private record SearchArea(int minX, int minZ, int endX, int endZ) {
        static SearchArea forCenter(Region region, Center center) {
            int side = region.plan().cells() * OreVeinLayout.CELL_SIZE;
            int x = region.x() * side;
            int z = region.z() * side;
            if (!center.vein().subregional()) return new SearchArea(x, z, x + side, z + side);
            int half = region.plan().cells() / 2 * OreVeinLayout.CELL_SIZE;
            int splitX = x + half;
            int splitZ = z + half;
            return new SearchArea(
                    center.x() < splitX ? x : splitX, center.z() < splitZ ? z : splitZ,
                    center.x() < splitX ? splitX : x + side, center.z() < splitZ ? splitZ : z + side);
        }

        boolean contains(int x, int z) {
            return x >= minX && x < endX && z >= minZ && z < endZ;
        }
    }

    private record Pending(int index, Center center, HeightRange range, SearchArea area, HeightRange reserve) {}

    private static Pending entry(Region region, Center center, int index) {
        var range = center.vein().heights(region.dimension()).intersect(region.bounds());
        var reserve = center.guaranteed()
                        && (range.isEmpty() || range.min() >= region.generator().getSeaLevel() - 32)
                ? GTOreVeins.reserveHeights(region.dimension(), center.vein().name(), region.bounds())
                        .orElse(null)
                : null;
        return new Pending(index, center, range, SearchArea.forCenter(region, center), reserve);
    }

    /** Resolves one debug-query center with the same terrain survey used during generation. */
    static Body locate(
            WorldGenLevel level,
            ChunkGenerator generator,
            String dimension,
            RegionalPlan plan,
            HeightRange bounds,
            int rx,
            int rz,
            Center center) {
        var region = new Region(
                level.getSeed(),
                dimension,
                generator,
                level.getLevel().getChunkSource().randomState(),
                plan,
                bounds,
                rx,
                rz);
        return new TerrainSurvey(level, region, new HashMap<>()).resolve(entry(region, center, 0));
    }

    private static final class PlannedRegion {
        private final Region region;
        private final List<Pending> pending = new ArrayList<>();
        private final Body[] bodies;
        private Map<Long, Integer> heights = new HashMap<>();
        private List<Body> resolved = List.of();

        private PlannedRegion(Region region) {
            this.region = region;
            var centers = region.plan().centers(region.seed(), region.x(), region.z());
            bodies = new Body[centers.size()];
            for (var center : centers) {
                pending.add(entry(region, center, pending.size()));
            }
        }

        private synchronized List<Body> forChunk(WorldGenLevel level, ChunkPos chunk) {
            if (pending.isEmpty()) return resolved;
            var terrain = new TerrainSurvey(level, region, heights);
            boolean changed = false;
            for (var it = pending.iterator(); it.hasNext(); ) {
                var entry = it.next();
                if (terrain.cannotReach(entry, chunk)) continue;
                bodies[entry.index()] = terrain.resolve(entry);
                it.remove();
                changed = true;
            }
            // Adjacent chunks can discover the bodies in a different order.
            if (changed)
                resolved = Arrays.stream(bodies).filter(Objects::nonNull).toList();
            if (pending.isEmpty()) heights = Map.of();
            return resolved;
        }
    }

    private record Site(int x, int z) {}

    private record IslandSite(int x, int z, HeightRange rock) {}

    private static final class TerrainSurvey {
        private final WorldGenLevel level;
        private final Region region;
        private final int side;
        private final int baseX;
        private final int baseZ;
        private final Map<Long, Integer> surfaceHeights;
        private List<Site> sites;

        private TerrainSurvey(WorldGenLevel level, Region region, Map<Long, Integer> surfaceHeights) {
            this.level = level;
            this.region = region;
            this.surfaceHeights = surfaceHeights;
            side = region.plan().cells() * OreVeinLayout.CELL_SIZE;
            baseX = region.x() * side;
            baseZ = region.z() * side;
        }

        private boolean cannotReach(Pending entry, ChunkPos chunk) {
            var center = entry.center();
            var range = entry.range();
            if (!center.guaranteed()
                    || !OreVeinDeclaration.OVERWORLD.equals(region.dimension())
                    || !(region.generator() instanceof NoiseBasedChunkGenerator)
                    || range.isEmpty()
                    || range.min() < region.generator().getSeaLevel() - 32) return false;
            var area = entry.area();
            int radius = center.radius();
            int minX = Math.max(area.minX(), chunk.getMinBlockX() - radius);
            int minZ = Math.max(area.minZ(), chunk.getMinBlockZ() - radius);
            int maxX = Math.min(area.endX() - 1, chunk.getMaxBlockX() + radius);
            int maxZ = Math.min(area.endZ() - 1, chunk.getMaxBlockZ() + radius);
            if (minX > maxX || minZ > maxZ) return true;
            int local = LOCAL_SAMPLE_SPACING * LOCAL_SEARCH_STEPS;
            if (center.x() >= minX - local
                    && center.x() <= maxX + local
                    && center.z() >= minZ - local
                    && center.z() <= maxZ + local) return false;

            int requiredY = range.min() + SURFACE_MARGIN;
            int atCenter = surface(center.x(), center.z());
            if (atCenter >= requiredY) return false; // The first local site succeeds and is cheap to resolve.
            var reserve = entry.reserve();
            if (reserve != null) {
                int reserveY = reserve.min() + SURFACE_MARGIN;
                if (atCenter < reserveY) requiredY = Math.min(requiredY, reserveY);
            }
            // Every local site (including the original and a locally satisfied reserve) is too far away.
            // Only a regional grid site inside this expanded chunk could place a body here.
            int spacing = REGION_SAMPLE_SPACING;
            int firstX = minX + Math.floorMod(baseX + spacing / 2 - minX, spacing);
            int firstZ = minZ + Math.floorMod(baseZ + spacing / 2 - minZ, spacing);
            for (int x = firstX; x <= maxX; x += spacing) {
                for (int z = firstZ; z <= maxZ; z += spacing) {
                    if (surface(x, z) < requiredY) continue;
                    // The center of its own grid cell is at most 12 blocks away on either axis.
                    // If it qualifies, the local spiral or a nearer grid site wins before this distant chunk.
                    int nearX = baseX + Math.floorDiv(center.x() - baseX, spacing) * spacing + spacing / 2;
                    int nearZ = baseZ + Math.floorDiv(center.z() - baseZ, spacing) * spacing + spacing / 2;
                    return surface(nearX, nearZ) >= range.min() + SURFACE_MARGIN;
                }
            }
            return true;
        }

        private Body resolve(Pending entry) {
            var center = entry.center();
            var range = entry.range();
            if (range.isEmpty()) return center.guaranteed() ? reserveBody(entry) : null;
            if (OreVeinDeclaration.END.equals(region.dimension())
                    || region.generator() instanceof NoiseBasedChunkGenerator) return body(entry);
            return center.body(
                    range.min(),
                    range.max(),
                    region.bounds().min(),
                    region.bounds().max());
        }

        private Body body(Pending entry) {
            var center = entry.center();
            var range = entry.range();
            if (OreVeinDeclaration.END.equals(region.dimension())) return endBody(center, range);
            if (center.guaranteed() && range.min() >= region.generator().getSeaLevel() - 32) {
                Site site = find(center, entry.area(), range.min() + SURFACE_MARGIN);
                if (site == null) {
                    Body fallback = reserveBody(entry);
                    if (fallback != null) return fallback;
                } else {
                    center = new Center(center.vein(), site.x(), site.z(), center.seed(), true);
                }
            }
            return center.body(
                    range.min(),
                    range.max(),
                    region.bounds().min(),
                    region.bounds().max());
        }

        private Body reserveBody(Pending entry) {
            var center = entry.center();
            var reserve = entry.reserve();
            if (reserve == null) return null;
            Site site = find(center, entry.area(), reserve.min() + SURFACE_MARGIN);
            if (site == null) return null;
            var placement = GTOreVeins.reservePlacement(reserve, surface(site.x(), site.z()));
            var relocated = new Center(center.vein(), site.x(), site.z(), center.seed(), true);
            return relocated.body(
                    placement.min(),
                    placement.max(),
                    region.bounds().min(),
                    region.bounds().max());
        }

        private Body endBody(Center center, HeightRange range) {
            var original = center.body(range.min(), range.max(), range.min(), range.max());
            int thickness = original.maxY() - original.minY() + 1;
            IslandSite site = findIsland(center, range, thickness);
            if (site == null) return null;
            var anchor = new Center(center.vein(), site.x(), site.z(), center.seed(), true);
            // In a floating island, a high surface is not evidence of rock below the island.
            return anchor.body(
                    site.rock().min(),
                    site.rock().max(),
                    region.bounds().min(),
                    region.bounds().max());
        }

        private IslandSite findIsland(Center center, HeightRange heights, int idealThickness) {
            IslandSite bestSite = null;
            long bestDistance = Long.MAX_VALUE;
            int bestThickness = 0;
            for (BlockPos offset :
                    BlockPos.spiralAround(BlockPos.ZERO, LOCAL_SEARCH_STEPS, Direction.EAST, Direction.SOUTH)) {
                int x = center.x() + offset.getX() * LOCAL_SAMPLE_SPACING;
                int z = center.z() + offset.getZ() * LOCAL_SAMPLE_SPACING;
                if (x < baseX || x >= baseX + side || z < baseZ || z >= baseZ + side) continue;
                var rock = rockBand(x, z, heights);
                int thickness = Math.min(idealThickness, rock.max() - rock.min() + 1);
                if (thickness == idealThickness) return new IslandSite(x, z, rock);
                long distance = MineralWorldgenRules.distanceSquared(center.x(), center.z(), x, z);
                if (thickness == 0
                        || thickness < bestThickness
                        || (thickness == bestThickness && distance >= bestDistance)) continue;
                bestThickness = thickness;
                bestDistance = distance;
                bestSite = new IslandSite(x, z, rock);
            }
            for (Site site : sites()) {
                long distance = MineralWorldgenRules.distanceSquared(center.x(), center.z(), site.x(), site.z());
                if (bestThickness == idealThickness && distance >= bestDistance) continue;
                int top = surface(site.x(), site.z());
                if (top < heights.min()) continue;
                int available = Math.min(idealThickness, Math.min(heights.max(), top) - heights.min() + 1);
                if (available < bestThickness || (available == bestThickness && distance >= bestDistance)) continue;
                var rock = rockBand(site.x(), site.z(), heights);
                int thickness = Math.min(idealThickness, rock.max() - rock.min() + 1);
                if (thickness == 0
                        || thickness < bestThickness
                        || (thickness == bestThickness && distance >= bestDistance)) continue;
                bestThickness = thickness;
                bestDistance = distance;
                bestSite = new IslandSite(site.x(), site.z(), rock);
            }
            return bestSite;
        }

        private HeightRange rockBand(int x, int z, HeightRange heights) {
            var column = region.generator().getBaseColumn(x, z, level, region.noise());
            int start = heights.min();
            int bestMin = start;
            int bestMax = start - 1;
            for (int y = heights.min(); y <= heights.max(); y++) {
                if (GTOreBlockResolver.oreHost(column.getBlock(y)) == null) {
                    start = y + 1;
                } else if (y - start > bestMax - bestMin) {
                    bestMin = start;
                    bestMax = y;
                }
            }
            return new HeightRange(bestMin, bestMax);
        }

        private Site find(Center center, SearchArea area, int requiredY) {
            for (BlockPos offset :
                    BlockPos.spiralAround(BlockPos.ZERO, LOCAL_SEARCH_STEPS, Direction.EAST, Direction.SOUTH)) {
                int x = center.x() + offset.getX() * LOCAL_SAMPLE_SPACING;
                int z = center.z() + offset.getZ() * LOCAL_SAMPLE_SPACING;
                if (!area.contains(x, z)) continue;
                if (surface(x, z) >= requiredY) return new Site(x, z);
            }
            var candidates = new ArrayList<Site>();
            for (Site site : sites()) {
                if (area.contains(site.x(), site.z())) candidates.add(site);
            }
            // A stable distance sort keeps the grid's original X/Z order for equally near sites.
            candidates.sort(Comparator.comparingLong(
                    site -> MineralWorldgenRules.distanceSquared(center.x(), center.z(), site.x(), site.z())));
            for (Site site : candidates) if (surface(site.x(), site.z()) >= requiredY) return site;
            return null;
        }

        private List<Site> sites() {
            if (sites == null) {
                sites = new ArrayList<>();
                int spacing = OreVeinDeclaration.END.equals(region.dimension()) ? 16 : REGION_SAMPLE_SPACING;
                for (int x = baseX + spacing / 2; x < baseX + side; x += spacing) {
                    for (int z = baseZ + spacing / 2; z < baseZ + side; z += spacing) {
                        sites.add(new Site(x, z));
                    }
                }
            }
            return sites;
        }

        private int surface(int x, int z) {
            return surfaceHeights.computeIfAbsent(
                    ChunkPos.asLong(x, z),
                    column -> region.generator()
                                    .getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, region.noise())
                            - 1);
        }
    }
}
