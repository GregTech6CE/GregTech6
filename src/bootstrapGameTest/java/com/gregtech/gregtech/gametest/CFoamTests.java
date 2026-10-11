package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.misc.CFoamBlock;
import com.gregtech.gregtech.blockentity.CFoamBlockEntity;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Guards GT6's construction foam ({@code MultiTileEntityCFoam}): the freshly sprayed block is wet,
 * dries after GT6's 100 tick grace period plus a {@code rng(5900) == 0} roll per tick, and wet foam
 * that a player removes is scraped away without dropping anything.
 *
 * <p>Before this batch both foam blocks were plain decorative blocks — nothing ever hardened.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class CFoamTests {
    private static final int BASE_X = 42000;
    private static final int BASE_Z = 42000;
    private static final int BASE_Y = 210;

    private static BlockPos base(int index) {
        return new BlockPos(BASE_X + index * 8, BASE_Y, BASE_Z);
    }

    private static BlockPos placeFresh(ServerLevel level, BlockPos pos) {
        level.getBlockState(pos);
        for (int dy = -1; dy <= 2; dy++) level.setBlock(pos.above(dy), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, GTDecorBlocks.CFOAM_FRESH.get().defaultBlockState(), 3);
        return pos;
    }

    /** The wet block owns the block entity, the hardened one does not. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void freshFoamIsABlockEntity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = placeFresh(level, base(0));
        helper.assertTrue(GTDecorBlocks.CFOAM_FRESH.get().fresh(), "cfoam_fresh is the wet variant");
        helper.assertFalse(GTDecorBlocks.CFOAM.get().fresh(), "cfoam is the hardened variant");
        helper.assertTrue(level.getBlockEntity(pos) instanceof CFoamBlockEntity,
                "placing wet foam creates its block entity");
        helper.assertTrue(GTBlockEntities.CFOAM.get().isValid(GTDecorBlocks.CFOAM_FRESH.get().defaultBlockState()),
                "the cfoam block entity type covers the wet block");
        helper.assertTrue(CFoamBlockEntity.GRACE_TICKS == 100, "GT6 waits 100 ticks before drying");
        helper.assertTrue(CFoamBlockEntity.DRY_CHANCE == 5900, "GT6 rolls rng(5900)");
        helper.assertTrue(CFoamBlock.blockEntityType() == GTBlockEntities.CFOAM.get(),
                "the block reports its own entity type");
        helper.succeed();
    }

    /** GT6's drying: nothing before the grace period, hardened foam after the roll (or by hand). */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void foamDriesIntoHardenedFoam(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = placeFresh(level, base(1));
        var foam = (CFoamBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(foam != null, "the wet foam has its block entity");
        for (int i = 0; i < CFoamBlockEntity.GRACE_TICKS; i++) foam.tick();
        helper.assertTrue(level.getBlockState(pos).is(GTDecorBlocks.CFOAM_FRESH.get()),
                "GT6 never dries the foam inside the 100 tick grace period");
        helper.assertTrue(foam.timer() == CFoamBlockEntity.GRACE_TICKS, "the timer counts every tick");

        // The roll is probabilistic, so the test drives it: either the loop hits it or dry() does.
        boolean hardened = false;
        for (int i = 0; i < 200_000 && !hardened; i++) {
            hardened = foam.dry();
        }
        helper.assertTrue(hardened, "the foam hardened");
        helper.assertTrue(level.getBlockState(pos).is(GTDecorBlocks.CFOAM.get()),
                "GT6 swaps the wet block for the hardened one");
        helper.assertTrue(foam.dried(), "the block entity remembers it dried");
        // A second call is a no-op (GT6's `if (mFoamDried) return F`).
        helper.assertFalse(foam.dry(), "drying an already hardened block does nothing");
        helper.succeed();
    }

    /** Wet foam is scraped away (GT6's removeFoam), hardened foam drops itself. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void wetFoamDropsNothing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = placeFresh(level, base(2));
        BlockState wet = level.getBlockState(pos);
        List<ItemStack> wetDrops = wet.getDrops(new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, level.getBlockEntity(pos)));
        helper.assertTrue(wetDrops.isEmpty(), "GT6 removes wet foam without drops, got " + wetDrops);

        level.setBlock(pos, GTDecorBlocks.CFOAM.get().defaultBlockState(), 3);
        BlockState dry = level.getBlockState(pos);
        List<ItemStack> dryDrops = dry.getDrops(new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY));
        helper.assertTrue(dryDrops.size() == 1 && dryDrops.get(0).is(GTDecorBlocks.CFOAM.get().asItem()),
                "the hardened foam drops itself, got " + dryDrops);
        helper.assertTrue(CFoamBlock.isFresh(GTDecorBlocks.CFOAM_FRESH.get().defaultBlockState())
                        && !CFoamBlock.isFresh(GTDecorBlocks.CFOAM.get().defaultBlockState()),
                "isFresh distinguishes the two blocks");
        helper.succeed();
    }
}
