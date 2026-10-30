package gregtech.content.blockentity.energy;

import gregtech.api.machine.BlockFace;
import gregtech.api.platform.machine.MachineControl;
import gregtech.api.signal.GtSignalCarrier;
import gregtech.api.signal.GtSignalReceiver;
import gregtech.api.signal.GtSignalSource;
import gregtech.content.block.energy.*;

//? if >=1.21.1 {
/*import gregtech.content.registry.GTSignalWires;
*///?} else {
import gregtech.content.registry.GTBlockEntities;
//?}

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** GT6 fractional redstone attenuation. Rebuild from sources so disconnected loops cannot latch.
 * Members cache their solved tick; mode changes invalidate that member. No world cache or chunk loading.
 */
public final class SignalWireBlockEntity extends BlockEntity implements MachineControl.Provider, GtSignalCarrier {
    public static final long UNIT = GtSignalCarrier.UNIT;

    /** Addon blocks that accept signal power and are not sources. Vanilla receivers stay on the instanceof list. */
    public static final TagKey<Block> RECEIVERS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("gt6ce",
            "signal_wire_receivers"));
    private static final ThreadLocal<Boolean> SAMPLING = ThreadLocal.withInitial(() -> false);
    private long signal, solvedTick = Long.MIN_VALUE;
    private int mode;
    private Direction received;

//? if >=1.21.1 {
    /*public SignalWireBlockEntity(BlockPos pos, BlockState state) {
        super(GTSignalWires.SIGNAL_WIRE.get(), pos, state);
    }
*///?} else {
    public SignalWireBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.SIGNAL_WIRE.get(), pos, state);
    }
//?}

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            gregtech.engine.machine.PipeModelData.refreshCrossChunkNeighbors(level, worldPosition);
        }
    }

