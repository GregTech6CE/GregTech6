package gregtech.gametest;

import gregtech.GregTechConfig;
import gregtech.content.block.energy.ElectricWireBlock;
import gregtech.content.block.machine.FluidPipeBlock;
import gregtech.content.block.misc.ChargingCraftingTableBlockEntity;
import gregtech.content.blockentity.energy.ElectricWireBlockEntity;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.blockentity.machine.FluidPipeBlockEntity;
import gregtech.content.blockentity.machine.FluidPipeContents;
import gregtech.content.blockentity.tool.ManualToolBlockEntity;
import gregtech.content.cover.CoverPlacement;
import gregtech.content.cover.CoverUtilityBehaviors;
import gregtech.content.data.GregTechTags;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.energy.ChemicalBatterySpec;
import gregtech.content.registry.GTChemicalBatteries;
import gregtech.content.registry.GTFluids;
import gregtech.content.transport.energy.ElectricNetwork;
import gregtech.content.transport.energy.ElectricNetworks;
import gregtech.content.transport.fluid.FluidPipeNetworks;
import gregtech.engine.fluid.FluidHazard;
import gregtech.engine.fluid.FluidHazards;
import gregtech.engine.stack.NbtCompat;
import gregtech.engine.stack.StackCompat;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
//?}

