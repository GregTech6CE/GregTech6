package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.machine.BehaviorMachineBlock;
import com.gregtech.gregtech.blockentity.machine.BehaviorMachineBlockEntity;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.client.gui.BehaviorMachineMenu;
import com.gregtech.gregtech.jei.RecipeMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
//?}

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
        helper.assertTrue(BuiltInRegistries.MENU.containsKey(ResourceLocation.parse("gt6ce:behavior_machine")),
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
//? if >=1.21.1 {
/*        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
*///?} else {
        var player = helper.makeMockPlayer();
//?}
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
//? if >=1.21.1 {
/*        IItemHandler items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        IFluidHandler fluids = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        helper.assertTrue(items != null && fluids != null, "pipe capabilities");
*///?} else {
        IItemHandler items = machine.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER)
                .orElseThrow(() -> new AssertionError("behavior machine has no item handler"));
        IFluidHandler fluids = machine.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER)
                .orElseThrow(() -> new AssertionError("behavior machine has no fluid handler"));
//?}
        helper.assertTrue(!items.insertItem(0, new ItemStack(Items.COBBLESTONE), false).isEmpty(), "cobble rejected");
        helper.assertTrue(items.insertItem(0, new ItemStack(Items.BONE_MEAL), false).isEmpty(), "bone meal accepted");
        helper.assertTrue(!items.insertItem(1, new ItemStack(Items.BONE_MEAL), false).isEmpty(), "output rejects insertion");
        helper.assertTrue(fluids.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "lava rejected");
        helper.assertTrue(fluids.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "water accepted");
        helper.assertTrue(fluids.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "input tank is not drained by pipes");
//? if >=1.21.1 {
/*        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
*///?} else {
        var player = helper.makeMockPlayer();
//?}
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
//? if >=1.21.1 {
/*        var lookup = helper.getLevel().registryAccess();
*///?} else {
//?}
        machine.inventory().setStackInSlot(0, new ItemStack(Items.BONE_MEAL, 3));
        machine.fluids().fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE);
        machine.flip(0);
        machine.state().setInt("watered", 4);
        machine.state().setString("note", "kept");
        helper.assertTrue(machine.doInject("EU", Direction.UP, 32, 2, true) == 2, "up accepts two packets");
        long energy = machine.storedEnergy();
//? if >=1.21.1 {
/*        var saved = machine.saveWithoutMetadata(lookup);
*///?} else {
        var saved = machine.saveWithoutMetadata();
//?}
        helper.getLevel().removeBlock(pos, false);
        var restored = place(helper, pos);
//? if >=1.21.1 {
/*        restored.loadWithComponents(saved, lookup);
*///?} else {
        restored.load(saved);
