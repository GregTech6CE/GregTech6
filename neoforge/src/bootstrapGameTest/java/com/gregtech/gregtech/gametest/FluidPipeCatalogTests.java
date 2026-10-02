package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity;
import com.gregtech.gregtech.content.transport.fluid.FluidTransportDefinitions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_fluid_channels")
@PrefixGameTestTemplate(false)
public final class FluidPipeCatalogTests {
    private static FluidPipeBlockEntity pipe(GameTestHelper h, String name) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech", "pipe_medium_" + name));
        h.assertTrue(block instanceof FluidPipeBlock, "registered " + name);
        var p = new FluidPipeBlockEntity(h.absolutePos(new BlockPos(2, 150, 2)), block.defaultBlockState());
        p.setLevel(h.getLevel());
        return p;
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void allRegisteredPipesRetainSharedSpecifications(GameTestHelper h) {
        var definitions = FluidTransportDefinitions.pipes();
        h.assertTrue(definitions.size() == 280, "40 materials and seven sizes");
        for (var expected : definitions) {
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech", expected.id()));
            h.assertTrue(block instanceof FluidPipeBlock, "registered " + expected.id());
            h.assertTrue(((FluidPipeBlock) block).spec().equals(expected), "complete shared spec survives registration: " + expected.id());
            h.assertTrue(block.getFlammability(block.defaultBlockState(), h.getLevel(), BlockPos.ZERO,
                    net.minecraft.core.Direction.UP) == expected.flammability(), "registered flammability " + expected.id());
            h.assertTrue(block.getFireSpreadSpeed(block.defaultBlockState(), h.getLevel(), BlockPos.ZERO,
                    net.minecraft.core.Direction.UP) == expected.flammability(), "registered fire spread " + expected.id());
            var p = new FluidPipeBlockEntity(BlockPos.ZERO, block.defaultBlockState());
            h.assertTrue(p.getTanks() == expected.tankCount(), "channel count " + expected.id());
            h.assertTrue(p.getTankCapacity(0) == Math.min(Integer.MAX_VALUE, expected.capacity()), "capability capacity " + expected.id());
        }
        h.assertTrue(pipe(h, "wood").spec().maxTemperature() == 340, "wood explicit 340 K");
        h.assertTrue(pipe(h, "plastic").spec().maxTemperature() == 370, "plastic explicit 370 K");
        h.assertTrue(pipe(h, "rubber").spec().maxTemperature() == 350, "rubber explicit 350 K");
        h.assertTrue(pipe(h, "desh").spec().magicProof() && !pipe(h, "desh").spec().plasmaProof(),
                "Desh magic proof must not be confused with plasma proof");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void reducedCapacityRetainsLegacyContentsUntilDrained(GameTestHelper h) {
        var p = pipe(h, "titanium");
        p.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
        var saved = p.saveWithoutMetadata(h.getLevel().registryAccess());
        saved.getCompound("gt.tank.0").putLong("Capacity", 2400);
        saved.getCompound("gt.tank.0").putLong("Amount", 2200);
        var restored = pipe(h, "titanium");
        restored.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertTrue(restored.getFluidInTank(0).getAmount() == 2200, "load must not discard overfull legacy contents");
        h.assertTrue(restored.fill(new FluidStack(Fluids.WATER, 1), FluidAction.EXECUTE) == 0, "overfull pipe cannot accept more");
        h.assertTrue(restored.drain(400, FluidAction.EXECUTE).getAmount() == 400, "excess contents remain drainable");
        h.assertTrue(restored.getTankCapacity(0) == 1800 && restored.getFluidInTank(0).getAmount() == 1800,
                "capacity returns to registered 1800 after draining excess");
        restored.drain(100, FluidAction.EXECUTE);
        h.assertTrue(restored.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE) == 100, "only new capacity refills");
        h.assertTrue(restored.saveWithoutMetadata(h.getLevel().registryAccess()).getCompound("gt.tank.0").getLong("Capacity") == 1800,
                "next save writes corrected base capacity");
        h.assertTrue(saved.getCompound("gt.tank.0").getLong("Capacity") == 2400, "load does not mutate input NBT");
        var synced = pipe(h, "titanium");
        synced.handleUpdateTag(saved, h.getLevel().registryAccess());
        h.assertTrue(synced.getFluidInTank(0).getAmount() == 2200, "update tag preserves excess too");
        synced.drain(2200, FluidAction.EXECUTE);
        h.assertTrue(synced.getTankCapacity(0) == 1800, "update tag cannot reinstate stale capacity");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void increasedCapacityIsNotOverriddenByLegacyTag(GameTestHelper h) {
        var p = pipe(h, "bronze");
        p.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
        var saved = p.saveWithoutMetadata(h.getLevel().registryAccess());
        saved.getCompound("gt.tank.0").putLong("Capacity", 720);
        var restored = pipe(h, "bronze");
        restored.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertTrue(restored.getTankCapacity(0) == 900, "bronze medium uses original 150 times six");
        h.assertTrue(restored.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE) == 800,
                "legacy 100 plus 800 reaches restored capacity 900");
        h.succeed();
    }
}

