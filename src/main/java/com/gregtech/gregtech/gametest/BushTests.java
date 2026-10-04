package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.plant.BushBlock;
import com.gregtech.gregtech.blockentity.BushBlockEntity;
import com.gregtech.gregtech.content.plant.GTBerryBushes;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTBushes;
import com.gregtech.gregtech.worldgen.GTBushesFeature;
import com.gregtech.gregtech.worldgen.GTFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Guards GT6's berry bush ({@code MultiTileEntityBush}, multi-tile 32759): its four stages, the
 * berry type it carries, GT6's growth timing and the harvest interaction, plus the
 * {@code WorldgenBushes} table.
 *
 * <p>GT6 tints the bush per stage from the {@code BushesGT} colour table and grows one stage per 256
 * growth increments in 128-tick cycles (rain and light give extra increments).
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class BushTests {
    private static final int BASE_X = 30000;
    private static final int BASE_Z = 30000;
    private static final int BASE_Y = 210;

    private static BlockPos base(int index) {
        return new BlockPos(BASE_X + index * 16, BASE_Y, BASE_Z);
    }

    private static BlockPos placeBush(ServerLevel level, BlockPos pos, String berry) {
        level.getBlockState(pos);
        // Drop any bush (and its block entity) left over from an earlier run: the GameTest world
        // directory is reused, so the berry type and growth counter would otherwise leak across runs.
        level.removeBlock(pos, false);
        level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        level.setBlock(pos, GTBushes.BUSH.get().defaultBlockState(), 3);
        if (level.getBlockEntity(pos) instanceof BushBlockEntity bush) bush.setBerry(berry == null ? "" : berry);
        return pos;
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                net.minecraft.core.Direction.UP, pos, false);
    }

    /** The block, its block entity, its assets and GT6's colour table. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void bushIsRegistered(GameTestHelper helper) throws Exception {
        List<String> problems = new ArrayList<>();
        BlockPos pos = placeBush(helper.getLevel(), base(0), "blueberry");
        if (!(helper.getLevel().getBlockEntity(pos) instanceof BushBlockEntity)) {
            problems.add("placing the bush must create its block entity");
        }
        if (!GTBlockEntities.BUSH.get().isValid(GTBushes.BUSH.get().defaultBlockState())) {
            problems.add("the bush block entity type must cover the bush");
        }
        for (String asset : new String[]{
                "assets/gregtech/blockstates/bush.json",
                "assets/gregtech/models/block/plants/bush_stage0.json",
                "assets/gregtech/models/block/plants/bush_stage3.json",
                "assets/gregtech/models/item/bush.json",
                "assets/gregtech/textures/block/plants/bush/colored/bush.png",
                "assets/gregtech/textures/block/plants/bush/overlay/berries.png"}) {
            if (BushTests.class.getClassLoader().getResource(asset) == null) problems.add("missing " + asset);
        }
        // GT6's BushesGT table: 8 berries with GT6's own colours.
        if (GTBerryBushes.size() != 8) problems.add("GT6 has 8 bush berries, got " + GTBerryBushes.size());
        var blueberry = GTBerryBushes.byId("blueberry");
        if (blueberry == null || blueberry.bush() != 0x22ff22 || blueberry.bloom() != 0xffcccc
                || blueberry.immature() != 0x6666dd || blueberry.berry() != 0x0000ff) {
            problems.add("blueberry colours are not GT6's BushesGT entry");
        }
        var cranberry = GTBerryBushes.byId("cranberry");
        if (cranberry == null || cranberry.berry() != 0xff0000) problems.add("cranberry berry colour");
        // The stage tints follow GT6's getRenderPasses2 switch.
        if (GTBerryBushes.stageColour(blueberry, 0) != blueberry.bush()
                || GTBerryBushes.stageColour(blueberry, 1) != blueberry.bloom()
                || GTBerryBushes.stageColour(blueberry, 2) != blueberry.immature()
                || GTBerryBushes.stageColour(blueberry, 3) != blueberry.berry()) {
            problems.add("stage tints do not follow GT6's switch");
        }
        if (GTBerryBushes.of(new ItemStack(Blocks.STONE.asItem())) != null) {
            problems.add("stone is not a GT6 bush berry");
        }
        helper.assertTrue(problems.isEmpty(), "bush registration (" + problems.size() + "): "
                + problems.subList(0, Math.min(5, problems.size())));
        helper.succeed();
    }

    /** GT6's growth timing: one stage per 256 increments, and nothing grows without a berry. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void bushGrowsOneStagePer256Increments(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = placeBush(level, base(1), "raspberry");
        var bush = (BushBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(bush != null, "the bush has a block entity");
        helper.assertTrue(bush.stage() == 0, "a planted bush starts at stage 0");

        int increments = 0;
        while (bush.stage() == 0 && increments < 4096) {
            increments += bush.grow();
        }
        helper.assertTrue(bush.stage() == 1, "the bush reached stage 1, stage is " + bush.stage());
        helper.assertTrue(increments == BushBlockEntity.GROWTH_PER_STAGE,
                "GT6 advances one stage per " + BushBlockEntity.GROWTH_PER_STAGE + " increments, got " + increments);

        // A bush without a berry never grows (GT6 requires a valid mBerry).
        BlockPos empty = placeBush(level, base(2), null);
        var emptyBush = (BushBlockEntity) level.getBlockEntity(empty);
        helper.assertTrue(emptyBush != null && emptyBush.berryId().isEmpty(), "the second bush has no berry");
        for (int i = 0; i < 600; i++) emptyBush.grow();
        helper.assertTrue(emptyBush.stage() == 0, "a berryless bush does not grow");

        // GT6's ground gate: the bush only grows on plantable greens.
        BlockPos stone = base(3);
        level.getBlockState(stone);
        level.setBlock(stone.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(stone, GTBushes.BUSH.get().defaultBlockState(), 3);
        var onStone = (BushBlockEntity) level.getBlockEntity(stone);
        helper.assertTrue(onStone != null, "the bush on stone has a block entity");
        onStone.setBerry("raspberry");
        onStone=(BushBlockEntity)level.getBlockEntity(stone);
        helper.assertTrue(onStone.speed() == 0, "GT6's mSpeed is 0 on stone");
        helper.assertTrue(onStone.grow() == 0, "no growth on stone");
        helper.succeed();
    }

    /** Harvesting hands out 1-2 berries of the bush's type and resets the stage. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void bushHarvestHandsOutItsBerries(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = placeBush(level, base(10), "black_currants");
        var bush = (BushBlockEntity) level.getBlockEntity(pos);
        BlockState ripe = level.getBlockState(pos).setValue(BushBlock.STAGE, 3);
        level.setBlock(pos, ripe, 3);

        Player player = helper.makeMockPlayer();
        InteractionResult result = GTBushes.BUSH.get().use(level.getBlockState(pos), level, pos, player,
                InteractionHand.MAIN_HAND, hit(pos));
        helper.assertTrue(result.consumesAction(), "harvesting a ripe bush is an interaction");
        ItemStack expected = bush.berryStack(1);
        int harvested = player.getInventory().countItem(expected.getItem());
        helper.assertTrue(harvested >= 1 && harvested <= 2, "GT6 hands out 1-2 berries, gave " + harvested);
        helper.assertTrue(level.getBlockState(pos).getValue(BushBlock.STAGE) == 0,
                "GT6 resets the stage to 0 after harvesting");

        // An unripe bush gives nothing.
        Player second = helper.makeMockPlayer();
        helper.assertTrue(GTBushes.BUSH.get().use(level.getBlockState(pos), level, pos, second,
                        InteractionHand.MAIN_HAND, hit(pos)) == InteractionResult.PASS,
                "an unripe bush does nothing");

        // A berryless bush adopts the berry the player holds (GT6 accepts any bush berry; gooseberry
        // is deliberately NOT one of GT6's eight BushList berries).
        BlockPos bare = placeBush(level, base(11), null);
        var bareBush = (BushBlockEntity) level.getBlockEntity(bare);
        Player planter = helper.makeMockPlayer();
        planter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ForgeRegistries.ITEMS.getValue(
                ResourceLocation.fromNamespaceAndPath("gregtech", "cranberry"))));
        InteractionResult planted = GTBushes.BUSH.get().use(level.getBlockState(bare), level, bare, planter,
                InteractionHand.MAIN_HAND, hit(bare));
        helper.assertTrue(planted.consumesAction(), "setting the berry type is an interaction");
        bareBush=(BushBlockEntity)level.getBlockEntity(bare);
        helper.assertTrue("cranberry".equals(bareBush.berryId()),
                "the bush adopted the cranberry, got " + bareBush.berryId());
        helper.succeed();
    }

    /** The worldgen table is GT6's: 1 bush per chunk with a 1/4 roll, plains and woods, noise-typed. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void bushWorldgenMatchesGt6(GameTestHelper helper) {
        helper.assertTrue(GTBushesFeature.AMOUNT == 1 && GTBushesFeature.PROBABILITY == 4,
                "GT6 registers WorldgenBushes with (amount 1, probability 4)");
        helper.assertTrue(GTBushesFeature.bushBiome(ResourceLocation.withDefaultNamespace("plains")),
                "plains get bushes");
        helper.assertTrue(GTBushesFeature.bushBiome(ResourceLocation.withDefaultNamespace("forest")),
                "woods get bushes");
        helper.assertFalse(GTBushesFeature.bushBiome(ResourceLocation.withDefaultNamespace("snowy_plains")),
                "frozen biomes never get bushes (GT6's !BIOMES_FROZEN)");
        helper.assertFalse(GTBushesFeature.bushBiome(ResourceLocation.withDefaultNamespace("desert")),
                "GT6 only plants plains and woods bushes");
        helper.assertTrue(GTFeatures.BUSHES.getId().getPath().equals("gt_bushes"), "feature id");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .containsKey(GTBushesFeature.CONFIGURED),
                "data/gregtech/worldgen/configured_feature/gt_bushes.json is loaded");
        // GT6 picks the berry with its value noise at (x/2, 300, z/2) — deterministic per position.
        int first = GTBushesFeature.berryIndex((net.minecraft.world.level.WorldGenLevel) helper.getLevel(),
                BASE_X, BASE_Z);
        int again = GTBushesFeature.berryIndex((net.minecraft.world.level.WorldGenLevel) helper.getLevel(),
                BASE_X, BASE_Z);
        helper.assertTrue(first == again, "the berry type is deterministic per position");
        helper.assertTrue(first >= 0 && first < GTBerryBushes.worldgenSize(),
                "the noise index is inside the berry table, got " + first);
        helper.succeed();
    }
}
