package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.content.cover.CoverUtilityBehaviors;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real removal callbacks, separate namespace so a small change needs only three finite cases. */
@GameTestHolder("gregtech_fluid_removal")
@PrefixGameTestTemplate(false)
public final class FluidPipeRemovalTests {
    private static FluidPipeBlockEntity pipe(GameTestHelper h, int x, String size) {
        var pos = new BlockPos(112900 + x, 180, 112900);
        h.getLevel().getChunkAt(pos);
        h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech", "pipe_" + size + "_steel"));
        h.assertTrue(block instanceof FluidPipeBlock, "registered steel pipe " + size);
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        return (FluidPipeBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    private static TankBlockEntity drum(GameTestHelper h, BlockPos pos) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech", "drum_stainless_steel"));
        h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        var entity = h.getLevel().getBlockEntity(pos);
        h.assertTrue(entity instanceof TankBlockEntity, "registered stainless drum");
        return (TankBlockEntity) entity;
    }

    private static void open(GameTestHelper h, FluidPipeBlockEntity pipe, Direction side) {
        h.getLevel().setBlockAndUpdate(pipe.getBlockPos(), pipe.getBlockState().setValue(FluidPipeBlock.propFor(side), true));
    }

    private static ItemStack cover(String id) { return new ItemStack(GTTechnological.get(id)); }

    private static int garbageAmount(GameTestHelper h, net.minecraft.world.level.material.Fluid fluid) {
        var data = com.gregtech.gregtech.world.GarbageData.get(h.getLevel());
        var tag = data.save(new net.minecraft.nbt.CompoundTag(), h.getLevel().registryAccess());
        for (var entry : tag.getList("Fluids", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            var stored = FluidStack.parseOptional(h.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag) entry);
            if (stored.getFluid() == fluid) return stored.getAmount();
        }
        return 0;
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void removalRespectsConnectionsAndBothCoverGates(GameTestHelper h) {
        // Closed source, closed receiver, source filter, receiver output pump, source input pump, open.
        for (int mode = 0; mode < 6; mode++) {
            var source = pipe(h, mode * 8, "huge");
            FluidPipeBlockEntity targetPipe = null;
            TankBlockEntity targetDrum = null;
            if (mode == 3) {
                targetDrum = drum(h, source.getBlockPos().east());
                h.assertTrue(targetDrum.attachCover(Direction.WEST, cover("compact_electric_pump_ulv")),
                        "receiver output pump attaches");
            } else {
                targetPipe = pipe(h, mode * 8 + 1, "huge");
                if (mode != 1) open(h, targetPipe, Direction.WEST);
            }
            if (mode != 0) open(h, source, Direction.EAST);
            if (mode == 2) h.assertTrue(source.attachCover(Direction.EAST, cover(CoverUtilityBehaviors.FILTER_FLUID)),
                    "source empty whitelist attaches");
            if (mode == 4) h.assertTrue(source.attachCover(Direction.EAST, cover("compact_electric_pump_ulv")),
                    "pipe input pump attaches");
            int previousGarbage = garbageAmount(h, Fluids.WATER);
            source.fill(new FluidStack(Fluids.WATER, 600), FluidAction.EXECUTE);
            h.getLevel().removeBlock(source.getBlockPos(), false);
            int received = targetPipe != null ? targetPipe.getFluidInTank(0).getAmount() : targetDrum.getFluidInTank(0).getAmount();
            int expected = mode == 5 ? 600 : 0;
            h.assertTrue(source.isRemoved() && source.getFluidInTank(0).isEmpty(),
                    "removed pipe discards rejected contents, mode " + mode);
            h.assertTrue(received == expected && source.getTransferredAmount() == expected,
                    "removal obeys live source/receiver cover and connection before dropping covers, mode " + mode);
            h.assertTrue(garbageAmount(h, Fluids.WATER) - previousGarbage == 600 - expected,
                    "rejected fluid enters existing garbage storage, mode " + mode);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void removalCountsPartialDeliveryAfterNormalDistribution(GameTestHelper h) {
        var source = pipe(h, 80, "huge");
        var east = pipe(h, 81, "tiny");
        open(h, source, Direction.EAST);
        open(h, east, Direction.WEST);
        source.fill(new FluidStack(Fluids.WATER, 600), FluidAction.EXECUTE);
        FluidPipeBlockEntity.serverTick(h.getLevel(), source.getBlockPos(), source.getBlockState(), source);
        h.assertTrue(source.getFluidInTank(0).getAmount() == 400 && east.getFluidInTank(0).getAmount() == 200
                && source.getTransferredAmount() == 200, "ordinary distribution provides actual prior transfer count");
        var west = drum(h, source.getBlockPos().west());
        int initial = west.getTankCapacity(0) - 150;
        h.assertTrue(west.fill(new FluidStack(Fluids.WATER, initial), FluidAction.EXECUTE) == initial,
                "registered drum has only 150 mB room");
        open(h, source, Direction.WEST);
        int previousGarbage = garbageAmount(h, Fluids.WATER);
        h.getLevel().removeBlock(source.getBlockPos(), false);
        h.assertTrue(source.isRemoved() && source.getFluidInTank(0).isEmpty()
                && west.getFluidInTank(0).getAmount() == initial + 150 && east.getFluidInTank(0).getAmount() == 200,
                "only free capacity is delivered, 250 mB rejected residue leaves pipe");
        h.assertTrue(source.getTransferredAmount() == 350, "removal adds exactly 150 to existing 200; discarded fluid is not output");
        h.assertTrue(garbageAmount(h, Fluids.WATER) - previousGarbage == 250,
                "250 mB rejected residue enters existing garbage storage");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void removalDoesNotCountIncompatibleChannelsAsOutput(GameTestHelper h) {
        var source = pipe(h, 100, "quadruple");
        var target = drum(h, source.getBlockPos().east());
        open(h, source, Direction.EAST);
        int previousWaterGarbage = garbageAmount(h, Fluids.WATER);
        int previousLavaGarbage = garbageAmount(h, Fluids.LAVA);
        source.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);
        source.fill(new FluidStack(Fluids.LAVA, 200), FluidAction.EXECUTE);
        h.getLevel().removeBlock(source.getBlockPos(), false);
        h.assertTrue(target.getFluidInTank(0).getFluid() == Fluids.WATER && target.getFluidInTank(0).getAmount() == 500,
                "single-fluid drum accepts only compatible channel");
        for (int i = 0; i < source.getTanks(); i++) h.assertTrue(source.getFluidInTank(i).isEmpty(),
                "all channels empty after actual removal");
        h.assertTrue(source.getTransferredAmount() == 500, "rejected lava is not counted as a transfer");
        h.assertTrue(garbageAmount(h, Fluids.WATER) == previousWaterGarbage
                && garbageAmount(h, Fluids.LAVA) - previousLavaGarbage == 200,
                "only rejected lava enters garbage storage");
        h.succeed();
    }
}
