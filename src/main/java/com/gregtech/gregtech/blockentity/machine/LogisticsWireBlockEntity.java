package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.content.logistics.LogisticsHost;
import com.gregtech.gregtech.content.logistics.LogisticsCoverHost;
import com.gregtech.gregtech.content.logistics.LogisticsCovers;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** State is held by the six block-state connections; no inventory or server ticker is needed. */
public final class LogisticsWireBlockEntity extends BlockEntity implements LogisticsCoverHost {
    private final LogisticsCovers covers = new LogisticsCovers(this, this);
    public LogisticsWireBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.LOGISTICS_WIRE.get(), pos, state);
    }

    @Override public boolean canLogistics(Direction side) {
        return !isRemoved() && (side == null || getBlockState().getValue(ElectricWireBlock.propFor(side)));
    }

    @Override public LogisticsCovers logisticsCovers() { return covers; }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); covers.save(tag); }
    @Override public void load(CompoundTag tag) { super.load(tag); covers.load(tag); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public void handleUpdateTag(CompoundTag tag) { load(tag); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) handleUpdateTag(packet.getTag());
    }
}
