package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real registered pipes, deterministic single tick fixtures; not a survival or save migration test. */
@GameTestHolder("gregtech_fluid_channels")
@PrefixGameTestTemplate(false)
public final class FluidPipeChannelTests {
    private static FluidPipeBlockEntity pipe(GameTestHelper h, int x, String size) {
        BlockPos pos = new BlockPos(112000 + x, 180, 112000);
        var level = h.getLevel();
        level.getChunkAt(pos);
        // Repeated runs must not invoke break-time dumping of the previous fixture into its neighbours.
        if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity previous) {
            for (int i = 0; i < previous.getTanks(); i++) previous.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
        }
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech", "pipe_" + size + "_steel"));
        h.assertTrue(block instanceof FluidPipeBlock, "registered steel pipe " + size);
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        // onPlace can reconnect to old neighbours; each fixture declares its own connections below.
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        return (FluidPipeBlockEntity) level.getBlockEntity(pos);
    }

    private static void connect(GameTestHelper h, FluidPipeBlockEntity a, FluidPipeBlockEntity b) {
        h.getLevel().setBlockAndUpdate(a.getBlockPos(), a.getBlockState().setValue(FluidPipeBlock.EAST, true));
        h.getLevel().setBlockAndUpdate(b.getBlockPos(), b.getBlockState().setValue(FluidPipeBlock.WEST, true));
    }

    private static void tick(GameTestHelper h, FluidPipeBlockEntity pipe) {
        FluidPipeBlockEntity.serverTick(h.getLevel(), pipe.getBlockPos(), pipe.getBlockState(), pipe);
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void matchingChannelBeforeEmptyAndFullDoesNotSpill(GameTestHelper h) {
        var p = pipe(h, 0, "quadruple");
        p.fill(new FluidStack(Fluids.LAVA, 100), FluidAction.EXECUTE);
        p.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
        p.drain(new FluidStack(Fluids.LAVA, 100), FluidAction.EXECUTE);
        h.assertTrue(p.fill(new FluidStack(Fluids.WATER, 50), FluidAction.SIMULATE) == 50,
                "simulation accepts existing channel");
        h.assertTrue(p.getFluidInTank(0).isEmpty() && p.getFluidInTank(1).getAmount() == 100,
                "simulation leaves both channels unchanged");
        p.fill(new FluidStack(Fluids.WATER, 50), FluidAction.EXECUTE);
        h.assertTrue(p.getFluidInTank(0).isEmpty() && p.getFluidInTank(1).getAmount() == 150,
                "existing water channel must win over earlier empty channel");
        p.fill(new FluidStack(Fluids.WATER, p.getTankCapacity(1)), FluidAction.EXECUTE);
        h.assertTrue(p.fill(new FluidStack(Fluids.WATER, 1), FluidAction.EXECUTE) == 0
                && p.getFluidInTank(0).isEmpty(), "full matching channel must not consume another channel");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void higherChannelCanFeedSinglePipe(GameTestHelper h) {
        var a = pipe(h, 20, "quadruple");
        var b = pipe(h, 21, "huge");
        a.fill(new FluidStack(Fluids.LAVA, 100), FluidAction.EXECUTE);
        a.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);
        a.drain(new FluidStack(Fluids.LAVA, 100), FluidAction.EXECUTE);
        connect(h, a, b);
        tick(h, a);
        h.assertTrue(a.getFluidInTank(1).getAmount() == 250 && b.getFluidInTank(0).getAmount() == 250,
                "source channel one feeds receiver channel zero and conserves 500 mB");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void differentlyOrderedChannelsBalanceByFluid(GameTestHelper h) {
        var a = pipe(h, 40, "quadruple");
        var b = pipe(h, 41, "quadruple");
        a.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);
        a.fill(new FluidStack(Fluids.LAVA, 100), FluidAction.EXECUTE);
        b.fill(new FluidStack(Fluids.LAVA, 300), FluidAction.EXECUTE);
        b.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
        connect(h, a, b);
        tick(h, a);
        h.assertTrue(a.getFluidInTank(0).getAmount() == 300 && b.getFluidInTank(1).getAmount() == 300,
                "water balances against water regardless of slot order");
        h.assertTrue(a.getFluidInTank(1).getAmount() == 100 && b.getFluidInTank(0).getAmount() == 300,
                "higher receiving lava pressure must not pull more lava");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void receivingWaterDoesNotBlockOtherFluid(GameTestHelper h) {
        var a = pipe(h, 60, "quadruple");
        var b = pipe(h, 61, "quadruple");
        a.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);
        b.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
        b.fill(new FluidStack(Fluids.LAVA, 400), FluidAction.EXECUTE);
        connect(h, a, b);
        tick(h, a);
        tick(h, b);
        h.assertTrue(a.getFluidInTank(0).getAmount() == 300 && b.getFluidInTank(0).getAmount() == 300,
                "received water does not immediately flow back");
        h.assertTrue(a.getFluidInTank(1).getAmount() == 200 && b.getFluidInTank(1).getAmount() == 200,
                "only received channel gets backflow marker; lava still transfers and conserves 400 mB");
        h.succeed();
    }

    private static FluidStack markedWater(String marker, int amount) {
        var stack = new FluidStack(Fluids.WATER, amount);
        var data = new net.minecraft.nbt.CompoundTag();
        data.putString("channel_test", marker);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(data));
        return stack;
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void fluidMetadataIsPartOfChannelIdentity(GameTestHelper h) {
        var p = pipe(h, 80, "quadruple");
        h.assertTrue(p.fill(markedWater("a", 100), FluidAction.EXECUTE) == 100, "first marked fluid admitted");
        h.assertTrue(p.fill(markedWater("b", 200), FluidAction.EXECUTE) == 200,
                "different fluid data uses a separate channel");
        h.assertTrue(p.drain(markedWater("b", 50), FluidAction.SIMULATE).getAmount() == 50,
                "typed simulation finds the requested metadata");
        h.assertTrue(p.getFluidInTank(0).getAmount() == 100 && p.getFluidInTank(1).getAmount() == 200,
                "typed simulation leaves contents unchanged");
        p.drain(markedWater("b", 50), FluidAction.EXECUTE);
        h.assertTrue(p.getFluidInTank(0).getAmount() == 100 && p.getFluidInTank(1).getAmount() == 150,
                "typed drain does not consume earlier same-fluid different-metadata channel");
        h.assertTrue(p.drain(markedWater("c", 50), FluidAction.EXECUTE).isEmpty(), "unknown metadata refused");
        h.succeed();
    }

    private static void open(GameTestHelper h, FluidPipeBlockEntity p, Direction side, boolean value) {
        h.getLevel().setBlockAndUpdate(p.getBlockPos(), p.getBlockState().setValue(FluidPipeBlock.propFor(side), value));
    }

    private static IFluidHandler face(GameTestHelper h, FluidPipeBlockEntity p, Direction side) {
        return h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, p.getBlockPos(), side);
    }

    private static com.gregtech.gregtech.blockentity.machine.TankBlockEntity drum(GameTestHelper h, BlockPos pos) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech", "drum_stainless_steel"));
        h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        var be = h.getLevel().getBlockEntity(pos);
        h.assertTrue(be instanceof com.gregtech.gregtech.blockentity.machine.TankBlockEntity, "registered drum");
        return (com.gregtech.gregtech.blockentity.machine.TankBlockEntity) be;
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void cauldronCostsAndPartialLevels(GameTestHelper h) {
        int[][] cases = {{0,333,0,333}, {0,334,1,0}, {0,666,1,332}, {0,667,2,0},
                {0,999,2,332}, {0,1000,3,0}, {1,667,3,0}, {2,1000,3,666}, {3,1000,3,1000}};
        for (int i = 0; i < cases.length; i++) {
            int[] c = cases[i];
            var p = pipe(h, 200 + i * 5, "huge");
            var pos = p.getBlockPos().east();
            var state = c[0] == 0 ? Blocks.CAULDRON.defaultBlockState()
                    : Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, c[0]);
            h.getLevel().setBlockAndUpdate(pos, state);
            open(h, p, Direction.EAST, true);
            p.fill(new FluidStack(Fluids.WATER, c[1]), FluidAction.EXECUTE);
            tick(h, p);
            var result = h.getLevel().getBlockState(pos);
            int actual = result.is(Blocks.CAULDRON) ? 0 : result.getValue(LayeredCauldronBlock.LEVEL);
            h.assertTrue(actual == c[2] && p.getFluidInTank(0).getAmount() == c[3], "cauldron cost case " + i);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void cauldronPriorityAndWaterClassification(GameTestHelper h) {
        var p = pipe(h, 300, "huge");
        var neighbor = pipe(h, 299, "huge");
        connect(h, neighbor, p);
        open(h, p, Direction.EAST, true);
        var pos = p.getBlockPos().east();
        h.getLevel().setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
        var distilled = com.gregtech.gregtech.registry.GTFluids.stack("DistW", 1000);
        h.assertTrue(distilled != null, "distilled water registered");
        p.fill(distilled, FluidAction.EXECUTE);
        tick(h, p);
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL) == 3
                && p.getFluidInTank(0).isEmpty() && neighbor.getFluidInTank(0).isEmpty(), "cauldron priority includes distilled water");
        h.getLevel().setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
        open(h, p, Direction.WEST, false);
        var seawater = com.gregtech.gregtech.registry.GTFluids.stack("Ocean", 1000);
        h.assertTrue(seawater != null, "sea water registered");
        p.fill(seawater, FluidAction.EXECUTE);
        tick(h, p);
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.CAULDRON)
                && p.getFluidInTank(0).getAmount() == 1000, "salt water is not original WATER classification");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void cauldronRespectsConnectionFilterAndContents(GameTestHelper h) {
        var p = pipe(h, 320, "huge");
        var pos = p.getBlockPos().east();
        h.getLevel().setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
        p.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
        tick(h, p);
        h.assertTrue(p.getFluidInTank(0).getAmount() == 1000, "closed outlet cannot fill cauldron");
        open(h, p, Direction.EAST, true);
        var cover = new net.minecraft.world.item.ItemStack(com.gregtech.gregtech.registry.GTTechnological.get(
                com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.FILTER_FLUID));
        h.assertTrue(p.attachCover(Direction.EAST, cover), "attach empty whitelist filter");
        tick(h, p);
        h.assertTrue(p.getFluidInTank(0).getAmount() == 1000, "filter prevents cauldron consumption");
        p.removeCover(Direction.EAST);
        h.getLevel().setBlockAndUpdate(pos, Blocks.LAVA_CAULDRON.defaultBlockState());
        tick(h, p);
        h.assertTrue(p.getFluidInTank(0).getAmount() == 1000
                && h.getLevel().getBlockState(pos).is(Blocks.LAVA_CAULDRON), "water cannot replace lava cauldron");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void multipleOutletsUseOneMean(GameTestHelper h) {
        var a = pipe(h, 400, "huge");
        var west = pipe(h, 399, "huge");
        var east = pipe(h, 401, "huge");
        connect(h, west, a);
        connect(h, a, east);
        a.fill(new FluidStack(Fluids.WATER, 600), FluidAction.EXECUTE);
        tick(h, a);
        h.assertTrue(a.getFluidInTank(0).getAmount() == 200 && west.getFluidInTank(0).getAmount() == 200
                && east.getFluidInTank(0).getAmount() == 200, "source and both outputs receive 200; no sequential 300/150 bias");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void pressurePushAfterCapacityLimitedBalancing(GameTestHelper h) {
        var a = pipe(h, 420, "medium");
        var west = pipe(h, 419, "tiny");
        var east = pipe(h, 421, "huge");
        connect(h, west, a);
        connect(h, a, east);
        a.fill(new FluidStack(Fluids.WATER, 1200), FluidAction.EXECUTE);
        west.fill(new FluidStack(Fluids.WATER, 200), FluidAction.EXECUTE);
        tick(h, a);
        h.assertTrue(a.getFluidInTank(0).getAmount() == 667 && west.getFluidInTank(0).getAmount() == 200
                && east.getFluidInTank(0).getAmount() == 533, "1400 total: mean 467 then pressure 66 to accepting pipe");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void pipeAndMachineShareOneMean(GameTestHelper h) {
        var a = pipe(h, 440, "huge");
        var b = pipe(h, 439, "huge");
        connect(h, b, a);
        open(h, a, Direction.EAST, true);
        var target = drum(h, a.getBlockPos().east());
        a.fill(new FluidStack(Fluids.WATER, 600), FluidAction.EXECUTE);
        tick(h, a);
        h.assertTrue(a.getFluidInTank(0).getAmount() == 200 && b.getFluidInTank(0).getAmount() == 200
                && target.getFluidInTank(0).getAmount() == 200, "pipe and drum share source-inclusive mean");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void rejectingMachineDoesNotCountAsTarget(GameTestHelper h) {
        var a = pipe(h, 460, "huge");
        var b = pipe(h, 459, "huge");
        connect(h, b, a);
        open(h, a, Direction.EAST, true);
        var target = drum(h, a.getBlockPos().east());
        target.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE);
        a.fill(new FluidStack(Fluids.WATER, 600), FluidAction.EXECUTE);
        tick(h, a);
        h.assertTrue(a.getFluidInTank(0).getAmount() == 300 && b.getFluidInTank(0).getAmount() == 300
                && target.getFluidInTank(0).getAmount() == 1000, "incompatible drum excluded by simulation");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void closedReceiverAndCachedFaceStayClosed(GameTestHelper h) {
        var a = pipe(h, 480, "huge");
        var b = pipe(h, 481, "huge");
        open(h, a, Direction.EAST, true);
        a.fill(new FluidStack(Fluids.WATER, 600), FluidAction.EXECUTE);
        tick(h, a);
        h.assertTrue(b.getFluidInTank(0).isEmpty(), "closed receiving face rejects direct pipe path");
        var handler = face(h, b, Direction.WEST);
        h.assertTrue(handler != null && handler.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE) == 0,
                "closed face rejects external fill");
        open(h, b, Direction.WEST, true);
        h.assertTrue(handler.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE) == 100,
                "cached handler observes opening");
        open(h, b, Direction.WEST, false);
        h.assertTrue(handler.drain(100, FluidAction.EXECUTE).isEmpty()
                && handler.drain(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE).isEmpty(),
                "cached handler observes closing for both drain paths");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void externalFillBackflowIsChannelLocalAndSimulationIsPure(GameTestHelper h) {
        var a = pipe(h, 500, "quadruple");
        var b = pipe(h, 501, "quadruple");
        connect(h, a, b);
        var handler = face(h, a, Direction.EAST);
        handler.fill(new FluidStack(Fluids.WATER, 400), FluidAction.EXECUTE);
        a.fill(new FluidStack(Fluids.LAVA, 400), FluidAction.EXECUTE);
        handler.fill(new FluidStack(Fluids.LAVA, 100), FluidAction.SIMULATE);
        tick(h, a);
        h.assertTrue(a.getFluidInTank(0).getAmount() == 400 && a.getFluidInTank(1).getAmount() == 200
                && b.getFluidInTank(0).getFluid() == Fluids.LAVA && b.getFluidInTank(0).getAmount() == 200,
                "executed fill blocks water backflow only; simulated lava fill does not mark a channel");
        tick(h, a);
        h.assertTrue(a.getFluidInTank(0).getAmount() == 200 && b.getFluidInTank(1).getAmount() == 200,
                "backflow mask expires after one distribution tick");
        h.succeed();
    }

}
