package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.FurnaceFuelValue;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.blockentity.machine.HeatExchangerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.blockentity.tool.DustFunnelBlockEntity;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

/** Targeted feedback checks, without running the broader paused source audit. */
@GameTestHolder("gt6ce_heat_feedback")
@PrefixGameTestTemplate(false)
public final class HeatAndAutomationFeedbackTests {
    @GameTest(template="test_empty", timeoutTicks=40)
    public static void allEightHeatExchangersConsumeHotFuelAndKeepColdOutput(GameTestHelper h) {
        var recipe = FuelRecipeMaps.Hot.mRecipeList.stream().filter(r -> r.mEnabled && r.mFluidInputs.length == 1
                && r.mFluidInputs[0].getFluid() == net.minecraft.world.level.material.Fluids.LAVA && r.mFluidOutputs.length == 1).findFirst().orElseThrow();
        int x = 1;
        for (var entry : GTHeatExchangers.all()) {
            var pos = h.absolutePos(new BlockPos(x++, 2, 1));
            h.getLevel().setBlockAndUpdate(pos, entry.get().defaultBlockState());
            var machine = (HeatExchangerBlockEntity)h.getLevel().getBlockEntity(pos);
            var fluid = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.SOUTH);
            h.assertTrue(fluid != null, "actual NeoForge heat-exchanger fluid capability");
            h.assertTrue(fluid.fill(recipe.mFluidInputs[0].copy(), FluidAction.EXECUTE) == recipe.mFluidInputs[0].getAmount(), "hot input admitted");
            machine.tick();
            h.assertTrue(machine.tankAmount(0) == 0 && machine.tankAmount(1) == recipe.mFluidOutputs[0].getAmount(), "source hot-to-cold conversion " + entry.getId());
            h.assertTrue(machine.getEnergyStored(GregTechTags.Energy.HU, null) > 0, "fuel produces buffered heat");
            h.assertTrue(machine.isEnergyEmittingTo(GregTechTags.Energy.HU, Direction.UP, true)
                    && !machine.isEnergyEmittingTo(GregTechTags.Energy.HU, Direction.NORTH, true), "heat only leaves top");
            h.assertTrue(h.getLevel().getRecipeManager().getRecipes().stream().anyMatch(r -> r.value().getResultItem(h.getLevel().registryAccess()).is(entry.get().asItem())), "real crafting recipe " + entry.getId());
        }
        h.succeed();
    }
    @GameTest(template="test_empty", timeoutTicks=40)
    public static void funnelPullsFractionsFromChestAndPushesConvertedDust(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(1, 2, 1));
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gt6ce:dust_funnel"));
        h.getLevel().setBlockAndUpdate(pos.above(), Blocks.CHEST.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.CHEST.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        var above = (ChestBlockEntity)h.getLevel().getBlockEntity(pos.above());
        var below = (ChestBlockEntity)h.getLevel().getBlockEntity(pos.below());
        var funnel = (DustFunnelBlockEntity)h.getLevel().getBlockEntity(pos);
        var iron = GTMaterialRegistry.get("Iron");
        above.setItem(0, GTItems.getStack(MaterialPrefix.dust, iron, 9));
        DustFunnelBlockEntity.serverTick(h.getLevel(), pos, block.defaultBlockState(), funnel);
        h.assertTrue(above.getItem(0).isEmpty() && below.getItem(0).is(GTBlocks.getStack(BlockMaterialPrefix.blockDust, iron).getItem()), "nine dust become one dust block through native chests");
        below.clearContent(); funnel.cycleMode(false);
        above.setItem(0, GTItems.getStack(MaterialPrefix.dustTiny, iron, 9));
        DustFunnelBlockEntity.serverTick(h.getLevel(), pos, block.defaultBlockState(), funnel);
        h.assertTrue(above.getItem(0).isEmpty() && below.getItem(0).is(GTItems.getStack(MaterialPrefix.dust, iron, 1).getItem()) && below.getItem(0).getCount() == 1, "nine tiny dust conserve one U");
        h.succeed();
    }
    @GameTest(template="test_empty", timeoutTicks=40)
    public static void cokeBlockHasNineTimesItemFuelInNativeFurnace(GameTestHelper h) {
        var coke = GTMaterialRegistry.get("CoalCoke");
        var ingot = GTItems.getStack(MaterialPrefix.ingot, coke, 1);
        var block = GTBlocks.getStack(BlockMaterialPrefix.blockIngot, coke);
        h.assertTrue(!ingot.isEmpty() && !block.isEmpty(), "actual registered coke forms");
        long one = FurnaceFuelValue.getBurnValue(ingot);
        h.assertTrue(one == 3200 && FurnaceFuelValue.getBurnValue(block) == one * 9, "source coke heat amounts");
        h.assertTrue(block.getBurnTime(RecipeType.SMELTING) == one * 9, "native furnace reads full block fuel");
        h.succeed();
    }
    @GameTest(template="test_empty", timeoutTicks=40)
    public static void ratedBronzeAndInvarSteamDoNotExplodeBronzeSluice(GameTestHelper h) {
        for (String grade : new String[]{"bronze", "invar"}) {
            var pos = h.absolutePos(new BlockPos(1, 2, grade.equals("bronze") ? 1 : 6));
            var turbine = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gt6ce:steam_turbine_" + grade));
            var washer = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gt6ce:sluice_bronze"));
            h.getLevel().setBlockAndUpdate(pos, turbine.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH));
            h.getLevel().setBlockAndUpdate(pos.north(), washer.defaultBlockState());
            var node = (EnergyNodeBlockEntity)h.getLevel().getBlockEntity(pos);
            var inlet = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.SOUTH);
            int maxSteam = (int)node.spec().inputRate() * 2;
            for (int tick = 0; tick < 100; tick++) {
                inlet.fill(new FluidStack(GTFluids.still("Steam").get(), maxSteam), FluidAction.EXECUTE);
                EnergyNodeBlockEntity.serverTick(h.getLevel(), pos, h.getLevel().getBlockState(pos), node);
                h.assertTrue(h.getLevel().getBlockEntity(pos) == node && h.getLevel().getBlockEntity(pos.north()) instanceof BasicMachineBlockEntity, "rated steady flow leaves both devices intact " + grade);
            }
            h.assertTrue(((BasicMachineBlockEntity)h.getLevel().getBlockEntity(pos.north())).getEnergyTick() > 0, "actual RU entered the bronze sluice " + grade);
        }
        h.succeed();
    }
    @GameTest(template="test_empty", timeoutTicks=40)
    public static void nominal192BoilerHasOriginal384HighPressureTransfer(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(1, 2, 1));
        var boilerBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gt6ce:strong_steam_boiler_bronze"));
        var turbineBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gt6ce:steam_turbine_invar"));
        h.getLevel().setBlockAndUpdate(pos, boilerBlock.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.above(), turbineBlock.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.UP));
        var boiler = (com.gregtech.gregtech.blockentity.machine.BoilerTankBlockEntity)h.getLevel().getBlockEntity(pos);
        var turbine = (EnergyNodeBlockEntity)h.getLevel().getBlockEntity(pos.above());
        var tank = boiler.steamTank();
        h.assertTrue(boiler.spec().steamOutput() == 192, "actual 192 L/t bronze strong boiler");
        tank.setFluid(new FluidStack(GTFluids.still("Steam").get(), 1), tank.capacity() * 3 / 4 + 19200);
        h.assertTrue(com.gregtech.gregtech.content.energy.BoilerSteamOutput.offered(tank.getAmount(), tank.capacity(), 192) == 384, "source high-pressure transfer is twice nameplate rating");
        com.gregtech.gregtech.blockentity.machine.BoilerTankBlockEntity.serverTick(h.getLevel(), pos, boiler.getBlockState(), boiler);
        h.assertTrue(turbine.turbineSteamAmount() == 384, "actual native boiler delivered the high-pressure pulse");
        h.succeed();
    }
    @GameTest(template="test_empty", timeoutTicks=40)
    public static void steamEngineWastesStrokeIntoAirAndStopsAtTrueCapacity(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(1, 2, 1));
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gt6ce:engine_steam_bronze"));
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        var engine = (com.gregtech.gregtech.blockentity.machine.KineticSteamEngineBlockEntity)h.getLevel().getBlockEntity(pos);
        long capacity = engine.energyCapacity();
        var saved = engine.saveWithoutMetadata(h.getLevel().registryAccess());
        saved.putLong("kuEnergy", capacity / 2); saved.putBoolean("enabled", true);
        engine.loadWithComponents(saved, h.getLevel().registryAccess());
        com.gregtech.gregtech.blockentity.machine.KineticSteamEngineBlockEntity.serverTick(h.getLevel(), pos, engine.getBlockState(), engine);
        h.assertTrue(engine.getEnergyStored(GregTechTags.Energy.KU, null) < capacity / 2, "operating piston spends KU while facing air");
        saved = engine.saveWithoutMetadata(h.getLevel().registryAccess()); saved.putLong("kuEnergy", capacity);
        saved.putInt("gt.engine_state", 31);
        engine.loadWithComponents(saved, h.getLevel().registryAccess());
        engine.steamTank().fill(new FluidStack(GTFluids.still("Steam").get(), 1000), FluidAction.EXECUTE);
        com.gregtech.gregtech.blockentity.machine.KineticSteamEngineBlockEntity.serverTick(h.getLevel(), pos, engine.getBlockState(), engine);
        h.assertTrue(engine.isShutdown() && !engine.isEnabled() && engine.steamTank().isEmpty(), "true over-capacity forces shutdown and vents steam");
        h.succeed();
    }
}