//?}
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

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void outputOnlyMachineEmitsAndAcceptsNothing(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 0, 2)).atY(250);
        var machine = place(helper, pos, "addon_sample:sample_wheel");
        for (Direction side : Direction.values()) {
            helper.assertTrue(machine.doInject("EU", side, 32, 1, true) == 0, side + " rejects EU");
            helper.assertTrue(!machine.isEnergyAcceptingFrom("EU", side, false), side + " accepts nothing");
        }
        helper.assertTrue(machine.storedEnergy() == 0, "rejected packets are not stored");
        helper.assertTrue(machine.declaration().energy() == null, "no main input");
        BehaviorMachineBlockEntity.serverTick(helper.getLevel(), pos, machine.getBlockState(), machine);
        helper.assertTrue(machine.state().getLong("seen") == 0 && machine.state().getLong("after") == 0,
                "the main cache reads as zero");
        helper.assertTrue(machine.state().getLong("added") == 32, "generate fills the output buffer");
        helper.assertTrue(machine.state().getLong("sent") == 0 && machine.storedEnergy() == 32, "the packet waits");
        var above = pos.above();
        var receiverBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("addon_sample:irrigator"));
        helper.getLevel().setBlockAndUpdate(above, receiverBlock.defaultBlockState().setValue(BehaviorMachineBlock.FACING, Direction.NORTH));
        var receiver = helper.getLevel().getBlockEntity(above);
        helper.assertTrue(receiver instanceof BehaviorMachineBlockEntity, "receiver");
        BehaviorMachineBlockEntity.serverTick(helper.getLevel(), pos, machine.getBlockState(), machine);
        helper.assertTrue(machine.state().getLong("sent") == 1, "one packet leaves through the top");
        helper.assertTrue(((BehaviorMachineBlockEntity) receiver).storedEnergy() == 32, "the neighbor stored the packet");
        helper.assertTrue(machine.storedEnergy() == 0, "the wheel spent the packet");
        helper.getLevel().removeBlock(pos, false);
        helper.getLevel().removeBlock(above, false);
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void softHammerTogglesAndWrenchStillRotates(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 0, 2)).atY(250);
        var machine = place(helper, pos, "addon_sample:sample_switch");
        helper.assertTrue(machine.getBlockState().getValue(BehaviorMachineBlock.FACING) == Direction.NORTH, "starts north");
        var player = survival(helper);
        var hammer = GTToolItem.create(GTToolType.SOFT_HAMMER, Materials.Steel, WoodMaterials.Wood);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        click(helper, pos, player, Direction.UP);
        helper.assertTrue(machine.state().getBoolean("armed"), "soft hammer toggles");
        helper.assertTrue(machine.getBlockState().getValue(BehaviorMachineBlock.FACING) == Direction.NORTH,
                "soft hammer does not rotate");
        helper.assertTrue(hammer.getDamageValue() == 100, "soft hammer spends one tool click");
        var crowbar = GTToolItem.create(GTToolType.CROWBAR, Materials.Steel, WoodMaterials.Wood);
        helper.assertTrue(!crowbar.isEmpty(), "crowbar");
        player.setItemInHand(InteractionHand.MAIN_HAND, crowbar);
        click(helper, pos, player, Direction.UP);
        helper.assertTrue(machine.state().getBoolean("armed")
                && machine.getBlockState().getValue(BehaviorMachineBlock.FACING) == Direction.NORTH,
                "an unrecognized tool keeps the old click");
        var wrench = GTToolItem.create(GTToolType.WRENCH, Materials.Steel, WoodMaterials.Wood);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        click(helper, pos, player, Direction.EAST);
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(BehaviorMachineBlock.FACING) == Direction.EAST,
                "wrench still rotates");
        helper.assertTrue(machine.state().getBoolean("armed"), "wrench does not toggle");
        helper.getLevel().removeBlock(pos, false);
        helper.succeed();
    }

    private static Player survival(GameTestHelper helper) {
//? if >=1.21.1 {
/*        return helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
*///?} else {
        var player = helper.makeMockPlayer();
        if (player instanceof net.minecraft.server.level.ServerPlayer server)
            server.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        return player;
//?}
    }

    private static void click(GameTestHelper helper, BlockPos pos, Player player, Direction side) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(side.getStepX() * 0.5, side.getStepY() * 0.5, side.getStepZ() * 0.5),
                side, pos, false);
        var state = helper.getLevel().getBlockState(pos);
//? if >=1.21.1 {
/*        state.useItemOn(player.getItemInHand(InteractionHand.MAIN_HAND), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
*///?} else {
        state.use(helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
//?}
    }

    private static BehaviorMachineBlockEntity place(GameTestHelper helper, BlockPos pos) {
        return place(helper, pos, "addon_sample:irrigator");
    }

    private static BehaviorMachineBlockEntity place(GameTestHelper helper, BlockPos pos, String id) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState().setValue(BehaviorMachineBlock.FACING, Direction.NORTH));
        var entity = helper.getLevel().getBlockEntity(pos);
        if (!(entity instanceof BehaviorMachineBlockEntity machine)) throw new AssertionError("No " + id);
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
