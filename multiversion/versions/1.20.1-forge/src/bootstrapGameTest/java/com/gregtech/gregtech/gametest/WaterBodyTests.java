package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.worldgen.GTFeatures;
import com.gregtech.gregtech.worldgen.GTWaterBodyFeature;
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
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * GT6's three water-body passes ({@code WorldgenOcean} / {@code WorldgenRiver} / {@code WorldgenSwamp},
 * {@code Loader_Worldgen:576-578}): the water of oceans, rivers and swamps becomes GT6's own sea,
 * river and swamp water, whose blocks the port had to add first.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class WaterBodyTests {
    private static final int BASE_X = 50000;
    private static final int BASE_Z = 50000;

    /** The three passes, their GT6 names, their fluid blocks and their biome gates. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void waterBodiesMatchGt6Table(GameTestHelper helper) {
        helper.assertTrue(GTWaterBodyFeature.PASSES.size() == 3, "GT6 registers three water bodies");
        // GT6's source insists on this order: ocean before river before swamp.
        helper.assertTrue(GTWaterBodyFeature.PASSES.get(0) == GTWaterBodyFeature.Kind.OCEAN
                        && GTWaterBodyFeature.PASSES.get(1) == GTWaterBodyFeature.Kind.RIVER
                        && GTWaterBodyFeature.PASSES.get(2) == GTWaterBodyFeature.Kind.SWAMP,
                "GT6's order is ocean -> river -> swamp");
        helper.assertTrue(GTWaterBodyFeature.Kind.OCEAN.registrationName().equals("ocean.seawater"),
                "Loader_Worldgen:576");
        helper.assertTrue(GTWaterBodyFeature.Kind.RIVER.registrationName().equals("river.riverwater"),
                "Loader_Worldgen:577");
        helper.assertTrue(GTWaterBodyFeature.Kind.SWAMP.registrationName().equals("swamp.dirtywater"),
                "Loader_Worldgen:578");

        List<String> problems = new ArrayList<>();
        for (GTWaterBodyFeature.Kind kind : GTWaterBodyFeature.PASSES) {
            Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(kind.blockId()));
            if (block == null || block == Blocks.AIR) {
                problems.add(kind.blockId() + " is not registered");
                continue;
            }
            if (!(block instanceof LiquidBlock liquid)) {
                problems.add(kind.blockId() + " is not a LiquidBlock");
                continue;
            }
            // The block must be a source block of the GT6 fluid with the same name.
            var fluidState = liquid.getFluidState(block.defaultBlockState());
            String expected = kind.blockId().substring("gregtech:".length());
            ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(fluidState.getType());
            if (fluidId == null || !fluidId.toString().equals("gregtech:" + expected)) {
                problems.add(kind.blockId() + " carries " + fluidId);
            } else if (!fluidState.isSource()) {
                problems.add(kind.blockId() + " does not default to a source block");
            }
        }
        helper.assertTrue(problems.isEmpty(), "water body blocks: " + problems);

        helper.assertTrue(GTFeatures.WATER_BODIES.getId().getPath().equals("gt_water_bodies"), "feature id");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .containsKey(GTWaterBodyFeature.CONFIGURED),
                "data/gregtech/worldgen/configured_feature/gt_water_bodies.json is loaded");
        helper.succeed();
    }

    /** GT6's biome gates, including the river pass's "not an ocean chunk" rule. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void biomeGatesMatchGt6(GameTestHelper helper) {
        ResourceLocation ocean = ResourceLocation.withDefaultNamespace("ocean");
        ResourceLocation river = ResourceLocation.withDefaultNamespace("river");
        ResourceLocation swamp = ResourceLocation.withDefaultNamespace("swamp");
        ResourceLocation beach = ResourceLocation.withDefaultNamespace("beach");
        ResourceLocation plains = ResourceLocation.withDefaultNamespace("plains");

        helper.assertTrue(GTWaterBodyFeature.matches(GTWaterBodyFeature.Kind.OCEAN, Set.of(ocean)),
                "an ocean biome runs the ocean pass");
        helper.assertTrue(!GTWaterBodyFeature.matches(GTWaterBodyFeature.Kind.OCEAN, Set.of(beach)),
                "GT6's BIOMES_OCEAN has no beaches");
        helper.assertTrue(GTWaterBodyFeature.matches(GTWaterBodyFeature.Kind.RIVER, Set.of(river)),
                "a river biome runs the river pass");
        helper.assertTrue(!GTWaterBodyFeature.matches(GTWaterBodyFeature.Kind.RIVER, Set.of(river, ocean)),
                "GT6 skips the river pass in a chunk that also touches an ocean");
        helper.assertTrue(GTWaterBodyFeature.matches(GTWaterBodyFeature.Kind.SWAMP, Set.of(swamp))
                        && GTWaterBodyFeature.matches(GTWaterBodyFeature.Kind.SWAMP,
                        Set.of(ResourceLocation.withDefaultNamespace("mangrove_swamp"))),
                "both 1.20.1 swamp biomes run the swamp pass");
        helper.assertTrue(!GTWaterBodyFeature.matches(GTWaterBodyFeature.Kind.SWAMP, Set.of(plains)),
                "plains get nothing");
        // A chunk that touches a river and a swamp: GT6 runs both passes, swamp last.
        helper.assertTrue(GTWaterBodyFeature.resolve(Set.of(river, swamp)) == GTWaterBodyFeature.Kind.SWAMP,
                "the swamp pass runs last, so it wins");
        helper.assertTrue(GTWaterBodyFeature.resolve(Set.of(ocean)) == GTWaterBodyFeature.Kind.OCEAN,
                "an ocean chunk is sea water");
        helper.assertTrue(GTWaterBodyFeature.resolve(Set.of(plains)) == null, "nothing to do in plains");
        helper.assertTrue(GTWorldgenBiomes.OCEAN.size() == 9 && GTWorldgenBiomes.OCEAN_BEACH.size() == 13,
                "GT6's BIOMES_OCEAN is BIOMES_OCEAN_BEACH without the four shores/beaches");
        helper.succeed();
    }

    /** GT6's per-column loop: the whole water column, down to the first opaque block. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void columnConversionMatchesGt6(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WorldGenLevel gen = (WorldGenLevel) level;
        int top = level.getSeaLevel();
        BlockState sea = state("seawater");
        BlockState river = state("riverwater");
        BlockState swamp = state("swampwater");
        helper.assertTrue(sea != null && river != null && swamp != null, "the three water blocks exist");

        // GT6 resets the first water block to metadata 0 but keeps the metadata of deeper
        // flowing/falling blocks when it swaps only their block ID.
        int x = BASE_X, z = BASE_Z;
        clearColumn(level, x, z, top);
        for (int y = top; y > top - 5; y--) {
            int fluidLevel = y == top ? 3 : y == top - 1 ? 6 : y == top - 2 ? 8 : 0;
            level.setBlock(new BlockPos(x, y, z),
                    Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, fluidLevel), 2);
        }
        level.setBlock(new BlockPos(x, top - 5, z), Blocks.SAND.defaultBlockState(), 2);
        helper.assertTrue(GTWaterBodyFeature.convertColumn(gen, x, z, top, sea, false),
                "the ocean pass converts water");
        for (int y = top; y > top - 5; y--) {
            helper.assertTrue(level.getBlockState(new BlockPos(x, y, z)).is(sea.getBlock()),
                    "GT6 converts the whole column, not just the surface (y=" + y + ")");
        }
        helper.assertTrue(level.getBlockState(new BlockPos(x, top, z)).getValue(LiquidBlock.LEVEL) == 0
                        && level.getBlockState(new BlockPos(x, top - 1, z)).getValue(LiquidBlock.LEVEL) == 6
                        && level.getBlockState(new BlockPos(x, top - 2, z)).getValue(LiquidBlock.LEVEL) == 8,
                "GT6 resets only the first water level and preserves the deeper flowing/falling levels");
        helper.assertTrue(level.getBlockState(new BlockPos(x, top - 5, z)).is(Blocks.SAND),
                "the opaque sea floor stays");

        // A column that is dry above the water line: nothing to convert, and no crash.
        int dx = BASE_X + 3;
        clearColumn(level, dx, z, top);
        level.setBlock(new BlockPos(dx, top, z), Blocks.STONE.defaultBlockState(), 2);
        helper.assertTrue(!GTWaterBodyFeature.convertColumn(gen, dx, z, top, river, false),
                "an opaque block at the top stops the column immediately");
        helper.assertTrue(level.getBlockState(new BlockPos(dx, top, z)).is(Blocks.STONE), "stone untouched");

        // GT6's non-water, non-opaque blocks (plants, ice) are skipped, not converted.
        int px = BASE_X + 6;
        clearColumn(level, px, z, top);
        level.setBlock(new BlockPos(px, top, z), Blocks.KELP.defaultBlockState(), 2);
        level.setBlock(new BlockPos(px, top - 1, z), Blocks.WATER.defaultBlockState(), 2);
        level.setBlock(new BlockPos(px, top - 2, z), Blocks.STONE.defaultBlockState(), 2);
        helper.assertTrue(GTWaterBodyFeature.convertColumn(gen, px, z, top, river, false),
                "the water under the kelp is converted");
        helper.assertTrue(level.getBlockState(new BlockPos(px, top, z)).is(Blocks.KELP),
                "the kelp stays kelp");
        helper.assertTrue(level.getBlockState(new BlockPos(px, top - 1, z)).is(river.getBlock()),
                "the water below it became river water");

        // The swamp pass also converts water an earlier pass already turned into GT water.
        int sx = BASE_X + 9;
        clearColumn(level, sx, z, top);
        level.setBlock(new BlockPos(sx, top, z), sea, 2);
        level.setBlock(new BlockPos(sx, top - 1, z), Blocks.WATER.defaultBlockState(), 2);
        level.setBlock(new BlockPos(sx, top - 2, z), Blocks.STONE.defaultBlockState(), 2);
        helper.assertTrue(GTWaterBodyFeature.convertColumn(gen, sx, z, top, swamp, true),
                "the swamp pass converts GT6's own waterlike blocks too");
        helper.assertTrue(level.getBlockState(new BlockPos(sx, top, z)).is(swamp.getBlock()),
                "sea water in a swamp column becomes swamp water");
        helper.assertTrue(level.getBlockState(new BlockPos(sx, top - 1, z)).is(swamp.getBlock()),
                "and the vanilla water below it too");
        // A non-swamp pass must leave GT water alone.
        helper.assertTrue(!GTWaterBodyFeature.convertColumn(gen, sx, z, top, river, false),
                "a plain pass has nothing left to convert");

        for (int cx : new int[] {x, dx, px, sx}) clearColumn(level, cx, z, top);
        helper.succeed();
    }

    private static void clearColumn(ServerLevel level, int x, int z, int top) {
        for (int y = top + 2; y > level.getMinBuildHeight(); y--) {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            if (state.isAir()) continue;
            level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
    }

    @Nullable
    private static BlockState state(String id) {
        Block block = ForgeRegistries.BLOCKS.getValue(
                ResourceLocation.fromNamespaceAndPath("gregtech", id));
        return block == null || block == Blocks.AIR ? null : block.defaultBlockState();
    }
}
