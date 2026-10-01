package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.transport.PipeConnections;
import com.gregtech.gregtech.content.logistics.LogisticsHost;
import com.gregtech.gregtech.content.logistics.LogisticsCoverHost;
import com.gregtech.gregtech.content.logistics.LogisticsCovers;
import com.gregtech.gregtech.platform.neoforge.logistics.LogisticsRegistries;
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
        super(LogisticsRegistries.LOGISTICS_WIRE.get(), pos, state);
    }

    @Override public boolean canLogistics(Direction side) {
        return !isRemoved() && (side == null || getBlockState().getValue(PipeConnections.propFor(side)));
    }

    @Override public LogisticsCovers logisticsCovers() { return covers; }
    @Override protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup); covers.save(tag,lookup);
    }
    @Override protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup); covers.load(tag,lookup);
    }
    @Override public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) { return saveWithoutMetadata(lookup); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
