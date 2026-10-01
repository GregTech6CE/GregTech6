package com.gregtech.gregtech.blockentity.energy;

import com.gregtech.gregtech.api.energy.AxleSpec;
import com.gregtech.gregtech.api.energy.EnergyNet;
import com.gregtech.gregtech.api.energy.EnergyTransfer;
import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.api.energy.IEnergyConductor;
import com.gregtech.gregtech.block.energy.AxleBlock;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTAxles;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** GT6 rotational axle block entity — RU conductor, pipe-style connection grid. */
public class AxleBlockEntity extends BlockEntity implements IEnergyConductor, IEnergyBlock {
    private boolean initialized;
    public static final net.neoforged.neoforge.client.model.data.ModelProperty<Integer> ROTATION = new net.neoforged.neoforge.client.model.data.ModelProperty<>();
    private long timer, transferredSpeed, transferredPower, transferredEnergy, transferredLast;
    private int rotationDir, previousRotationDir;
    public int rotationDirection() { return rotationDir; }
    public long transferredLast() { return transferredLast; }
    public long transferredPower() { return transferredPower; }

    public AxleBlockEntity(BlockPos pos, BlockState state) {
        super(GTAxles.AXLE.get(), pos, state);
    }

    private AxleSpec axleSpec() {
        BlockState st = getBlockState();
        if (st.getBlock() instanceof AxleBlock ab) return ab.spec();
        return new AxleSpec("wood", null, 1, 16, 1, 1);
    }

    public long maxSpeed()     { return axleSpec().maxSpeed(); }
    public long maxPower()     { return axleSpec().maxPower(); }
    public long lossPerBlock() { return 0; }

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
     * Releases this axle's {@link EnergyNet} entry when the block entity goes away.
     *
     * <p>{@link #onRemoved} is otherwise reached only from {@code AxleBlock#onRemove}, which runs when
     * the block is replaced or broken - a chunk unload never goes through the block. Vanilla unloads a
     * chunk by calling {@code BlockEntity#onChunkUnloaded} and then {@code setRemoved} on every block
     * entity of the chunk and dropping them from the chunk map
     * ({@code LevelChunk#clearAllBlockEntities}). Without these two overrides the axle position stayed
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
        EnergyNet.onConnectionChange(level, worldPosition, state, AxleBlock.CONNECTIONS);
        setChanged();
    }

    private static boolean canConnectTo(BlockEntity entity,Direction side) {
        return entity instanceof IEnergyBlock energy && (energy.isEnergyAcceptingFrom(GregTechTags.Energy.RU,side,true)
                || energy.isEnergyEmittingTo(GregTechTags.Energy.RU,side,true));
    }

    public void autoConnectOnPlace(@Nullable Direction clickedFace) {
        if (level == null || level.isClientSide) return;

        // Determine the connection axis from the clicked face. Axles only connect
        // in straight lines (max 2 connections on the same axis).
        if (clickedFace != null) {
            Direction toNeighbor = clickedFace.getOpposite();
            Direction.Axis axis = toNeighbor.getAxis();
            BlockPos neighborPos = worldPosition.relative(toNeighbor);
            BlockEntity be = level.getBlockEntity(neighborPos);

            BlockState state = getBlockState();
            if (be instanceof AxleBlockEntity neighborAxle) {
                // Connect to the clicked neighbor, but only if the neighbor's
                // resulting state is a valid straight line.
                BlockState neighborCandidate =
                        neighborAxle.getBlockState().setValue(AxleBlock.propFor(clickedFace), true);
                if (!AxleBlock.isValidConnectionState(neighborCandidate)) {
                    onConnectionChanged();
                    return;
                }
                state = state.setValue(AxleBlock.propFor(toNeighbor), true);
                level.setBlockAndUpdate(worldPosition, state);
                level.setBlockAndUpdate(neighborPos, neighborCandidate);
            } else if (canConnectTo(be,clickedFace)) {
                state = state.setValue(AxleBlock.propFor(toNeighbor), true);
                level.setBlockAndUpdate(worldPosition, state);
            }

            // Also check the opposite direction on the same axis for through-connection
            Direction opposite = toNeighbor.getOpposite();
            if (opposite != toNeighbor) {
                BlockPos oppositePos = worldPosition.relative(opposite);
                BlockEntity oppositeBe = level.getBlockEntity(oppositePos);
                if (oppositeBe instanceof AxleBlockEntity oppositeAxle) {
                    BlockState currentState = getBlockState();
                    BlockState ourCandidate = currentState.setValue(AxleBlock.propFor(opposite), true);
                    BlockState oppositeCandidate =
                            oppositeAxle.getBlockState().setValue(AxleBlock.propFor(opposite.getOpposite()), true);
                    if (AxleBlock.isValidConnectionState(ourCandidate)
                            && AxleBlock.isValidConnectionState(oppositeCandidate)) {
                        level.setBlockAndUpdate(worldPosition, ourCandidate);
                        level.setBlockAndUpdate(oppositePos, oppositeCandidate);
                    }
                } else if (canConnectTo(oppositeBe,opposite.getOpposite())) {
                    BlockState currentState = getBlockState();
                    if (AxleBlock.isValidConnectionState(currentState.setValue(AxleBlock.propFor(opposite), true))) {
                        level.setBlockAndUpdate(worldPosition,
                                currentState.setValue(AxleBlock.propFor(opposite), true));
                    }
                }
            }
        } else {
            // No clicked face: scan all directions but only auto-connect along one axis
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = worldPosition.relative(dir);
                BlockEntity be = level.getBlockEntity(neighborPos);
                if (!(be instanceof AxleBlockEntity neighborAxle)) continue;

                BlockState neighborState = neighborAxle.getBlockState();
                if (neighborState.getValue(AxleBlock.propFor(dir.getOpposite()))) {
                    BlockState state = getBlockState();
                    BlockState candidate = state.setValue(AxleBlock.propFor(dir), true);
                    if (AxleBlock.isValidConnectionState(candidate)) {
                        level.setBlockAndUpdate(worldPosition, candidate);
                        break; // only one auto-connection without a clicked face
                    }
                }
            }
        }

        onConnectionChanged();
    }

    @Override
    public void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        rotationDir = Math.max(0, Math.min(2, tag.getByte("Rotation")));
    }

    @Override
    public void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        tag.putByte("Rotation", (byte)rotationDir);
    }

