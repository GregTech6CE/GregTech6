package com.gregtech.gregtech.blockentity.inventory;

import com.gregtech.gregtech.client.gui.HopperContainerMenu;
import com.gregtech.gregtech.registry.GTMenuTypes;
import com.gregtech.gregtech.world.GarbageData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/**
 * GT6 Ender Garbage Bin: a 9-slot buffer that periodically teleports its
 * contents (and any piped-in fluids) into the global {@link GarbageData}
 * garbage dump. A redstone signal pauses it.
 */
public class EnderGarbageBlockEntity extends BlockEntity implements MenuProvider, com.gregtech.gregtech.api.inventory.BlockContents {

    private final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    public EnderGarbageBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.ENDER_GARBAGE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnderGarbageBlockEntity bin) {
        // GT6 cadence: once every 100 ticks, paused by a redstone signal
        if (level.getGameTime() % 100 != 50) return;
        if (level.hasNeighborSignal(pos)) return;
        bin.dumpToGarbage();
    }

    private void dumpToGarbage() {
        if (level == null) return;
        GarbageData garbage = GarbageData.get(level);
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            garbage.trash(stack);
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    // ── GUI ──────────────────────────────────────────────────────────────

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.gregtech.ender_garbage_bin");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new HopperContainerMenu(GTMenuTypes.forSlotCount(9), containerId, playerInv, inventory);
    }

    // ── automation: insert only; fluids are voided straight into the dump ─

    private final IItemHandler insertOnly = new IItemHandler() {
        @Override public int getSlots() { return inventory.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (level != null && level.hasNeighborSignal(worldPosition)) return stack;
            return inventory.insertItem(slot, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }
    };

    private final IFluidHandler fluidVoid = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return Integer.MAX_VALUE; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return true; }
        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource == null || resource.isEmpty()) return 0;
            if (level == null || level.isClientSide || level.hasNeighborSignal(worldPosition)) return 0;
            if (action.execute()) GarbageData.get(level).trash(resource.copy());
            return resource.getAmount();
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    };

    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> insertOnly);
    private final LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluidVoid);

    @Override
    public <T> @org.jetbrains.annotations.NotNull LazyOptional<T> getCapability(
            @org.jetbrains.annotations.NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        if (capability == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
        fluidCap.invalidate();
    }

    // ── NBT ──────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) inventory.deserializeNBT(tag.getCompound("Inventory"));
    }
    @Override public void dropContents() {
        if (level == null || level.isClientSide) return;
        com.gregtech.gregtech.api.inventory.BlockContents.drop(this, inventory);
        setChanged();
    }
}
