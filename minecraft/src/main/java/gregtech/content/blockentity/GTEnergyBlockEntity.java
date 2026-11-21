package gregtech.content.blockentity;

import gregtech.content.data.GregTechTags;
import gregtech.engine.energy.EnergyBlockDefaults;
import gregtech.engine.energy.IEnergyBlock;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Base block entity with GT6 default energy validation (GT6 {@code TileEntityBase01Root}). */
public abstract class GTEnergyBlockEntity extends GTBlockEntity implements IEnergyBlock {

    private int changeBatchDepth;
    private boolean dataChanged, inventoryChanged;

    /** Batch one synchronous machine operation; external inventory writes still notify immediately. */
    protected final void beginMachineChanges() {
        changeBatchDepth++;
    }

    protected final void endMachineChanges() {
        if (--changeBatchDepth != 0) return;
        boolean notify = inventoryChanged, save = dataChanged;
        inventoryChanged = dataChanged = false;
        if (isRemoved()) return;
        if (notify) super.setChanged();
        else if (save && level != null) level.blockEntityChanged(worldPosition);
    }

    /** Energy/progress changes need persistence, not an inventory/comparator neighbour scan. */
    protected final void markMachineDataChanged() {
        if (changeBatchDepth > 0) dataChanged = true;
        else if (level != null) level.blockEntityChanged(worldPosition);
    }

    @Override
    public void setChanged() {
        if (changeBatchDepth > 0) inventoryChanged = dataChanged = true;
        else super.setChanged();
    }

    protected GTEnergyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return EnergyBlockDefaults.isEnergyEmittingTo(this, energyType, side, theoretical);
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return EnergyBlockDefaults.isEnergyAcceptingFrom(this, energyType, side, theoretical);
    }

    @Override
    public synchronized long doEnergyInjection(
            GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doInject) {
        return EnergyBlockDefaults.doEnergyInjection(this, energyType, side, size, amount, doInject);
    }

    @Override
    public synchronized long doEnergyExtraction(
            GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doExtract) {
        return EnergyBlockDefaults.doEnergyExtraction(this, energyType, side, size, amount, doExtract);
    }

    @Override
    public long getEnergySizeOutputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        return EnergyBlockDefaults.getEnergySizeOutputMin(this, energyType, side);
    }

    @Override
    public long getEnergySizeOutputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        return EnergyBlockDefaults.getEnergySizeOutputMax(this, energyType, side);
    }

    @Override
    public long getEnergySizeInputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        return EnergyBlockDefaults.getEnergySizeInputMin(this, energyType, side);
    }

    @Override
    public long getEnergySizeInputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        return EnergyBlockDefaults.getEnergySizeInputMax(this, energyType, side);
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return EnergyBlockDefaults.emptyTypes();
    }

    /** Capacitor discovery follows actual storage implementations, not input/output conversion. */
    @Override
    public Collection<GregTechTags.Tag> getEnergyCapacitorTypes(@Nullable Direction side) {
        return getEnergyTypes(side).stream()
                .filter(type -> getEnergyCapacity(type, side) > 0)
                .toList();
    }

    @Override
    public boolean isEnergyCapacitorType(GregTechTags.Tag type, @Nullable Direction side) {
        return getEnergyCapacitorTypes(side).contains(type);
    }

    // ── Client sync ──────────────────────────────────────────────────────────

    protected void syncToClient() {
        if (level == null || level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;
        gregtech.engine.multiblock.tracking.StructureScanProbe.pause();
        try {
            setChanged();
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            ClientboundBlockEntityDataPacket packet = null;
            double x = worldPosition.getX() + 0.5;
            double y = worldPosition.getY() + 0.5;
            double z = worldPosition.getZ() + 0.5;
            double distSq = 64.0 * 64.0;
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(x, y, z) <= distSq) {
                    // Keep the immediate delivery, but serialize only if it has a recipient.
                    if (packet == null) packet = ClientboundBlockEntityDataPacket.create(this);
                    player.connection.send(packet);
                }
            }
        } finally {
            gregtech.engine.multiblock.tracking.StructureScanProbe.resume();
        }
    }

    @Override
    protected void writeUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        saveEntityAdditional(tag, lookup);
        gregtech.engine.multiblock.tracking.ClientFormedSync.write(this, tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void handleDataPacket(
            Connection net, ClientboundBlockEntityDataPacket pkt, @Nullable HolderLookup.Provider lookup) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) readUpdateTag(tag, lookup);
    }

    @Override
    protected void readUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.readUpdateTag(tag, lookup);
        gregtech.engine.multiblock.tracking.ClientFormedSync.read(this, tag);
    }
}
