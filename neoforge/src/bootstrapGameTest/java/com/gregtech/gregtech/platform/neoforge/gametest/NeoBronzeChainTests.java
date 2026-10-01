package com.gregtech.gregtech.platform.neoforge.gametest;

import com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput;
import com.gregtech.gregtech.api.machine.crucible.CrucibleMaterialStack;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.platform.neoforge.smeltery.*;
import com.gregtech.gregtech.registry.GTItems;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;

/** One stage-level real chain check. Given equipment/ingredients, not a survival acquisition claim. */
@GameTestHolder("gregtech_playflow")
@PrefixGameTestTemplate(false)
public final class NeoBronzeChainTests {
    @GameTest(template = "test_bronze_chain", timeoutTicks = 9800)
    public static void coalCopperTinToFourBronzeIngots(GameTestHelper helper) {
        BlockPos boxPos = new BlockPos(2, 1, 3), cruciblePos = boxPos.above(), moldPos = cruciblePos.east();
        helper.setBlock(boxPos.below(), Blocks.STONE);
        helper.setBlock(moldPos.below(), Blocks.STONE);
        helper.setBlock(boxPos, SmelteryRegistries.BRICK_BURNING_BOX.get());
        helper.setBlock(cruciblePos, SmelteryRegistries.CERAMIC_CRUCIBLE.get());
        helper.setBlock(moldPos, SmelteryRegistries.CERAMIC_MOLD.get());
        var box = (SolidBurningBoxEntity) helper.getBlockEntity(boxPos);
        var crucible = (SmeltingCrucibleEntity) helper.getBlockEntity(cruciblePos);
        var mold = (MoldEntity) helper.getBlockEntity(moldPos);
        Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var wood = com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood");
        var chisel = SmelteryRegistries.CHISEL.get().assemble(Materials.Steel, wood);
        var pincers = SmelteryRegistries.PINCERS.get().assemble(Materials.Steel, wood);
        helper.assertTrue(!chisel.isEmpty() && !pincers.isEmpty() && !player.isCreative(), "Real assembled steel tools must consume wear");
        player.setItemInHand(InteractionHand.MAIN_HAND, chisel);
        for (int z = 0; z < 5; z++) for (int x = 0; x < 3; x++)
            click(helper, player, mold.getBlockPos(), Direction.UP, 0.125 + (x + 0.5) * 0.15, 0.1875, 0.125 + (z + 0.5) * 0.15);
        helper.assertTrue(chisel.getDamageValue() == 15 && mold.getMoldRequiredMaterialUnits() == GTValues.U, "Fifteen real chisel clicks carve one-U ingot form");
        List<ItemEntity> charges = List.of(drop(helper, crucible, GTItems.getStack(MaterialPrefix.ingot, Materials.Copper, 3), 0.35),
                drop(helper, crucible, GTItems.getStack(MaterialPrefix.ingot, Materials.Tin, 1), 0.65));
        int[] phase = {0}, pickups = {0};
        boolean[] finished = {false};
        helper.onEachTick(() -> {
            if (finished[0]) return;
            long units = CrucibleMaterialStack.total(crucible.getContentView()) + itemUnits(crucible.getCacheStack())
                    + mold.getMoldContentAmount() + bronzeCount(player) * GTValues.U;
            for (var entity : charges) if (entity.isAlive()) units += itemUnits(entity.getItem());
            helper.assertTrue(units == 4L * GTValues.U, "Copper/tin/bronze must conserve four U each tick");
            long bronze = crucible.getContentView().stream().filter(s -> s.material == Materials.Bronze.resolve()).mapToLong(s -> s.amount).sum();
            if (crucible.getTemperature() < 1357) helper.assertTrue(bronze == 0 && mold.getMoldContentAmount() == 0 && bronzeCount(player) == 0,
                    "No bronze may appear before the real 1357 K reaction");
            if (phase[0] == 0 && crucible.getCacheStack().isEmpty() && charges.stream().noneMatch(ItemEntity::isAlive)) {
                helper.assertTrue(crucible.getContentView().size() == 2 && crucible.getEnergyBuffer() == 0, "Inputs must enter cold without artificial HU");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COAL, 8));
                click(helper, player, box.getBlockPos(), Direction.NORTH, 0.5, 0.5, 0);
                helper.assertTrue(player.getMainHandItem().isEmpty() && box.getFuelStack().getCount() == 8, "Front click must move real coal into inventory");
                ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
                player.setItemInHand(InteractionHand.MAIN_HAND, flint);
                click(helper, player, box.getBlockPos(), Direction.NORTH, 0.5, 0.5, 0);
                helper.assertTrue(box.isBurning() && flint.getDamageValue() == 1, "Front ignition must wear actual flint and steel");
                phase[0] = 1;
            } else if (phase[0] == 1 && bronze > 0) {
                helper.assertTrue(bronze == 4L * GTValues.U && box.getFuelStack().getCount() < 8 && !box.getAshStack().isEmpty(),
                        "Natural coal heat and material ash must produce exactly four U bronze");
                LogUtils.getLogger().info("GT6_NEO_PLAYFLOW_ALLOY tick={} temperature={} amount={}", helper.getTick(), crucible.getTemperature(), bronze);
                helper.setBlock(boxPos, Blocks.AIR);
                player.setItemInHand(InteractionHand.MAIN_HAND, pincers);
                phase[0] = 2;
            } else if (phase[0] == 2) {
                long before = CrucibleMaterialStack.total(crucible.getContentView());
                int count = bronzeCount(player);
                click(helper, player, mold.getBlockPos(), Direction.UP, 0.5, 0.1875, 0.5);
                if (mold.getMoldContentAmount() > 0) {
                    long after = CrucibleMaterialStack.total(crucible.getContentView());
                    helper.assertTrue(mold.getMoldContentAmount() == GTValues.U && before - after <= GTValues.U, "Pour only one U; occupied mold refuses additional input");
                }
                if (bronzeCount(player) > count) pickups[0]++;
                if (pickups[0] == 4) {
                    helper.assertTrue(pincers.getDamageValue() == 4 && bronzeCount(player) == 4 && crucible.getContentView().isEmpty()
                            && mold.getMoldContentAmount() == 0, "Four real casts deliver four ingots with four tool wear");
                    phase[0] = 3;
                }
            } else if (phase[0] == 3) {
                click(helper, player, mold.getBlockPos(), Direction.UP, 0.5, 0.1875, 0.5);
                helper.assertTrue(bronzeCount(player) == 4 && mold.getMoldContentAmount() == 0, "Repeated empty interaction must not mint a fifth ingot");
                finished[0] = true;
                LogUtils.getLogger().info("GT6_NEO_PLAYFLOW_COMPLETE tick={} ingots=4 chiselWear=15 pincersWear=4", helper.getTick());
                helper.succeed();
            }
        });
    }
    private static void click(GameTestHelper helper, Player player, BlockPos pos, Direction side, double x, double y, double z) {
        var hit = new BlockHitResult(new Vec3(pos.getX() + x, pos.getY() + y, pos.getZ() + z), side, pos, false);
        var state = helper.getLevel().getBlockState(pos);
        state.useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    }
    private static ItemEntity drop(GameTestHelper helper, SmeltingCrucibleEntity crucible, ItemStack stack, double x) {
        helper.assertTrue(!stack.isEmpty(), "Registered input item must exist");
        BlockPos pos = crucible.getBlockPos();
        ItemEntity entity = new ItemEntity(helper.getLevel(), pos.getX() + x, pos.getY() + 1.1, pos.getZ() + 0.5, stack);
        entity.setDeltaMovement(Vec3.ZERO);
        helper.assertTrue(helper.getLevel().addFreshEntity(entity), "Actual input entity must spawn");
        return entity;
    }
    private static long itemUnits(ItemStack stack) { return CrucibleMaterialStack.total(CrucibleItemInput.parse(stack)) * stack.getCount(); }
    private static int bronzeCount(Player player) {
        ItemStack bronze = GTItems.getStack(MaterialPrefix.ingot, Materials.Bronze);
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (ItemStack.isSameItemSameComponents(stack, bronze)) count += stack.getCount();
        }
        return count;
    }
}
