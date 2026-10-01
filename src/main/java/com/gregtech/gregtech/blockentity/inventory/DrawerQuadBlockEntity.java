package com.gregtech.gregtech.blockentity.inventory;

import com.gregtech.gregtech.api.inventory.BlockContents;
import com.gregtech.gregtech.block.inventory.DrawerQuadBlock;
import com.gregtech.gregtech.client.gui.HopperContainerMenu;
import com.gregtech.gregtech.registry.GTMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.IntStream;

/** Four 36-slot inventories. Side mapping follows GT6 MultiTileEntityDrawerQuad and CS.FACING_ROTATIONS. */
public class DrawerQuadBlockEntity extends BlockEntity implements BlockContents {
    public static final int COMPARTMENTS = com.gregtech.gregtech.content.storage.ContainerStorageRules.COMPARTMENTS, PAGE_SIZE = com.gregtech.gregtech.content.storage.ContainerStorageRules.PAGE_SIZE, SLOT_COUNT = com.gregtech.gregtech.content.storage.ContainerStorageRules.DRAWER_SLOTS;
    private boolean sidedAccess, loading, refilling, capsValid = true;
    // Only used to retain over-capacity contents from the earlier, incorrect bulk implementation.
    private final List<ItemStack> overflow = new ArrayList<>();
    private final Map<Integer, LazyOptional<IItemHandler>> capabilities = new HashMap<>();
    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override protected void onContentsChanged(int slot) {
            if (loading) return;
            refillLegacyOverflow();
            setChanged();
            if (level != null && !level.isClientSide) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    };
    public DrawerQuadBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.DRAWER_QUAD.get(), pos, state);
    }
    public ItemStackHandler items() { return inventory; }
    public boolean sidedAccess() { return sidedAccess; }
    public void toggleSidedAccess() { sidedAccess = !sidedAccess; resetCapabilities(); setChanged(); }
    private void resetCapabilities() { capabilities.values().forEach(LazyOptional::invalidate); capabilities.clear(); }
    @Override public void setBlockState(BlockState state) { super.setBlockState(state); resetCapabilities(); }
    @Override public void invalidateCaps() { super.invalidateCaps(); capsValid = false; resetCapabilities(); }
    @Override public void reviveCaps() { super.reviveCaps(); capsValid = true; }
    public int[] accessibleSlots(@Nullable Direction side) {
        return com.gregtech.gregtech.content.storage.ContainerStorageRules.drawerSlots(sidedAccess,getBlockState().getValue(DrawerQuadBlock.FACING).ordinal(),side==null?-1:side.ordinal());
    }
    private IItemHandlerModifiable view(int[] slots) {
        return new IItemHandlerModifiable() {
            private int actual(int slot) { if (slot < 0 || slot >= slots.length) throw new IndexOutOfBoundsException(slot); return slots[slot]; }
            public int getSlots() { return slots.length; }
            public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(actual(slot)); }
            public void setStackInSlot(int slot, ItemStack stack) { inventory.setStackInSlot(actual(slot), stack); }
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return inventory.insertItem(actual(slot), stack, simulate); }
            public ItemStack extractItem(int slot, int amount, boolean simulate) { return inventory.extractItem(actual(slot), amount, simulate); }
            public int getSlotLimit(int slot) { return inventory.getSlotLimit(actual(slot)); }
            public boolean isItemValid(int slot, ItemStack stack) { return inventory.isItemValid(actual(slot), stack); }
        };
    }
    public HopperContainerMenu createMenu(int page, int id, Inventory playerInventory) {
        if (page < 0 || page >= COMPARTMENTS) throw new IllegalArgumentException("Invalid drawer page " + page);
        return new HopperContainerMenu(GTMenuTypes.forSlotCount(PAGE_SIZE), id, playerInventory,
                view(IntStream.range(page * PAGE_SIZE, (page + 1) * PAGE_SIZE).toArray())) {
            @Override public boolean stillValid(Player player) {
                return !isRemoved() && level != null && level.getBlockEntity(worldPosition) == DrawerQuadBlockEntity.this
                        && player.distanceToSqr(worldPosition.getX()+.5, worldPosition.getY()+.5, worldPosition.getZ()+.5) <= 64;
            }
        };
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return capsValid && !isRemoved()
                ? capabilities.computeIfAbsent(side == null ? 6 : side.ordinal(), ignored -> LazyOptional.of(() -> view(accessibleSlots(side)))).cast()
                : LazyOptional.empty();
        return super.getCapability(cap, side);
    }
    private void refillLegacyOverflow() {
        if (refilling || loading || overflow.isEmpty()) return;
        refilling = true;
        try {
            for (int i = overflow.size()-1; i >= 0; i--) {
                ItemStack rest = ItemHandlerHelper.insertItemStacked(inventory, overflow.get(i), false);
                if (rest.isEmpty()) overflow.remove(i); else overflow.set(i, rest);
            }
        } finally { refilling = false; }
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.put("Inventory", inventory.serializeNBT()); tag.putBoolean("SidedAccess", sidedAccess);
        ListTag extras = new ListTag(); for (ItemStack stack : overflow) extras.add(stack.save(new CompoundTag())); tag.put("LegacyOverflow", extras);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); loading = true;
        try {
            inventory.setSize(SLOT_COUNT); overflow.clear(); sidedAccess = tag.getBoolean("SidedAccess");
            if (tag.contains("Inventory", 10)) {
                CompoundTag data = tag.getCompound("Inventory").copy(); data.putInt("Size", SLOT_COUNT); inventory.deserializeNBT(data);
            } else {
                ListTag old = tag.getList("gt.compartments", 10);
                for (int page = 0; page < Math.min(4, old.size()); page++) {
                    ItemStack item = ItemStack.of(old.getCompound(page).getCompound("item"));
                    long count = Math.max(0, Math.min(4096, old.getCompound(page).getLong("count")));
                    if (item.isEmpty()) continue;
                    int slot = page * PAGE_SIZE;
                    while (count > 0) {
                        int n = (int)Math.min(count, item.getMaxStackSize()); ItemStack stack = item.copyWithCount(n);
                        if (slot < (page+1)*PAGE_SIZE) inventory.setStackInSlot(slot++, stack); else overflow.add(stack);
                        count -= n;
                    }
                }
            }
            for (var element : tag.getList("LegacyOverflow", 10)) { ItemStack stack = ItemStack.of((CompoundTag)element); if (!stack.isEmpty()) overflow.add(stack); }
        } finally { loading = false; }
        resetCapabilities(); refillLegacyOverflow();
    }
    @Override public void dropContents() {
        if (level == null || level.isClientSide) return;
        loading = true;
        try {
            for (int i=0; i<SLOT_COUNT; i++) { BlockContents.drop(this, inventory.getStackInSlot(i).copy()); inventory.setStackInSlot(i, ItemStack.EMPTY); }
            for (ItemStack stack : overflow) BlockContents.drop(this, stack.copy()); overflow.clear();
        } finally { loading = false; }
        setChanged();
    }
}
