package com.gregtech.gregtech.blockentity.energy;

import com.gregtech.gregtech.api.energy.EnergyNet;
import com.gregtech.gregtech.api.energy.EnergyTransfer;
import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.api.energy.IEnergyConductor;
import com.gregtech.gregtech.api.energy.WireSpec;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.data.GregTechConstants;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** GT6 electric wire block entity (EU cable, pipe-style connection grid). */
public class ElectricWireBlockEntity extends BlockEntity implements IEnergyConductor, IEnergyBlock {
    private boolean initialized;
    // GT6 runtime-only wire counters: aggregate transfers between entity ticks.
    private long lastWattage;
    private long transferredWattage;
    private long transferredAmperes;
    private long timer;
    private int burnCounter;

    public ElectricWireBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.ELECTRIC_WIRE.get(), pos, state);
    }

    private WireSpec wireSpec() {
        BlockState st = getBlockState();
        if (st.getBlock() instanceof ElectricWireBlock wb) return wb.spec();
        // fallback defaults
        return new WireSpec("copper", null, 1, 32, 1, 1);
    }

    public long voltage()     { return wireSpec().voltage(); }
    public long amperage()    { return wireSpec().amperage(); }
    public long lossPerBlock(){ return wireSpec().lossPerBlock(); }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            com.gregtech.gregtech.api.machine.PipeModelData.refreshCrossChunkNeighbors(level, worldPosition);
        }
        if (!initialized && level != null && !level.isClientSide) {
            initialized = true;
            EnergyNet.onPlace(level, worldPosition);
            onConnectionChanged();
        }
    }

    public void onRemoved() {
        if (level != null && !level.isClientSide) EnergyNet.onRemove(level, worldPosition);
    }

    /**
     * Releases this wire's {@link EnergyNet} entry when the block entity goes away.
     *
     * <p>{@link #onRemoved} is otherwise reached only from {@code ElectricWireBlock#onRemove}, which
     * runs when the block is replaced or broken - a chunk unload never goes through the block. Vanilla
     * unloads a chunk by calling {@code BlockEntity#onChunkUnloaded} and then {@code setRemoved} on
     * every block entity of the chunk and dropping them from the chunk map
     * ({@code LevelChunk#clearAllBlockEntities}). Without these two overrides the wire position stayed
     * in the per-dimension adjacency map for the lifetime of the level: the map grew with every chunk a
     * player visited and never shrank, which is the unbounded growth this fixes. Both calls are
     * idempotent, and a reloaded chunk builds a fresh block entity that registers itself again from
     * {@link #onLoad}.</p>
     */
    @Override
    public void setRemoved() {
        onRemoved();
        super.setRemoved();
    }

    /** The other half of vanilla's unload pair; see {@link #setRemoved()}. */
    @Override
    public void onChunkUnloaded() {
        onRemoved();
        super.onChunkUnloaded();
    }

    public void onConnectionChanged() {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        EnergyNet.onConnectionChange(level, worldPosition, state, ElectricWireBlock.CONNECTIONS);
        setChanged();
    }

    /**
     * Auto-connect on placement: connects to the clicked face, then scans all 6 sides
     * and auto-connects to any adjacent same-type wire that already has its connection
     * facing toward this new wire (symmetry healing).
     */
    public void autoConnectOnPlace(@Nullable Direction clickedFace) {
        if (level == null || level.isClientSide) return;

        // First, connect to the clicked face
        if (clickedFace != null) {
            Direction toNeighbor = clickedFace.getOpposite();
            BlockPos neighborPos = worldPosition.relative(toNeighbor);
            BlockEntity be = level.getBlockEntity(neighborPos);
            if (be instanceof ElectricWireBlockEntity neighborWire) {
                BlockState state = getBlockState();
                BlockState newState = state.setValue(ElectricWireBlock.propFor(toNeighbor), true);
                level.setBlockAndUpdate(worldPosition, newState);

                BlockState neighborState = neighborWire.getBlockState();
                if (!neighborState.getValue(ElectricWireBlock.propFor(clickedFace))) {
                    level.setBlockAndUpdate(neighborPos,
                            neighborState.setValue(ElectricWireBlock.propFor(clickedFace), true));
                }
            }
        }

        // Then scan all 6 sides for adjacent wires already connected toward us
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockEntity be = level.getBlockEntity(neighborPos);
            if (!(be instanceof ElectricWireBlockEntity neighborWire)) continue;

            BlockState neighborState = neighborWire.getBlockState();
            if (neighborState.getValue(ElectricWireBlock.propFor(dir.getOpposite()))) {
                BlockState state = getBlockState();
                if (!state.getValue(ElectricWireBlock.propFor(dir))) {
                    level.setBlockAndUpdate(worldPosition, state.setValue(ElectricWireBlock.propFor(dir), true));
                }
            }
        }

        onConnectionChanged();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
    }

    // --- IEnergyBlock ---

    @Override
    public boolean isEnergyType(GregTechTags.Tag energyType, @Nullable Direction side, boolean emitting) {
        return energyType == GregTechTags.Energy.EU;
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return GregTechTags.Energy.EU.asList();
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return energyType == GregTechTags.Energy.EU && !isRemoved() && side != null && getBlockState().getValue(ElectricWireBlock.propFor(side));
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return isEnergyAcceptingFrom(energyType,side,theoretical);
    }

    @Override
    public long doEnergyInjection(GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doInject) {
        if (level == null || level.isClientSide || size == 0 || size == Long.MIN_VALUE || amount <= 0
                || !isEnergyAcceptingFrom(energyType,side,false)) return 0;
        // GT6's simulation reports local connection acceptance, not a reservation of downstream space.
        if (!doInject) return amount;
        return transferElectricity(side, size, amount, new HashSet<>());
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag energyType, @Nullable Direction side, long size) { return 0; }

    @Override
    public long doEnergyExtraction(GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doExtract) {
        return 0;
    }

    @Override
    public long getEnergyOffered(GregTechTags.Tag energyType, @Nullable Direction side, long size) { return 0; }

    @Override
    public long getEnergySizeInputMin(GregTechTags.Tag energyType, @Nullable Direction side) { return 0; }

    @Override
    public long getEnergySizeOutputMin(GregTechTags.Tag energyType, @Nullable Direction side) { return 0; }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) { return voltage(); }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) { return voltage(); }

    @Override
    public long getEnergySizeInputMax(GregTechTags.Tag energyType, @Nullable Direction side) { return voltage(); }

    @Override
    public long getEnergySizeOutputMax(GregTechTags.Tag energyType, @Nullable Direction side) { return voltage(); }

    public boolean isConducting() {
        return level != null && !isRemoved() && lastWattage > 0;
    }

    /** GT6 mWattageLast: actual post-loss wattage accumulated during the previous tick. */
    public long lastWattage() { return lastWattage; }
    public long transferredAmperes() { return transferredAmperes; }
    public int burnCounter() { return burnCounter; }

    public void serverTick() {
        if (level == null || level.isClientSide || isRemoved()) return;
        timer++;
        if (burnCounter >= 16) {
            // GT6 setToFire replaces the wire, without dropping a recoverable cable.
            level.setBlockAndUpdate(worldPosition, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
            return;
        }
        if (timer % 512 == 2 && burnCounter > 0) burnCounter--;
        lastWattage = transferredWattage;
        transferredWattage = 0;
        transferredAmperes = 0;
    }

    /** False means an overload consumed the remaining offered packets, as in GT6. */
    public boolean addToEnergyTransferred(long packetVoltage, long packetAmperage) {
        if (packetAmperage <= 0 || packetVoltage == Long.MIN_VALUE || packetVoltage == 0) return true;
        long magnitude = Math.abs(packetVoltage);
        transferredAmperes = saturatedAdd(transferredAmperes, packetAmperage);
        long watts = magnitude > Long.MAX_VALUE / packetAmperage ? Long.MAX_VALUE : magnitude * packetAmperage;
        transferredWattage = saturatedAdd(transferredWattage, watts);
        if (magnitude > voltage() || transferredAmperes > amperage()) {
            if (burnCounter < 16) burnCounter++;
            return false;
        }
        return true;
    }

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    /** Recursive energy transfer along wire network. */
    public long transferElectricity(Direction fromSide, long packetVoltage, long packetAmperage, Set<BlockPos> visited) {
        if (level == null || level.isClientSide || isRemoved() || packetVoltage == Long.MIN_VALUE || packetAmperage <= 0 || Math.abs(packetVoltage) <= lossPerBlock()) return 0;
        if (!visited.add(getBlockPos())) return 0;

        if (packetVoltage > 0) packetVoltage -= lossPerBlock();
        else packetVoltage += lossPerBlock();

        long usedAmperes = 0;
        for (Direction side : Direction.values()) {
            if (side == fromSide) continue;
            if (usedAmperes >= packetAmperage) break;
            if (!getBlockState().getValue(ElectricWireBlock.propFor(side))) continue;

            BlockPos nbPos = worldPosition.relative(side);
            if (visited.contains(nbPos)) continue;

            long remaining = packetAmperage - usedAmperes;
            if (!level.hasChunkAt(nbPos)) continue;
            BlockEntity neighbor = level.getBlockEntity(nbPos);
            if (neighbor instanceof ElectricWireBlockEntity wire) {
                // The called wire marks itself. Pre-marking it here used to abort every second segment.
                if (wire.isEnergyAcceptingFrom(GregTechTags.Energy.EU,side.getOpposite(),false))
                    usedAmperes += wire.transferElectricity(side.getOpposite(), packetVoltage, remaining, visited);
            } else {
                visited.add(nbPos);
                usedAmperes += EnergyTransfer.insertEnergyInto(
                        GregTechTags.Energy.EU, side.getOpposite(), packetVoltage, remaining, this, neighbor);
            }
        }
        return usedAmperes > 0 ? (addToEnergyTransferred(packetVoltage, usedAmperes) ? usedAmperes : packetAmperage) : 0;
    }

    // --- IEnergyConductor ---

    @Override public boolean isEnergyConducting(GregTechTags.Tag energyType) { return energyType == GregTechTags.Energy.EU; }
    @Override public long getEnergyMaxSize(GregTechTags.Tag energyType) { return energyType == GregTechTags.Energy.EU ? voltage() : 0; }
    @Override public long getEnergyMaxPackets(GregTechTags.Tag energyType) { return energyType == GregTechTags.Energy.EU ? amperage() : 0; }
    @Override public long getEnergyLossPerMeter(GregTechTags.Tag energyType) { return energyType == GregTechTags.Energy.EU ? lossPerBlock() : 0; }
    // ── Client model data (dynamic pipe model neighbor sizes) ───────────────

    @Override
    public net.minecraftforge.client.model.data.ModelData getModelData() {
        float[] halves = new float[6];
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            halves[d.ordinal()] = level == null
                    ? (float) com.gregtech.gregtech.api.machine.PipeGeometry.NONE
                    : (float) com.gregtech.gregtech.api.machine.PipeGeometry.wireHalfOf(
                            level.getBlockState(worldPosition.relative(d)));
        }
        return net.minecraftforge.client.model.data.ModelData.builder()
                .with(com.gregtech.gregtech.api.machine.PipeModelData.NEIGHBOR_HALVES, halves)
                .build();
    }

}
