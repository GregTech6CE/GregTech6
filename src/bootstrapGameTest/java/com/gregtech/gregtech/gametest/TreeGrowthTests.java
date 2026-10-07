package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.wood.WoodSpecies;
import com.gregtech.gregtech.registry.GTWoods;
import com.gregtech.gregtech.worldgen.GTFeatures;
import com.gregtech.gregtech.worldgen.GTTreeGrower;
import com.gregtech.gregtech.worldgen.GTTreeShapes;
import com.gregtech.gregtech.worldgen.GTTreesFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Guards the port's GT6 trees: {@link GTTreeShapes} (the literal port of
 * {@code BlockTreeSaplingAB.grow} / {@code BlockTreeSaplingCD.grow}) and
 * {@link GTTreesFeature} (the port of {@code Loader_Worldgen:608-616}'s {@code WorldgenTree*}).
 *
 * <p>Before this batch the port's species saplings carried a vanilla {@code OakTreeGrower}, so every
 * GT6 sapling grew oak logs and oak leaves and GT6's tree shapes, hazel/cinnamon/coconut/blue spruce
 * and all tree worldgen were missing. The trees are grown far outside the test structures (x/z
 * 20000+) and above any terrain (the GameTest level is a real world, so y=100 is not guaranteed to
 * be open air) so they cannot interact with any other test.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class TreeGrowthTests {
    private static final int BASE_X = 20000;
    private static final int BASE_Z = 20000;
    /**
     * Above any generated terrain and below the GameTest level's build limit: the test world is a
     * 256-tall overworld (y=260 made every {@code maxHeight} call return 0) whose terrain reaches
     * roughly y=140, and the tallest shape needs 16 free blocks plus its canopy.
     */
    private static final int BASE_Y = 210;
    /** Canopies reach at most 7 blocks (maple/willow/spruce), so 40 blocks never overlap. */
    private static final int SPACING = 40;

    private static BlockPos base(int index) {
        return new BlockPos(BASE_X + index * SPACING, BASE_Y, BASE_Z);
    }

    /**
     * Clears the site before growing, so the test is repeatable: the GameTest world directory is
     * reused between runs, so the trees of a previous run are still standing at the same positions.
     */
    private static void clear(ServerLevel level, BlockPos center, int radius, int height) {
        level.getBlockState(center); // force the chunk
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -2; dy <= height; dy++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.getBlockState(cursor).isAir()) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static int count(ServerLevel level, BlockPos base, Predicate<BlockState> test, int radius, int height) {
        int found = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y <= height; y++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(base.getX() + dx, base.getY() + y, base.getZ() + dz);
                    if (test.test(level.getBlockState(cursor))) found++;
                }
            }
        }
        return found;
    }

    /** Distinct block ids matching {@code test} in the scan box, for failure messages. */
    private static String describe(ServerLevel level, BlockPos base, Predicate<BlockState> test, int radius, int height) {
        Map<String, Integer> found = new LinkedHashMap<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y <= height; y++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(base.getX() + dx, base.getY() + y, base.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (test.test(state)) {
                        found.merge(String.valueOf(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock())), 1, Integer::sum);
                    }
                }
            }
        }
        return found.toString();
    }

    /** Every species builds a tree out of its own logs/leaves, tall enough for GT6's gate. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void everySpeciesGrowsItsOwnGt6Tree(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(level.getMaxBuildHeight() > BASE_Y + 24,
                "the tree test needs 24 blocks of headroom at y=" + BASE_Y
                        + ", build limit is " + level.getMaxBuildHeight());
        RandomSource random = RandomSource.create(4242L);
        List<String> problems = new ArrayList<>();
        int index = 0;
        for (WoodSpecies species : WoodSpecies.values()) {
            BlockPos pos = base(index++);
            clear(level, pos, 8, 22);
            GTTreeShapes.Shape shape = GTTreeShapes.shapeOf(species);
            if (!GTTreeShapes.grow(level, pos, species, random)) {
                problems.add(species + ": the " + shape + " shape refused to grow (free height "
                        + GTTreeShapes.maxHeight(level, pos, shape.minHeight()) + "/" + shape.minHeight()
                        + ", above=" + net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(
                                level.getBlockState(pos.above()).getBlock())
                        + ", above2=" + net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(
                                level.getBlockState(pos.above(2)).getBlock())
                        + ", surface=" + level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                                pos.getX(), pos.getZ())
                        + ", build limit " + level.getMaxBuildHeight() + ")");
                continue;
            }
            int logs = count(level, pos, state -> state.is(GTWoods.log(species)), 1, 24);
            int leaves = count(level, pos, state -> state.is(GTWoods.leaves(species)), 8, 24);
            int foreign = count(level, pos,
                    state -> state.is(BlockTags.LOGS) && !state.is(GTWoods.log(species)), 8, 24);
            if (logs < shape.minTrunk()) {
                problems.add(species + ": " + logs + " logs, GT6's " + shape + " produces at least "
                        + shape.minTrunk());
            }
            if (leaves == 0) problems.add(species + ": no leaves of its own species");
            if (foreign != 0) {
                problems.add(species + ": " + foreign + " foreign logs "
                        + describe(level, pos, state -> state.is(BlockTags.LOGS) && !state.is(GTWoods.log(species)), 8, 24));
            }
        }
        helper.assertTrue(problems.isEmpty(), "GT6 trees, problems (" + problems.size() + "): "
                + problems.subList(0, Math.min(6, problems.size())));
        helper.succeed();
    }

    /** The sapling path ({@code SaplingBlock} → {@link GTTreeGrower}) grows the species tree, not an oak. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void saplingsGrowTheirGt6Tree(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RandomSource random = RandomSource.create(90210L);
        List<String> problems = new ArrayList<>();
        int index = 100; // keep clear of the shape test's positions
        for (WoodSpecies species : List.of(WoodSpecies.RUBBER, WoodSpecies.MAPLE, WoodSpecies.HAZEL,
                WoodSpecies.COCONUT, WoodSpecies.BLUE_SPRUCE)) {
            BlockPos pos = base(index++);
            clear(level, pos, 8, 22);
            level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            level.setBlock(pos, GTWoods.sapling(species).defaultBlockState(), 3);
            GTTreeGrower grower = new GTTreeGrower(species);
            boolean grew = grower.growTree(level, level.getChunkSource().getGenerator(), pos,
                    level.getBlockState(pos), random);
            if (!grew) {
                problems.add(species + ": the grower did not grow the tree");
                continue;
            }
            if (level.getBlockState(pos).is(GTWoods.sapling(species))) {
                problems.add(species + ": the sapling survived the growth (GT6 replaces it with a log)");
            }
            if (count(level, pos, state -> state.is(GTWoods.log(species)), 1, 24) == 0) {
                problems.add(species + ": no logs of its own species");
            }
            int oaks = count(level, pos, state -> state.is(Blocks.OAK_LOG) || state.is(Blocks.OAK_LEAVES), 8, 24);
            if (oaks != 0) problems.add(species + ": grew " + oaks + " vanilla oak blocks");
            if (count(level, pos, state -> state.is(GTWoods.leaves(species)), 8, 24) == 0) {
                problems.add(species + ": no leaves of its own species");
            }
        }
        helper.assertTrue(problems.isEmpty(), "sapling growth, problems (" + problems.size() + "): "
                + problems.subList(0, Math.min(6, problems.size())));
        helper.succeed();
    }

    /**
     * GT6's height gate in {@code BlockTreeSaplingCD.grow} is {@code getMaxHeight(..., 16) < 16}.
     * {@code getMaxHeight} only inspects the 15 offsets above the sapling, so 14 free blocks must
     * fail and 15 must pass — the spruce is exactly one block more lenient than its own constant.
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void blueSpruceHeightGate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RandomSource random = RandomSource.create(7L);
        BlockPos tight = base(200);
        clear(level, tight, 8, 20);
        level.setBlock(tight.above(15), Blocks.STONE.defaultBlockState(), 3); // 14 free blocks
        helper.assertTrue(GTTreeShapes.maxHeight(level, tight, 16) == 14,
                "14 free blocks, got " + GTTreeShapes.maxHeight(level, tight, 16));
        helper.assertFalse(GTTreeShapes.grow(level, tight, WoodSpecies.BLUE_SPRUCE, random),
                "14 free blocks must not fit GT6's blue spruce");
        helper.assertTrue(count(level, tight, state -> state.is(GTWoods.log(WoodSpecies.BLUE_SPRUCE)), 1, 24) == 0,
                "the failed gate must not leave a trunk behind");
        level.setBlock(tight.above(15), Blocks.AIR.defaultBlockState(), 3);

        BlockPos roomy = base(201);
        clear(level, roomy, 8, 20);
        level.setBlock(roomy.above(16), Blocks.STONE.defaultBlockState(), 3); // 15 free blocks
        int free = GTTreeShapes.maxHeight(level, roomy, 16);
        helper.assertTrue(free == 16, "15 free blocks, got " + free);
        helper.assertTrue(GTTreeShapes.grow(level, roomy, WoodSpecies.BLUE_SPRUCE, random),
                "15 free blocks fit GT6's blue spruce");
        int trunk = count(level, roomy, state -> state.is(GTWoods.log(WoodSpecies.BLUE_SPRUCE)), 1, 17);
        helper.assertTrue(trunk >= 14 && trunk <= 16,
                "GT6's formula gives a 14-16 tall tree, got " + trunk);
        helper.succeed();
    }

    /** The worldgen table is GT6's own list: species, probabilities and biome sets. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void worldgenTreeTableMatchesGt6(GameTestHelper helper) {
        Map<WoodSpecies, Integer> expected = new LinkedHashMap<>();
        expected.put(WoodSpecies.RUBBER, 5);       // Loader_Worldgen:608
        expected.put(WoodSpecies.MAPLE, 5);        // :609
        expected.put(WoodSpecies.WILLOW, 4);       // :610
        expected.put(WoodSpecies.BLUE_MAHOE, 3);   // :611
        expected.put(WoodSpecies.HAZEL, 32);       // :612
        expected.put(WoodSpecies.CINNAMON, 3);     // :613
        expected.put(WoodSpecies.COCONUT, 1);      // :614
        expected.put(WoodSpecies.BLUE_SPRUCE, 32); // :616
        helper.assertTrue(GTTreesFeature.ENTRIES.size() == expected.size(),
                "GT6 registers 8 overworld trees, got " + GTTreesFeature.ENTRIES.size() + ": "
                        + GTTreesFeature.ENTRIES.stream().map(e -> e.species().id()).toList());
        for (GTTreesFeature.Entry entry : GTTreesFeature.ENTRIES) {
            Integer want = expected.get(entry.species());
            helper.assertTrue(want != null, "unexpected entry " + entry.species());
            helper.assertTrue(entry.probability() == want,
                    entry.species() + ": GT6 uses 1/" + want + ", got 1/" + entry.probability());
            helper.assertTrue(!entry.biomes().isEmpty(),
                    entry.species() + ": no 1.20.1 biome for GT6's " + entry.gt6Biomes());
            helper.assertTrue(GTTreeShapes.shapeOf(entry.species()) != null,
                    entry.species() + ": no GT6 shape");
        }
        // Rainbowood is GT6-registered but its biome set is the modded "Enchanted Forest" only.
        helper.assertTrue(GTTreesFeature.ENTRIES.stream().noneMatch(e -> e.species() == WoodSpecies.RAINBOWOOD),
                "rainbowood has no vanilla biome in GT6 either");
        helper.assertTrue(GTTreeShapes.shapeOf(WoodSpecies.RAINBOWOOD) == GTTreeShapes.Shape.RAINBOWOOD,
                "its sapling still grows GT6's rainbowood shape");
        helper.assertTrue(GTTreesFeature.ENTRIES.stream()
                        .filter(e -> e.species() == WoodSpecies.RUBBER).findFirst().orElseThrow()
                        .biomes().contains(ResourceLocation.withDefaultNamespace("taiga")),
                "rubber is GT6's taiga tree (BIOMES_RUBBER)");
        // The feature is registered and its configured feature exists in the data pack.
        helper.assertTrue(GTFeatures.TREES.getId().getPath().equals("gt_trees"), "feature id");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .containsKey(GTTreesFeature.CONFIGURED),
                "data/gregtech/worldgen/configured_feature/gt_trees.json is loaded");
        helper.assertTrue(GTTreeShapes.shapeOf(WoodSpecies.PINE) == GTTreeShapes.Shape.BLUE_SPRUCE
                        && GTTreeShapes.shapeOf(WoodSpecies.EBONY) == GTTreeShapes.Shape.MAPLE
                        && GTTreeShapes.shapeOf(WoodSpecies.WHITE_MAHOE) == GTTreeShapes.Shape.BLUE_MAHOE,
                "the wood-dictionary species fall back to the closest GT6 shape");
        helper.succeed();
    }
}
