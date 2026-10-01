package com.gregtech.gregtech.blockentity.inventory;

import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/** GT6 nine-slot bottle crate: whole stacks, synchronized contents and packed block drops. */
public class BottleCrateBlockEntity extends BlockEntity {
    /** GT6's {@code mDisplay[9]} - nine bottles, in three rows of three. */
    public static final int SLOTS = 9;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return com.gregtech.gregtech.block.inventory.BottleCrateBlock.isBottle(stack);
        }
    };

    private CompoundTag removedInventory;


    public BottleCrateBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.BOTTLE_CRATE.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    /** The first slot without a bottle, or {@code -1} when the crate is full. */
    public int firstFree() {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (items.getStackInSlot(slot).isEmpty()) return slot;
        }
        return -1;
    }

    /** The highest slot with a bottle, or {@code -1} when the crate is empty (the last one goes out first). */
    public int lastFilled() {
        for (int slot = SLOTS - 1; slot >= 0; slot--) {
            if (!items.getStackInSlot(slot).isEmpty()) return slot;
        }
        return -1;
    }

    /** One bottle into the first free slot; the leftover is empty when it fit. */
    public ItemStack insert(ItemStack held) {
        int slot = firstFree();
        if (slot < 0 || !com.gregtech.gregtech.block.inventory.BottleCrateBlock.isBottle(held)) return held;
        return items.insertItem(slot, held, false);
    }

    /** The last bottle out, or an empty stack when the crate is empty. */
    public ItemStack extractLast() {
        int slot = lastFilled();
        return slot < 0 ? ItemStack.EMPTY : items.extractItem(slot, 1, false);
    }

    /** Every bottle of the crate, in slot order - what the crate drops when it is broken. */
    public java.util.List<ItemStack> contents() {
        java.util.List<ItemStack> out = new java.util.ArrayList<>();
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) out.add(stack.copy());
        }
        return out;
    }

    @Override
    protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        tag.put("gt_bottles", items.serializeNBT(lookup));
    }

    @Override
    protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        removedInventory = null;
        CompoundTag inventory = tag.getCompound("gt_bottles").copy();
        inventory.putInt("Size", SLOTS);
        items.deserializeNBT(lookup,inventory);
    }

    /** Preserve a snapshot for vanilla playerDestroy, which requests loot after onRemove. */
    public void detachInventory() {
        if (removedInventory != null) return;
        removedInventory = saveWithoutMetadata(level.registryAccess());
        for (int slot = 0; slot < SLOTS; slot++) items.setStackInSlot(slot, ItemStack.EMPTY);
    }

    public ItemStack packedStack() {
        ItemStack result = new ItemStack(getBlockState().getBlock());
        CompoundTag data = removedInventory == null ? saveWithoutMetadata(level.registryAccess()) : removedInventory.copy();
        if (!data.getCompound("gt_bottles").getList("Items", 10).isEmpty()) {
            data.putString("id","gregtech:bottle_crate");
            result.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(data));
        }
        return result;
    }

    @Override public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) { return saveWithoutMetadata(lookup); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

}
