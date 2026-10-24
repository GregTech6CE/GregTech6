package gregtech.content.blockentity.machine;

//? if >=1.21.1 {
/*import gregtech.internal.transport.PipeConnections;
*///?} else {
import gregtech.content.block.energy.ElectricWireBlock;
//?}
import gregtech.content.logistics.LogisticsHost;
import gregtech.content.logistics.LogisticsCoverHost;
import gregtech.content.logistics.LogisticsCovers;
//? if >=1.21.1 {
/*import gregtech.platform.neoforge.logistics.LogisticsRegistries;
*///?} else {
import gregtech.content.registry.GTBlockEntities;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Connections live in block state; installed covers are persisted and synchronized separately. */
public final class LogisticsWireBlockEntity extends BlockEntity implements LogisticsCoverHost {
    private final LogisticsCovers covers = new LogisticsCovers(this, this);

    public LogisticsWireBlockEntity(BlockPos pos, BlockState state) {
//? if >=1.21.1 {
        /*super(LogisticsRegistries.LOGISTICS_WIRE.get(), pos, state);
*///?} else {
        super(GTBlockEntities.LOGISTICS_WIRE.get(), pos, state);
//?}
    }

    @Override
    public boolean canLogistics(Direction side) {
//? if >=1.21.1 {
        /*return !isRemoved() && (side == null || getBlockState().getValue(PipeConnections.propFor(side)));
*///?} else {
        return !isRemoved() && (side == null || getBlockState().getValue(ElectricWireBlock.propFor(side)));
//?}
    }

    @Override
    public LogisticsCovers logisticsCovers() {
        return covers;
    }
//? if >=1.21.1 {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        covers.save(tag, lookup);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        covers.load(tag, lookup);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
        return saveWithoutMetadata(lookup);
    }
*///?} else {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        covers.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        covers.load(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }
//?}
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
//? if >=1.21.1 {
//?} else {
    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) handleUpdateTag(packet.getTag());
    }
//?}
}