    // --- IEnergyBlock ---

    @Override
    public boolean isEnergyType(GregTechTags.Tag energyType, @Nullable Direction side, boolean emitting) {
        return energyType == GregTechTags.Energy.RU;
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return GregTechTags.Energy.RU.asList();
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return energyType == GregTechTags.Energy.RU && !isRemoved() && side != null
                && AxleBlock.isValidConnectionState(getBlockState()) && getBlockState().getValue(AxleBlock.propFor(side));
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return isEnergyAcceptingFrom(energyType,side,theoretical);
    }

    @Override
    public long doEnergyInjection(GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doInject) {
        if (level == null || level.isClientSide || size == 0 || size == Long.MIN_VALUE || amount <= 0
                || !isEnergyAcceptingFrom(energyType,side,false)) return 0;
        return doInject ? transferRotation(side,size,amount,new HashSet<>()) : amount;
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
    public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) { return maxSpeed(); }
    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) { return maxSpeed(); }
    @Override
    public long getEnergySizeInputMax(GregTechTags.Tag energyType, @Nullable Direction side) { return maxSpeed(); }
    @Override
    public long getEnergySizeOutputMax(GregTechTags.Tag energyType, @Nullable Direction side) { return maxSpeed(); }

    public boolean isConducting() { return !isRemoved() && rotationDir != 0; }

    public void serverTick() {
        if (level == null || level.isClientSide || isRemoved()) return;
        timer++;
        if (timer > 5 && transferredSpeed == 0) rotationDir = 0;
        transferredLast = transferredEnergy;
        transferredSpeed = transferredPower = transferredEnergy = 0;
        if (rotationDir != previousRotationDir) {
            setChanged();
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
        }
        previousRotationDir = rotationDir;
    }

    /** A stopped axle consumes the first input interval to spin up; only the opposite end emits. */
    public long transferRotation(Direction fromSide, long speed, long power, Set<BlockPos> visited) {
        if (level == null || level.isClientSide || isRemoved() || timer < 1 || speed == 0
                || speed == Long.MIN_VALUE || power <= 0
                || !isEnergyAcceptingFrom(GregTechTags.Energy.RU,fromSide,false) || !visited.add(worldPosition)) return 0;
        int next = speed < 0 ? 1+(fromSide.ordinal()&1) : 2-(fromSide.ordinal()&1);
        if (rotationDir != next) { rotationDir=next; setChanged(); }
        if (previousRotationDir == 0) return recordTransfer(speed,power,power);
        Direction output=fromSide.getOpposite();
        long used=0;
        if (isEnergyEmittingTo(GregTechTags.Energy.RU,output,false)) {
            BlockPos target=worldPosition.relative(output);
            if (level.hasChunkAt(target) && !visited.contains(target)) {
                BlockEntity neighbor=level.getBlockEntity(target);
                if (neighbor instanceof AxleBlockEntity axle) {
                    if (axle.isEnergyAcceptingFrom(GregTechTags.Energy.RU,fromSide,false))
                        used=axle.transferRotation(fromSide,speed,power,visited);
                } else {
                    visited.add(target);
                    used=EnergyTransfer.insertEnergyInto(GregTechTags.Energy.RU,fromSide,speed,power,this,neighbor);
                }
            }
        }
        return recordTransfer(speed,power,used);
    }

    private long recordTransfer(long speed,long offered,long used) {
        transferredSpeed = saturatedAdd(transferredSpeed,speed);
        transferredPower = saturatedAdd(transferredPower,used);
        long magnitude=Math.abs(speed);
        long energy=used>0 && magnitude>Long.MAX_VALUE/used ? Long.MAX_VALUE : magnitude*used;
        transferredEnergy=saturatedAdd(transferredEnergy,energy);
        if (magnitude>maxSpeed() || transferredPower>maxPower()) {
            level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.ITEM_BREAK,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
            // Original popOff drops the axle once, then removes it.
            level.destroyBlock(worldPosition,true);
            return offered;
        }
        return used;
    }
    private static long saturatedAdd(long left,long right) {
        try {return Math.addExact(left,right);} catch (ArithmeticException overflow) {return right<0?Long.MIN_VALUE:Long.MAX_VALUE;}
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        CompoundTag tag=new CompoundTag(); tag.putByte("Rotation",(byte)rotationDir); return tag;
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public void handleUpdateTag(CompoundTag tag,HolderLookup.Provider lookup) {
        loadAdditional(tag,lookup); requestModelDataUpdate();
        if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    @Override public void onDataPacket(net.minecraft.network.Connection connection,net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet,HolderLookup.Provider lookup) {
        if(packet.getTag()!=null)handleUpdateTag(packet.getTag(),lookup);
    }

    // --- IEnergyConductor ---

    @Override public boolean isEnergyConducting(GregTechTags.Tag energyType) { return energyType == GregTechTags.Energy.RU; }
    @Override public long getEnergyMaxSize(GregTechTags.Tag energyType) { return energyType == GregTechTags.Energy.RU ? maxSpeed() : 0; }
    @Override public long getEnergyMaxPackets(GregTechTags.Tag energyType) { return energyType == GregTechTags.Energy.RU ? maxPower() : 0; }
    @Override public long getEnergyLossPerMeter(GregTechTags.Tag energyType) { return energyType == GregTechTags.Energy.RU ? lossPerBlock() : 0; }

    // --- Client model data ---

    @Override
    public ModelData getModelData() {
        float[] halves = new float[6];
        for (Direction d : Direction.values()) {
            halves[d.ordinal()] = level == null
                    ? (float) com.gregtech.gregtech.api.machine.PipeGeometry.NONE
                    : (float) com.gregtech.gregtech.api.machine.PipeGeometry.axleHalfOf(
                            level.getBlockState(worldPosition.relative(d)));
        }
        return ModelData.builder()
                .with(com.gregtech.gregtech.api.machine.PipeModelData.NEIGHBOR_HALVES, halves)
                .with(ROTATION,rotationDir)
                .build();
    }
}
