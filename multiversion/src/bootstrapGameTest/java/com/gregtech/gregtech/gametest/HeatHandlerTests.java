package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.BoilerSpec;
import com.gregtech.gregtech.api.machine.HeatHandler;
import com.gregtech.gregtech.blockentity.machine.BoilerTankBlockEntity;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** The signed heat facade over boilers and crucibles, including their existing hazard rules. */
@GameTestHolder("gregtech_heat_handler")
@PrefixGameTestTemplate(false)
public final class HeatHandlerTests {
    private HeatHandlerTests() {}

    private static BoilerTankBlockEntity boiler(GameTestHelper helper, BlockPos pos) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "steam_boiler_bronze"));
        helper.assertTrue(block instanceof com.gregtech.gregtech.block.machine.BoilerTankBlock, "bronze boiler block");
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        var entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof BoilerTankBlockEntity, "boiler block entity");
        return (BoilerTankBlockEntity) entity;
    }

    private static HeatHandler crucible(GameTestHelper helper, BlockPos pos) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "smelting_crucible_steel"));
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        var entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof HeatHandler, "crucible exposes the heat facade");
        return (HeatHandler) entity;
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void boilerAdapterSimulatesThenBoilsWater(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(3, 2, 3));
        var boiler = boiler(helper, pos);
        HeatHandler heat = boiler;
        long capacity = heat.heatCapacity();
        helper.assertTrue(capacity > 0 && heat.heatStored() == 0, "empty boiler reports its rated buffer");
        helper.assertTrue(heat.temperatureK() >= 0, "temperature is never negative");

        helper.assertTrue(heat.insertHeat(capacity / 2, true) == capacity / 2 && heat.heatStored() == 0,
                "simulated heat does not change the buffer");
        helper.assertTrue(heat.insertHeat(capacity / 2, false) == capacity / 2 && heat.heatStored() == capacity / 2,
                "executed heat fills the buffer");
        helper.assertTrue(heat.extractHeat(capacity / 4, true) == capacity / 4 && heat.heatStored() == capacity / 2,
                "simulated extraction does not change the buffer");
        helper.assertTrue(heat.extractHeat(capacity / 4, false) == capacity / 4
                && heat.heatStored() == capacity / 2 - capacity / 4, "executed extraction cools the buffer");
        long beforeNegative = heat.heatStored();
        helper.assertTrue(heat.insertHeat(-capacity / 8, false) == -capacity / 8
                && heat.heatStored() == beforeNegative - capacity / 8, "a negative insert cools the boiler");

        long distilled = boiler.waterTank().fill(new FluidStack(GTFluids.still("DistW").get(), 1_000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(distilled == 1_000, "boiler takes distilled water");
        long waterBefore = boiler.waterTank().getAmount();
        heat.insertHeat(BoilerSpec.HU_PER_WATER * 100, false);
        BoilerTankBlockEntity.serverTick(helper.getLevel(), boiler.getBlockPos(), boiler.getBlockState(), boiler);
        helper.assertTrue(boiler.steamTank().getAmount() > 0, "inserted HU produces steam under the original rule");
        helper.assertTrue(boiler.waterTank().getAmount() < waterBefore, "boiling consumes water");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void boilerOverfillStillExplodesOnItsTick(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(3, 4, 3));
        var boiler = boiler(helper, pos);
        HeatHandler heat = boiler;
        heat.insertHeat(heat.heatCapacity() * 2, false);
        helper.assertTrue(heat.heatStored() > heat.heatCapacity(), "the facade does not swallow an overfill");
        BoilerTankBlockEntity.serverTick(helper.getLevel(), boiler.getBlockPos(), boiler.getBlockState(), boiler);
        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "overfilled boiler still explodes on its tick");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void crucibleAdapterUsesASignedUnboundedBuffer(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(3, 6, 3));
        HeatHandler heat = crucible(helper, pos);
        helper.assertTrue(heat.heatCapacity() == Long.MAX_VALUE, "crucible buffer is unbounded");
        helper.assertTrue(heat.heatStored() == 0, "new crucible starts without stored heat");
        helper.assertTrue(heat.temperatureK() >= 0, "crucible temperature is never negative");

        helper.assertTrue(heat.insertHeat(100, true) == 100 && heat.heatStored() == 0,
                "simulated crucible heat does not change the buffer");
        helper.assertTrue(heat.insertHeat(100, false) == 100 && heat.heatStored() == 100,
                "executed heat enters the buffer");
        helper.assertTrue(heat.extractHeat(40, false) == 40 && heat.heatStored() == 60,
                "executed extraction removes heat");
        helper.assertTrue(heat.extractHeat(1_000, false) == 60 && heat.heatStored() == 0,
                "extraction stops at the stored amount");
        helper.assertTrue(heat.insertHeat(-10, false) == 0 && heat.heatStored() == 0,
                "cooling an empty crucible accepts nothing");
        heat.insertHeat(20, false);
        helper.assertTrue(heat.insertHeat(-5, false) == -5 && heat.heatStored() == 15,
                "a signed negative insert cools the buffer");
        helper.assertTrue(heat.temperatureK() >= 0, "temperature stays non-negative after signed writes");
        helper.succeed();
    }
}
