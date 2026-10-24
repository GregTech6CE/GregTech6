package gregtech.gametest;

import gregtech.api.signal.GtSignalCarrier;
import gregtech.api.signal.GtSignalReceiver;
import gregtech.block.energy.ElectricWireBlock;
import gregtech.block.energy.SignalWireBlock;
import gregtech.blockentity.energy.SignalWireBlockEntity;
import gregtech.gametest.SignalProbes.SignalProbeSourceBlockEntity;
import gregtech.registry.GTSignalWires;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Addon source, receiver, and tag behavior. Existing SignalWireTests stay unchanged. */

/** Addon source, receiver, and tag behavior. */
@GameTestHolder("gregtech_signal_wire")
@PrefixGameTestTemplate(false)
public final class SignalWireAddonTests {
    private SignalWireAddonTests() {}

    private static SignalWireBlock wireBlock() {
        return GTSignalWires.all().stream().map(r -> r.get())
                .filter(b -> b.material().getName().equalsIgnoreCase("RedAlloy") && !b.insulated())
                .findFirst().orElseThrow();
    }

    private static SignalWireBlockEntity place(GameTestHelper helper, BlockPos pos) {
        var state = wireBlock().defaultBlockState();
        for (var property : ElectricWireBlock.CONNECTIONS) state = state.setValue(property, true);
        helper.getLevel().setBlockAndUpdate(pos, state);
        return (SignalWireBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    private static void solve(SignalWireBlockEntity wire) {
        wire.setMode(wire.mode());
        wire.tickNetwork();
    }

    /** The production tag file must stay empty. A same-path test datapack is rejected as a test-only jar entry. 1.21.1 makes bindTags package-private. */
    private static void includeInReceiverTag(Block block) {
        var holder = block.builtInRegistryHolder();
        var tags = new java.util.ArrayList<>(holder.tags().toList());
        if (tags.contains(SignalWireBlockEntity.RECEIVERS)) return;
        tags.add(SignalWireBlockEntity.RECEIVERS);
        try {
            var method = holder.getClass().getDeclaredMethod("bindTags", java.util.Collection.class);
            method.setAccessible(true);
            method.invoke(holder, java.util.List.copyOf(tags));
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("cannot add the signal receiver tag", failure);
        }
    }

    /** A powered wall lever strongly powers the block it is attached to. A redstone block only weakly powers its neighbors. */
    private static void stronglyPower(GameTestHelper helper, BlockPos block) {
        helper.getLevel().setBlockAndUpdate(block.west(), Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.WALL)
                .setValue(LeverBlock.FACING, Direction.WEST)
                .setValue(LeverBlock.POWERED, true));
    }

    private static SignalProbeSourceBlockEntity source(GameTestHelper helper, BlockPos pos, long emitted) {
        helper.getLevel().setBlockAndUpdate(pos, SignalProbes.SOURCE.defaultBlockState());
        var probe = (SignalProbeSourceBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(probe != null, "source probe block entity");
        probe.emitted = emitted;
        return probe;
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void vanillaRedstoneBlockStillFeedsTheWire(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(4, 2, 4));
        helper.getLevel().setBlockAndUpdate(pos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        var wire = place(helper, pos);
        solve(wire);
        helper.assertTrue(wire.signal() == 15 * SignalWireBlockEntity.UNIT - wire.loss(), "redstone block still feeds the wire");
        helper.assertTrue(wire.output(Direction.WEST) == 0, "no feedback into the receiving face");
        helper.assertTrue(wire.output(Direction.SOUTH) == 15, "full output into air");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void vanillaStoneConductsStrongPower(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(4, 4, 4));
        var stone = pos.west();
        helper.getLevel().setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
        stronglyPower(helper, stone);
        var wire = place(helper, pos);
        helper.assertTrue(helper.getLevel().getSignal(stone, Direction.WEST) == 15, "stone still conducts strong power");
        solve(wire);
        helper.assertTrue(wire.signal() == 15 * GtSignalCarrier.UNIT - wire.loss(), "a plain conductor is still a source");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void fractionalSourceEntersAndPropagates(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(3, 3, 4));
        var first = place(helper, pos);
        var second = place(helper, pos.east());
        long emitted = GtSignalCarrier.UNIT / 2;
        var probe = source(helper, pos.west(), emitted);
        solve(first);
        long loss = first.loss();
        helper.assertTrue(SignalWireBlockEntity.UNIT == GtSignalCarrier.UNIT && GtSignalCarrier.UNIT == Integer.MAX_VALUE,
                "platform unit still points at the shared constant");
        helper.assertTrue(probe.queriedSide == Direction.WEST.ordinal(), "wire asks the face it is facing");
        helper.assertTrue(((GtSignalCarrier) first).signal() == emitted - loss, "fractional strength enters the wire");
        helper.assertTrue(second.signal() == emitted - 2 * loss, "the next hop keeps the fraction");
        helper.assertTrue(first.comparator() == 0, "comparator still floors a partial level");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void outOfRangeSourceIsClamped(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(4, 6, 4));
        var wire = place(helper, pos);
        var probe = source(helper, pos.west(), Long.MAX_VALUE);
        solve(wire);
        helper.assertTrue(wire.signal() == 15 * GtSignalCarrier.UNIT - wire.loss(), "a huge source clamps to fifteen levels");
        probe.emitted = 15L * GtSignalCarrier.UNIT + 1;
        solve(wire);
        helper.assertTrue(wire.signal() == 15 * GtSignalCarrier.UNIT - wire.loss(), "one unit past fifteen levels clamps");
        probe.emitted = -1;
        solve(wire);
        helper.assertTrue(probe.queriedSide == Direction.WEST.ordinal() && wire.signal() == 0, "a negative source clamps to zero");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void receiverBlockEntityIsNotASource(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(6, 2, 4));
        var receiver = pos.west();
        helper.getLevel().setBlockAndUpdate(receiver, SignalProbes.RECEIVER.defaultBlockState());
        stronglyPower(helper, receiver);
        helper.assertTrue(helper.getLevel().getBlockEntity(receiver) instanceof GtSignalReceiver, "receiver probe is marked");
        helper.assertTrue(!helper.getLevel().getBlockState(receiver).is(SignalWireBlockEntity.RECEIVERS), "receiver probe is not in the tag");
        helper.assertTrue(helper.getLevel().getSignal(receiver, Direction.WEST) == 15, "vanilla would read this powered block");
        var wire = place(helper, pos);
        solve(wire);
        helper.assertTrue(wire.signal() == 0, "a receiver block entity is not a signal source");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void taggedBlockIsNotASource(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(6, 4, 4));
        var tagged = pos.west();
        includeInReceiverTag(SignalProbes.TAGGED);
        helper.getLevel().setBlockAndUpdate(tagged, SignalProbes.TAGGED.defaultBlockState());
        stronglyPower(helper, tagged);
        helper.assertTrue(helper.getLevel().getBlockState(tagged).is(SignalWireBlockEntity.RECEIVERS), "tagged probe is in the receiver tag");
        helper.assertTrue(helper.getLevel().getBlockEntity(tagged) == null, "tag path does not need a block entity");
        helper.assertTrue(helper.getLevel().getSignal(tagged, Direction.WEST) == 15, "vanilla would read this powered block");
        var wire = place(helper, pos);
        solve(wire);
        helper.assertTrue(wire.signal() == 0, "a tagged block is not a signal source");
        helper.succeed();
    }
}
