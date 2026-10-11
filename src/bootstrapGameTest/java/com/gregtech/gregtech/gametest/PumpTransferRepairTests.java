package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.energy.PumpBlock;
import com.gregtech.gregtech.blockentity.energy.PumpBlockEntity;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTPumps;
import com.gregtech.gregtech.registry.GTTanks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Original pump output must conserve fluid even without rotational energy. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class PumpTransferRepairTests {
    private static final BlockPos PUMP = new BlockPos(2, 1, 2);

    private static PumpBlockEntity pump(GameTestHelper h) {
        PumpBlock block = GTPumps.all().get(0).get();
        h.setBlock(PUMP, block.defaultBlockState().setValue(PumpBlock.FACING, Direction.NORTH));
        return (PumpBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(PUMP));
    }

    private static TankBlockEntity receiver(GameTestHelper h, Direction side) {
        BlockPos pos = PUMP.relative(side);
        h.setBlock(pos, GTTanks.DRUM_STEEL.get());
        return (TankBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
    }

    private static IFluidHandler fluids(PumpBlockEntity pump) {
        return pump.getCapability(ForgeCapabilities.FLUID_HANDLER, null).orElseThrow(IllegalStateException::new);
    }

    private static void tick(GameTestHelper h, PumpBlockEntity pump) {
        PumpBlockEntity.serverTick(h.getLevel(), pump.getBlockPos(), pump.getBlockState(), pump);
    }

    @GameTest(template = "test_empty")
    public static void emptyTankReceivesEntireTaggedFluidWithoutRu(GameTestHelper h) {
        PumpBlockEntity pump = pump(h);
        TankBlockEntity east = receiver(h, Direction.EAST);
        FluidStack water = new FluidStack(Fluids.WATER, 500);
        water.getOrCreateTag().putString("origin", "pump");
        h.assertTrue(fluids(pump).fill(water, IFluidHandler.FluidAction.EXECUTE) == 500,
                "pump accepts the tagged 500 L fixture");

        tick(h, pump);

        h.assertTrue(fluids(pump).getFluidInTank(0).isEmpty(), "empty receiver receives all 500 L without RU");
        FluidStack arrived = east.getFluidTank().getFluid();
        h.assertTrue(arrived.getAmount() == 500 && arrived.getFluid() == Fluids.WATER
                        && arrived.hasTag() && "pump".equals(arrived.getTag().getString("origin")),
                "fluid type, amount and NBT survive the entire transfer");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void partialReceiverOnlyConsumesAcceptedAmountAndFrontBackStayDry(GameTestHelper h) {
        PumpBlockEntity pump = pump(h);
        TankBlockEntity east = receiver(h, Direction.EAST);
        TankBlockEntity front = receiver(h, Direction.NORTH);
        TankBlockEntity back = receiver(h, Direction.SOUTH);
        int capacity = east.getFluidTank().getCapacity();
        h.assertTrue(east.getFluidTank().fill(new FluidStack(Fluids.WATER, capacity - 250),
                IFluidHandler.FluidAction.EXECUTE) == capacity - 250, "receiver has exactly 250 L space");
        h.assertTrue(fluids(pump).fill(new FluidStack(Fluids.WATER, 1000),
                IFluidHandler.FluidAction.EXECUTE) == 1000, "pump holds one bucket");

        tick(h, pump);

        h.assertTrue(east.getFluidTank().getAmount() == capacity && fluids(pump).getFluidInTank(0).getAmount() == 750,
                "only the 250 L accepted by the receiver leaves the pump");
        h.assertTrue(front.getFluidTank().isEmpty() && back.getFluidTank().isEmpty(),
                "original pump exports only through the four faces perpendicular to its axis");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void worldSourceWaitsForOriginal8192RuBuffer(GameTestHelper h) {
        PumpBlockEntity pump = pump(h);
        BlockPos source = PUMP.north();
        h.setBlock(source, Blocks.WATER);
        CompoundTag saved = pump.saveWithoutMetadata();
        saved.putInt("pumpX", 64);
        saved.putInt("pumpZ", 64);
        saved.putLong("energyBuffer", 8191);
        pump.load(saved);

        tick(h, pump);
        h.assertTrue(h.getLevel().getFluidState(h.absolutePos(source)).isSource()
                        && fluids(pump).getFluidInTank(0).isEmpty(),
                "8191 RU cannot remove a full source block");

        saved.putLong("energyBuffer", 8192);
        pump.load(saved);
        tick(h, pump);
        h.assertTrue(h.getLevel().getFluidState(h.absolutePos(source)).isEmpty()
                        && fluids(pump).getFluidInTank(0).getAmount() == 1000,
                "8192 RU removes exactly one source and stores its 1000 L");
        h.assertTrue(pump.saveWithoutMetadata().getLong("energyBuffer") == 6144,
                "one drained bucket spends the original 2048 RU");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void bronzePumpCanAccumulateOriginalBufferWithoutSimulationMutation(GameTestHelper h) {
        PumpBlockEntity pump = pump(h);
        h.assertTrue(pump.getEnergyDemanded(GregTechTags.Energy.RU, Direction.SOUTH, 32) == 256,
                "bronze pump needs 256 original 32-RU packets for the 8192-RU buffer");
        h.assertTrue(pump.doEnergyInjection(GregTechTags.Energy.RU, Direction.SOUTH, 32, 256, false) == 256
                        && pump.saveWithoutMetadata().getLong("energyBuffer") == 0,
                "RU simulation reports accepted packets without changing state");
        h.assertTrue(pump.doEnergyInjection(GregTechTags.Energy.EU, Direction.SOUTH, 32, 256, true) == 0
                        && pump.doEnergyInjection(GregTechTags.Energy.RU, Direction.NORTH, 32, 256, true) == 0,
                "wrong type and input face reject without consuming packets");
        h.assertTrue(pump.doEnergyInjection(GregTechTags.Energy.RU, Direction.SOUTH, 32, 256, true) == 256
                        && pump.saveWithoutMetadata().getLong("energyBuffer") == 8192,
                "bronze tier can reach the original 8192 RU extraction threshold");
        h.assertTrue(pump.getEnergyDemanded(GregTechTags.Energy.RU, Direction.SOUTH, 32) == 0
                        && pump.doEnergyInjection(GregTechTags.Energy.RU, Direction.SOUTH, 32, 1, true) == 0,
                "full buffer declines another packet");
        var nearlyFull = pump.saveWithoutMetadata();
        nearlyFull.putLong("energyBuffer", 8191);
        pump.load(nearlyFull);
        h.assertTrue(pump.getEnergyDemanded(GregTechTags.Energy.RU, Direction.SOUTH, 32) == 0
                        && pump.doEnergyInjection(GregTechTags.Energy.RU, Direction.SOUTH, 32, 1, true) == 0
                        && pump.saveWithoutMetadata().getLong("energyBuffer") == 8191,
                "a whole RU packet cannot overflow the 8192-RU buffer");
        h.succeed();
    }
}
