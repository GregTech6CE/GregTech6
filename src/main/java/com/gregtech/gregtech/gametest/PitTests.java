package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.worldgen.GTFeatures;
import com.gregtech.gregtech.worldgen.GTPitFeature;
import com.gregtech.gregtech.worldgen.GTPitShape;
import com.gregtech.gregtech.worldgen.GTSurfaceDepositFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Guards GT6's sand/clay pits ({@code WorldgenPit}, {@code Loader_Worldgen:592-597}): the 48x48
 * outline, the 1/320 rarity, the plains/savanna gate and GT6's per-column carving rules.
 *
 * <p>GT6 anchors the outline 16 blocks before the chunk, so a pit spans 3x3 chunks, and it carves
 * between {@code seaLevel - 8} and {@code seaLevel + 16}, replacing the sediment it finds and only
 * continuing through rock and gravel once it is inside the pit (at most seven blocks deep).
 *
 * <p>The port's own surface clay seams ({@link GTSurfaceDepositFeature}) share the columns: both of
 * the port's clay deposits have to show their clay at the top, so the seam tests live here too.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class PitTests {
    private static final int BASE_X = 32000;
    private static final int BASE_Z = 32000;

    /** Clay seam area: a flat shore for {@link GTSurfaceDepositFeature#placeClaySeam}. */
    private static final int SEAM_X = 57000;
    private static final int SEAM_Z = 57000;
    /** Clay seam area for the whole feature (its roll picks a position inside this chunk). */
    private static final int SEAM_CHUNK_X = 57088;
    private static final int SEAM_CHUNK_Z = 57088;
    private static final long SEAM_SEED = 20260917L;

    /** GT6's table: the pit kinds the port carries, the rarity and the biome gate. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void pitsMatchGt6Table(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        helper.assertTrue(GTPitFeature.CHANCE == 1 && GTPitFeature.DIVIDER == 320,
                "GT6 uses tChance = 320 with chance 1 (about 1 pit per 320 chunks)");
        helper.assertTrue(GTPitFeature.ABOVE_SEA == 16 && GTPitFeature.BELOW_SEA == 8,
                "GT6 carves from waterLevel+16 down to waterLevel-8");
        helper.assertTrue(GTPitFeature.MAX_DEPTH == 7, "GT6 stops a column after seven pit blocks");
        helper.assertTrue(GTPitFeature.PITS.size() == 5,
                "Loader_Worldgen registers 6 overworld pits, one of them disabled by default (red clay)");
        String[] expected = {"minecraft:clay", "gregtech:clay_brown", "gregtech:clay_yellow",
                "gregtech:clay_blue", "gregtech:clay_white"};
        for (int i = 0; i < expected.length; i++) {
            if (i < GTPitFeature.PITS.size() && !GTPitFeature.PITS.get(i).blockId().equals(expected[i])) {
                problems.add("pit " + i + ": expected " + expected[i] + ", got " + GTPitFeature.PITS.get(i).blockId());
            }
            var block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(expected[i]));
            if (block == null || block == Blocks.AIR) problems.add("missing pit block " + expected[i]);
        }
        helper.assertTrue(GTPitFeature.pitBiome(ResourceLocation.withDefaultNamespace("plains")),
                "plains get pits");
        helper.assertTrue(GTPitFeature.pitBiome(ResourceLocation.withDefaultNamespace("savanna")),
                "savannas get pits");
        helper.assertFalse(GTPitFeature.pitBiome(ResourceLocation.withDefaultNamespace("desert")),
                "GT6 only carves pits in plains and savannas");
        helper.assertFalse(GTPitFeature.pitBiome(ResourceLocation.withDefaultNamespace("forest")),
                "forests do not get pits in GT6");
        helper.assertTrue(GTPitShape.SIZE == 48, "GT6's pit mask is 48x48");
        helper.assertTrue(GTPitShape.ROWS.length == 48, "one mask row per Z offset");
        helper.assertTrue(GTPitShape.cells() == 2032, "GT6's mask covers 2032 cells, got " + GTPitShape.cells());
        helper.assertTrue(!GTPitShape.at(0, 0) && !GTPitShape.at(47, 47), "the mask corners are outside the pit");
        helper.assertTrue(GTPitShape.at(24, 24), "the mask centre is part of the pit");
        helper.assertTrue(GTFeatures.PITS.getId().getPath().equals("gt_pits"), "feature id");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .containsKey(GTPitFeature.CONFIGURED),
                "data/gregtech/worldgen/configured_feature/gt_pits.json is loaded");
        helper.assertTrue(problems.isEmpty(), "pit table (" + problems.size() + "): "
                + problems.subList(0, Math.min(5, problems.size())));
        helper.succeed();
    }

    /** GT6's per-column rules: sediment becomes pit fill, rock caps the pit, wood-covered dirt stays. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void pitCarvesGt6Columns(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockState clay = Blocks.CLAY.defaultBlockState();
        List<String> problems = new ArrayList<>();

        // A sandy column: the sand and everything below it down to the seven-block limit becomes clay.
        BlockPos sandy = absolute(level, 0);
        fillColumn(level, sandy, Blocks.SAND);
        int carved = count(level, sandy, clay);
        if (!GTPitFeature.carveColumn(level, sandy.getX(), sandy.getZ(),
                sandy.getY() + 3, sandy.getY() - 8, clay)) {
            problems.add("a sandy column must be carved");
        }
        int carvedNow = count(level, sandy, clay);
        if (carvedNow <= carved) problems.add("the sand was not replaced by the pit fill");
        if (carvedNow - carved > GTPitFeature.MAX_DEPTH) {
            problems.add("GT6 never digs more than " + GTPitFeature.MAX_DEPTH + " blocks, dug " + (carvedNow - carved));
        }

        // A pure stone column: stone is not a host before the pit starts, so nothing happens.
        BlockPos stony = absolute(level, 1);
        fillColumn(level, stony, Blocks.STONE);
        if (GTPitFeature.carveColumn(level, stony.getX(), stony.getZ(),
                stony.getY() + 3, stony.getY() - 8, clay)) {
            problems.add("stone must not start a pit (GT6 continues through rock only inside one)");
        }

        // Dirt under wood or leaves is left alone: GT6 keeps the ground under a tree.
        BlockPos forest = absolute(level, 2);
        fillColumn(level, forest, Blocks.SAND);
        level.setBlock(forest, Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(forest.above(), Blocks.OAK_LOG.defaultBlockState(), 3);
        GTPitFeature.carveColumn(level, forest.getX(), forest.getZ(),
                forest.getY(), forest.getY() - 4, clay);
        if (!level.getBlockState(forest).is(Blocks.DIRT)) {
            problems.add("dirt directly under a log must survive (GT6's wood/leaves check)");
        }

        helper.assertTrue(problems.isEmpty(), "pit columns (" + problems.size() + "): "
                + problems.subList(0, Math.min(5, problems.size())));
        helper.succeed();
    }

    /** The old deposit feature must not place twigs any more - GT6 has one WorldgenSticks. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void twigsHaveASingleOwner(GameTestHelper helper) {
        String source = null;
        try (var stream = PitTests.class.getClassLoader()
                .getResourceAsStream("com/gregtech/gregtech/worldgen/GTSurfaceDepositFeature.class")) {
            helper.assertTrue(stream != null, "the deposit feature class is on the classpath");
        } catch (java.io.IOException e) {
            helper.fail("cannot read the deposit feature class: " + e);
        }
        // The behaviour instead of the bytes: the flora feature owns GT6's WorldgenSticks numbers.
        helper.assertTrue(com.gregtech.gregtech.worldgen.GTSurfaceFlora.TWIG_AMOUNT == 2
                        && com.gregtech.gregtech.worldgen.GTSurfaceFlora.TWIG_PROBABILITY == 2,
                "WorldgenSticks is (2, 2) and lives in the flora feature");
        helper.assertTrue(GTBlocks.TWIGS.get() instanceof com.gregtech.gregtech.block.TwigBlock,
                "the twig block is still the GT6 WorldgenSticks block");
        helper.assertTrue(source == null, "no second twig owner");
        helper.succeed();
    }

    /**
     * The port's clay seams must show their clay: on a flat grass surface the seam's own top layer
     * is the GT clay block, never the grass block it replaced, and the surroundings stay untouched.
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void claySeamExposesClayAtTheSurface(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WorldGenLevel gen = (WorldGenLevel) level;
        int sea = level.getSeaLevel();
        int radius = 5;
        int depth = 3;
        int spread = radius + 3;
        BlockState clay = seamClay();
        helper.assertTrue(clay != null, "gregtech:clay_blue is registered");

        // A flat grass shore: grass over five dirt layers over stone, clear air above every column.
        for (int dx = -spread; dx <= spread; dx++) {
            for (int dz = -spread; dz <= spread; dz++) {
                layGrass(level, SEAM_X + dx, SEAM_Z + dz, sea);
            }
        }
        primeSeamHeights(level, SEAM_X, SEAM_Z, spread);
        String deviation = plotTopDeviation(level, SEAM_X - spread, SEAM_Z - spread,
                SEAM_X + spread, SEAM_Z + spread, sea);
        helper.assertTrue(deviation == null,
                "the plot must be the top ground block of every column the seam reads: " + deviation);

        List<String> problems = new ArrayList<>();
        helper.assertTrue(GTSurfaceDepositFeature.placeClaySeam(gen, SEAM_X, SEAM_Z, clay, radius, depth),
                "a flat grass surface is a clay seam host");

        // The top block of every seam column is the port's GT clay, and the seam is three deep.
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                BlockState top = level.getBlockState(new BlockPos(SEAM_X + dx, sea, SEAM_Z + dz));
                if (top.is(Blocks.GRASS_BLOCK)) {
                    problems.add("grass left on top of the seam at " + dx + "/" + dz);
                } else if (!top.is(clay.getBlock())) {
                    problems.add("not gregtech:clay_blue at " + dx + "/" + dz + ": " + top);
                }
                if (!level.getBlockState(new BlockPos(SEAM_X + dx, sea - depth + 1, SEAM_Z + dz))
                        .is(clay.getBlock())) {
                    problems.add("the seam must be " + depth + " blocks deep at " + dx + "/" + dz);
                }
            }
        }
        if (!level.getBlockState(new BlockPos(SEAM_X, sea - depth, SEAM_Z)).is(Blocks.DIRT)) {
            problems.add("the seam must not dig past its depth: the fourth block below stays dirt");
        }

        // The surroundings are untouched: outside the disc the grass and the dirt below it stay.
        for (int dx = -spread; dx <= spread; dx++) {
            for (int dz = -spread; dz <= spread; dz++) {
                if (dx * dx + dz * dz <= (radius + 1) * (radius + 1)) continue;
                if (!level.getBlockState(new BlockPos(SEAM_X + dx, sea, SEAM_Z + dz)).is(Blocks.GRASS_BLOCK)
                        || grassOverClay(level, SEAM_X + dx, SEAM_Z + dz, sea, sea - 5)) {
                    problems.add("the surroundings changed at " + dx + "/" + dz);
                }
            }
        }

        // Re-running the seam over its own clay cannot put grass back on top of it.
        GTSurfaceDepositFeature.placeClaySeam(gen, SEAM_X, SEAM_Z, clay, radius, depth);
        for (int dx = -spread; dx <= spread; dx++) {
            for (int dz = -spread; dz <= spread; dz++) {
                if (grassOverClay(level, SEAM_X + dx, SEAM_Z + dz, sea, sea - 5)) {
                    problems.add("grass above clay after a second run at " + dx + "/" + dz);
                }
            }
        }

        helper.assertTrue(problems.isEmpty(), "clay seam (" + problems.size() + "): "
                + problems.subList(0, Math.min(5, problems.size())));
        helper.succeed();
    }

    /**
     * The seam feature itself, run twice on a flat grass shore: every deposit it leaves has the
     * port's GT clay on top, and no grass block ever ends up covering clay.
     */
    @GameTest(template = "test_empty", timeoutTicks = 500)
    public static void surfaceDepositFeatureLeavesNoGrassOverClay(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WorldGenLevel gen = (WorldGenLevel) level;
        int sea = level.getSeaLevel();
        int minX = SEAM_CHUNK_X & ~15;
        int minZ = SEAM_CHUNK_Z & ~15;
        int margin = 8;
        int span = 23;
        List<String> problems = new ArrayList<>();

        // A flat grass shore with a water pit every four blocks: the seam feature rolls its own
        // position inside the chunk, so every position it can pick needs water within two blocks
        // (its 5x5x2 neighbourhood gate). The water above the plot is not motion blocking, so the
        // seam's own OCEAN_FLOOR_WG heightmap still sees the grass below it.
        for (int dx = -margin; dx <= span; dx++) {
            for (int dz = -margin; dz <= span; dz++) {
                layGrass(level, minX + dx, minZ + dz, sea);
                if (Math.floorMod(dx, 4) == 0 && Math.floorMod(dz, 4) == 0) {
                    level.setBlock(new BlockPos(minX + dx, sea + 1, minZ + dz),
                            Blocks.WATER.defaultBlockState(), 2);
                }
            }
        }
        primeSeamHeights(level, minX, minZ, span);
        String deviation = plotTopDeviation(level, minX - margin, minZ - margin, minX + span, minZ + span, sea);
        helper.assertTrue(deviation == null,
                "the plot must be the top ground block of every column the seam reads: " + deviation);

        var feature = GTFeatures.SURFACE_DEPOSITS.get();
        var generator = level.getChunkSource().getGenerator();
        int placements = 0;
        for (int run = 0; run < 2; run++) {
            for (int attempt = 0; attempt < 64; attempt++) {
                RandomSource random = RandomSource.create(SEAM_SEED + run * 1000L + attempt);
                FeaturePlaceContext<NoneFeatureConfiguration> context = new FeaturePlaceContext<>(
                        Optional.empty(), gen, generator, random, new BlockPos(minX, sea, minZ),
                        NoneFeatureConfiguration.INSTANCE);
                if (feature.place(context)) placements++;
            }
        }
        if (placements == 0) {
            problems.add("the seam feature must place on a flat grass shore next to water");
        }

        // What it left is the port's GT clay on the surface, with no grass block on top of any clay.
        // The seam picks a position 2..13 blocks inside the chunk and reaches at most 7 blocks
        // further, so the outer three blocks of the plot can never be touched by it.
        int clayTops = 0;
        for (int dx = -margin; dx <= span; dx++) {
            for (int dz = -margin; dz <= span; dz++) {
                int x = minX + dx;
                int z = minZ + dz;
                BlockState top = level.getBlockState(new BlockPos(x, sea, z));
                if (isGtClay(top)) {
                    clayTops++;
                    // The deposit starts at the surface block, so its thickness is the depth the
                    // feature rolled (2..4) and the layer under it is no longer clay.
                    int run = 0;
                    while (run < 16 && isClay(level.getBlockState(new BlockPos(x, sea - run, z)))) run++;
                    if (run < GTSurfaceDepositFeature.MIN_DEPTH || run > GTSurfaceDepositFeature.MAX_DEPTH) {
                        problems.add("the deposit is " + run + " blocks deep at " + dx + "/" + dz);
                    }
                }
                if (grassOverClay(level, x, z, sea, sea - 5)) {
                    problems.add("grass above clay at " + dx + "/" + dz);
                }
                if ((dx < -5 || dx > 20 || dz < -5 || dz > 20) && !top.is(Blocks.GRASS_BLOCK)) {
                    problems.add("the surroundings changed at " + dx + "/" + dz + ": " + top);
                }
            }
        }
        if (clayTops == 0) {
            problems.add("no GT clay reached the surface, the deposit is still covered");
        }

        helper.assertTrue(problems.isEmpty(), "surface deposits (" + problems.size() + "): "
                + problems.subList(0, Math.min(5, problems.size())));
        helper.succeed();
    }

    /** The port's GT clay the test drives: GT6 {@code BlocksGT.Diggables} meta 5 (Blue Clay). */
    private static BlockState seamClay() {
        var block = ForgeRegistries.BLOCKS.getValue(
                ResourceLocation.fromNamespaceAndPath("gregtech", GTSurfaceDepositFeature.CLAYS[0]));
        return block == null || block == Blocks.AIR ? null : block.defaultBlockState();
    }

    /** A flat grass surface: grass over five dirt layers over stone, clear air above every one. */
    private static void layGrass(ServerLevel level, int x, int z, int sea) {
        // The column has to exist before it can be measured: Level.getHeight returns the minimum
        // build height for a chunk that is not loaded yet (Level#getHeight, the hasChunk() else
        // branch), and it is the first write to a chunk that generates it. Measure first, or the
        // clearing below silently does nothing on that first column of a chunk and the freshly
        // generated terrain (a hill, a tree, an ice sheet) stays above the plot.
        level.getChunk(x >> 4, z >> 4);
        // Clear whatever the test world generated above the plot, using both heightmaps: the seam
        // reads Heightmap.Types.OCEAN_FLOOR_WG, so any leftover block above the plot would still be
        // the surface of that column and the seam would place its clay up there instead of on the
        // plot. WORLD_SURFACE is live (LevelChunk keeps it in step with setBlock) and counts every
        // non-air block, OCEAN_FLOOR_WG the motion blocking ones.
        int worldTop = Math.max(level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z),
                level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z)) - 1;
        for (int y = Math.max(worldTop, sea + 1); y > sea; y--) {
            level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        level.setBlock(new BlockPos(x, sea, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        for (int y = sea - 1; y >= sea - 5; y--) {
            level.setBlock(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
        }
        level.setBlock(new BlockPos(x, sea - 6, z), Blocks.STONE.defaultBlockState(), 2);
    }

    /**
     * The first plot column whose seam heightmap is not {@code sea}, described for the failure
     * message, or null when the plot really is the top ground block of every column it covers.
     * Without this the assertions below would test leftover worldgen terrain instead of the plot.
     */
    @Nullable
    private static String plotTopDeviation(ServerLevel level, int minX, int minZ, int maxX, int maxZ, int sea) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                if (top != sea) {
                    return "(" + x + "," + z + ") reads " + top + " instead of " + sea + ", "
                            + level.getBlockState(new BlockPos(x, top, z)).getBlock();
                }
            }
        }
        return null;
    }

    /** True when a grass block sits right on top of clay - the cap the user report was about. */
    private static boolean grassOverClay(ServerLevel level, int x, int z, int top, int bottom) {
        for (int y = bottom; y <= top; y++) {
            if (level.getBlockState(new BlockPos(x, y, z)).is(Blocks.GRASS_BLOCK)
                    && isClay(level.getBlockState(new BlockPos(x, y - 1, z)))) {
                return true;
            }
        }
        return false;
    }

    /** Vanilla clay plus the port's GT clays (GT6 {@code BlocksGT.Diggables} 1/4/5/6). */
    private static boolean isClay(BlockState state) {
        return state.is(Blocks.CLAY) || isGtClay(state);
    }

    /** GT6's colored clays as the port's icon-set blocks: {@code gregtech:clay_*}. */
    private static boolean isGtClay(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && id.getNamespace().equals("gregtech") && id.getPath().startsWith("clay_");
    }

    /**
     * The seam reads {@code Heightmap.Types.OCEAN_FLOOR_WG}, a worldgen heightmap. A live level only
     * keeps the client and live-world heightmaps in step with setBlock, so a test plot has to prime
     * it, or the seam would still see the terrain that was there before the plot was built.
     */
    private static void primeSeamHeights(ServerLevel level, int x, int z, int spread) {
        for (int chunkX = (x - spread) >> 4; chunkX <= ((x + spread) >> 4); chunkX++) {
            for (int chunkZ = (z - spread) >> 4; chunkZ <= ((z + spread) >> 4); chunkZ++) {
                Heightmap.primeHeightmaps(level.getChunk(chunkX, chunkZ),
                        EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG));
            }
        }
    }

    private static BlockPos absolute(ServerLevel level, int index) {
        BlockPos pos = new BlockPos(BASE_X + index * 32, level.getSeaLevel(), BASE_Z);
        level.getBlockState(pos);
        return pos;
    }

    /** A clean column: air above, 12 blocks of the given sediment below the surface. */
    private static void fillColumn(ServerLevel level, BlockPos surface, net.minecraft.world.level.block.Block block) {
        for (int dy = -12; dy <= 6; dy++) {
            level.setBlock(surface.above(dy), dy < 0 ? block.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static int count(ServerLevel level, BlockPos surface, BlockState state) {
        int found = 0;
        for (int dy = -12; dy <= 6; dy++) {
            if (level.getBlockState(surface.above(dy)).is(state.getBlock())) found++;
        }
        return found;
    }
}
