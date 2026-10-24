package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.api.machine.AdjacentCapabilityCache;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.*;
//?}

@GameTestHolder("gt6ce_machine_performance")
@PrefixGameTestTemplate(false)
public final class MachinePerformanceTests {
    private static RecipeMap map() {
        return new RecipeMap(new ArrayList<>(), "performance." + UUID.randomUUID(), "Performance fixture", "test",
                4, 2, 0, 4, 2, 0, 1, false, false, false, false);
    }
    private static Recipe row(ItemStack[] items, FluidStack... fluids) {
        return new Recipe(items, new ItemStack[]{new ItemStack(Items.DIAMOND)}, null, null,
                fluids, new FluidStack[0], 20, 1, 0);
    }
    private static void remove(RecipeMap map) {
        RecipeMap.unregister(map);
    }
    private static FluidStack water(int n) { return new FluidStack(Fluids.WATER, n); }
    private static Block block(String id) { return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gt6ce:" + id)); }

    @GameTest(template = "test_empty")
    public static void cachedCandidatesKeepPriorityCountsFlagsOutputsAndReloadLive(GameTestHelper h) {
        var map = map();
        try {
            var first = row(new ItemStack[]{new ItemStack(Items.IRON_INGOT, 4)}, water(100));
            var second = row(new ItemStack[]{new ItemStack(Items.IRON_INGOT)}, water(1));
            map.add(first); map.add(second);
            var items = new ArrayList<>(List.of(new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY));
            var fluids = List.of(water(1));
            h.assertTrue(map.findRecipe(items, fluids, true, 1, 1) == second, "first row lacks quantities");
            items.get(0).setCount(4); fluids.get(0).setAmount(100);
            h.assertTrue(map.findRecipe(items, fluids, true, 1, 1) == first, "cached candidate list rechecks quantities and priority");
            first.mEnabled = false;
            h.assertTrue(map.findRecipe(items, fluids, true, 1, 1) == second, "enabled flag remains live");
            first.mEnabled = true; first.mNeedsEmptyOutput = true;
            items.set(1, new ItemStack(Items.STONE));
            h.assertTrue(map.findRecipe(items, fluids, true, 1, 1) == second, "output requirement remains live");
            items.set(1, ItemStack.EMPTY);
            h.assertTrue(map.findRecipe(items, fluids, true, 1, 1) == first, "empty output immediately wakes first row");
            map.mRecipeList.removeAll(List.of(first));
            var replacement = row(new ItemStack[]{new ItemStack(Items.GOLD_INGOT)}, water(1));
            map.mRecipeList.add(replacement);
            items.set(0, new ItemStack(Items.GOLD_INGOT));
            h.assertTrue(map.findRecipe(items, fluids, false, 1, 1) == replacement, "same-size public collection replacement invalidates index");
            map.mRecipeList.removeIf(r -> r == replacement);
            h.assertTrue(map.findRecipe(items, fluids, false, 1, 1) == null, "iterator removal invalidates cached hit");
            map.add(replacement);
            h.assertTrue(map.findRecipe(items, fluids, false, 1, 1) == replacement, "adding row invalidates cached miss");
            replacement.mInputs[0] = new ItemStack(Items.COPPER_INGOT);
            map.invalidateLookup(); items.set(0, new ItemStack(Items.COPPER_INGOT));
            h.assertTrue(map.findRecipe(items, fluids, false, 1, 1) == replacement, "explicit in-place ingredient invalidation");
        } finally { remove(map); }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void matchingReservesCatalystsAndSplitInputsWithoutMutatingSnapshots(GameTestHelper h) {
        var recipe = row(new ItemStack[]{new ItemStack(Items.IRON_INGOT, 2), new ItemStack(Items.IRON_INGOT)},
                water(100), water(1), water(5)).withCatalystInputs(1).withPresenceFluidInputs(1).withCatalystFluidInputs(2);
        var items = List.of(new ItemStack(Items.IRON_INGOT, 2), new ItemStack(Items.IRON_INGOT, 3));
        var fluids = List.of(water(103), water(102));
        var matcher = new RecipeInputs.Matcher(items, fluids);
        h.assertTrue(matcher.matches(recipe, 2) && !matcher.matches(recipe, 3) && matcher.matches(recipe, 2), "reusable workspace resets after both success and failure");
        var remaining = RecipeInputs.consume(recipe, items, fluids, 2);
        h.assertTrue(remaining != null && remaining.items().stream().mapToInt(ItemStack::getCount).sum() == 1
                && remaining.fluids().stream().mapToInt(FluidStack::getAmount).sum() == 5, "exact parallel consumption retains catalysts");
        h.assertTrue(items.get(1).getCount() == 3 && fluids.get(0).getAmount() == 103, "caller stacks remain unchanged");
        fluids.get(1).setAmount(101);
        h.assertTrue(!matcher.matches(recipe, 2), "one missing catalyst mB rejects batch");
        var presence = row(new ItemStack[0], water(100), water(1)).withPresenceFluidInputs(1);
        h.assertTrue(RecipeInputs.consume(presence, List.of(), List.of(water(100)), 1).fluids().get(0).isEmpty(), "presence sees original tank after full consumption");
        h.assertTrue(RecipeInputs.consume(row(new ItemStack[0]), List.of(ItemStack.EMPTY), List.of(FluidStack.EMPTY), 1)
                .fluids().get(0).isEmpty(), "empty fluid slots never attempt a forbidden Forge EMPTY mutation");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void dynamicProvidersAreNotSuppressedByCachedMisses(GameTestHelper h) {
        var map = map();
        try {
            var calls = new java.util.concurrent.atomic.AtomicInteger();
            var dynamic = row(new ItemStack[]{new ItemStack(Items.STICK)});
            map.dynamicRecipes((items, fluids) -> calls.incrementAndGet() == 1 ? null : dynamic);
            var items = List.of(new ItemStack(Items.STICK));
            h.assertTrue(map.findRecipe(items, List.of(), false, 1, 1) == null, "first provider result is missing");
            h.assertTrue(map.findRecipe(items, List.of(), false, 1, 1) == dynamic && calls.get() == 2,
                    "dynamic provider evaluated again for unchanged items");
        } finally { remove(map); }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void boilerKeepsProductionAndPersistentStateWithCompactClientData(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, block("steam_boiler_bronze"));
        h.setBlock(pos.above(), Blocks.AIR);
        var boiler = (BoilerTankBlockEntity) h.getBlockEntity(pos);
        boiler.waterTank().fill(new FluidStack(GTFluids.still("DistW").get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        for (int i = 0; i < 20; i++) {
            boiler.insertHeat(80, false);
            BoilerTankBlockEntity.serverTick(h.getLevel(), boiler.getBlockPos(), boiler.getBlockState(), boiler);
        }
        h.assertTrue(boiler.waterTank().getAmount() == 980 && boiler.steamTank().getAmount() == 3200 && boiler.storedHeat() == 0,
                "twenty ticks retain original water/HU/steam conversion");
//? if >=1.21.1 {
        /*var visual = boiler.getUpdateTag(h.getLevel().registryAccess());
        var saved = boiler.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        var visual = boiler.getUpdateTag();
        var saved = boiler.saveWithoutMetadata();
//?}
        h.assertTrue(visual.getAllKeys().equals(Set.of("gt.boiler_visual")), "update packet contains only gauge state");
        var restored = new BoilerTankBlockEntity(boiler.getBlockPos(), boiler.getBlockState());
//? if >=1.21.1 {
        /*restored.loadWithComponents(saved, h.getLevel().registryAccess());
*///?} else {
        restored.load(saved);
//?}
        h.assertTrue(restored.waterTank().getAmount() == 980 && restored.steamTank().getAmount() == 3200
                && restored.efficiency() == boiler.efficiency(), "compact client packet does not remove persisted data");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void neighbourCacheFollowsRotationRemovalAndReplacement(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, block("steam_boiler_bronze"));
        var owner = h.getBlockEntity(pos);
        var targetPos = pos.above();
//? if >=1.21.1 {
        /*var cache = new AdjacentCapabilityCache<IItemHandler>(owner, Capabilities.ItemHandler.BLOCK);
*///?} else {
        var cache = new AdjacentCapabilityCache<IItemHandler>(owner, ForgeCapabilities.ITEM_HANDLER);
//?}
        for (var facing : Direction.Plane.HORIZONTAL) {
            h.setBlock(targetPos, block("sluice_bronze").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing));
            var target = h.getBlockEntity(targetPos);
            for (var side : Direction.values()) {
//? if >=1.21.1 {
                /*var direct = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, target.getBlockPos(), side);
*///?} else {
                var direct = target.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
//?}
                var cached = cache.get(target, side);
                h.assertTrue((direct == null) == (cached == null), "capability presence follows rotation " + facing + side);
                if (direct != null) h.assertTrue(direct.insertItem(0, new ItemStack(Items.DIRT), true).getCount()
                        == cached.insertItem(0, new ItemStack(Items.DIRT), true).getCount(), "cached sided admission follows rotation");
            }
        }
        var old = h.getBlockEntity(targetPos);
        h.setBlock(targetPos, Blocks.AIR);
        h.assertTrue(cache.get(old, Direction.DOWN) == null, "removed target is rejected");
        h.setBlock(targetPos, Blocks.CHEST);
        var chest = cache.get(h.getBlockEntity(targetPos), Direction.DOWN);
        h.assertTrue(chest != null && chest.insertItem(0, new ItemStack(Items.DIRT), false).isEmpty(), "replacement inventory is usable immediately");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void unchangedTankWritesDoNotNotifyButRealTransfersDo(GameTestHelper h) {
        var updates = new java.util.concurrent.atomic.AtomicInteger();
        var tank = new FluidTankGT(1000).setOnChanged(updates::incrementAndGet);
        tank.setEmpty();
        h.assertTrue(updates.get() == 0, "empty-to-empty is silent");
        tank.setFluid(water(1), 100);
        tank.setFluid(water(1), 100);
        h.assertTrue(updates.get() == 1, "identical identity and quantity is silent");
        tank.fill(water(50), IFluidHandler.FluidAction.SIMULATE);
        tank.drain(50, IFluidHandler.FluidAction.SIMULATE);
        h.assertTrue(updates.get() == 1 && tank.getAmount() == 100, "simulations are silent and non-mutating");
        tank.fill(water(50), IFluidHandler.FluidAction.EXECUTE);
        tank.drain(50, IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(updates.get() == 3 && tank.getAmount() == 100, "real transfers still notify");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void fullInputWakesImmediatelyAndMachineKeepsItsProcessingRate(GameTestHelper h) {
        var map = map();
        try {
            var pos = new BlockPos(2, 2, 2);
            var block = (com.gregtech.gregtech.block.machine.BasicMachineBlock) block("sluice_bronze");
            h.setBlock(pos, block);
            h.setBlock(pos.above(), Blocks.CHEST);
            h.setBlock(pos.below(), Blocks.CHEST);
            var machine = (BasicMachineBlockEntity) h.getBlockEntity(pos);
            var source = (net.minecraft.world.Container) h.getBlockEntity(pos.above());
            var target = (net.minecraft.world.Container) h.getBlockEntity(pos.below());
            machine.setSpec(com.gregtech.gregtech.api.machine.BasicMachineSpec.builder("performance_fixture", block.basicSpec().material())
                    .machineType("sluice").energy(com.gregtech.gregtech.data.GregTechTags.Energy.TU, 1).energyRange(1, 1)
                    .processingEfficiency(10000).parallel(1).recipes(map)
                    .faces(new com.gregtech.gregtech.api.energy.FaceConfig(63, 63, 63, 63, 0, 0, 1, 0, -1, -1)).build());
            map.add(new Recipe(new ItemStack[]{new ItemStack(Items.COBBLESTONE)}, new ItemStack[]{new ItemStack(Items.STICK, 4)},
                    null, null, new FluidStack[0], new FluidStack[0], 4, 1, 0));
            for (int i = 0; i < machine.inputSlots(); i++) machine.inventory().setStackInSlot(i, new ItemStack(Items.DIRT, 64));
            source.setItem(0, new ItemStack(Items.COBBLESTONE, 3));
            BasicMachineBlockEntity.serverTick(h.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
            h.assertTrue(source.getItem(0).getCount() == 3, "full inputs do not drain source");
            for (int i = 0; i < machine.inputSlots(); i++) machine.inventory().setStackInSlot(i, ItemStack.EMPTY);
            BasicMachineBlockEntity.serverTick(h.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
            h.assertTrue(source.getItem(0).isEmpty() && machine.inventory().getStackInSlot(0).getCount() == 3,
                    "freeing input space immediately restores automatic transfer without a wrench");
            for (int i = 0; i < 12; i++)
                BasicMachineBlockEntity.serverTick(h.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
            int sticks = 0;
            for (int i = 0; i < target.getContainerSize(); i++) if (target.getItem(i).is(Items.STICK)) sticks += target.getItem(i).getCount();
            h.assertTrue(sticks == 12 && machine.inventory().getStackInSlot(0).isEmpty(), "three four-tick jobs deliver twelve sticks in twelve ticks");
        } finally { remove(map); }
        h.succeed();
    }
}
