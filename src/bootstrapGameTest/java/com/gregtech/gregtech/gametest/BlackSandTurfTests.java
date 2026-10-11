package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.worldgen.GTBlackSandFeature;
import com.gregtech.gregtech.worldgen.GTCellNoise;
import com.gregtech.gregtech.worldgen.GTFeatures;
import com.gregtech.gregtech.worldgen.GTPitShape;
import com.gregtech.gregtech.worldgen.GTTurfFeature;
import com.gregtech.gregtech.worldgen.GTWorldgenBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GT6's two "dig into the ground the same way the clay pits do" worldgen objects: the river placer
 * black sands ({@code WorldgenBlackSand}, {@code Loader_Worldgen:581}, {@code "river.magnetite"})
 * and the swamp turf bogs ({@code WorldgenTurf}, {@code Loader_Worldgen:582}, {@code "swamp.turf"}).
 * Both reuse the 48x48 {@link GTPitShape} mask and the {@code waterLevel + 1 .. waterLevel - 12}
 * window, but stop after two blocks per column.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class BlackSandTurfTests {
    private static final int BASE_X = 48000;
    private static final int BASE_Z = 48000;

    /** GT6's registration values, the three sand variants, the turf block and the biome sets. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void blackSandAndTurfMatchGt6Tables(GameTestHelper helper) {
        helper.assertTrue(GTBlackSandFeature.DIVIDER == 64, "GT6 rolls nextInt(64) for the black sands");
        helper.assertTrue(GTTurfFeature.DIVIDER == 32, "GT6 rolls nextInt(32) for the turf");
        helper.assertTrue(GTBlackSandFeature.ABOVE_SEA == 1 && GTBlackSandFeature.BELOW_SEA == 12,
                "GT6's black sand window is waterLevel + 1 .. waterLevel - 12");
        helper.assertTrue(GTTurfFeature.ABOVE_SEA == 1 && GTTurfFeature.BELOW_SEA == 12,
                "GT6's turf window is waterLevel + 1 .. waterLevel - 12");
        helper.assertTrue(GTBlackSandFeature.MAX_DEPTH == 2 && GTTurfFeature.MAX_DEPTH == 2,
                "both stop a column after two blocks");
        helper.assertTrue(GTBlackSandFeature.SAND_VARIANTS == 3, "BlocksGT.Sands has metas 0/1/2");
        helper.assertTrue(GTBlackSandFeature.NOISE_Y == 360.0F, "GT6 samples the variant noise at y = 360");

        List<String> problems = new ArrayList<>();
        for (String id : GTBlackSandFeature.BLACK_SANDS) {
            Block block = ForgeRegistries.BLOCKS.getValue(
                    ResourceLocation.fromNamespaceAndPath("gregtech", id));
            if (block == null || block == Blocks.AIR) problems.add("gregtech:" + id);
        }
        Block turf = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(GTTurfFeature.TURF));
        if (turf == null || turf == Blocks.AIR) problems.add(GTTurfFeature.TURF);
        helper.assertTrue(problems.isEmpty(), "missing blocks: " + problems);

        helper.assertTrue(GTFeatures.BLACK_SAND.getId().getPath().equals("gt_black_sand"), "feature id");
        helper.assertTrue(GTFeatures.TURF.getId().getPath().equals("gt_turf"), "feature id");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .containsKey(GTBlackSandFeature.CONFIGURED),
                "data/gregtech/worldgen/configured_feature/gt_black_sand.json is loaded");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .containsKey(GTTurfFeature.CONFIGURED),
                "data/gregtech/worldgen/configured_feature/gt_turf.json is loaded");

        // GT6's biome sets: rivers drive the black sands, swamps the turf.
        helper.assertTrue(GTWorldgenBiomes.RIVER.contains(ResourceLocation.withDefaultNamespace("river"))
                        && GTWorldgenBiomes.RIVER.contains(ResourceLocation.withDefaultNamespace("frozen_river")),
                "GT6's BIOMES_RIVER");
        helper.assertTrue(GTWorldgenBiomes.SWAMP.contains(ResourceLocation.withDefaultNamespace("swamp"))
                        && GTWorldgenBiomes.SWAMP.contains(ResourceLocation.withDefaultNamespace("mangrove_swamp")),
                "GT6's BIOMES_SWAMP");
        for (String beach : new String[] {"beach", "snowy_beach", "stony_shore", "deep_ocean", "ocean"}) {
            helper.assertTrue(GTWorldgenBiomes.OCEAN_BEACH.contains(ResourceLocation.withDefaultNamespace(beach)),
                    "GT6's BIOMES_OCEAN_BEACH must contain " + beach);
        }
        helper.assertTrue(!GTWorldgenBiomes.RIVER.contains(ResourceLocation.withDefaultNamespace("swamp"))
                        && !GTWorldgenBiomes.SWAMP.contains(ResourceLocation.withDefaultNamespace("river")),
                "the river and swamp sets must not overlap");
        // The chunk biome lookup mirrors GT6's aBiomeNames (1..16 biome cells per chunk).
        var biomes = GTWorldgenBiomes.chunkBiomes((WorldGenLevel) helper.getLevel(), BASE_X, BASE_Z);
        helper.assertTrue(!biomes.isEmpty() && biomes.size() <= 16,
                "the chunk biome lookup must return 1..16 biomes, got " + biomes.size());
        helper.assertTrue(biomes.stream().allMatch(b -> helper.getLevel().registryAccess()
                        .registryOrThrow(Registries.BIOME).containsKey(b)),
                "every chunk biome must be a registered biome: " + biomes);
        helper.succeed();
    }

    /** GT6's per-column rules: two blocks deep, only through sediment, never starting on rock. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void blackSandCarvesRiverColumns(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WorldGenLevel gen = (WorldGenLevel) level;
        int sea = level.getSeaLevel();
        int upper = sea + GTBlackSandFeature.ABOVE_SEA;
        int lower = sea - GTBlackSandFeature.BELOW_SEA;
        BlockState fill = state("sand_magnetite");
        helper.assertTrue(fill != null, "gregtech:sand_magnetite is registered");

        // A river bed: two sand layers over stone.
        int x = BASE_X, z = BASE_Z;
        clearColumn(level, x, z, upper, lower);
        level.setBlock(new BlockPos(x, sea, z), Blocks.SAND.defaultBlockState(), 2);
        level.setBlock(new BlockPos(x, sea - 1, z), Blocks.SAND.defaultBlockState(), 2);
        level.setBlock(new BlockPos(x, sea - 2, z), Blocks.SAND.defaultBlockState(), 2);
        fillStone(level, x, z, sea - 3, lower);
        helper.assertTrue(GTBlackSandFeature.carveColumn(gen, x, z, upper, lower, fill),
                "a sand column gets black sand");
        helper.assertTrue(level.getBlockState(new BlockPos(x, sea, z)).is(fill.getBlock()),
                "the top sand block became black sand");
        helper.assertTrue(level.getBlockState(new BlockPos(x, sea - 1, z)).is(fill.getBlock()),
                "the second sand block became black sand");
        helper.assertTrue(level.getBlockState(new BlockPos(x, sea - 2, z)).is(Blocks.SAND),
                "GT6 stops after two blocks (tGenerated < 2)");
        helper.assertTrue(level.getBlockState(new BlockPos(x, sea - 3, z)).is(Blocks.STONE),
                "stone below the deposit stays stone");

        // Rock never starts a deposit: GT6 skips non-host blocks while nothing was generated.
        int rx = BASE_X + 3;
        clearColumn(level, rx, z, upper, lower);
        fillStone(level, rx, z, upper, lower);
        helper.assertTrue(!GTBlackSandFeature.carveColumn(gen, rx, z, upper, lower, fill),
                "a pure stone column must not start a black sand deposit");
        helper.assertTrue(level.getBlockState(new BlockPos(rx, sea, z)).is(Blocks.STONE), "stone untouched");

        // Once started, rock may continue the deposit.
        int cx = BASE_X + 6;
        clearColumn(level, cx, z, upper, lower);
        level.setBlock(new BlockPos(cx, sea, z), Blocks.SAND.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx, sea - 1, z), Blocks.STONE.defaultBlockState(), 2);
        fillStone(level, cx, z, sea - 2, lower);
        helper.assertTrue(GTBlackSandFeature.carveColumn(gen, cx, z, upper, lower, fill),
                "a deposit that started may continue through rock");
        helper.assertTrue(level.getBlockState(new BlockPos(cx, sea - 1, z)).is(fill.getBlock()),
                "the rock under the first sand block became black sand");

        // GT6 leaves the dirt under a tree alone.
        int tx = BASE_X + 9;
        clearColumn(level, tx, z, upper, lower);
        level.setBlock(new BlockPos(tx, upper, z), Blocks.OAK_LOG.defaultBlockState(), 2);
        level.setBlock(new BlockPos(tx, sea, z), Blocks.DIRT.defaultBlockState(), 2);
        level.setBlock(new BlockPos(tx, sea - 1, z), Blocks.DIRT.defaultBlockState(), 2);
        level.setBlock(new BlockPos(tx, sea - 2, z), Blocks.DIRT.defaultBlockState(), 2);
        fillStone(level, tx, z, sea - 3, lower);
        GTBlackSandFeature.carveColumn(gen, tx, z, upper, lower, fill);
        helper.assertTrue(level.getBlockState(new BlockPos(tx, sea, z)).is(Blocks.DIRT),
                "GT6 does not take the dirt block below a tree");
        helper.assertTrue(level.getBlockState(new BlockPos(tx, sea - 1, z)).is(fill.getBlock())
                        && level.getBlockState(new BlockPos(tx, sea - 2, z)).is(fill.getBlock()),
                "the dirt below it is fair game, two blocks deep");

        clearColumn(level, x, z, upper, lower);
        clearColumn(level, rx, z, upper, lower);
        clearColumn(level, cx, z, upper, lower);
        clearColumn(level, tx, z, upper, lower);
        helper.succeed();
    }

    /** The variant is GT6's single noise sample per chunk area: {@code noise(minX/4, 360, minZ/4, 3)}. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void blackSandVariantIsTheGt6Noise(GameTestHelper helper) {
        WorldGenLevel gen = (WorldGenLevel) helper.getLevel();
        GTCellNoise noise = new GTCellNoise(helper.getLevel().getSeed());
        Map<String, Integer> seen = new HashMap<>();
        for (int chunk = 0; chunk < 400; chunk++) {
            int minX = chunk * 16;
            int minZ = -chunk * 16;
            String actual = GTBlackSandFeature.sandId(gen, minX, minZ);
            int expected = noise.get(minX / 4.0F, GTBlackSandFeature.NOISE_Y, minZ / 4.0F,
                    GTBlackSandFeature.SAND_VARIANTS);
            helper.assertTrue(actual.equals(GTBlackSandFeature.BLACK_SANDS.get(expected)),
                    "chunk " + chunk + ": expected " + GTBlackSandFeature.BLACK_SANDS.get(expected)
                            + " but got " + actual);
            helper.assertTrue(actual.equals(GTBlackSandFeature.sandId(gen, minX, minZ)),
                    "the variant must be deterministic");
            seen.merge(actual, 1, Integer::sum);
        }
        helper.assertTrue(seen.size() == GTBlackSandFeature.SAND_VARIANTS,
                "all three BlocksGT.Sands variants must be reachable, saw " + seen.keySet());
        // The mask the two features share is GT6's pit shape.
        helper.assertTrue(GTPitShape.SIZE == 48, "GT6 uses the 48x48 pit mask");
        helper.succeed();
    }

    /** GT6's turf rules: dirt only, two deep, continues through rock but not through gravel. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void turfCarvesSwampColumns(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WorldGenLevel gen = (WorldGenLevel) level;
        int sea = level.getSeaLevel();
        int upper = sea + GTTurfFeature.ABOVE_SEA;
        int lower = sea - GTTurfFeature.BELOW_SEA;
        BlockState turf = state("turf");
        helper.assertTrue(turf != null, "gregtech:turf is registered");

        // Swamp soil: three dirt layers over stone.
        int x = BASE_X + 1, z = BASE_Z + 1;
        clearColumn(level, x, z, upper, lower);
        level.setBlock(new BlockPos(x, sea, z), Blocks.DIRT.defaultBlockState(), 2);
        level.setBlock(new BlockPos(x, sea - 1, z), Blocks.DIRT.defaultBlockState(), 2);
        level.setBlock(new BlockPos(x, sea - 2, z), Blocks.DIRT.defaultBlockState(), 2);
        fillStone(level, x, z, sea - 3, lower);
        helper.assertTrue(GTTurfFeature.carveColumn(gen, x, z, upper, lower, turf), "a dirt column gets turf");
        helper.assertTrue(level.getBlockState(new BlockPos(x, sea, z)).is(turf.getBlock()),
                "the top dirt became turf");
        helper.assertTrue(level.getBlockState(new BlockPos(x, sea - 1, z)).is(turf.getBlock()),
                "the second dirt became turf");
        helper.assertTrue(level.getBlockState(new BlockPos(x, sea - 2, z)).is(Blocks.DIRT),
                "GT6 stops after two blocks (tGenerated < 2)");

        // Dirt under a tree is left alone; the dirt below it still becomes turf.
        int tx = BASE_X + 4;
        clearColumn(level, tx, z, upper, lower);
        level.setBlock(new BlockPos(tx, upper, z), Blocks.OAK_LOG.defaultBlockState(), 2);
        level.setBlock(new BlockPos(tx, sea, z), Blocks.DIRT.defaultBlockState(), 2);
        level.setBlock(new BlockPos(tx, sea - 1, z), Blocks.DIRT.defaultBlockState(), 2);
        fillStone(level, tx, z, sea - 2, lower);
        GTTurfFeature.carveColumn(gen, tx, z, upper, lower, turf);
        helper.assertTrue(level.getBlockState(new BlockPos(tx, sea, z)).is(Blocks.DIRT),
                "GT6 does not take the dirt block below a tree");
        helper.assertTrue(level.getBlockState(new BlockPos(tx, sea - 1, z)).is(turf.getBlock()),
                "the dirt below it becomes turf");

        // Turf only continues through rock — gravel stops it.
        int gx = BASE_X + 7;
        clearColumn(level, gx, z, upper, lower);
        level.setBlock(new BlockPos(gx, sea, z), Blocks.DIRT.defaultBlockState(), 2);
        level.setBlock(new BlockPos(gx, sea - 1, z), Blocks.GRAVEL.defaultBlockState(), 2);
        level.setBlock(new BlockPos(gx, sea - 2, z), Blocks.DIRT.defaultBlockState(), 2);
        fillStone(level, gx, z, sea - 3, lower);
        GTTurfFeature.carveColumn(gen, gx, z, upper, lower, turf);
        helper.assertTrue(level.getBlockState(new BlockPos(gx, sea, z)).is(turf.getBlock()),
                "the first dirt became turf");
        helper.assertTrue(level.getBlockState(new BlockPos(gx, sea - 1, z)).is(Blocks.GRAVEL),
                "gravel is not a turf host and stops the bog");
        helper.assertTrue(level.getBlockState(new BlockPos(gx, sea - 2, z)).is(Blocks.DIRT),
                "the bog stopped, the dirt below stays dirt");

        // ...but rock does not stop it.
        int rx = BASE_X + 10;
        clearColumn(level, rx, z, upper, lower);
        level.setBlock(new BlockPos(rx, sea, z), Blocks.DIRT.defaultBlockState(), 2);
        level.setBlock(new BlockPos(rx, sea - 1, z), Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(new BlockPos(rx, sea - 2, z), Blocks.DIRT.defaultBlockState(), 2);
        fillStone(level, rx, z, sea - 3, lower);
        GTTurfFeature.carveColumn(gen, rx, z, upper, lower, turf);
        helper.assertTrue(level.getBlockState(new BlockPos(rx, sea - 1, z)).is(turf.getBlock()),
                "rock continues a started bog");
        helper.assertTrue(level.getBlockState(new BlockPos(rx, sea - 2, z)).is(Blocks.DIRT),
                "and GT6 still stops after two blocks");

        for (int cx : new int[] {x, tx, gx, rx}) clearColumn(level, cx, z, upper, lower);
        helper.succeed();
    }

    private static BlockState state(String id) {
        Block block = ForgeRegistries.BLOCKS.getValue(
                ResourceLocation.fromNamespaceAndPath("gregtech", id));
        return block == null || block == Blocks.AIR ? null : block.defaultBlockState();
    }

    /** Resets a column (and the block above GT6's window) so repeated runs start clean. */
    private static void clearColumn(ServerLevel level, int x, int z, int upper, int lower) {
        for (int y = upper + 2; y >= lower - 1; y--) {
            level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static void fillStone(ServerLevel level, int x, int z, int from, int to) {
        for (int y = from; y >= to; y--) {
            level.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 2);
        }
    }
}
