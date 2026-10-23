package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.block.energy.SignalWireBlock;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.energy.SignalWireBlockEntity;
import com.gregtech.gregtech.content.cover.PanelCoverHost;
import com.gregtech.gregtech.registry.GTSignalWires;
import com.gregtech.gregtech.registry.GTToolItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Ignored native draft: original explicit connector ports and real vanilla cover-item dispatch. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class SignalWirePlacementCoverTests {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static SignalWireBlock wire(boolean insulated) {
        return GTSignalWires.all().stream().map(r -> r.get())
                .filter(b -> b.material().getName().equals("RedAlloy") && b.insulated() == insulated)
                .findFirst().orElseThrow();
    }
    private static int mask(BlockState state) {
        int result = 0;
        for (var side : Direction.values()) if (state.getValue(ElectricWireBlock.propFor(side))) result |= 1 << side.ordinal();
        return result;
    }
    private static BlockState ports(SignalWireBlock block, int mask) {
        var state = block.defaultBlockState();
        for (var side : Direction.values()) state = state.setValue(ElectricWireBlock.propFor(side), (mask & 1 << side.ordinal()) != 0);
        return state;
    }
    private static CompoundTag save(GameTestHelper h, BlockEntity entity) {
//? if >=1.21.1 {
/*        return entity.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        return entity.saveWithoutMetadata();
//?}
    }
    private static void load(GameTestHelper h, BlockEntity entity, CompoundTag data) {
//? if >=1.21.1 {
/*        entity.loadWithComponents(data, h.getLevel().registryAccess());
*///?} else {
        entity.load(data);
//?}
    }
    private static final class Fixture implements AutoCloseable {
        private record Saved(BlockState state, CompoundTag data) {}
        final GameTestHelper h;
        final BlockPos pos;
        final ServerPlayer player;
        final Map<BlockPos, Saved> saved = new LinkedHashMap<>();
        Fixture(GameTestHelper h, int index) {
            this.h = h;
            pos = new BlockPos(21_950_000 + index * 128, 230, 21_950_000);
//? if >=1.21.1 {
/*            player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
*///?} else {
            player = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
//?}
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "[gt-signal-port]"));
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.setShiftKeyDown(false);
            clear();
        }
        void set(BlockPos at, BlockState state) {
            if (!saved.containsKey(at)) {
                var entity = h.getLevel().getBlockEntity(at);
                saved.put(at.immutable(), new Saved(h.getLevel().getBlockState(at), entity == null ? null : save(h, entity)));
            }
            h.getLevel().removeBlockEntity(at);
            h.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), FLAGS);
            h.getLevel().setBlock(at, state, FLAGS);
        }
        void clear() {
            for (var at : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) set(at, Blocks.AIR.defaultBlockState());
        }
        SignalWireBlockEntity node(BlockPos at, boolean insulated, int initialMask) {
            set(at, ports(wire(insulated), initialMask));
            var result = h.getLevel().getBlockEntity(at);
            h.assertTrue(result instanceof SignalWireBlockEntity, "actual registered signal-wire entity");
            return (SignalWireBlockEntity) result;
        }
        void click(BlockPos at, Direction side) {
            var normal = Vec3.atLowerCornerOf(side.getNormal());
            var point = Vec3.atCenterOf(at).add(normal.scale(.5));
            var stand = Vec3.atCenterOf(at).add(normal.scale(2.5));
            player.setPos(stand.x, stand.y, stand.z);
            player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(point, side, at, false));
        }
        SignalWireBlockEntity place(Direction supportSide) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(wire(false), 2));
            click(pos.relative(supportSide), supportSide.getOpposite());
            h.assertTrue(h.getLevel().getBlockEntity(pos) instanceof SignalWireBlockEntity
                    && player.getMainHandItem().getCount() == 1, "native BlockItem placement succeeds and consumes exactly one wire");
            return (SignalWireBlockEntity) h.getLevel().getBlockEntity(pos);
        }
        void cutter() {
            player.setItemInHand(InteractionHand.MAIN_HAND, GTToolHelper.write(GTToolItems.empty(GTToolType.WIRE_CUTTER),
                    GTMaterialRegistry.get("Steel"), GTMaterialRegistry.get("Wood")));
        }
        @Override public void close() {
            player.closeContainer();
            player.getInventory().clearContent();
            player.discard();
            for (var at : saved.keySet()) {
                h.getLevel().removeBlockEntity(at);
                h.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), FLAGS);
            }
            for (var entry : saved.entrySet()) {
                h.getLevel().setBlock(entry.getKey(), entry.getValue().state(), FLAGS);
                var entity = h.getLevel().getBlockEntity(entry.getKey());
                if (entity != null && entry.getValue().data() != null) load(h, entity, entry.getValue().data());
            }
        }
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void signalWirePlacementHonorsExplicitNeighborPorts(GameTestHelper h) {
        var observations = new ArrayList<String>();
        boolean correct = true;
        try (var f = new Fixture(h, 0)) {
            for (var support : Direction.values()) {
                // Original redstone canConnect=true intentionally allows the clicked ordinary stone face.
                f.clear();
                f.set(f.pos.relative(support), Blocks.STONE.defaultBlockState());
                var lone = f.place(support);
                h.assertTrue(mask(lone.getBlockState()) == 1 << support.ordinal(), "isolated placement opens only its support face: " + support);
                f.clear();
                f.set(f.pos.relative(support), SignalProbes.SOURCE.defaultBlockState());
                var selectedSource = (SignalProbes.SignalProbeSourceBlockEntity) h.getLevel().getBlockEntity(f.pos.relative(support));
                selectedSource.emitted = 5 * SignalWireBlockEntity.UNIT;
                var selectedWire = f.place(support);
                selectedWire.setMode(0); selectedWire.tickNetwork();
                h.assertTrue(mask(selectedWire.getBlockState()) == 1 << support.ordinal()
                                && selectedWire.signal() == 5 * SignalWireBlockEntity.UNIT - selectedWire.loss()
                                && selectedSource.queriedSide == support.ordinal(),
                        "explicit clicked port still reads native GtSignalSource on the original query direction: " + support);
                f.clear();
                f.set(f.pos.relative(support), Blocks.STONE.defaultBlockState());
                Direction closed = support.getAxis() == Direction.Axis.X ? Direction.NORTH : Direction.EAST;
                Direction open = closed.getOpposite();
                f.node(f.pos.relative(closed), true, 0);
                f.node(f.pos.relative(open), true, 1 << open.getOpposite().ordinal());
                for (var source : Direction.values()) if (source != support && source != closed && source != open)
                    f.set(f.pos.relative(source), source == support.getOpposite()
                            ? SignalProbes.SOURCE.defaultBlockState() : Blocks.REDSTONE_BLOCK.defaultBlockState());
                var unselectedSource = (SignalProbes.SignalProbeSourceBlockEntity) h.getLevel().getBlockEntity(f.pos.relative(support.getOpposite()));
                unselectedSource.emitted = 5 * SignalWireBlockEntity.UNIT;
                var placed = f.place(support);
                placed.setMode(0); placed.tickNetwork();
                int actual = mask(placed.getBlockState());
                int expected = (1 << support.ordinal()) | (1 << open.ordinal());
                int closedNeighbor = mask(h.getLevel().getBlockState(f.pos.relative(closed)));
                boolean row = actual == expected && closedNeighbor == 0 && unselectedSource.queriedSide == Integer.MIN_VALUE
                        && h.getLevel().getBlockState(f.pos.relative(open)).getValue(ElectricWireBlock.propFor(open.getOpposite()));
                observations.add(support + " mask=" + actual + ", expected=" + expected + ", closedNeighbor=" + closedNeighbor
                        + ", unselectedSourceQuery=" + unselectedSource.queriedSide);
                correct &= row;
            }
        }
        h.assertTrue(correct, "only support plus already-open compatible neighbors connect, not unrelated sources/closed ends: " + observations);
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void signalWirePortsSurviveNeighborPowerAndRemoval(GameTestHelper h) {
        try (var f = new Fixture(h, 1)) {
            var source = f.pos.east();
            f.set(source, Blocks.REDSTONE_BLOCK.defaultBlockState());
            var node = f.node(f.pos, false, 1 << Direction.EAST.ordinal());
            f.cutter();
            f.click(f.pos, Direction.EAST);
            h.assertTrue(!node.connected(Direction.EAST), "native cutter closes a source-facing port");
            h.getLevel().updateNeighborsAt(source, Blocks.REDSTONE_BLOCK);
            boolean cutStaysClosed = !node.connected(Direction.EAST);
            f.clear();
            f.set(source, Blocks.REDSTONE_BLOCK.defaultBlockState());
            node = f.node(f.pos, false, 1 << Direction.EAST.ordinal());
            h.getLevel().removeBlock(source, false);
            boolean openPortRetained = node.connected(Direction.EAST);
            node.setMode(0);
            node.tickNetwork();
            h.assertTrue(node.signal() == 0, "source removal still clears signal even with an explicitly open port");
            h.assertTrue(cutStaysClosed && openPortRetained,
                    "port state is explicit, not inferred from neighbor presence: cutStaysClosed=" + cutStaysClosed
                            + ", openPortRetained=" + openPortRetained);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void bareAndInsulatedSignalWiresAcceptNativeTorchCovers(GameTestHelper h) {
        var observations = new ArrayList<String>();
        boolean correct = true;
        try (var f = new Fixture(h, 2)) {
            // Retain unrelated-host rejection before any candidate-failing assertion.
            for (var block : new Block[]{BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof ElectricWireBlock).findFirst().orElseThrow(),
                    BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof BasicMachineBlock).findFirst().orElseThrow()}) {
                f.clear();
                f.set(f.pos, block.defaultBlockState());
                f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.REDSTONE_TORCH, 3));
                f.click(f.pos, Direction.NORTH);
                var host = (PanelCoverHost) h.getLevel().getBlockEntity(f.pos);
                h.assertTrue(host.getCover(Direction.NORTH).isEmpty() && f.player.getMainHandItem().getCount() == 3,
                        "non-signal host rejects torch without consuming it");
            }
            for (boolean insulated : new boolean[]{true, false}) for (var item : new net.minecraft.world.item.Item[]{Items.REDSTONE_TORCH, Items.REPEATER})
                for (var side : Direction.values()) {
                    f.clear();
                    var node = f.node(f.pos, insulated, 63);
                    var host = (PanelCoverHost) (Object) node;
                    f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 3));
                    f.click(f.pos, side);
                    boolean attached = host.getCover(side).is(item);
                    boolean consumed = f.player.getMainHandItem().getCount() == 2;
                    boolean disconnected = mask(node.getBlockState()) == (63 & ~(1 << side.ordinal()));
                    boolean noWorldBlock = h.getLevel().getBlockState(f.pos.relative(side)).isAir();
                    boolean occupiedRejected = !host.attachCover(side, new ItemStack(item));
                    host.panels().afterTick();
                    int idle = node.output(side);
                    node.setMode(15); node.tickNetwork(); host.panels().afterTick();
                    int powered = node.output(side);
                    boolean signalCorrect = item == Items.REDSTONE_TORCH ? idle == 15 && powered == 0 : idle == 0 && powered == 15;
                    boolean row = attached && consumed && disconnected && noWorldBlock && occupiedRejected && signalCorrect;
                    observations.add((insulated ? "cable" : "bare") + ":" + item + ":" + side + " attached=" + attached
                            + ", held=" + f.player.getMainHandItem().getCount() + ", mask=" + mask(node.getBlockState())
                            + ", idle/powered=" + idle + "/" + powered);
                    if (insulated) h.assertTrue(row, "existing insulated positive control: " + observations.get(observations.size() - 1));
                    correct &= row;
                }
        }
        h.assertTrue(correct, "original bare class inherits torch/repeater host support; native item-first attachment: " + observations);
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void signalPlacementAndCutterRespectNeighborTorchBlocker(GameTestHelper h) {
        try (var f = new Fixture(h, 3)) {
            var other = f.node(f.pos.east(), true, 0);
            var otherHost = (PanelCoverHost) (Object) other;
            h.assertTrue(otherHost.attachCover(Direction.WEST, new ItemStack(Items.REDSTONE_TORCH)), "real insulated neighbor accepts blocker");
            var placed = f.place(Direction.EAST);
            boolean placementBlocked = !placed.connected(Direction.EAST) && !other.connected(Direction.WEST);
            // Explicitly closed setup isolates cutter admission from the separate placement defect.
            h.getLevel().setBlock(f.pos, ports(wire(false), 0), FLAGS);
            f.cutter();
            f.click(f.pos, Direction.EAST);
            boolean cutterBlocked = !placed.connected(Direction.EAST) && !other.connected(Direction.WEST);
            h.assertTrue(otherHost.getCover(Direction.WEST).is(Items.REDSTONE_TORCH), "connection attempts do not remove neighboring cover");
            h.assertTrue(placementBlocked && cutterBlocked,
                    "original interceptConnect applies to clicked support and cutter on both loaders: placementBlocked="
                            + placementBlocked + ", cutterBlocked=" + cutterBlocked);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void pairedSignalWiresStayDisconnectedAfterNativeCutter(GameTestHelper h) {
        var observations = new ArrayList<String>();
        boolean correct = true;
        try (var f = new Fixture(h, 4)) {
            for (boolean insulated : new boolean[]{false, true}) for (var towardPeer : Direction.values()) {
                f.clear();
                var support = f.pos.relative(towardPeer.getOpposite());
                var peerPos = f.pos.relative(towardPeer);
                f.set(support, Blocks.STONE.defaultBlockState());
                f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(wire(insulated), 3));
                f.click(support, towardPeer);
                h.assertTrue(h.getLevel().getBlockEntity(f.pos) instanceof SignalWireBlockEntity
                                && f.player.getMainHandItem().getCount() == 2,
                        "first actual BlockItem placement consumes exactly one wire");
                f.click(f.pos, towardPeer);
                h.assertTrue(h.getLevel().getBlockEntity(peerPos) instanceof SignalWireBlockEntity
                                && f.player.getMainHandItem().getCount() == 1,
                        "second actual BlockItem placement consumes exactly one wire");
                var first = (SignalWireBlockEntity) h.getLevel().getBlockEntity(f.pos);
                var peer = (SignalWireBlockEntity) h.getLevel().getBlockEntity(peerPos);
                int supportBit = 1 << towardPeer.getOpposite().ordinal();
                int joinBit = 1 << towardPeer.ordinal();
                h.assertTrue(mask(h.getLevel().getBlockState(f.pos)) == (supportBit | joinBit)
                                && mask(h.getLevel().getBlockState(peerPos)) == supportBit
                                && first.connected(towardPeer) && peer.connected(towardPeer.getOpposite()),
                        "native placed pair has a reciprocal selected connection: " + towardPeer);
                first.setMode(5); peer.setMode(0); first.tickNetwork();
                h.assertTrue(peer.signal() == 5 * SignalWireBlockEntity.UNIT - first.loss() - peer.loss(),
                        "connected pair transmits the source mode before cutting");
                f.cutter();
                f.click(f.pos, towardPeer);
                int cutFirst = mask(h.getLevel().getBlockState(f.pos));
                int cutPeer = mask(h.getLevel().getBlockState(peerPos));
                boolean cut = cutFirst == supportBit && cutPeer == 0
                        && !first.connected(towardPeer) && !peer.connected(towardPeer.getOpposite());
                h.getLevel().updateNeighborsAt(f.pos, wire(insulated));
                h.getLevel().updateNeighborsAt(peerPos, wire(insulated));
                first.setMode(7); peer.setMode(0); first.tickNetwork(); peer.tickNetwork();
                int updatedFirst = mask(h.getLevel().getBlockState(f.pos));
                int updatedPeer = mask(h.getLevel().getBlockState(peerPos));
                long isolatedSignal = peer.signal();
                boolean staysCut = updatedFirst == supportBit && updatedPeer == 0
                        && !first.connected(towardPeer) && !peer.connected(towardPeer.getOpposite())
                        && isolatedSignal == 0 && first.signal() == 7 * SignalWireBlockEntity.UNIT - first.loss();
                observations.add((insulated ? "cable:" : "bare:") + towardPeer + " cut=" + cutFirst + "/" + cutPeer
                        + ", afterUpdates=" + updatedFirst + "/" + updatedPeer + ", peerSignal=" + isolatedSignal);
                correct &= cut && staysCut;
                // Independent reconnection positive: seed both closed ends without neighbor notifications,
                // so a baseline cut failure cannot prevent exercising native cutter opening as well.
                h.getLevel().setBlock(f.pos, ports(wire(insulated), supportBit), FLAGS);
                h.getLevel().setBlock(peerPos, ports(wire(insulated), 0), FLAGS);
                h.assertTrue(!first.connected(towardPeer) && !peer.connected(towardPeer.getOpposite()),
                        "independent closed-end reconnection fixture is visible to both real entities");
                f.click(f.pos, towardPeer);
                first.setMode(5); peer.setMode(0); first.tickNetwork();
                h.assertTrue(mask(h.getLevel().getBlockState(f.pos)) == (supportBit | joinBit)
                                && mask(h.getLevel().getBlockState(peerPos)) == supportBit
                                && first.connected(towardPeer) && peer.connected(towardPeer.getOpposite())
                                && peer.signal() == 5 * SignalWireBlockEntity.UNIT - first.loss() - peer.loss()
                                && h.getLevel().getBlockState(support).is(Blocks.STONE),
                        "native cutter reconnects the same pair, preserves support, and restores transmission");
            }
        }
        h.assertTrue(correct, "paired red-alloy ports stay cut through reciprocal notifications and signal activity: " + observations);
        h.succeed();
    }
}
