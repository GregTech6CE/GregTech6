package gregtech.content.blockentity;

import gregtech.engine.stack.NbtCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Thin {@link BlockEntity} adapter that absorbs 1.20.1 / 1.21.1 serialization and update-sync
 * signature differences without holding gameplay fields or state.
 */
public abstract class GTBlockEntity extends BlockEntity {

    protected GTBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Serializes subclass state; {@code lookup} is {@code null} on 1.20.1. */
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {}

    /** Deserializes subclass state; {@code lookup} is {@code null} on 1.20.1. */
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {}

    /** Populates client chunk-update sync payload into {@code tag}; {@code lookup} is {@code null} on 1.20.1. */
    protected void writeUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {}

    /**
     * Applies client chunk-update sync payload.
     *
     * <p>By default delegates once to vanilla {@code handleUpdateTag}, which routes through
     * {@code load} (1.20.1) or {@code loadWithComponents}/{@code loadAdditional} (1.21.1) and
     * therefore invokes {@link #loadData} once without double-loading.</p>
     */
    protected void readUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
//? if >=1.21.1 {
        /*super.handleUpdateTag(tag, lookup);
*///?} else {
        super.handleUpdateTag(tag);
//?}
    }

    /** Handles an incoming {@link ClientboundBlockEntityDataPacket}; defaults to vanilla {@code onDataPacket}. */
    protected void handleDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt,
            @Nullable HolderLookup.Provider lookup) {
//? if >=1.21.1 {
        /*super.onDataPacket(net, pkt, lookup);
*///?} else {
        super.onDataPacket(net, pkt);
//?}
    }

    /** Copies {@code saveWithoutMetadata} output (including {@link #saveData}) into {@code tag}. */
    protected final void writeWithoutMetadata(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        tag.merge(NbtCompat.saveWithoutMetadata(this, lookup));
    }

    /** Invokes {@code saveAdditional} directly into {@code tag} without encoding data components. */
    protected final void saveEntityAdditional(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
//? if >=1.21.1 {
        /*saveAdditional(tag, lookup);
*///?} else {
        saveAdditional(tag);
//?}
    }

    /** Invokes {@code load}/{@code loadAdditional} directly without vanilla component decoding. */
    protected final void loadFromTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
//? if >=1.21.1 {
        /*loadAdditional(tag, lookup);
*///?} else {
        load(tag);
//?}
    }

//? if >=1.21.1 {
    /*@Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        saveData(tag, lookup);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        loadData(tag, lookup);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        CompoundTag tag = super.getUpdateTag(lookup);
        writeUpdateTag(tag, lookup);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookup) {
        readUpdateTag(tag, lookup);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookup) {
        handleDataPacket(net, pkt, lookup);
    }
*///?} else {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveData(tag, null);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        loadData(tag, null);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        writeUpdateTag(tag, null);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        readUpdateTag(tag, null);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        handleDataPacket(net, pkt, null);
    }
//?}
}
