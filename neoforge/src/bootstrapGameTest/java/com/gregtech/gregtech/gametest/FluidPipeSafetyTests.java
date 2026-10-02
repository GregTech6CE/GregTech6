package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.fluid.FluidHazards;
import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Registered pipes with only rare destruction rolls controlled; normal world damage and removal. */
@GameTestHolder("gregtech_fluid_channels")
@PrefixGameTestTemplate(false)
public final class FluidPipeSafetyTests {
    private static final class Pipe extends FluidPipeBlockEntity {
        private final boolean destroy;
        Pipe(BlockPos pos, BlockState state, boolean destroy) {
            super(pos, state);
            this.destroy = destroy;
        }
        @Override protected boolean corrosionDestroysPipe() { return destroy; }
        @Override protected boolean overheatDestroysPipe() { return false; }
    }

    private static Pipe pipe(GameTestHelper h, int test, int x, String kind, boolean destroy) {
        var pos = h.absolutePos(new BlockPos(x, 20 + test * 12, 2));
        var level = h.getLevel();
        if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity previous) {
            for (int i = 0; i < previous.getTanks(); i++) previous.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
        }
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech", "pipe_" + kind));
        h.assertTrue(block instanceof FluidPipeBlock, "registered fixture pipe " + kind);
        var state = block.defaultBlockState();
        level.setBlockAndUpdate(pos, state);
        level.setBlockAndUpdate(pos, state);
        var pipe = new Pipe(pos, state, destroy);
        level.setBlockEntity(pipe);
        return pipe;
    }

    private static FluidStack fluid(GameTestHelper h, String field, int amount) {
        var holder = GTFluids.still(field);
        h.assertTrue(holder != null, "registered fluid " + field);
        return new FluidStack(holder.get(), amount);
    }

    private static void tick(GameTestHelper h, FluidPipeBlockEntity p) {
        FluidPipeBlockEntity.serverTick(h.getLevel(), p.getBlockPos(), p.getBlockState(), p);
    }

    private static void connect(GameTestHelper h, FluidPipeBlockEntity a, FluidPipeBlockEntity b) {
        h.getLevel().setBlockAndUpdate(a.getBlockPos(), a.getBlockState().setValue(FluidPipeBlock.EAST, true));
        h.getLevel().setBlockAndUpdate(b.getBlockPos(), b.getBlockState().setValue(FluidPipeBlock.WEST, true));
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void gasProofStillCorrodesWithoutGasLoss(GameTestHelper h) {
        var p = pipe(h, 0, 2, "medium_steel", false);
        var acid = fluid(h, "GenGas_HydrochloricAcid", 100);
        h.assertTrue(FluidHazards.isGas(acid.getFluid()) && FluidHazards.isAcid(acid.getFluid()), "compound acid gas");
        h.assertTrue(p.fill(acid, FluidAction.EXECUTE) == 100, "pipe accepts hazardous fluid before ticking");
        tick(h, p);
        h.assertTrue(p.getFluidInTank(0).getAmount() == 84 && p.getTransferredAmount() == 16,
                "steel loses only 16 acid units, not the gas-proof 8 units");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void compoundLossClampsAndStatisticsReset(GameTestHelper h) {
        var p = pipe(h, 1, 2, "medium_wood", false);
        p.fill(fluid(h, "GenGas_HydrochloricAcid", 100), FluidAction.EXECUTE);
        tick(h, p);
        h.assertTrue(p.getFluidInTank(0).getAmount() == 76 && p.getTransferredAmount() == 24, "gas and acid both leak");
        p.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
        p.fill(fluid(h, "GenGas_HydrochloricAcid", 10), FluidAction.EXECUTE);
        tick(h, p);
        h.assertTrue(p.getFluidInTank(0).isEmpty() && p.getTransferredAmount() == 10, "only actual loss counted");
        tick(h, p);
        h.assertTrue(p.getTransferredAmount() == 0, "empty following tick clears previous losses");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void plasmaUsesItsOwnProof(GameTestHelper h) {
        var vulnerable = pipe(h, 2, 2, "medium_steel", false);
        var proof = pipe(h, 2, 5, "medium_netherite", false);
        var plasma = fluid(h, "HeliumPlasma", 100);
        h.assertTrue(FluidHazards.isPlasma(plasma.getFluid()), "plasma fixture");
        vulnerable.fill(plasma, FluidAction.EXECUTE);
        proof.fill(plasma, FluidAction.EXECUTE);
        tick(h, vulnerable);
        tick(h, proof);
        h.assertTrue(vulnerable.getFluidInTank(0).getAmount() == 36 && vulnerable.getTransferredAmount() == 64,
                "gas-proof steel still loses 64 plasma units");
        h.assertTrue(proof.getFluidInTank(0).getAmount() == 100 && proof.getTransferredAmount() == 0,
                "plasma-proof netherite retains plasma");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void hottestChannelThenColdReplacementAndEmptyCooling(GameTestHelper h) {
        var p = pipe(h, 3, 2, "quadruple_steel", false);
        var water = new FluidStack(Fluids.WATER, 100);
        var lava = new FluidStack(Fluids.LAVA, 100);
        long hot = lava.getFluid().getFluidType().getTemperature(lava);
        long cold = water.getFluid().getFluidType().getTemperature(water);
        p.fill(water, FluidAction.EXECUTE);
        p.fill(lava, FluidAction.EXECUTE);
        tick(h, p);
        h.assertTrue(p.getTemperature() == hot, "later hottest channel sets temperature");
        p.drain(lava, FluidAction.EXECUTE);
        tick(h, p);
        h.assertTrue(p.getTemperature() == cold, "remaining cold fluid replaces previous hot temperature");
        p.drain(water, FluidAction.EXECUTE);
        p.fill(lava, FluidAction.EXECUTE);
        tick(h, p);
        p.drain(lava, FluidAction.EXECUTE);
        tick(h, p);
        h.assertTrue(p.getTemperature() == hot - 1, "empty pipe cools by one kelvin");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void leaksAndDeliveryShareStatistics(GameTestHelper h) {
        var b = pipe(h, 4, 3, "medium_wood", false);
        var a = pipe(h, 4, 2, "medium_wood", false);
        a.fill(fluid(h, "Gas_Natural", 100), FluidAction.EXECUTE);
        connect(h, a, b);
        tick(h, a);
        h.assertTrue(a.getFluidInTank(0).getAmount() == 46 && b.getFluidInTank(0).getAmount() == 46,
                "8 leaked then 92 balanced between two pipes");
        h.assertTrue(a.getTransferredAmount() == 54, "8 leaked plus 46 delivered in same tick");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void corrosionDestroysAllChannelsWithoutBreakDump(GameTestHelper h) {
        var b = pipe(h, 5, 3, "quadruple_steel", false);
        var a = pipe(h, 5, 2, "quadruple_steel", true);
        a.fill(fluid(h, "GenGas_HydrochloricAcid", 100), FluidAction.EXECUTE);
        a.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
        connect(h, a, b);
        tick(h, a);
        h.assertTrue(h.getLevel().getBlockState(a.getBlockPos()).isAir(), "forced corrosion removes pipe");
        for (int i = 0; i < a.getTanks(); i++) {
            h.assertTrue(a.getFluidInTank(i).isEmpty() && b.getFluidInTank(i).isEmpty(),
                    "destroyed contents must not be dumped or distributed");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void leakingSteamUsesFluidHeatOnNearbyEntity(GameTestHelper h) {
        var p = pipe(h, 6, 2, "medium_wood", false);
        var steam = fluid(h, "Steam", 100);
        h.assertTrue(GTFluids.entryForFluid(steam.getFluid()).temperature() > 320, "hot steam fixture");
        var cow = h.spawn(EntityType.COW, new BlockPos(3, 92, 2));
        float health = cow.getHealth();
        p.fill(steam, FluidAction.EXECUTE);
        tick(h, p);
        h.assertTrue(cow.getHealth() < health, "steam leaks its actual heat instead of ambient pipe temperature");
        cow.discard();
        h.succeed();
    }
}

