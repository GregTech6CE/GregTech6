package gregtech.content.transport.pipe;

import gregtech.content.blockentity.GTBlockEntity;
import gregtech.content.cover.CoverItems;
import gregtech.content.cover.CoverStore;
import gregtech.content.cover.PanelCoverRuntime;
import gregtech.content.cover.PipeCoverHost;
import gregtech.engine.machine.PipeGeometry;
import gregtech.engine.machine.PipeModelData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.model.data.ModelData;
*///?} else {
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.util.LazyOptional;
//?}

/** Shared world lifecycle, cover storage and external capability lifetime of native pipes. */
public abstract class PipeBlockEntity<H> extends GTBlockEntity implements PipeCoverHost {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final String MANUAL_DISCONNECTS = "gt.manual_disconnects";

    protected final CoverStore coverStore = new CoverStore(this);
    private int manualDisconnects;

    protected PipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract int connectionMask(BlockState state);

    protected abstract double neighborHalf(BlockState state, Direction side);

    protected abstract void joinedLevel();

    protected abstract void leftLevel();

    protected abstract void connectionsChanged();

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null) return;
        if (level.isClientSide) {
            requestModelDataUpdate();
            PipeModelData.refreshCrossChunkNeighbors(level, worldPosition);
        } else {
            joinedLevel();
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) leftLevel();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && !level.isClientSide) leftLevel();
        super.onChunkUnloaded();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setBlockState(BlockState state) {
        int oldMask = connectionMask(getBlockState());
        super.setBlockState(state);
        if (level != null && !level.isClientSide && connectionMask(state) != oldMask) connectionsChanged();
    }

    protected final boolean isFaceManuallyDisconnected(Direction side) {
        return (manualDisconnects & (1 << side.ordinal())) != 0;
    }

    public final void setFaceManuallyDisconnected(Direction side, boolean disconnected) {
        int bit = 1 << side.ordinal();
        int next = disconnected ? manualDisconnects | bit : manualDisconnects & ~bit;
        if (next == manualDisconnects) return;
        manualDisconnects = next;
        setChanged();
        manualDisconnectChanged();
    }

    protected void manualDisconnectChanged() {}

    protected final void loadManualDisconnects(CompoundTag tag) {
        manualDisconnects = tag.getByte(MANUAL_DISCONNECTS) & 0x3F;
    }

    protected final void saveManualDisconnects(CompoundTag tag) {
        tag.putByte(MANUAL_DISCONNECTS, (byte) manualDisconnects);
    }

    @Override
    public ItemStack getCover(Direction side) {
        return coverStore.get(side);
    }

    @Override
    public PanelCoverRuntime panels() {
        return coverStore.panels();
    }

    public boolean hasCovers() {
        return coverStore.hasCovers();
    }

    @Override
    public boolean attachCover(Direction side, ItemStack stack) {
        return coverStore.attach(side, stack);
    }

    @Override
    public ItemStack removeCover(Direction side) {
        return coverStore.remove(side);
    }

    public void dropCovers() {
        if (!coverStore.hasCovers()) return;
        coverStore.drop();
        setChanged();
    }

    protected final @Nullable String coverIdOf(Direction side) {
        return CoverItems.behavior(getCover(side));
    }

    @Override
    public ModelData getModelData() {
        float[] halves = new float[DIRECTIONS.length];
        for (Direction side : DIRECTIONS) {
            halves[side.ordinal()] = level == null
                    ? (float) PipeGeometry.NONE
                    : (float) neighborHalf(level.getBlockState(worldPosition.relative(side)), side);
        }
        return ModelData.builder().with(PipeModelData.NEIGHBOR_HALVES, halves).build();
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

    protected abstract H unsidedPipeHandler();

    protected abstract H createFaceHandler(Direction side);

//? if >=1.21.1 {
    /*private @Nullable SidedCapabilityCache<H> capabilities;

    public final H capabilityHandler(@Nullable Direction side) {
        if (capabilities == null) {
            capabilities = new SidedCapabilityCache<>(this::unsidedPipeHandler, this::createFaceHandler);
        }
        return capabilities.get(side);
    }

    protected final H externalPipeHandler(Direction side) {
        return capabilityHandler(side);
    }

    protected final void invalidatePipeFace(Direction side) {
        if (capabilities != null) capabilities.remove(side);
        notifyPipeCapabilitiesChanged();
    }

    protected final void notifyPipeCapabilitiesChanged() {
        if (level != null) level.invalidateCapabilities(worldPosition);
    }
*///?} else {
    private @Nullable SidedCapabilityCache<LazyOptional<H>> capabilities;
    private boolean capabilitiesLive = true;

    protected final LazyOptional<H> pipeCapability(@Nullable Direction side) {
        if (!capabilitiesLive || isRemoved()) return LazyOptional.empty();
        if (capabilities == null) {
            capabilities = new SidedCapabilityCache<>(
                    () -> LazyOptional.of(this::unsidedPipeHandler),
                    face -> LazyOptional.of(() -> createFaceHandler(face)));
        }
        return capabilities.get(side);
    }

    protected final H externalPipeHandler(Direction side) {
        return pipeCapability(side).orElseThrow(() -> new IllegalStateException("Missing pipe face handler"));
    }

    protected final void invalidatePipeFace(Direction side) {
        if (capabilities == null) return;
        var old = capabilities.remove(side);
        if (old != null) old.invalidate();
    }

    protected final void notifyPipeCapabilitiesChanged() {}

    @Override
    public void invalidateCaps() {
        capabilitiesLive = false;
        super.invalidateCaps();
        if (capabilities != null) capabilities.clear(LazyOptional::invalidate);
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        capabilitiesLive = true;
    }
//?}
}