/** Wire snapshots, lazy overload counters, the idle fluid-pipe wake path and the sifting table's automation handler. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class TransportSchedulingTests {
    private static final String BATCH = "transport_scheduling";
    private static final String BALANCED_BATCH = "balanced_fluid_pipes";

    private TransportSchedulingTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void acidGasNeedsBothProtections(GameTestHelper helper) {
        FluidStack acid = GTFluids.stack("GenGas_HydrochloricAcid", 1000);
        helper.assertTrue(acid != null && !acid.isEmpty(), "the hydrochloric acid fixture is registered");
        var fluid = acid.getFluid();
        helper.assertTrue(FluidHazards.pipeLosses(fluid, Set.of()).equals(new FluidHazards.PipeLosses(8, 0, 16)),
                "acid and gas leakage stack independently");
        var gasAndPlasmaProof = Set.of(FluidHazard.GAS, FluidHazard.PLASMA);
        helper.assertTrue(
                FluidHazards.pipeLosses(fluid, gasAndPlasmaProof).equals(new FluidHazards.PipeLosses(0, 0, 16))
                        && FluidHazards.hasUnprotectedPhysicalHazard(fluid, gasAndPlasmaProof),
                "plasma protection leaves the acid hazard active");
        var gasAndAcidProof = Set.of(FluidHazard.GAS, FluidHazard.ACID);
        helper.assertTrue(
                FluidHazards.pipeLosses(fluid, gasAndAcidProof).equals(new FluidHazards.PipeLosses(0, 0, 0))
                        && !FluidHazards.hasUnprotectedPhysicalHazard(fluid, gasAndAcidProof),
                "the two relevant protections stop both losses and allow admission");
        helper.succeed();
    }

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

    @GameTest(template = "test_empty", batch = BALANCED_BATCH, timeoutTicks = 100)
    public static void balancedFluidPipesLevelAcrossTheComponent(GameTestHelper helper) {
        GregTechConfig.overrideFluidDistributionForTesting(GregTechConfig.FluidDistribution.BALANCED);
        var medium = (FluidPipeBlock) BuiltInRegistries.BLOCK.get(id("pipe_medium_steel"));
        var tiny = (FluidPipeBlock) BuiltInRegistries.BLOCK.get(id("pipe_tiny_steel"));
        var action = IFluidHandler.FluidAction.EXECUTE;
        var pipes = new FluidPipeBlockEntity[5];
        for (int i = 0; i < pipes.length; i++)
            pipes[i] = placeHorizontalPipe(helper, i == 1 ? tiny : medium, i, 2, pipes.length);
        int filled = pipes[0].fill(new FluidStack(Fluids.WATER, 500), action);
        helper.assertTrue(filled == 500, "the first pipe accepts the sample fluid");
        helper.runAfterDelay(30, () -> {
            long min = Long.MAX_VALUE;
            long max = 0;
            long total = 0;
            for (FluidPipeBlockEntity pipe : pipes) {
                long amount = pipe.gibblValue(null);
                min = Math.min(min, amount);
                max = Math.max(max, amount);
                total += amount;
            }
            helper.assertTrue(min > 0, "the far end of the line receives fluid");
            helper.assertTrue(max - min <= 1, "the whole line levels within one unit");
            helper.assertTrue(total == filled, "levelling conserves the entire line's fluid");
            for (var pipe : pipes) pipe.drain(Integer.MAX_VALUE, action);

            var filter = new ItemStack(BuiltInRegistries.ITEM.get(id("fluid_filter")));
            helper.assertTrue(CoverUtilityBehaviors.setFluidFilter(filter, new FluidStack(Fluids.LAVA, 1),
                                    helper.getLevel().registryAccess()) && pipes[2].attachCover(Direction.WEST, filter),
                    "the receiving pipe has a lava-only filter on its west face");
            var valve = new ItemStack(BuiltInRegistries.ITEM.get(id("pressure_value")));
            helper.assertTrue(pipes[3].attachCover(Direction.UP, valve), "the valve attaches off the east-west line");

            int capacity = Math.toIntExact(pipes[0].spec().capacity());
            helper.assertTrue(pipes[0].fill(new FluidStack(Fluids.WATER, capacity), action) == capacity
                            && pipes[2].fill(new FluidStack(Fluids.WATER, 600), action) == 600
                            && pipes[3].fill(new FluidStack(Fluids.WATER, 300), action) == 300,
                    "each group accepts its initial amount");
        });
        helper.runAfterDelay(60, () -> {
            for (int i = 0; i < pipes.length - 1; i++) {
                helper.assertTrue(pipes[i].getBlockState().getValue(FluidPipeBlock.EAST)
                                && pipes[i + 1].getBlockState().getValue(FluidPipeBlock.WEST),
                        "the covers leave the physical east-west connections in place");
            }
            long smallCapacity = pipes[1].spec().capacity();
            long[] expected = {pipes[0].spec().capacity() - smallCapacity, smallCapacity, 600, 300, 0};
            for (int i = 0; i < pipes.length; i++) {
                long amount = pipes[i].gibblValue(null);
                helper.assertTrue(amount == expected[i],
                        "pipe " + i + " kept " + amount + " mB, expected " + expected[i]);
            }
            finish(helper);
        });
    }

    @AfterBatch(batch = BALANCED_BATCH)
    public static void restoreFluidDistribution(ServerLevel level) {
        GregTechConfig.overrideFluidDistributionForTesting(null);
    }

    @GameTest(template = "test_empty", batch = BALANCED_BATCH, timeoutTicks = 20)
    public static void balancedPipesReachAroundAHalfConnectedEdge(GameTestHelper helper) {
        GregTechConfig.overrideFluidDistributionForTesting(GregTechConfig.FluidDistribution.BALANCED);
        var block = (FluidPipeBlock) BuiltInRegistries.BLOCK.get(id("pipe_medium_steel"));
        var start = placeAt(helper, block, 1, 1);
        var east = placeAt(helper, block, 2, 1);
        var corner = placeAt(helper, block, 2, 2);
        var south = placeAt(helper, block, 1, 2);
        var outside = placeAt(helper, block, 0, 1);
        south.setFaceManuallyDisconnected(Direction.NORTH, true);
        outside.setFaceManuallyDisconnected(Direction.EAST, true);
        openFaces(start, Direction.EAST, Direction.SOUTH, Direction.WEST);
        openFaces(east, Direction.WEST, Direction.SOUTH);
        openFaces(corner, Direction.NORTH, Direction.WEST);
        openFaces(south, Direction.EAST);
        openFaces(outside);
        var execute = IFluidHandler.FluidAction.EXECUTE;
        start.fill(new FluidStack(Fluids.WATER, 80), execute);
        outside.fill(new FluidStack(Fluids.WATER, 17), execute);

        var report = FluidPipeContents.describe(start);
        helper.assertTrue(report.size() == 3 && report.get(2).getString().startsWith("80 "),
                "the single-sided external pipe is outside the reported network");
        FluidPipeBlockEntity.serverTick(helper.getLevel(), start.getBlockPos(), start.getBlockState(), start);
        for (var pipe : new FluidPipeBlockEntity[] {start, east, corner, south}) {
            helper.assertTrue(pipe.gibblValue(null) == 20, "the alternate open route reaches every corner");
        }
        helper.assertTrue(outside.gibblValue(null) == 17, "the single-sided pipe remains untouched");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BALANCED_BATCH, timeoutTicks = 20)
    public static void balancedPipesLevelEachWaterVariant(GameTestHelper helper) {
        GregTechConfig.overrideFluidDistributionForTesting(GregTechConfig.FluidDistribution.BALANCED);
        var block = (FluidPipeBlock) BuiltInRegistries.BLOCK.get(id("pipe_quadruple_bronze"));
        var first = placeHorizontalPipe(helper, block, 0, 2, 2);
        var second = placeHorizontalPipe(helper, block, 1, 2, 2);
        var plain = new FluidStack(Fluids.WATER, 100);
        var marked = markedWater(helper, 80);
        var execute = IFluidHandler.FluidAction.EXECUTE;
        first.fill(plain, execute);
        first.fill(marked, execute);
        var report = FluidPipeContents.describe(first);
        helper.assertTrue(report.size() == 5 && report.get(3).getString().startsWith("100 ")
                        && report.get(4).getString().startsWith("80 "),
                "network rows retain both variants in first-seen order");

        FluidPipeBlockEntity.serverTick(helper.getLevel(), first.getBlockPos(), first.getBlockState(), first);
        for (var pipe : new FluidPipeBlockEntity[] {first, second}) {
            var firstSlot = pipe.getFluidInTank(0);
            var secondSlot = pipe.getFluidInTank(1);
            helper.assertTrue(StackCompat.sameFluid(firstSlot, plain) && firstSlot.getAmount() == 50
                            && StackCompat.sameFluid(secondSlot, marked) && secondSlot.getAmount() == 40,
                    "each water variant levels its own channel and retains its data");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void pipeDiagnosticsShowFullStoredAmounts(GameTestHelper helper) {
        var block = (FluidPipeBlock) BuiltInRegistries.BLOCK.get(id("pipe_medium_steel"));
        var first = placeHorizontalPipe(helper, block, 0, 2, 2);
        var second = placeHorizontalPipe(helper, block, 1, 2, 2);
        restoreWaterAmount(helper, first, 5_000_000_000L);
        restoreWaterAmount(helper, second, Long.MAX_VALUE);
        var report = FluidPipeContents.describe(first);
        helper.assertTrue(report.size() == 3 && report.get(0).getString().startsWith("5_000_000_000 ")
                        && report.get(2).getString().startsWith("9_223_372_041_854_775_807 "),
                "both local storage and component totals retain the full legacy quantities");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "shared_fluid_pipes", timeoutTicks = 20)
    public static void pipeDiagnosticsPreserveSharedDrainOrder(GameTestHelper helper) {
        var block = (FluidPipeBlock) BuiltInRegistries.BLOCK.get(id("pipe_medium_steel"));
        var first = placeHorizontalPipe(helper, block, 0, 2, 2);
        var second = placeHorizontalPipe(helper, block, 1, 2, 2);
        var execute = IFluidHandler.FluidAction.EXECUTE;
        try {
            GregTechConfig.overrideFluidDistributionForTesting(GregTechConfig.FluidDistribution.LOCAL);
            first.fill(new FluidStack(Fluids.WATER, 25), execute);
            second.fill(new FluidStack(Fluids.LAVA, 35), execute);
            GregTechConfig.overrideFluidDistributionForTesting(GregTechConfig.FluidDistribution.SHARED);
            var baseline = first.drain(100, IFluidHandler.FluidAction.SIMULATE);
            FluidPipeNetworks.invalidate(first);
//? if >=1.21.1 {
            /*second.printNetworkInfo(helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL));
*///?} else {
            second.printNetworkInfo(helper.makeMockPlayer());
