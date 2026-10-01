package com.gregtech.gregtech.blockentity;

import com.gregtech.gregtech.api.energy.EnergyBlockDefaults;
import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Collection;

/** Base block entity with GT6 default energy validation (GT6 {@code TileEntityBase01Root}). */
public abstract class GTEnergyBlockEntity extends BlockEntity implements IEnergyBlock {

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
    public synchronized long doEnergyInjection(GregTechTags.Tag energyType, @Nullable Direction side,
                                               long size, long amount, boolean doInject) {
        return EnergyBlockDefaults.doEnergyInjection(this, energyType, side, size, amount, doInject);
    }

    @Override
    public synchronized long doEnergyExtraction(GregTechTags.Tag energyType, @Nullable Direction side,
                                                long size, long amount, boolean doExtract) {
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
    @Override public Collection<GregTechTags.Tag> getEnergyCapacitorTypes(@Nullable Direction side){
        return getEnergyTypes(side).stream().filter(type->getEnergyCapacity(type,side)>0).toList();
    }
    @Override public boolean isEnergyCapacitorType(GregTechTags.Tag type,@Nullable Direction side){return getEnergyCapacitorTypes(side).contains(type);}

    // ── Client sync ──────────────────────────────────────────────────────────

    protected void syncToClient() {
        if (level == null || level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;
        setChanged();
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this);
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 0.5;
        double z = worldPosition.getZ() + 0.5;
        double distSq = 64.0 * 64.0;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(x, y, z) <= distSq) {
                player.connection.send(packet);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) handleUpdateTag(tag);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        load(tag);
    }
}
