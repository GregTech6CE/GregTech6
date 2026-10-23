package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.content.nuclear.ReactorCoolants;
import com.gregtech.gregtech.content.nuclear.ReactorNetwork;
import com.gregtech.gregtech.content.nuclear.ReactorPorts;
import com.gregtech.gregtech.registry.GTEnergyNodes;
import com.gregtech.gregtech.registry.GTFuelRods;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
//?}

/** Registered cores/drums and the real queued reactor phase, with bounded residual-heat fixtures. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class ReactorCapacityOrderTests {
    private static ReactorCoreBlockEntity core(GameTestHelper h, boolean four) {
        var pos = h.absolutePos(new BlockPos(2, 3, 2));
        h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
        var block = four ? GTEnergyNodes.REACTOR_CORE_2X2.get() : GTEnergyNodes.REACTOR_CORE_BLOCK.get();
        var state = block.defaultBlockState().setValue(ReactorPorts.HOT, Direction.EAST)
                .setValue(ReactorPorts.COLD, Direction.EAST);
        h.getLevel().setBlockAndUpdate(pos, state);
        var core = (ReactorCoreBlockEntity) h.getLevel().getBlockEntity(pos);
        h.assertTrue(core != null && core.rods.length == (four ? 4 : 1), "actual registered core variant");
        h.assertTrue(core.insertRod(GTFuelRods.stack(9221)), "real fuel rod installed");
        core.setStopped(true); // Residual heat must still cool; no fabricated new neutron heat.
        return core;
    }

    private static TankBlockEntity sink(GameTestHelper h, ReactorCoreBlockEntity core) {
        var pos = core.getBlockPos().east();
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "drum_titanium"));
        h.assertTrue(block != Blocks.AIR, "registered titanium drum exists");
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        var sink = (TankBlockEntity) h.getLevel().getBlockEntity(pos);
        h.assertTrue(sink != null && sink.getTankCapacity(0) >= 64001, "native accepting sink has room for old and new hot coolant");
        return sink;
    }

    /** Execute actual BE enqueue and actual private END drain, not a copied scheduler. */
    private static void cycle(GameTestHelper h, ReactorCoreBlockEntity core) {
        ReactorCoreBlockEntity.serverTick(h.getLevel(), core.getBlockPos(), core.getBlockState(), core);
        try {
            var end = ReactorNetwork.class.getDeclaredMethod("finishTick", Level.class);
            end.setAccessible(true);
            end.invoke(null, h.getLevel());
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("actual reactor END phase failed", failure);
        }
    }

    @GameTest(template="test_empty")
    public static void fluidSensitiveFillPreservesDefaultAndMapContracts(GameTestHelper h) {
        int[] changes = {0};
        var tank = new FluidTankGT(64000).setOnChanged(() -> changes[0]++);
        var water = new FluidStack(Fluids.WATER, 64160);
        h.assertTrue(tank.fill(water, FluidAction.SIMULATE) == 64000 && tank.isEmpty() && changes[0] == 0,
                "default simulation bounds request without mutating/callback");
        h.assertTrue(tank.fill(water, FluidAction.EXECUTE) == 64000 && tank.getAmount() == 64000 && changes[0] == 1,
                "default committed fill remains bounded with one callback");
        h.assertTrue(tank.fill(new FluidStack(Fluids.LAVA, 1), FluidAction.EXECUTE) == 0
                && tank.fill(new FluidStack(Fluids.WATER, 1), FluidAction.EXECUTE) == 0 && changes[0] == 1,
                "wrong fluid and full tank remain rejected");
        h.assertTrue(tank.drain(160, FluidAction.EXECUTE).getAmount() == 160
                && tank.fill(new FluidStack(Fluids.WATER, 200), FluidAction.EXECUTE) == 160
                && tank.getAmount() == 64000, "partial drain/refill conserves default capacity");
        String currentKey = Fluids.WATER.getFluidType().toString();
        var mapped = new FluidTankGT(64000).setAdjustableCapacity(java.util.Map.of(currentKey, 128000L), 1);
        h.assertTrue(mapped.fill(water, FluidAction.SIMULATE) == 64160 && mapped.isEmpty(), "offered-fluid map is consulted before first fill");
        h.assertTrue(mapped.fill(water, FluidAction.EXECUTE) == 64160 && mapped.getAmount() == 64160,
                "matching current capacity key governs commit too");
        var otherKey = new FluidTankGT(64000).setAdjustableCapacity(java.util.Map.of("fixture:nonmatching_fluid_key", 128000L), 1);
        h.assertTrue(otherKey.fill(water, FluidAction.SIMULATE) == 64000, "no key migration or alias matching introduced");
        String recipeKey = BuiltInRegistries.FLUID.getKey(Fluids.WATER).toString();
        var recipeMapTank = new FluidTankGT(64000).setAdjustableCapacity(java.util.Map.of(recipeKey, 128000L), 1);
        h.assertTrue(recipeMapTank.fill(water, FluidAction.SIMULATE) == (recipeKey.equals(currentKey) ? 64160 : 64000),
                "registry-key capacity works only where existing fluid-type key matches; mapping policy unchanged");
        com.mojang.logging.LogUtils.getLogger().info("REACTOR_CAPACITY_KEYS recipe={} type={}", recipeKey, currentKey);
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void reactorSteamExpansionCommitsWholeBatchesAndSurvivesSaveLoad(GameTestHelper h) {
        for (boolean four : new boolean[]{false, true}) {
            var core = core(h, four);
            var steam = new FluidStack(ReactorCoolants.DISTILLED.outputFluid(), 64160);
            h.assertTrue(core.output.fill(steam, FluidAction.SIMULATE) == 64160 && core.output.isEmpty(), "expanded Steam simulation on empty core");
            h.assertTrue(core.output.fill(steam, FluidAction.EXECUTE) == 64160
                    && core.output.fill(new FluidStack(steam.getFluid(), 160), FluidAction.EXECUTE) == 160,
                    "empty and occupied Steam fills exceed old nominal64k bound");
            h.assertTrue(core.output.getAmount() == 64320 && core.output.capacity() == 10240000
                    && core.fluids.getTankCapacity(1) == 10240000, "occupied native capacity agrees with Steam allowance");
            var saved = new CompoundTag();
//? if >=1.21.1 {
/*            core.output.writeToNBT(saved, h.getLevel().registryAccess());
*///?} else {
            core.output.writeToNBT(saved);
//?}
            core.output.setEmpty();
//? if >=1.21.1 {
/*            core.output.readFromNBT(saved, h.getLevel().registryAccess());
*///?} else {
            core.output.readFromNBT(saved);
//?}
            h.assertTrue(core.output.getAmount() == 64320 && core.output.capacity() == 10240000, "native tank save/load retains expanded content and identity");
            core.output.setEmpty();
            core.input.fill(new FluidStack(ReactorCoolants.DISTILLED.inputFluid(), 401), FluidAction.EXECUTE);
            core.storedHeat = 32080;
            cycle(h, core);
            h.assertTrue(!core.failed && core.rodCount() == 1 && core.stopped && core.input.isEmpty()
                    && core.output.getAmount() == 64160 && core.storedHeat == 0,
                    "401L residual-heat cooling produces64160L Steam, consumes32080HU, retains stopped rods");
            core.output.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
            h.assertTrue(core.output.capacity() == 64000
                    && core.output.fill(new FluidStack(ReactorCoolants.INDUSTRIAL.outputFluid(), 64160), FluidAction.EXECUTE) == 64000,
                    "after emptying, non-Steam capacity remains64000");
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void reactorExportsOldHotCoolantBeforeQueuedCoolingExactlyOnce(GameTestHelper h) {
        for (boolean four : new boolean[]{false, true}) {
            var core = core(h, four);
            var sink = sink(h, core);
            var hot = new FluidStack(ReactorCoolants.INDUSTRIAL.outputFluid(), 64000);
            h.assertTrue(sink.fill(hot, FluidAction.SIMULATE) == 64000, "real drum accepts registered hot coolant");
            core.output.fill(hot, FluidAction.EXECUTE);
            core.input.fill(new FluidStack(ReactorCoolants.INDUSTRIAL.inputFluid(), 1), FluidAction.EXECUTE);
            core.storedHeat = 20;
            cycle(h, core);
            h.assertTrue(!core.failed && core.rodCount() == 1 && core.stopped && core.input.isEmpty() && core.storedHeat == 0,
                    "available sink prevents needless failure while stopped residual heat cools");
            h.assertTrue(sink.getFluidTank().getAmount() == 64000 && core.output.getAmount() == 1,
                    "old64000 leaves before cooling; new1 remains, no duplicate post-reaction export");
            cycle(h, core);
            h.assertTrue(sink.getFluidTank().getAmount() == 64001 && core.output.isEmpty() && core.rodCount() == 1,
                    "next ordinary tick exports new coolant once, total64001 conserved");
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void blockedReactorOutputsStillDestroyRodsWithoutConsumingCoolant(GameTestHelper h) {
        for (boolean four : new boolean[]{false, true}) for (int mode = 0; mode < 4; mode++) {
            var core = core(h, four);
            boolean steam = mode == 3;
            var coolant = steam ? ReactorCoolants.DISTILLED : ReactorCoolants.INDUSTRIAL;
            if (mode == 1 || mode == 2) {
                var sink = sink(h, core);
                var contents = mode == 1 ? coolant.outputFluid() : Fluids.WATER;
                sink.getFluidTank().setFluid(new FluidStack(contents, 1), mode == 1 ? sink.getFluidTank().capacity() : 1);
            }
            long full = steam ? 10240000 : 64000;
            core.output.setFluid(new FluidStack(coolant.outputFluid(), 1), full);
            core.input.fill(new FluidStack(coolant.inputFluid(), 1), FluidAction.EXECUTE);
            core.storedHeat = coolant.heat;
            cycle(h, core);
            h.assertTrue(core.failed && core.stopped && core.rodCount() == 0,
                    "absent/full/wrong-fluid sink or full expanded Steam remains a real failure: " + mode);
            h.assertTrue(core.input.getAmount() == 1 && core.output.getAmount() == full && core.storedHeat == coolant.heat,
                    "failed whole batch consumes neither coolant nor heat/output: " + mode);
            h.assertTrue(h.getLevel().getBlockEntity(core.getBlockPos()) == core,
                    "existing no-terrain-explosion policy preserved");
        }
        h.succeed();
    }
}
