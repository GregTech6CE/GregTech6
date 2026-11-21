package gregtech.content.blockentity.energy;

import gregtech.content.blockentity.GTBlockEntity;
import gregtech.content.registry.GTBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Preserve the exact rod stack (long fuel life, breeding and moderation data) across placement. */
public final class PlacedReactorRodBlockEntity extends GTBlockEntity {
    private ItemStack stored = ItemStack.EMPTY;

    public PlacedReactorRodBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.PLACED_REACTOR_ROD.get(), pos, state);
    }

    public ItemStack rod() {
        return stored.isEmpty() ? new ItemStack(getBlockState().getBlock()) : stored.copy();
    }

    public void setRod(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != getBlockState().getBlock().asItem()) return;
        stored = stack.copyWithCount(1);
        // BlockEntityTag is not needed inside our stack and must not grow recursively.
        gregtech.engine.stack.BlockEntityData.remove(stored);
        setChanged();
    }

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        tag.put("Rod", gregtech.engine.stack.NbtCompat.saveItem(rod(), lookup));
    }

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        stored = ItemStack.EMPTY;
        setRod(gregtech.engine.stack.NbtCompat.readItem(tag.getCompound("Rod"), lookup));
    }

    @Override
    protected void writeUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        writeWithoutMetadata(tag, lookup);
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
