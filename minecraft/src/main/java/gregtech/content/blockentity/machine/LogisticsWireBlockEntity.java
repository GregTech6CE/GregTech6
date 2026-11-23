package gregtech.content.blockentity.machine;

import gregtech.content.blockentity.GTBlockEntity;
import gregtech.content.logistics.LogisticsCoverHost;
import gregtech.content.logistics.LogisticsCovers;
import gregtech.content.registry.GTBlockEntities;
import gregtech.engine.transport.PipeConnections;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;

/** Connections live in block state; installed covers are persisted and synchronized separately. */
public final class LogisticsWireBlockEntity extends GTBlockEntity implements LogisticsCoverHost {
    private final LogisticsCovers covers = new LogisticsCovers(this, this);

    public LogisticsWireBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.LOGISTICS_WIRE.get(), pos, state);
    }

    @Override
    public boolean canLogistics(Direction side) {
        return !isRemoved() && (side == null || getBlockState().getValue(PipeConnections.propFor(side)));
    }

    @Override
    public LogisticsCovers logisticsCovers() {
        return covers;
    }

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        covers.save(tag, lookup);
    }

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        covers.load(tag, lookup);
    }

    @Override
    protected void writeUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        writeWithoutMetadata(tag, lookup);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void handleDataPacket(
            Connection connection, ClientboundBlockEntityDataPacket packet, @Nullable HolderLookup.Provider lookup) {
        if (packet.getTag() != null) readUpdateTag(packet.getTag(), lookup);
    }
}
