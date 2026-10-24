package gregtech.gametest;

import gregtech.content.block.energy.ElectricWireBlock;
import gregtech.content.block.energy.SignalWireBlock;
import gregtech.content.block.machine.FluidPipeBlock;
import gregtech.content.block.machine.ItemPipeBlock;
import gregtech.content.blockentity.machine.FluidPipeBlockEntity;
import gregtech.content.blockentity.machine.ItemPipeBlockEntity;
import gregtech.content.registry.GTItemPipes;
import gregtech.content.registry.GTSignalWires;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Addon marker pipe opt-in stays unchanged; signal-wire ports are explicit placement/tool state. */
@GameTestHolder("gregtech_pipe_auto")
@PrefixGameTestTemplate(false)
public final class PipeAutoConnectTests {
    private PipeAutoConnectTests() {}

    private static FluidPipeBlock fluidPipe() {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "pipe_huge_steel"));
        if (!(block instanceof FluidPipeBlock pipe)) throw new IllegalStateException("fluid pipe missing");
        return pipe;
    }

    /** Mirrors BlockItem: the block first, then the placement hook that can see the block entity. */
    private static FluidPipeBlockEntity placeFluid(GameTestHelper helper, BlockPos pos) {
        var block = fluidPipe();
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        block.setPlacedBy(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), null,
                net.minecraft.world.item.ItemStack.EMPTY);
        var entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof FluidPipeBlockEntity, "fluid pipe block entity");
        return (FluidPipeBlockEntity) entity;
    }

    private static ItemPipeBlockEntity placeItem(GameTestHelper helper, BlockPos pos) {
        var block = GTItemPipes.PIPE_MEDIUM_BRASS.get();
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        block.setPlacedBy(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), null,
                net.minecraft.world.item.ItemStack.EMPTY);
        var entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof ItemPipeBlockEntity, "item pipe block entity");
        return (ItemPipeBlockEntity) entity;
    }

    private static boolean fluidOpen(GameTestHelper helper, FluidPipeBlockEntity pipe, Direction side) {
        return helper.getLevel().getBlockState(pipe.getBlockPos()).getValue(FluidPipeBlock.propFor(side));
    }

    private static boolean itemOpen(GameTestHelper helper, ItemPipeBlockEntity pipe, Direction side) {
        return helper.getLevel().getBlockState(pipe.getBlockPos()).getValue(ItemPipeBlock.propFor(side));
    }

    private static SignalWireBlock wireBlock() {
        return GTSignalWires.all().stream().map(r -> r.get())
                .filter(b -> b.material().getName().equalsIgnoreCase("RedAlloy") && !b.insulated())
                .findFirst().orElseThrow();
    }

    private static void placeWire(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos, wireBlock().defaultBlockState());
    }

    private static boolean wireOpen(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getBlockState(pos).getValue(ElectricWireBlock.propFor(side));
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void fluidPipeConnectsToConnectableBlockPlacedAfter(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(3, 2, 3));
        var pipe = placeFluid(helper, pos);
        helper.assertTrue(!fluidOpen(helper, pipe, Direction.EAST), "a lone pipe starts closed");
        helper.getLevel().setBlockAndUpdate(pos.east(), SignalProbes.FLUID_CONNECTABLE.defaultBlockState());
        helper.assertTrue(fluidOpen(helper, pipe, Direction.EAST), "a neighbour appearing later opens the pipe");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void fluidPipePlacedAfterConnectableBlockConnects(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(3, 4, 3));
        helper.getLevel().setBlockAndUpdate(pos.east(), SignalProbes.FLUID_CONNECTABLE.defaultBlockState());
        var pipe = placeFluid(helper, pos);
        helper.assertTrue(fluidOpen(helper, pipe, Direction.EAST), "placement finds the connectable neighbour");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void itemPipeAutoConnectsInBothOrders(GameTestHelper helper) {
        var first = helper.absolutePos(new BlockPos(3, 6, 3));
        var pipe = placeItem(helper, first);
        helper.getLevel().setBlockAndUpdate(first.east(), SignalProbes.ITEM_CONNECTABLE.defaultBlockState());
        helper.assertTrue(itemOpen(helper, pipe, Direction.EAST), "item pipe opens toward a later block");
        var second = helper.absolutePos(new BlockPos(3, 8, 3));
        helper.getLevel().setBlockAndUpdate(second.west(), SignalProbes.ITEM_CONNECTABLE.defaultBlockState());
        var placed = placeItem(helper, second);
        helper.assertTrue(itemOpen(helper, placed, Direction.WEST), "item pipe opens on placement");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void wrenchDisconnectSurvivesNeighbourChanges(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(3, 2, 6));
        var pipe = placeFluid(helper, pos);
        helper.getLevel().setBlockAndUpdate(pos.east(), SignalProbes.FLUID_CONNECTABLE.defaultBlockState());
        helper.assertTrue(fluidOpen(helper, pipe, Direction.EAST), "connectable neighbour opens the pipe");
        var block = (FluidPipeBlock) helper.getLevel().getBlockState(pos).getBlock();

        block.connectionToggled(helper.getLevel(), pos, Direction.EAST, false);
        helper.getLevel().setBlockAndUpdate(pos, pipe.getBlockState().setValue(FluidPipeBlock.propFor(Direction.EAST), false));
        helper.assertTrue(pipe.isFaceManuallyDisconnected(Direction.EAST), "the wrench records the override");

        helper.getLevel().setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos.east(), SignalProbes.FLUID_CONNECTABLE.defaultBlockState());
        helper.assertTrue(!fluidOpen(helper, pipe, Direction.EAST), "auto-connect leaves a wrenched face closed");

        block.connectionToggled(helper.getLevel(), pos, Direction.EAST, true);
        helper.assertTrue(!pipe.isFaceManuallyDisconnected(Direction.EAST), "reconnecting clears the override");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void placementScanRepairsHalfConnection(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(3, 4, 6));
        var first = placeFluid(helper, pos);
        var second = placeFluid(helper, pos.east());
        helper.getLevel().setBlock(pos.east(), second.getBlockState().setValue(
                FluidPipeBlock.propFor(Direction.WEST), true), 0);
        helper.assertTrue(!fluidOpen(helper, first, Direction.EAST), "the half-connection is still missing on this side");
        first.autoConnectOnPlace(null);
        helper.assertTrue(fluidOpen(helper, first, Direction.EAST), "the load-time scan completes the half-connection");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void signalWireIgnoresUnselectedSourcePlacedAfter(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(3, 6, 6));
        placeWire(helper, pos);
        helper.assertTrue(!wireOpen(helper, pos, Direction.EAST), "a lone wire starts closed");
        helper.getLevel().setBlockAndUpdate(pos.east(), SignalProbes.SOURCE.defaultBlockState());
        helper.assertTrue(!wireOpen(helper, pos, Direction.EAST), "GtSignalSource compatibility does not open an unselected port");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void signalWireStatePlacementDoesNotInferSourcePorts(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(3, 8, 6));
        helper.getLevel().setBlockAndUpdate(pos.east(), SignalProbes.SOURCE.defaultBlockState());
        placeWire(helper, pos);
        helper.assertTrue(!wireOpen(helper, pos, Direction.EAST), "a closed blockstate is not rewritten from nearby signal sources");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void signalWireRetainsExplicitPortWhenSourceIsRemoved(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(3, 10, 6));
        helper.getLevel().setBlockAndUpdate(pos.east(), SignalProbes.SOURCE.defaultBlockState());
        // Seed the requested port before onPlace: a closed-state placement followed by an identical
        // world-state write also exercises the separate recursive-onPlace stale-BE defect.
        helper.getLevel().setBlockAndUpdate(pos,wireBlock().defaultBlockState().setValue(ElectricWireBlock.EAST,true));
        var source=(SignalProbes.SignalProbeSourceBlockEntity)helper.getLevel().getBlockEntity(pos.east());
        source.emitted=5*gregtech.content.blockentity.energy.SignalWireBlockEntity.UNIT;
        var wire=(gregtech.content.blockentity.energy.SignalWireBlockEntity)helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(wire.getBlockState().equals(helper.getLevel().getBlockState(pos))
                && wire.connected(Direction.EAST),"initial explicit port agrees in the world and block entity");
        wire.setMode(0);wire.tickNetwork();
        helper.assertTrue(wire.signal()>0&&wireOpen(helper,pos,Direction.EAST),"an explicitly open port reads the addon source");
        helper.getLevel().removeBlock(pos.east(), false);
        wire.setMode(0);wire.tickNetwork();
        helper.assertTrue(wireOpen(helper,pos,Direction.EAST)&&wire.signal()==0,"source removal preserves the port while explicit recomputation clears power");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void signalWiresKeepUnselectedNeighborPortsClosed(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(3, 12, 6));
        placeWire(helper, pos);
        placeWire(helper, pos.east());
        helper.assertTrue(!wireOpen(helper, pos, Direction.EAST), "first closed wire does not infer a new port");
        helper.assertTrue(!wireOpen(helper, pos.east(), Direction.WEST), "second closed wire does not force its neighbor open");
        helper.succeed();
    }
}
