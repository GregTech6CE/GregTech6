package gregtech.blockentity.inventory;

import gregtech.client.gui.HopperContainerMenu;
import gregtech.registry.GTMenuTypes;
import gregtech.world.GarbageData;
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
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
//?}

import javax.annotation.Nullable;

/**
 * GT6 Ender Garbage Bin: a 9-slot buffer that periodically teleports its
 * contents (and any piped-in fluids) into the global {@link GarbageData}
 * garbage dump. A redstone signal pauses it.
 */
public class EnderGarbageBlockEntity extends BlockEntity implements MenuProvider, gregtech.api.inventory.BlockContents {

    private final ItemStackHandler inventory = new ItemStackHandler(9) {

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public EnderGarbageBlockEntity(BlockPos pos, BlockState state) {
        super(gregtech.registry.GTBlockEntities.ENDER_GARBAGE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnderGarbageBlockEntity bin) {
        // GT6 cadence: once every 100 ticks, paused by a redstone signal
        if (!gregtech.content.storage.GarbageStorageRules.binDue(level.getGameTime())) return;
        if (level.hasNeighborSignal(pos)) return;
        bin.dumpToGarbage();
    }

    private void dumpToGarbage() {
        if (level == null) return;
        GarbageData garbage = GarbageData.get(level);
        for (int slotIndex = 0; slotIndex < inventory.getSlots(); slotIndex++) {
            ItemStack stack = inventory.getStackInSlot(slotIndex);
            if (stack.isEmpty()) continue;
            garbage.trash(stack);
            inventory.setStackInSlot(slotIndex, ItemStack.EMPTY);
        }
    }

    // ── GUI ──────────────────────────────────────────────────────────────

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.gt6ce.ender_garbage_bin");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new HopperContainerMenu(GTMenuTypes.forSlotCount(9), containerId, playerInv, inventory) {
            @Override protected void inventorySlotChanged(int index) { EnderGarbageBlockEntity.this.setChanged(); }
            @Override
            public boolean stillValid(Player player) {
                return !isRemoved() && level != null && player.level() == level
                        && level.getBlockEntity(worldPosition) == EnderGarbageBlockEntity.this
                        && player.distanceToSqr(worldPosition.getX() + .5,
                                worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64;
            }
        };
    }

    // ── automation: insert only; fluids are voided straight into the dump ─

    private final IItemHandler insertOnly = new IItemHandler() {

        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (level != null && level.hasNeighborSignal(worldPosition)) return stack;
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    };

    private final IFluidHandler fluidVoid = new IFluidHandler() {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource == null || resource.isEmpty()) return 0;
            if (level == null || level.isClientSide || level.hasNeighborSignal(worldPosition)) return 0;
            if (action.execute()) GarbageData.get(level).trash(resource.copy());
            return resource.getAmount();
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

//? if >=1.21.1 {
    /*public IItemHandler itemHandler() {
        return isRemoved() ? null : insertOnly;
    }

    public IFluidHandler fluidHandler() {
        return isRemoved() ? null : fluidVoid;
    }
    // ── NBT ──────────────────────────────────────────────────────────────
*///?} else {
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> insertOnly);
    private final LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluidVoid);
//?}

    @Override
//? if >=1.21.1 {
    /*protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.put("Inventory", inventory.serializeNBT(lookup));
*///?} else {
    public <T> @org.jetbrains.annotations.NotNull LazyOptional<T> getCapability(
            @org.jetbrains.annotations.NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        if (capability == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        return super.getCapability(capability, side);
//?}
    }

    @Override
//? if >=1.21.1 {
    /*protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        if (tag.contains("Inventory")) inventory.deserializeNBT(lookup, tag.getCompound("Inventory"));
*///?} else {
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
//?}
    }

    @Override
    public void dropContents() {
        if (level == null || level.isClientSide) return;
        gregtech.api.inventory.BlockContents.drop(this, inventory);
        setChanged();
    }
}
