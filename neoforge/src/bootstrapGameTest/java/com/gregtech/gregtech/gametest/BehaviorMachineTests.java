package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.block.machine.BehaviorMachineBlock;
import com.gregtech.gregtech.blockentity.machine.BehaviorMachineBlockEntity;
import com.gregtech.gregtech.client.gui.BehaviorMachineMenu;
import com.gregtech.gregtech.jei.RecipeMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** Irrigator shell: world edit, energy, pipes, menu data and save. */
@GameTestHolder("gregtech_card16_behavior")
@PrefixGameTestTemplate(false)
public final class BehaviorMachineTests {
    private BehaviorMachineTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void irrigatorRegistersOutsideTheRecipeCatalog(GameTestHelper helper) {
        var blockId = ResourceLocation.parse("addon_sample:irrigator");
        helper.assertTrue(BuiltInRegistries.BLOCK.get(blockId) instanceof BehaviorMachineBlock, "irrigator block");
        helper.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ResourceLocation.parse("addon_sample:be_irrigator")),
                "shared block entity id");
        helper.assertTrue(BuiltInRegistries.MENU.containsKey(ResourceLocation.parse("gregtech:behavior_machine")),
                "one behavior menu");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(blockId), "irrigator item");
        for (var map : RecipeMap.RECIPE_MAP_LIST)
            helper.assertTrue(!"irrigator".equals(map.mNameInternal), "behavior machine creates no recipe map");
        var item = BuiltInRegistries.ITEM.get(blockId);
        for (var stacks : RecipeMachines.collect().values())
            for (var stack : stacks)
                helper.assertTrue(!stack.is(item), "irrigator is not a recipe catalyst");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void irrigatorWatersFarmlandAndObeysTheToggle(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 0, 2)).atY(250);
        var machine = place(helper, pos);
        var farmland = pos.east();
        helper.getLevel().setBlockAndUpdate(farmland, Blocks.FARMLAND.defaultBlockState());
        helper.assertTrue(machine.fluids().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000,
                "tank starts with water");
        helper.assertTrue(machine.doInject("EU", Direction.NORTH, 32, 2, true) == 0, "front face rejects EU");
        helper.assertTrue(machine.storedEnergy() == 0, "rejected EU is not stored");
        run(helper, machine, 5);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new BehaviorMachineMenu(1, player.getInventory(), machine);
        helper.assertTrue(menu.progress() == 0.5F, "menu shows progress");
        menu.removed(player);
        run(helper, machine, 7);
        helper.assertTrue(helper.getLevel().getBlockState(farmland).getValue(FarmBlock.MOISTURE) == 7, "farmland watered");
        helper.assertTrue(machine.state().getInt("watered") == 1, "behavior counted the watering");
        helper.assertTrue(machine.fluids().getFluidInTank(0).getAmount() == 900, "one hundred millibuckets consumed");
        helper.getLevel().setBlockAndUpdate(farmland, Blocks.FARMLAND.defaultBlockState());
        machine.flip(0);
        run(helper, machine, 12);
        helper.assertTrue(helper.getLevel().getBlockState(farmland).getValue(FarmBlock.MOISTURE) == 0, "toggle stops watering");
        helper.assertTrue(machine.state().getInt("watered") == 1, "toggle spends nothing");
        helper.getLevel().removeBlock(pos, false);
        helper.getLevel().removeBlock(farmland, false);
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void pipesFollowSlotRoles(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 0, 2)).atY(250);
        var machine = place(helper, pos);
        IItemHandler items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        IFluidHandler fluids = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        helper.assertTrue(items != null && fluids != null, "pipe capabilities");
        helper.assertTrue(!items.insertItem(0, new ItemStack(Items.COBBLESTONE), false).isEmpty(), "cobble rejected");
        helper.assertTrue(items.insertItem(0, new ItemStack(Items.BONE_MEAL), false).isEmpty(), "bone meal accepted");
        helper.assertTrue(!items.insertItem(1, new ItemStack(Items.BONE_MEAL), false).isEmpty(), "output rejects insertion");
        helper.assertTrue(fluids.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "lava rejected");
        helper.assertTrue(fluids.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "water accepted");
        helper.assertTrue(fluids.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "input tank is not drained by pipes");
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new BehaviorMachineMenu(1, player.getInventory(), machine);
        helper.assertTrue(menu.slots.get(0).x == 44 && menu.slots.get(0).y == 26, "input slot position");
        helper.assertTrue(menu.slots.get(1).x == 116 && menu.slots.get(1).y == 26, "output slot position");
        helper.assertTrue(menu.slots.get(2).y == 84, "player inventory position");
        helper.assertTrue(menu.isOn(0) && menu.tankAmount(0) == 1000, "menu shows the toggle and the tank");
        helper.assertTrue(menu.clickMenuButton(player, 0) && !menu.isOn(0), "menu button flips the toggle");
        menu.removed(player);
        helper.getLevel().removeBlock(pos, false);
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void restartKeepsInventoryFluidEnergyToggleAndState(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 0, 2)).atY(250);
        var machine = place(helper, pos);
        var lookup = helper.getLevel().registryAccess();
        machine.inventory().setStackInSlot(0, new ItemStack(Items.BONE_MEAL, 3));
        machine.fluids().fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE);
        machine.flip(0);
        machine.state().setInt("watered", 4);
        machine.state().setString("note", "kept");
        helper.assertTrue(machine.doInject("EU", Direction.UP, 32, 2, true) == 2, "up accepts two packets");
        long energy = machine.storedEnergy();
        var saved = machine.saveWithoutMetadata(lookup);
        helper.getLevel().removeBlock(pos, false);
        var restored = place(helper, pos);
        restored.loadWithComponents(saved, lookup);
        helper.assertTrue(restored.inventory().getStackInSlot(0).is(Items.BONE_MEAL)
                && restored.inventory().getStackInSlot(0).getCount() == 3, "items reload");
        helper.assertTrue(restored.fluids().getFluidInTank(0).getFluid() == Fluids.WATER
                && restored.fluids().getFluidInTank(0).getAmount() == 500, "fluid reloads");
        helper.assertTrue(restored.storedEnergy() == energy, "energy reloads");
        helper.assertTrue(!restored.toggle(0), "toggle reloads");
        helper.assertTrue(restored.state().getInt("watered") == 4, "behavior int reloads");
        helper.assertTrue("kept".equals(restored.state().getString("note")), "behavior string reloads");
        helper.getLevel().removeBlock(pos, false);
        helper.succeed();
    }

    private static BehaviorMachineBlockEntity place(GameTestHelper helper, BlockPos pos) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("addon_sample:irrigator"));
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState().setValue(BehaviorMachineBlock.FACING, Direction.NORTH));
        var entity = helper.getLevel().getBlockEntity(pos);
        if (!(entity instanceof BehaviorMachineBlockEntity machine)) throw new AssertionError("No irrigator");
        return machine;
    }

    private static void run(GameTestHelper helper, BehaviorMachineBlockEntity machine, int ticks) {
        var pos = machine.getBlockPos();
        for (int tick = 0; tick < ticks; tick++) {
            machine.doInject("EU", Direction.UP, 32, 2, true);
            BehaviorMachineBlockEntity.serverTick(helper.getLevel(), pos, machine.getBlockState(), machine);
        }
    }
}