//?}
            helper.assertTrue(first.network() == null && second.network() == null,
                    "inspection from the far end did not create a shared-pool membership");
            var actual = first.drain(100, execute);
            helper.assertTrue(baseline.getFluid() == Fluids.WATER && StackCompat.sameFluid(actual, baseline)
                            && actual.getAmount() == 25 && second.getFluidInTank(0).getFluid() == Fluids.LAVA,
                    "the same water is withdrawn after inspection from the opposite end");
        } finally {
            GregTechConfig.overrideFluidDistributionForTesting(null);
        }
        helper.succeed();
    }

    private static FluidPipeBlockEntity placeAt(GameTestHelper helper, FluidPipeBlock block, int x, int z) {
        var pos = helper.absolutePos(new BlockPos(x, 2, z));
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        return (FluidPipeBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    private static void openFaces(FluidPipeBlockEntity pipe, Direction... faces) {
        var state = pipe.getBlockState().getBlock().defaultBlockState();
        for (Direction face : faces) state = state.setValue(FluidPipeBlock.propFor(face), true);
        pipe.getLevel().setBlock(pipe.getBlockPos(), state, Block.UPDATE_CLIENTS);
    }

    private static void restoreWaterAmount(GameTestHelper helper, FluidPipeBlockEntity pipe, long amount) {
        var lookup = helper.getLevel().registryAccess();
        var contents = new CompoundTag();
        contents.put("Fluid", NbtCompat.saveFluid(new FluidStack(Fluids.WATER, 1), lookup));
        contents.putLong("Amount", amount);
        var tag = new CompoundTag();
        tag.put("gt.tank.0", contents);
        NbtCompat.loadBlockEntity(pipe, tag, lookup);
    }

    private static FluidStack markedWater(GameTestHelper helper, int amount) {
        var tag = new CompoundTag();
        tag.putString("FluidName", "minecraft:water");
        tag.putInt("Amount", amount);
        var data = new CompoundTag();
        data.putBoolean("marked", true);
        tag.put("Tag", data);
        return NbtCompat.readFluid(tag, helper.getLevel().registryAccess());
    }

    @GameTest(template = "test_empty", batch = "shared_fluid_pipes", timeoutTicks = 20)
    public static void sharedPoolDrainKeepsEachFluidIdentity(GameTestHelper helper) {
        var block = (FluidPipeBlock) BuiltInRegistries.BLOCK.get(id("pipe_quadruple_bronze"));
        var first = placeHorizontalPipe(helper, block, 0, 2, 2);
        var second = placeHorizontalPipe(helper, block, 1, 2, 2);
        int amount = 100;
        var water = new FluidStack(Fluids.WATER, amount);
        var lava = new FluidStack(Fluids.LAVA, amount);
        var marked = markedWater(helper, amount);

        try {
            GregTechConfig.overrideFluidDistributionForTesting(GregTechConfig.FluidDistribution.LOCAL);
            var execute = IFluidHandler.FluidAction.EXECUTE;
            helper.assertTrue(first.fill(water, execute) == amount && first.fill(marked, execute) == amount
                            && first.fill(lava, execute) == amount && second.fill(water, execute) == amount,
                    "both pipes hold the fixture fluids");
            GregTechConfig.overrideFluidDistributionForTesting(GregTechConfig.FluidDistribution.SHARED);
            var preview = first.drain(4 * amount, IFluidHandler.FluidAction.SIMULATE);
            helper.assertTrue(StackCompat.sameFluid(water, preview) && preview.getAmount() == 2 * amount
                            && first.getFluidInTank(0).getAmount() == amount && second.getFluidInTank(0).getAmount() == amount,
                    "simulation counts only plain water from both pipes and consumes none");
            var drained = first.drain(4 * amount, execute);
            helper.assertTrue(StackCompat.sameFluid(water, drained) && drained.getAmount() == 2 * amount
                            && first.getFluidInTank(0).isEmpty() && second.getFluidInTank(0).isEmpty(),
                    "the quantity drain collects all plain water across both pipes");
            var remaining = first.getFluidInTank(1);
            helper.assertTrue(StackCompat.sameFluid(marked, remaining) && remaining.getAmount() == amount
                            && first.getFluidInTank(2).getFluid() == Fluids.LAVA && first.getFluidInTank(2).getAmount() == amount,
                    "water with different data and lava remain intact");
        } finally {
            GregTechConfig.overrideFluidDistributionForTesting(null);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "shared_fluid_pipes", timeoutTicks = 20)
    public static void sharedPoolSkipsPressureValveStorage(GameTestHelper helper) {
        var block = (FluidPipeBlock) BuiltInRegistries.BLOCK.get(id("pipe_medium_steel"));
        var source = placeHorizontalPipe(helper, block, 0, 2, 3);
        var valvePipe = placeHorizontalPipe(helper, block, 1, 2, 3);
        var target = placeHorizontalPipe(helper, block, 2, 2, 3);
        var execute = IFluidHandler.FluidAction.EXECUTE;
        var simulate = IFluidHandler.FluidAction.SIMULATE;
        var valve = new ItemStack(BuiltInRegistries.ITEM.get(id(CoverUtilityBehaviors.PRESSURE_VALVE)));
        int capacity = source.getTankCapacity(0);
        var request = new FluidStack(Fluids.WATER, capacity + 100);
        try {
            GregTechConfig.overrideFluidDistributionForTesting(GregTechConfig.FluidDistribution.LOCAL);
            helper.assertTrue(valvePipe.attachCover(Direction.UP, valve)
                            && valvePipe.fill(new FluidStack(Fluids.WATER, 40), execute) == 40,
                    "the middle pipe holds a local reserve behind its pressure valve");
            GregTechConfig.overrideFluidDistributionForTesting(GregTechConfig.FluidDistribution.SHARED);
            helper.assertTrue(source.fill(request, simulate) == request.getAmount()
                            && source.gibblValue(null) == 0 && target.gibblValue(null) == 0,
                    "a simulated pool fill reaches through the valve pipe without filling any tank");
            helper.assertTrue(source.fill(request, execute) == request.getAmount()
                            && source.gibblValue(null) == capacity && target.gibblValue(null) == 100
                            && valvePipe.gibblValue(null) == 40 && request.getAmount() == capacity + 100,
                    "pool fills the connected non-valve tanks in order without mutating the request");
            helper.assertTrue(source.drain(request, simulate).getAmount() == request.getAmount()
                            && source.gibblValue(null) == capacity && target.gibblValue(null) == 100,
                    "a typed simulation reads both pool members without draining either");
            var drained = source.drain(new FluidStack(Fluids.WATER, capacity + 140), execute);
            helper.assertTrue(drained.getAmount() == request.getAmount() && valvePipe.gibblValue(null) == 40,
                    "a typed pool drain leaves the pressure valve's reserve intact");
            helper.assertTrue(valvePipe.drain(100, execute).getAmount() == 40
                            && valvePipe.fill(request, execute) == capacity
                            && source.gibblValue(null) == 0 && target.gibblValue(null) == 0,
                    "a request at the pressure valve's host uses its local tank only");
        } finally {
            GregTechConfig.overrideFluidDistributionForTesting(null);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "shared_fluid_pipes", timeoutTicks = 20)
    public static void sharedPoolSideFilterUsesFirstStoredFluid(GameTestHelper helper) {
        var block = (FluidPipeBlock) BuiltInRegistries.BLOCK.get(id("pipe_quadruple_bronze"));
        var port = placeHorizontalPipe(helper, block, 0, 2, 2);
        var reservoir = placeHorizontalPipe(helper, block, 1, 2, 2);
        var execute = IFluidHandler.FluidAction.EXECUTE;
        var filter = new ItemStack(BuiltInRegistries.ITEM.get(id(CoverUtilityBehaviors.FILTER_FLUID)));
        openFaces(port, Direction.UP, Direction.EAST);
//? if >=1.21.1 {
        /*var side = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, port.getBlockPos(), Direction.UP);
*///?} else {
        var side = port.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, Direction.UP)
                .orElseThrow(IllegalStateException::new);
//?}
        helper.assertTrue(side != null, "the port exposes the upward fluid handler");
        try {
            GregTechConfig.overrideFluidDistributionForTesting(GregTechConfig.FluidDistribution.LOCAL);
            reservoir.fill(new FluidStack(Fluids.WATER, 80), execute);
            reservoir.fill(new FluidStack(Fluids.LAVA, 50), execute);
            helper.assertTrue(CoverUtilityBehaviors.setFluidFilter(
                                    filter, new FluidStack(Fluids.LAVA, 1), helper.getLevel().registryAccess())
                            && port.attachCover(Direction.UP, filter),
                    "the port admits lava only");
            GregTechConfig.overrideFluidDistributionForTesting(GregTechConfig.FluidDistribution.SHARED);
            helper.assertTrue(side.drain(100, execute).isEmpty() && reservoir.gibblValue(null) == 130,
                    "the filter refuses a quantity drain when the pool's first stored fluid is water");
            port.removeCover(Direction.UP);
            var water = side.drain(100, execute);
            helper.assertTrue(water.getFluid() == Fluids.WATER && water.getAmount() == 80,
                    "the same face drains the pool's first fluid after the filter is removed");
            helper.assertTrue(port.attachCover(Direction.UP, filter), "the filter can be put back");
            var lava = side.drain(100, execute);
            helper.assertTrue(lava.getFluid() == Fluids.LAVA && lava.getAmount() == 50,
                    "the filter admits the new first fluid in the pool");
        } finally {
            GregTechConfig.overrideFluidDistributionForTesting(null);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 120)
    public static void siftingTableAcceptsAutomationInputAndOutput(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(pos,
                BuiltInRegistries.BLOCK.get(id("sifting_table")).defaultBlockState());
        if (!(helper.getLevel().getBlockEntity(pos) instanceof ManualToolBlockEntity table)) {
            helper.fail("sifting table block entity missing");
            return;
        }
        IItemHandler handler = itemHandler(helper, pos, Direction.UP);
        if (handler == null) {
            helper.fail("sifting table exposes no item handler");
            return;
        }

        ItemStack input = deterministicSiftingInput();
        helper.assertFalse(input.isEmpty(), "a deterministic sifting row exists");
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.STICK), false).getCount() == 1,
                "a non-siftable item is rejected");
        helper.assertTrue(handler.insertItem(0, input.copyWithCount(3), false).getCount() == 2,
                "the input slot holds one item");
        helper.assertTrue(handler.getStackInSlot(0).is(input.getItem())
                        && handler.getStackInSlot(0).getCount() == 1,
                "the input slot received the sifting input");
        helper.assertTrue(handler.insertItem(0, input.copyWithCount(1), false).getCount() == 1,
                "an occupied input slot rejects further insertion");
        helper.assertTrue(handler.extractItem(0, 1, false).isEmpty(), "the input slot is not extractable");
        helper.assertTrue(handler.insertItem(1, input.copyWithCount(1), false).getCount() == 1,
                "output slots are not insertable");

        ServerPlayer player = placeSiftingPlayer(helper, pos);
        helper.runAfterDelay(2, () -> table.interact(player, InteractionHand.MAIN_HAND, Direction.UP));
        helper.runAfterDelay(60, () -> {
            ItemStack output = ItemStack.EMPTY;
            for (int slot = 1; slot < handler.getSlots() && output.isEmpty(); slot++) {
                output = handler.extractItem(slot, 64, false);
            }
            helper.assertFalse(output.isEmpty(), "sifting produced an extractable output");
            helper.assertTrue(handler.getStackInSlot(0).isEmpty(), "the input was consumed");
            helper.assertTrue(handler.insertItem(0, input.copyWithCount(1), false).isEmpty(),
                    "the next input is accepted once the output is drained");
        });
        helper.runAfterDelay(110, () -> {
            ItemStack output = ItemStack.EMPTY;
            for (int slot = 1; slot < handler.getSlots() && output.isEmpty(); slot++) {
                output = handler.extractItem(slot, 64, false);
            }
            helper.assertFalse(output.isEmpty(), "the standing player resumed sifting for the next input");
            helper.getLevel().getServer().getPlayerList().remove(player);
            finish(helper);
        });
    }

    /** A real server player in the player list; the 1.20.1 helper's bare connection has no channel. */
    private static ServerPlayer placeSiftingPlayer(GameTestHelper helper, BlockPos pos) {
        var level = helper.getLevel();
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "sifting-test");
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
//? if >=1.21.1 {
        /*var player = new ServerPlayer(level.getServer(), level, profile,
                net.minecraft.server.level.ClientInformation.createDefault());
*///?} else {
        var player = new ServerPlayer(level.getServer(), level, profile);
//?}
        try {
//? if >=1.21.1 {
            /*level.getServer().getPlayerList().placeNewPlayer(connection, player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false));
*///?} else {
            level.getServer().getPlayerList().placeNewPlayer(connection, player);
//?}
        } catch (UnsupportedOperationException unsentLoginPayload) {
            // The embedded connection has no negotiated client channels; the player is registered already.
        }
        player.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0, 90);
        return player;
    }

    private static IItemHandler itemHandler(GameTestHelper helper, BlockPos pos, Direction side) {
//? if >=1.21.1 {
        /*return helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, side);
*///?} else {
        return helper.getLevel().getBlockEntity(pos)
                .getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, side)
                .orElse(null);
