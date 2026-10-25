package gregtech.gametest;

import gregtech.content.block.misc.LongDistanceTransformerBlock;
import gregtech.content.block.misc.LongDistanceTransformerBlockEntity;
import gregtech.content.data.GregTechTags;
import gregtech.content.registry.GTMiscBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Candidate only: real sender and remote transformer, observing downstream consumer fixture. */
@GameTestHolder("gt6ce_long_distance_reload")
@PrefixGameTestTemplate(false)
public final class LongDistanceWireReloadTests {
    private static final BlockPos SOURCE = new BlockPos(2, 2, 2);
    private static final BlockPos RECEIVER = new BlockPos(5, 2, 2);
    private static final long PACKET = 2048;

    /** Not a replacement for the remote endpoint; observes its downstream EU injection. */
    private static final class Sink extends LongDistanceTransformerBlockEntity {
        long calls, received;
        Sink(BlockPos pos, BlockState state) { super(pos, state); }
        @Override public void tickActivity() {}
        @Override public boolean isEnergyType(GregTechTags.Tag type, Direction side, boolean emitting) {
            return type == GregTechTags.Energy.EU;
        }
        @Override public long doEnergyInjection(GregTechTags.Tag type, Direction side, long size, long amount, boolean execute) {
            if (type != GregTechTags.Energy.EU || side != Direction.WEST || size <= 0 || amount <= 0) return 0;
            if (execute) { calls++; received += size; }
            return 1;
        }
        @Override public long getEnergyDemanded(GregTechTags.Tag type, Direction side, long size) { return 1; }
        @Override public long getEnergyOffered(GregTechTags.Tag type, Direction side, long size) { return 0; }
    }

    /** Caller-owned packet budget, not a claim that the stateless transformer has an energy buffer. */
    private static final class Producer {
        long stored = PACKET * 10;
        long inject(LongDistanceTransformerBlockEntity source) {
            long used = source.doEnergyInjection(GregTechTags.Energy.EU, Direction.WEST, PACKET, 1, true);
            if (used < 0 || used > 1) throw new AssertionError("invalid accepted packet count");
            stored -= used * PACKET;
            return used;
        }
    }

    private static LongDistanceTransformerBlockEntity source(GameTestHelper h) {
        return (LongDistanceTransformerBlockEntity) h.getBlockEntity(SOURCE);
    }

    /** Fresh BE instances at unchanged block states; no wrench, rescan or topology mutation. */
    private static void reloadEndpoints(GameTestHelper h) {
        var oldSource = source(h);
        var oldReceiver = (LongDistanceTransformerBlockEntity) h.getBlockEntity(RECEIVER);
        var sourceTag = oldSource.saveWithoutMetadata();
        var receiverTag = oldReceiver.saveWithoutMetadata();
        h.assertTrue(sourceTag.contains("gt.target") && sourceTag.getLong("gt.distance") > 0
                && sourceTag.getLong("gt.throughput") > 0, "must save a genuinely established nonempty route");
        h.assertTrue(BlockPos.of(sourceTag.getLong("gt.target")).equals(h.absolutePos(RECEIVER)), "saved remote coordinate");
        for (var pos : new BlockPos[] { SOURCE, RECEIVER }) h.getLevel().removeBlockEntity(h.absolutePos(pos));
        var newSource = new LongDistanceTransformerBlockEntity(h.absolutePos(SOURCE), h.getBlockState(SOURCE));
        var newReceiver = new LongDistanceTransformerBlockEntity(h.absolutePos(RECEIVER), h.getBlockState(RECEIVER));
        newSource.load(sourceTag);
        newReceiver.load(receiverTag);
        h.getLevel().setBlockEntity(newSource);
        h.getLevel().setBlockEntity(newReceiver);
        h.assertTrue(newSource != oldSource && newReceiver != oldReceiver
                && source(h) == newSource && h.getBlockEntity(RECEIVER) == newReceiver,
                "reload must use new source and receiver objects, not an in-place NBT reset");
    }

    private static void positive(GameTestHelper h, Producer producer, Sink sink) {
        long before = producer.stored, received = sink.received, calls = sink.calls;
        h.assertTrue(source(h).doEnergyInjection(GregTechTags.Energy.EU, Direction.WEST, PACKET, 1, false) == 1
                && sink.calls == calls && sink.received == received && producer.stored == before,
                "valid-route simulation has no energy effects");
        h.assertTrue(producer.inject(source(h)) == 1 && producer.stored == before - PACKET
                && sink.calls == calls + 1 && sink.received == received + 1984,
                "one positive packet: source spends 2048, remote emits 1984, line loses 64");
        h.assertTrue(source(h).getEnergyOffered(GregTechTags.Energy.EU, null, PACKET) == 0, "no duplicate pull output");
    }

    private static void broken(GameTestHelper h, Producer producer, Sink sink) {
        long before = producer.stored, received = sink.received, calls = sink.calls;
        h.assertTrue(!source(h).isEnergyAcceptingFrom(GregTechTags.Energy.EU, Direction.WEST, false)
                && source(h).getEnergyDemanded(GregTechTags.Energy.EU, Direction.WEST, PACKET) == 0,
                "broken restored route must not request input");
        h.assertTrue(source(h).doEnergyInjection(GregTechTags.Energy.EU, Direction.WEST, PACKET, 1, false) == 0,
                "broken restored route must refuse simulation");
        h.assertTrue(producer.inject(source(h)) == 0 && producer.stored == before
                && sink.calls == calls && sink.received == received,
                "broken route accepts zero: caller loses no EU and downstream receives no EU");
    }

    private static void exercise(GameTestHelper h, boolean queryBeforeBreak) {
        var state = GTMiscBlocks.LONG_DIST_TRANSFORMER_ULV.get().defaultBlockState()
                .setValue(LongDistanceTransformerBlock.FACING, Direction.WEST);
        h.setBlock(SOURCE, state);
        h.setBlock(SOURCE.east(), GTMiscBlocks.LONG_DIST_WIRE.get());
        h.setBlock(SOURCE.east(2), GTMiscBlocks.LONG_DIST_WIRE.get());
        h.setBlock(RECEIVER, state);
        h.setBlock(RECEIVER.east(), state);
        var sink = new Sink(h.absolutePos(RECEIVER.east()), state);
        h.getLevel().setBlockEntity(sink);
        var producer = new Producer();
        positive(h, producer, sink); // Necessary: establishes gt.target, distance and throughput before saving.
        reloadEndpoints(h);
        if (queryBeforeBreak) positive(h, producer, sink);
        h.setBlock(SOURCE.east(2), Blocks.AIR);
        broken(h, producer, sink);
        h.setBlock(SOURCE.east(2), GTMiscBlocks.LONG_DIST_WIRE.get());
        positive(h, producer, sink); // Repair without wrench/soft hammer.
        reloadEndpoints(h);
        positive(h, producer, sink); // Complete restored route recovers automatically.
        h.succeed();
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 100)
    public static void wireFreshReloadThenTransferThenBreak(GameTestHelper h) { exercise(h, true); }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 100)
    public static void wireFreshReloadThenBreakBeforeFirstQuery(GameTestHelper h) { exercise(h, false); }
}