//? if >=1.21.1 {
    /*@Override
    public net.neoforged.neoforge.client.model.data.ModelData getModelData() {
*///?} else {
    @Override
    public net.minecraftforge.client.model.data.ModelData getModelData() {
//?}

        float[] halves = new float[6];
        java.util.Arrays.fill(halves, -1);
        if (level != null)
            for (var side : Direction.values()) {
                var pos = worldPosition.relative(side);
                if (level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() instanceof SignalWireBlock wire)
                    halves[side.ordinal()] = wire.halfThickness();
            }

//? if >=1.21.1 {
        /*return net.neoforged.neoforge.client.model.data.ModelData.builder().with(gregtech.engine.machine.PipeModelData.NEIGHBOR_HALVES,
                halves).build();
*///?} else {
        return net.minecraftforge.client.model.data.ModelData.builder().with(gregtech.engine.machine.PipeModelData.NEIGHBOR_HALVES,
                halves).build();
//?}

    }

    public int mode() {
        return mode;
    }

    public void setMode(int value) {
        if (isRemoved() || level != null && level.isClientSide) return;
        mode = Math.max(0, Math.min(15, value));
        setChanged();
        solvedTick = Long.MIN_VALUE;
    }

    public long signal() {
        return signal;
    }

    public long loss() {
        return gregtech.content.energy.SignalWireRules.loss(((SignalWireBlock) getBlockState().getBlock()).range());
    }

    public boolean connected(Direction side) {
        return getBlockState().getValue(ElectricWireBlock.propFor(side));
    }

    public int comparator() {
        return gregtech.content.energy.SignalWireRules.comparator(signal);
    }

    public int output(Direction side) {
        if (level == null || isRemoved()) return 0;
        // GT6 attachment outputs are stored post-tick cover signals, not the wire's
        // transient network value. Keep torch/repeater outputs visible through
        // vanilla conductors while sampling, without relaxing other feedback guards.
        if ((Object) this instanceof gregtech.content.cover.PanelCoverHost host
                && !host.getCover(side).isEmpty()) {
            var cover = gregtech.content.cover.CoverItems.behavior(host.getCover(side));
            boolean attachment = gregtech.content.cover.CoverItems.REDSTONE_TORCH.equals(cover)
                    || gregtech.content.cover.CoverItems.REDSTONE_REPEATER.equals(cover);
            return !SAMPLING.get() || attachment ? host.panels().signal(side) : 0;
        }
        if (SAMPLING.get()) return 0;
        if (signal <= 0 || side == received || !connected(side)) return 0;
        var pos = worldPosition.relative(side);
        if (!level.hasChunkAt(pos)) return 0;
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof SignalWireBlock) return 0;
        return gregtech.content.energy.SignalWireRules.vanillaOutput(signal,
                state.getBlock() instanceof RedStoneWireBlock || state.isRedstoneConductor(level, pos));
    }

    private SignalWireBlockEntity neighbor(Direction side) {
        var pos = worldPosition.relative(side);
        return connected(side) && level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof SignalWireBlockEntity other
                && !other.isRemoved() && other.connected(side.getOpposite()) ? other : null;
    }

    private record Wave(SignalWireBlockEntity wire, long value) {
    }

    public void tickNetwork() {
        if (level == null || level.isClientSide || isRemoved() || solvedTick == level.getGameTime()) return;
        var component = new LinkedHashSet<SignalWireBlockEntity>();
        var queuedWires = new ArrayDeque<SignalWireBlockEntity>();
        component.add(this);
        queuedWires.add(this);
        while (!queuedWires.isEmpty()) {
            var wire = queuedWires.remove();
            for (var side : Direction.values()) {
                var other = wire.neighbor(side);
                if (other != null && component.add(other)) queuedWires.add(other);
            }
        }
        var values = new HashMap<SignalWireBlockEntity, Long>();
        var inputSides = new HashMap<SignalWireBlockEntity, Direction>();
        var propagationQueue = new PriorityQueue<Wave>(Comparator.comparingLong(Wave::value).reversed());
        SAMPLING.set(true);
        try {
            for (var wire : component) {
                long value = Math.max(0, wire.mode * UNIT - wire.loss());
                for (var side : Direction.values()) if (wire.connected(side)) {
                    var pos = wire.worldPosition.relative(side);
                    if (!level.hasChunkAt(pos)) continue;
                    var state = level.getBlockState(pos);
                    // Vanilla consumers are not sources, even when strongly powered through their solid body.
                    if (state.getBlock() instanceof SignalWireBlock || state.getBlock() instanceof DispenserBlock
                            || state.getBlock() instanceof HopperBlock || state.getBlock() instanceof PistonBaseBlock
                            || state.getBlock() instanceof TntBlock || state.getBlock() instanceof NoteBlock
                            || state.getBlock() instanceof TrapDoorBlock || state.getBlock() instanceof DoorBlock
                            || state.getBlock() instanceof PoweredRailBlock || state.getBlock() instanceof RedstoneLampBlock) continue;
                    if (state.is(RECEIVERS)) continue;
                    long emitted;
                    if (state.hasBlockEntity()) {
                        var entity = level.getBlockEntity(pos);
                        if (entity instanceof GtSignalReceiver) continue;
                        if (entity instanceof GtSignalSource source) emitted = GtSignalCarrier.clamp(source.gtSignal(BlockFace.of(side.ordinal())));
                        else emitted = level.getSignal(pos, side) * UNIT;
                    } else emitted = level.getSignal(pos, side) * UNIT;
                    long candidate = emitted - wire.loss();
                    if (candidate > value) {
                        value = candidate;
                        inputSides.put(wire, side);
                    }
                }
                values.put(wire, value);
                if (value > 0) propagationQueue.add(new Wave(wire, value));
            }
        } finally {
            SAMPLING.remove();
        }
        while (!propagationQueue.isEmpty()) {
            var wave = propagationQueue.remove();
            if (wave.value() != values.get(wave.wire())) continue;
            long next = wave.value() - wave.wire().loss();
            if (next <= 0) continue;
            for (var side : Direction.values()) {
                var other = wave.wire().neighbor(side);
                if (other != null && next > values.get(other)) {
                    values.put(other, next);
                    inputSides.put(other, side.getOpposite());
                    propagationQueue.add(new Wave(other, next));
                }
            }
        }
        var changedWires = new ArrayList<SignalWireBlockEntity>();
        for (var wire : component) {
            long next = values.get(wire);
            var input = inputSides.get(wire);
            if (wire.signal != next || wire.received != input
                    || wire.getBlockState().getValue(SignalWireBlock.POWER) != (next + UNIT - 1) / UNIT) changedWires.add(wire);
            wire.signal = next;
            wire.received = input;
            wire.solvedTick = level.getGameTime();
        }
        // Publish the entire solution before notifying consumers; observers never see half an update.
        for (var wire : changedWires) {
            int power = (int)((wire.signal + UNIT - 1) / UNIT);
            var state = wire.getBlockState();
            if (state.getValue(SignalWireBlock.POWER) != power) level.setBlock(wire.worldPosition, state.setValue(SignalWireBlock.POWER, power), 2);
            level.updateNeighborsAt(wire.worldPosition, state.getBlock());
            for (var side : Direction.values()) if (wire.connected(side) && level.hasChunkAt(wire.worldPosition.relative(side)))
                level.updateNeighborsAt(wire.worldPosition.relative(side), state.getBlock());
            level.updateNeighbourForOutputSignal(wire.worldPosition, state.getBlock());
        }
    }

//? if >=1.21.1 {
    /*@Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("mode", mode);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mode = Math.max(0, Math.min(15, tag.getInt("mode")));
        signal = 0;
        received = null;
        solvedTick = Long.MIN_VALUE;
    }
*///?} else {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("mode", mode);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        mode = Math.max(0, Math.min(15, tag.getInt("mode")));
        signal = 0;
        received = null;
        solvedTick = Long.MIN_VALUE;
    }
//?}

    @Override
    public MachineControl machineControl(Direction side) {
        return new MachineControl() {

            public boolean available() {
                return !isRemoved();
            }

            public boolean supportsSwitch() {
                return false;
            }

            public boolean supportsMode() {
                return true;
            }

            public int mode() {
                return available() ? SignalWireBlockEntity.this.mode : 0;
            }

            public int setMode(int value) {
                SignalWireBlockEntity.this.setMode(value);
                return mode();
            }

            public boolean enabled() {
                return available();
            }

            public boolean setEnabled(boolean value) {
                return enabled();
            }

            public boolean running() {
                return available() && signal > 0;
            }

            public boolean active() {
                return running();
            }

            public long progress() {
                return available() ? 1000 * signal / UNIT : 0;
            }

            public long progressMax() {
                return 16000;
            }
        };
    }
}