//?}
    }

    /** The first row whose first output is guaranteed, so the sifting result is not random. */
    private static ItemStack deterministicSiftingInput() {
        for (var row : MachineRecipeMaps.Sifting.recipeRows) {
            if (!row.enabled || row.fakeRecipe || row.itemInputs.length != 1 || row.itemInputs[0].isEmpty()
                    || row.itemInputs[0].getCount() != 1 || row.getOutputChance(0) < 10000
                    || row.itemOutputs.length == 0 || row.itemOutputs[0].isEmpty()) continue;
            ItemStack candidate = row.itemInputs[0].copyWithCount(1);
            var match = MachineRecipeMaps.Sifting.findRecipe(
                    java.util.List.of(candidate), java.util.List.of(), false, 1, 12);
            if (match != null && match.getOutputChance(0) >= 10000 && match.itemOutputs.length > 0
                    && !match.itemOutputs[0].isEmpty()) return candidate;
        }
        return ItemStack.EMPTY;
    }

    private static BlockPos at(GameTestHelper helper, int height) {
        return helper.absolutePos(new BlockPos(0, height, 0));
    }

    private static FluidPipeBlockEntity placeHorizontalPipe(
            GameTestHelper helper, FluidPipeBlock block, int x, int y, int length) {
        BlockPos pos = helper.absolutePos(new BlockPos(x, y, 0));
        var state = block.defaultBlockState();
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

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 40)
    public static void pressureValvePreviewMatchesAttachOnMultiTankAndConnectedPipes(GameTestHelper helper) {
        var level = helper.getLevel();
        FluidPipeBlockEntity single = placePipe(helper, 2);
        placePipe(helper, 3);
        level.setBlockAndUpdate(at(helper, 4), BuiltInRegistries.BLOCK.get(id("pipe_quadruple_bronze")).defaultBlockState());
        level.setBlockAndUpdate(at(helper, 5), BuiltInRegistries.BLOCK.get(id("mixer_bronze")).defaultBlockState());
        var quad = (FluidPipeBlockEntity) level.getBlockEntity(at(helper, 4));
        var machine = (BasicMachineBlockEntity) level.getBlockEntity(at(helper, 5));
        var valve = new ItemStack(BuiltInRegistries.ITEM.get(id("pressure_value")));

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(CoverPlacement.canPlace(level, at(helper, 2), Direction.EAST, valve),
                    "single-tank fluid pipe preview allows pressure valve on open face");
            helper.assertFalse(CoverPlacement.canPlace(level, at(helper, 2), Direction.UP, valve),
                    "single-tank fluid pipe preview rejects pressure valve facing another fluid pipe");
            helper.assertFalse(single.attachCover(Direction.UP, valve),
                    "single-tank fluid pipe attach rejects pressure valve facing another fluid pipe");
            helper.assertFalse(CoverPlacement.canPlace(level, at(helper, 4), Direction.EAST, valve),
                    "quadruple fluid pipe preview rejects pressure valve");
            helper.assertFalse(quad.attachCover(Direction.EAST, valve),
                    "quadruple fluid pipe attach rejects pressure valve");
            helper.assertFalse(CoverPlacement.canPlace(level, at(helper, 5), Direction.UP, valve),
                    "basic machine preview rejects pressure valve");
            helper.assertFalse(machine.attachCover(Direction.UP, valve),
                    "basic machine attach rejects pressure valve");
            helper.assertTrue(single.attachCover(Direction.EAST, valve),
                    "single-tank fluid pipe attaches pressure valve on open face");
            for (int height = 2; height <= 5; height++) {
                if (level.getBlockEntity(at(helper, height)) instanceof FluidPipeBlockEntity pipe) {
                    for (Direction face : Direction.values()) pipe.removeCover(face);
                }
                level.setBlockAndUpdate(at(helper, height), Blocks.AIR.defaultBlockState());
            }
            finish(helper);
        });
    }

    private static void finish(GameTestHelper helper) {
        helper.succeed();
    }
}
