package com.gregtech.gregtech.blockentity.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * GT6's wild bumblebee hive ({@code MultiTileEntityBumbleHive}, {@code gregtech/tileentity/misc}):
 * a nine-slot container that GT6 fills with one comb, a princess and a handful of drones when the
 * world generator places it ({@code WorldgenHives.placeHive:203}).
 *
 * <p>GT6 never exposes those slots: {@code getAccessibleSlotsFromSide2} returns an empty array and
 * both {@code canInsertItem2}/{@code canExtractItem2} are false, so hoppers cannot touch a hive —
 * the only way to get the bees out is to break it (and only a player's break drops them,
 * {@code onBlockHarvested}/{@code canDrop}).</p>
 */
public class BumbleHiveBlockEntity extends BlockEntity {

    /** GT6's {@code getDefaultInventory}: nine slots. */
    public static final int SLOTS = 9;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    public BumbleHiveBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.BUMBLE_HIVE.get(), pos, state);
    }

    /** The hive's contents, in GT6's slot order. */
    public ItemStackHandler inventory() { return inventory; }

    /** Everything inside, for the drop logic and the tooltips. */
    public java.util.List<ItemStack> contents() {
        java.util.List<ItemStack> out = new java.util.ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) out.add(stack.copy());
        }
        return out;
    }

    /** Empties the hive (GT6 clears the inventory once the contents dropped). */
    public void clearContents() {
        for (int i = 0; i < SLOTS; i++) inventory.setStackInSlot(i, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", inventory.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inventory")) inventory.deserializeNBT(tag.getCompound("inventory"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.put("inventory", inventory.serializeNBT());
        return tag;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable net.minecraft.core.Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            // GT6 exposes no slots on any side: a hive is not a container for automation.
            return LazyOptional.empty();
        }
        return super.getCapability(capability, side);
    }

    /** Kept for the tests: the raw handler without the capability gate. */
    public IItemHandler rawHandler() { return inventory; }
}
