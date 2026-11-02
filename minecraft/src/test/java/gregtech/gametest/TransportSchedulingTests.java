package gregtech.gametest;

import gregtech.GregTechConfig;
import gregtech.content.block.energy.ElectricWireBlock;
import gregtech.content.block.machine.FluidPipeBlock;
import gregtech.content.block.misc.ChargingCraftingTableBlockEntity;
import gregtech.content.blockentity.energy.ElectricWireBlockEntity;
import gregtech.content.blockentity.machine.FluidPipeBlockEntity;
import gregtech.content.data.GregTechTags;
import gregtech.content.energy.ChemicalBatterySpec;
import gregtech.content.registry.GTChemicalBatteries;
import gregtech.content.transport.energy.ElectricNetwork;
import gregtech.content.transport.energy.ElectricNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
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

/** Wire snapshots, lazy overload counters and the idle fluid-pipe wake path. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class TransportSchedulingTests {
    private static final String BATCH = "transport_scheduling";

    private TransportSchedulingTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void wireSnapshotTracksConnectionChanges(GameTestHelper helper) {
        ElectricWireBlockEntity lower = placeWire(helper, 2);
        ElectricWireBlockEntity upper = placeWire(helper, 3);
        helper.runAfterDelay(2, () -> {
            ElectricNetwork network = ElectricNetworks.networkOf(lower);
            helper.assertTrue(network != null && network.contains(upper), "both wires share one snapshot");
            var connected = lower.getBlockState();
            helper.getLevel().setBlockAndUpdate(at(helper, 2), connected.setValue(ElectricWireBlock.UP, false));
            helper.assertFalse(network.valid(), "opening a connection invalidates the snapshot");
            helper.assertTrue(lower.network() == null, "invalid membership is released");
            helper.getLevel().setBlockAndUpdate(at(helper, 2), connected.setValue(ElectricWireBlock.UP, true));
            ElectricNetwork rebuilt = ElectricNetworks.networkOf(lower);
            helper.assertTrue(rebuilt != null && rebuilt != network && rebuilt.contains(upper),
                    "reconnecting rebuilds the component");
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void wireInjectionUsesSnapshotAndPublishesPreviousTick(GameTestHelper helper) {
        ElectricWireBlockEntity lower = placeWire(helper, 2);
        placeWire(helper, 3);
        placeChargingTable(helper, 1);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(lower.doEnergyInjection(GregTechTags.Energy.EU, Direction.UP, lower.voltage(), 1, true) == 1,
                    "the charging table accepts one packet through the snapshot");
            helper.assertTrue(lower.transferredAmperes() == 1, "the traversal counts the accepted ampere");
        });
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(lower.lastWattage() > 0, "the previous tick's post-loss wattage is published");
            helper.assertTrue(lower.transferredAmperes() == 0, "the next window starts empty");
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void wireOverloadBurnsAtTheNextLevelTick(GameTestHelper helper) {
        ElectricWireBlockEntity wire = placeWire(helper, 2);
        placeChargingTable(helper, 1);
        helper.runAfterDelay(2, () -> {
            long overload = wire.voltage() + wire.lossPerBlock() + 1;
            for (int i = 0; i < 16; i++) {
                helper.assertTrue(wire.doEnergyInjection(GregTechTags.Energy.EU, Direction.UP, overload, 1, true) == 1,
                        "each overload packet is delivered and counted");
            }
            helper.assertTrue(wire.burnCounter() == 16, "sixteen overload events saturate the burn counter");
            helper.assertTrue(helper.getLevel().getBlockState(at(helper, 2)).is(wire.getBlockState().getBlock()),
                    "burning waits for the level tick");
        });
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(at(helper, 2)).getBlock() == Blocks.FIRE,
                    "the start-of-tick queue replaces the overloaded cable with fire");
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void idleFluidPipeWakesOnExternalFill(GameTestHelper helper) {
        FluidPipeBlockEntity lower = placePipe(helper, 2);
        FluidPipeBlockEntity upper = placePipe(helper, 3);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(lower.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) > 0,
                    "external fill is accepted");
            helper.assertFalse(lower.isIdle(), "the filled pipe leaves idle state");
        });
        // Long enough for BALANCED's 10-tick component pass; LOCAL distributes sooner.
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(upper.gibblValue(null) > 0, "the woken pipe distributes into the network");
            finish(helper);
        });
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void balancedFluidPipesLevelAcrossTheComponent(GameTestHelper helper) {
        GregTechConfig.overrideFluidDistributionForTesting(GregTechConfig.FluidDistribution.BALANCED);
        int length = 5;
        FluidPipeBlockEntity[] pipes = new FluidPipeBlockEntity[length];
        for (int i = 0; i < length; i++) pipes[i] = placeHorizontalPipe(helper, i, 2, length);
        long capacity = pipes[0].spec().capacity();
        int filled = pipes[0].fill(new FluidStack(Fluids.WATER, (int) Math.min(Integer.MAX_VALUE, capacity)),
                IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled > 0, "the first pipe accepts the sample fluid");
        helper.runAfterDelay(30, () -> {
            long min = Long.MAX_VALUE;
            long max = 0;
            for (FluidPipeBlockEntity pipe : pipes) {
                long amount = pipe.gibblValue(null);
                min = Math.min(min, amount);
                max = Math.max(max, amount);
            }
            try {
                helper.assertTrue(min > 0, "the far end of the line receives fluid");
                helper.assertTrue(max - min <= 1, "the whole line levels within one unit");
            } finally {
                GregTechConfig.overrideFluidDistributionForTesting(null);
            }
            finish(helper);
        });
    }

    private static BlockPos at(GameTestHelper helper, int height) {
        return helper.absolutePos(new BlockPos(0, height, 0));
    }

    private static FluidPipeBlockEntity placeHorizontalPipe(GameTestHelper helper, int x, int y, int length) {
        BlockPos pos = helper.absolutePos(new BlockPos(x, y, 0));
        var state = fluidPipeBlock().defaultBlockState();
        if (x > 0) state = state.setValue(FluidPipeBlock.WEST, true);
        if (x < length - 1) state = state.setValue(FluidPipeBlock.EAST, true);
        helper.getLevel().setBlockAndUpdate(pos, state);
        return (FluidPipeBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    private static FluidPipeBlock fluidPipeBlock() {
        return (FluidPipeBlock) BuiltInRegistries.BLOCK.stream()
                .filter(candidate -> candidate instanceof FluidPipeBlock)
                .findFirst()
                .orElseThrow();
    }

    private static ResourceLocation id(String path) {
//? if >=1.21.1 {
        /*return ResourceLocation.fromNamespaceAndPath("gt6ce", path);
*///?} else {
        return new ResourceLocation("gt6ce", path);
