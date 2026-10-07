package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.blockentity.tool.PortableContainerBlockEntity;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import com.gregtech.gregtech.worldgen.GTSurfaceFlora;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.List;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class DungeonCupRepairTests {
    private static GTDungeonData data(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        return new GTDungeonData(helper.getLevel(), origin.getX(), origin.getY(), origin.getZ(),
                StoneType.LIMESTONE, StoneType.SLATE, 3, new byte[5][5], 2, 2, 0,
                new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(),
                RandomSource.create(12));
    }

    @GameTest(template = "test_empty")
    public static void dungeonCupIsTheRealPlaceableVessel(GameTestHelper helper) {
        GTDungeonData data = data(helper);
        helper.assertTrue(data.cup(1, 1, 1), "dungeon cup placed");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(
                ForgeRegistries.BLOCKS.getValue(GregTech.id("fluid_cup"))), "uses GT cup, not flower pot");
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof PortableContainerBlockEntity,
                "cup has a fluid vessel block entity");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void dungeonCupKeepsTheOriginalQuarterBucketDrink(GameTestHelper helper) {
        GTDungeonData data = data(helper);
        helper.assertTrue(data.cup(1, 1, 1, "Purple_Drink"), "filled dungeon cup placed");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof PortableContainerBlockEntity, "cup holds fluid in the vessel");
        var handler = entity.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(
                () -> new AssertionError("dungeon cup has no fluid handler"));
        var expected = GTFluids.stack("Purple_Drink", 250);
        helper.assertTrue(expected != null && handler.getFluidInTank(0).isFluidEqual(expected)
                        && handler.getFluidInTank(0).getAmount() == 250,
                "GT6 cup preserves its 250 mB drink");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void dungeonGlowtusUsesGt6DyeMetadataAndWaterPlacement(GameTestHelper helper) {
        helper.assertTrue(GTSurfaceFlora.GLOWTUS_COLOURS.equals(List.of(
                "black", "red", "green", "brown", "blue", "purple", "cyan", "light_gray",
                "gray", "pink", "lime", "yellow", "light_blue", "magenta", "orange", "white")),
                "GT6 DYE_NAMES metadata order");
        GTDungeonData data = data(helper);
        helper.assertTrue(data.set(1, 1, 1, Blocks.WATER.defaultBlockState()), "water under Glowtus");
        RandomSource expectedRolls = RandomSource.create(12);
        int expectedMeta = expectedRolls.nextInt(3) == 0 ? 3 : expectedRolls.nextInt(16);
        helper.assertTrue(data.glowtus(1, 2, 1), "dungeon placed Glowtus");
        BlockPos plant = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(helper.getLevel().getBlockState(plant).is(GTSurfaceFlora.glowtus(
                GTSurfaceFlora.GLOWTUS_COLOURS.get(expectedMeta))),
                "GT6 nextMetaA chooses the matching colour and floating block");
        helper.assertTrue(helper.getLevel().getBlockState(plant).getLightEmission() == 15,
                "GT6 Glowtus is a full-strength light source");
        helper.succeed();
    }
}
