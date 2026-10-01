package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.energy.EnergyNodeBlock;
import com.gregtech.gregtech.block.energy.GearboxBlock;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.blockentity.energy.GearboxBlockEntity;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Survival installation and signed RU flow across a GT6 custom gearbox. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class GearboxMechanicsRepairTests {
    private static final BlockPos BOX = new BlockPos(2, 1, 2);

    private static GearboxBlock block() {
        return (GearboxBlock) ForgeRegistries.BLOCKS.getValue(GregTech.id("gearbox_bronze"));
    }

    private static GearboxBlockEntity place(GameTestHelper h) {
        h.setBlock(BOX, block());
        return (GearboxBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(BOX));
    }

    private static BlockHitResult centerHit(BlockPos absolute, Direction face) {
        return new BlockHitResult(Vec3.atCenterOf(absolute).add(
                face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5),
                face, absolute, false);
    }

    @GameTest(template = "test_empty")
    public static void wrenchMountsARealMatchingGearAndReturnsIt(GameTestHelper h) {
        GearboxBlockEntity box = place(h);
        var player = h.makeMockPlayer();
        player.getAbilities().instabuild = false;
        ItemStack wrench = GTToolItem.create(GTToolType.WRENCH, Materials.Steel, GTMaterialRegistry.get("Wood"));
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        BlockPos pos = box.getBlockPos();
        var hit = centerHit(pos, Direction.NORTH);
        h.assertTrue(!box.hasGear(Direction.NORTH), "custom gearbox starts with no free gear");
        block().use(box.getBlockState(), h.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(!box.hasGear(Direction.NORTH), "ordinary wrench cannot conjure a missing gear");

        ItemStack wrong = GTItems.getStack(MaterialPrefix.gearGt, Materials.Steel, 1);
        player.getInventory().add(wrong);
        block().use(box.getBlockState(), h.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(!box.hasGear(Direction.NORTH), "wrong material gear cannot mount");

        ItemStack bronze = GTItems.getStack(MaterialPrefix.gearGt, Materials.Bronze, 2);
        h.assertTrue(!bronze.isEmpty(), "bronze large gear item exists");
        player.getInventory().add(bronze);
        block().use(box.getBlockState(), h.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(box.hasGear(Direction.NORTH) && box.gearCount() == 1,
                "matching physical gear mounts on clicked face");
        int afterMount = countBronzeGears(player);
        h.assertTrue(afterMount == 1, "one bronze gear consumed from survival inventory");

        block().use(box.getBlockState(), h.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(!box.hasGear(Direction.NORTH) && countBronzeGears(player) == 2,
                "dismount returns the actual bronze gear to inventory");
        h.succeed();
    }

    private static int countBronzeGears(net.minecraft.world.entity.player.Player player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (com.gregtech.gregtech.item.MaterialItem.isMaterialItem(stack, MaterialPrefix.gearGt, Materials.Bronze)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @GameTest(template = "test_empty")
    public static void monkeyWrenchChangesAxleWithoutDestroyingGearsAndSoftHammerStops(GameTestHelper h) {
        GearboxBlockEntity box = place(h);
        box.mountGear(Direction.NORTH);
        box.mountGear(Direction.SOUTH);
        h.assertTrue(!box.gearsWork(), "opposite pair without an axle cannot turn");
        var player = h.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND,
                GTToolItem.create(GTToolType.MONKEY_WRENCH, Materials.Steel, GTMaterialRegistry.get("Wood")));
        BlockPos pos = box.getBlockPos();
        block().use(box.getBlockState(), h.getLevel(), pos, player, InteractionHand.MAIN_HAND,
                centerHit(pos, Direction.NORTH));
        h.assertTrue(box.hasAxis(Direction.Axis.Z) && box.gearsWork() && box.gearCount() == 2,
                "Z axle makes N/S pair work without clearing either installed gear");

        player.setItemInHand(InteractionHand.MAIN_HAND,
                GTToolItem.create(GTToolType.SOFT_HAMMER, Materials.Steel, GTMaterialRegistry.get("Wood")));
        block().use(box.getBlockState(), h.getLevel(), pos, player, InteractionHand.MAIN_HAND,
                centerHit(pos, Direction.NORTH));
        h.assertTrue(box.isJammed() && !box.isEnergyAcceptingFrom(GregTechTags.Energy.RU, Direction.NORTH, false),
                "soft hammer switches the gearbox off rather than adding a gear");
        block().use(box.getBlockState(), h.getLevel(), pos, player, InteractionHand.MAIN_HAND,
                centerHit(pos, Direction.NORTH));
        h.assertTrue(!box.isJammed() && box.isEnergyAcceptingFrom(GregTechTags.Energy.RU, Direction.NORTH, false),
                "second soft-hammer click restarts the existing arrangement");

        var saved = box.saveForItem();
        h.assertTrue(saved.getByte("gearMask") != 0 && saved.getByte("axisCode") == 3,
                "gearbox item keeps mounted gear/axle data");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void gearedCornerEmitsOnServerTickAndConflictingInputsJam(GameTestHelper h) {
        GearboxBlockEntity box = place(h);
        box.mountGear(Direction.NORTH);
        box.mountGear(Direction.EAST);
        var receiverBlock = (EnergyNodeBlock) ForgeRegistries.BLOCKS.getValue(GregTech.id("rotation_transformer_bronze"));
        BlockPos east = BOX.east();
        h.setBlock(east, receiverBlock.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.WEST));
        var receiver = (EnergyNodeBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(east));
        var ru = GregTechTags.Energy.RU;
        h.assertTrue(box.gearsWork() && box.doEnergyInjection(ru, Direction.NORTH, 32, 2, false) == 2,
                "corner gears accept a simulated packet without changing receiver");
        h.assertTrue(receiver.stored() == 0 && box.doEnergyInjection(ru, Direction.NORTH, 32, 2, true) == 2,
                "two real 32-RU packets enter the gearbox");
        GearboxBlockEntity.serverTick(h.getLevel(), box.getBlockPos(), box.getBlockState(), box);
        h.assertTrue(receiver.stored() == 64 && box.transferredLast() == 64,
                "two packets leave adjacent face on the tick, conserving 64 RU");
        h.assertTrue(box.doEnergyInjection(ru, Direction.NORTH, 32, 1, true) == 1
                        && box.doEnergyInjection(ru, Direction.EAST, 32, 1, true) == 1
                        && box.isJammed(),
                "same-sign rotation from both adjacent gears conflicts and jams");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void freeAxlePassesStraightThroughAndOverspeedBreaksGears(GameTestHelper h) {
        GearboxBlockEntity box = place(h);
        box.toggleAxis(Direction.Axis.Z);
        var receiverBlock = (EnergyNodeBlock) ForgeRegistries.BLOCKS.getValue(GregTech.id("rotation_transformer_bronze"));
        BlockPos south = BOX.south();
        h.setBlock(south, receiverBlock.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH));
        var receiver = (EnergyNodeBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(south));
        var ru = GregTechTags.Energy.RU;
        h.assertTrue(box.doEnergyInjection(ru, Direction.NORTH, -32, 1, true) == 1
                        && receiver.stored() == 32,
                "un-geared selected axle passes signed RU to the opposite face immediately");
        h.assertTrue(receiver.saveWithoutMetadata().getBoolean("gt.rotation_negative_input"),
                "straight-through preserves rotation sign");

        box.mountGear(Direction.NORTH);
        for (int i = 0; i < 10; i++) {
            GearboxBlockEntity.serverTick(h.getLevel(), box.getBlockPos(), box.getBlockState(), box);
        }
        h.assertTrue(box.getEnergySizeInputMax(ru, Direction.NORTH) == 64
                        && box.doEnergyInjection(ru, Direction.NORTH, 65, 1, true) == 1
                        && box.gearCount() == 0,
                "after GT6 grace period, 65-RU packet destroys 64-RU gear");
        h.succeed();
    }
}