//?}
    }

    private static ElectricWireBlock wireBlock() {
        return BuiltInRegistries.BLOCK.stream()
                .filter(block -> block instanceof ElectricWireBlock)
                .map(block -> (ElectricWireBlock) block)
                .filter(block -> block.spec().insulated() && block.spec().size() == 1)
                .min(java.util.Comparator.comparingLong(block -> block.spec().voltage()))
                .orElseThrow();
    }

    private static ElectricWireBlockEntity placeWire(GameTestHelper helper, int height) {
        var state = wireBlock().defaultBlockState()
                .setValue(ElectricWireBlock.UP, true)
                .setValue(ElectricWireBlock.DOWN, true);
        helper.getLevel().setBlockAndUpdate(at(helper, height), state);
        return (ElectricWireBlockEntity) helper.getLevel().getBlockEntity(at(helper, height));
    }

    private static ChargingCraftingTableBlockEntity placeChargingTable(GameTestHelper helper, int height) {
        Block block = BuiltInRegistries.BLOCK.get(id("charging_crafting_table"));
        helper.getLevel().setBlockAndUpdate(at(helper, height), block.defaultBlockState());
        var table = (ChargingCraftingTableBlockEntity) helper.getLevel().getBlockEntity(at(helper, height));
        table.items().setStackInSlot(16,
                new ItemStack(GTChemicalBatteries.item(ChemicalBatterySpec.Chemistry.NICKEL_CADMIUM, 1)));
        return table;
    }

    private static FluidPipeBlockEntity placePipe(GameTestHelper helper, int height) {
        var state = fluidPipeBlock().defaultBlockState()
                .setValue(FluidPipeBlock.UP, true)
                .setValue(FluidPipeBlock.DOWN, true);
        helper.getLevel().setBlockAndUpdate(at(helper, height), state);
        return (FluidPipeBlockEntity) helper.getLevel().getBlockEntity(at(helper, height));
    }

    private static void finish(GameTestHelper helper) {
        helper.succeed();
    }
}
