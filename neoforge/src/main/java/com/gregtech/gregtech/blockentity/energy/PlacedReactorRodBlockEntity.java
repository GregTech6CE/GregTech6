package com.gregtech.gregtech.blockentity.energy;

import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Preserve the exact rod stack (long fuel life, breeding and moderation data) across placement. */
public final class PlacedReactorRodBlockEntity extends BlockEntity {
    private ItemStack stored = ItemStack.EMPTY;
    public PlacedReactorRodBlockEntity(BlockPos pos, BlockState state) { super(GTBlockEntities.PLACED_REACTOR_ROD.get(), pos, state); }
    public ItemStack rod() { return stored.isEmpty() ? new ItemStack(getBlockState().getBlock()) : stored.copy(); }
    public void setRod(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != getBlockState().getBlock().asItem()) return;
        stored = stack.copyWithCount(1);
        // BlockEntityTag is not needed inside our stack and must not grow recursively.
        stored.remove(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) { super.saveAdditional(tag,lookup); tag.put("Rod", rod().saveOptional(lookup)); }
    @Override protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup); stored = ItemStack.EMPTY; setRod(ItemStack.parseOptional(lookup,tag.getCompound("Rod")));
    }
    @Override public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) { return saveWithoutMetadata(lookup); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
