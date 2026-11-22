package gregtech.content.blockentity.machine;

import gregtech.content.cover.ComponentCoverRuntime;
import gregtech.engine.energy.FaceConfig;
import gregtech.engine.machine.MachineInventory;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
//?}

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** External inventory views and the per-face access rules for a basic machine. */
final class BasicMachineIOPorts {
    private final BasicMachineBlockEntity machine;
    private IItemHandler itemView;

//? if <1.21.1 {
    private final LazyOptional<IItemHandler> internalItemCap;
    private final LazyOptional<IFluidHandler> internalFluidCap;
    @SuppressWarnings("unchecked")
    private final LazyOptional<IItemHandler>[] itemCapsBySide = new LazyOptional[6];
    @SuppressWarnings("unchecked")
    private final LazyOptional<IFluidHandler>[] fluidCapsBySide = new LazyOptional[6];
//?}

    BasicMachineIOPorts(BasicMachineBlockEntity machine) {
        this.machine = machine;
//? if <1.21.1 {
        internalItemCap = LazyOptional.of(() -> {
            MachineInventory inventory = machine.inventoryStorage();
            return inventory == null ? null : inventory.items();
        });
        internalFluidCap = LazyOptional.of(() -> machine);
//?}
    }

    void rebuildItemView() {
        itemView = new MachineItemView();
    }

    IItemHandler itemView() {
        return itemView;
    }

//? if >=1.21.1 {
/*    IItemHandler itemCapability(@Nullable Direction side, FaceConfig faces) {
        if (machine.inventoryStorage() == null) return null;
        if (side == null) return itemView;
        int relativeSide = machine.relativeDir(side);
        boolean canIn = FaceConfig.has(faces.itemInputs(), relativeSide);
        boolean canOut = FaceConfig.has(faces.itemOutputs(), relativeSide);
        return canIn || canOut ? new SidedItemHandler(machine, relativeSide, itemView, canIn, canOut) : null;
    }

    IFluidHandler fluidCapability(@Nullable Direction side, FaceConfig faces) {
        if (machine.inventoryStorage() == null) return null;
        if (side == null) return machine;
        int relativeSide = machine.relativeDir(side);
        boolean canIn = FaceConfig.has(faces.fluidInputs(), relativeSide);
        boolean canOut = FaceConfig.has(faces.fluidOutputs(), relativeSide);
        return canIn || canOut ? new SidedFluidHandler(relativeSide, canIn, canOut) : null;
    }
*///?} else {
    LazyOptional<IItemHandler> itemCapability(@Nullable Direction side, FaceConfig faces) {
        if (side == null) return internalItemCap;
        int relativeSide = machine.relativeDir(side);
        boolean canIn = FaceConfig.has(faces.itemInputs(), relativeSide);
        boolean canOut = FaceConfig.has(faces.itemOutputs(), relativeSide);
        if (!canIn && !canOut) return LazyOptional.empty();
        if (itemCapsBySide[relativeSide] == null) {
            itemCapsBySide[relativeSide] =
                    LazyOptional.of(() -> new SidedItemHandler(machine, relativeSide, itemView, canIn, canOut));
        }
        return itemCapsBySide[relativeSide];
    }

    LazyOptional<IFluidHandler> fluidCapability(@Nullable Direction side, FaceConfig faces) {
        if (side == null) return internalFluidCap;
        int relativeSide = machine.relativeDir(side);
        boolean canIn = FaceConfig.has(faces.fluidInputs(), relativeSide);
        boolean canOut = FaceConfig.has(faces.fluidOutputs(), relativeSide);
        if (!canIn && !canOut) return LazyOptional.empty();
        if (fluidCapsBySide[relativeSide] == null) {
            fluidCapsBySide[relativeSide] = LazyOptional.of(() -> new SidedFluidHandler(relativeSide, canIn, canOut));
        }
        return fluidCapsBySide[relativeSide];
    }

    void invalidateSideCaps() {
        for (int i = 0; i < 6; i++) {
            if (itemCapsBySide[i] != null) {
                itemCapsBySide[i].invalidate();
                itemCapsBySide[i] = null;
            }
            if (fluidCapsBySide[i] != null) {
                fluidCapsBySide[i].invalidate();
                fluidCapsBySide[i] = null;
            }
        }
    }

    void invalidateCaps() {
        internalItemCap.invalidate();
        internalFluidCap.invalidate();
        for (int i = 0; i < 6; i++) {
            if (itemCapsBySide[i] != null) itemCapsBySide[i].invalidate();
            if (fluidCapsBySide[i] != null) fluidCapsBySide[i].invalidate();
        }
    }
//?}

    IItemHandler componentItems() {
        if (machine.inventoryStorage() == null) return null;
        return new IItemHandler() {
            @Override public int getSlots() { return itemView.getSlots(); }
            @Override public ItemStack getStackInSlot(int slot) { return itemView.getStackInSlot(slot); }
            @Override public int getSlotLimit(int slot) { return itemView.getSlotLimit(slot); }

            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return machine.canInsertAutomatically(slot, stack) && itemView.isItemValid(slot, stack);
            }

            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return machine.canInsertAutomatically(slot, stack) ? itemView.insertItem(slot, stack, simulate) : stack;
            }

            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return itemView.extractItem(slot, amount, simulate);
            }
        };
    }

    /** The same face policy gates active auto-I/O and passive capability traffic. */
    static boolean allowsItemTraffic(BasicMachineBlockEntity machine, Direction side, ItemStack stack, boolean inserting) {
        return !machine.isRemoved()
                && !machine.isFaceShuttered(side)
                && ComponentCoverRuntime.allowsItem(machine.getCover(side), inserting)
                && machine.coverFilterPermits(side, stack);
    }

    // Cached capabilities stay live with a closed shutter. Each transfer checks the current face and cover.
    private record SidedItemHandler(
            BasicMachineBlockEntity be, int relativeSide, IItemHandler inner, boolean canInsert, boolean canExtract)
            implements IItemHandler {
        @Override public int getSlots() { return inner.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return inner.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return inner.getSlotLimit(slot); }

        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            Direction side = be.relativeToAbsolute(relativeSide);
            return canInsert && be.canInsertAutomatically(slot, stack) && allowsItemTraffic(be, side, stack, true)
                    ? inner.insertItem(slot, stack, simulate) : stack;
        }

        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < be.visibleInputSlots()) return ItemStack.EMPTY;
            Direction side = be.relativeToAbsolute(relativeSide);
            ItemStack existing = inner.getStackInSlot(slot);
            return canExtract && allowsItemTraffic(be, side, existing, false)
                    ? inner.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override public boolean isItemValid(int slot, ItemStack stack) {
            Direction side = be.relativeToAbsolute(relativeSide);
            return canInsert && be.canInsertAutomatically(slot, stack) && allowsItemTraffic(be, side, stack, true)
                    && inner.isItemValid(slot, stack);
        }
    }

    private final class SidedFluidHandler implements IFluidHandler {
        private final int relativeSide;
        private final boolean canFill;
        private final boolean canDrain;

        SidedFluidHandler(int relativeSide, boolean canFill, boolean canDrain) {
            this.relativeSide = relativeSide;
            this.canFill = canFill;
            this.canDrain = canDrain;
        }

        @Override public int getTanks() { return machine.getTanks(); }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return machine.getFluidInTank(tank); }
        @Override public int getTankCapacity(int tank) { return machine.getTankCapacity(tank); }

        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            Direction side = machine.relativeToAbsolute(relativeSide);
            return allowsFluidTraffic(side, stack, true) && machine.isFluidValid(tank, stack);
        }

        @Override public int fill(FluidStack resource, FluidAction action) {
            Direction side = machine.relativeToAbsolute(relativeSide);
            return allowsFluidTraffic(side, resource, true) ? machine.fill(resource, action) : 0;
        }

        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            Direction side = machine.relativeToAbsolute(relativeSide);
            return allowsFluidTraffic(side, resource, false) ? machine.drain(resource, action) : FluidStack.EMPTY;
        }

        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            Direction side = machine.relativeToAbsolute(relativeSide);
            FluidStack existing = machine.drain(maxDrain, FluidAction.SIMULATE);
            return allowsFluidTraffic(side, existing, false) ? machine.drain(maxDrain, action) : FluidStack.EMPTY;
        }

        private boolean allowsFluidTraffic(Direction side, FluidStack stack, boolean filling) {
            return ComponentCoverRuntime.allowsFluid(machine.getCover(side), filling)
                    && (filling ? canFill : canDrain)
                    && !machine.isRemoved()
                    && !machine.isFaceShuttered(side)
                    && machine.coverFluidFilterPermits(side, stack);
        }
    }

    /** Ordinary inputs, the dedicated potion inputs, then ordinary outputs. */
    private final class MachineItemView implements IItemHandler {
        @Override public int getSlots() {
            MachineInventory inventory = machine.inventoryStorage();
            return inventory.items().getSlots() + inventory.potionCount();
        }

        @Override public ItemStack getStackInSlot(int slot) {
            MachineInventory inventory = machine.inventoryStorage();
            return isPotionSlot(inventory, slot)
                    ? inventory.potionInput().getStackInSlot(0)
                    : inventory.items().getStackInSlot(innerSlot(inventory, slot));
        }

        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            MachineInventory inventory = machine.inventoryStorage();
            if (isPotionSlot(inventory, slot)) return inventory.potionInput().insertItem(0, stack, simulate);
            return slot < inventory.inputCount() ? inventory.items().insertItem(slot, stack, simulate) : stack;
        }

        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < machine.visibleInputSlots()) return ItemStack.EMPTY;
            MachineInventory inventory = machine.inventoryStorage();
            return inventory.items().extractItem(innerSlot(inventory, slot), amount, simulate);
        }

        @Override public int getSlotLimit(int slot) {
            MachineInventory inventory = machine.inventoryStorage();
            return isPotionSlot(inventory, slot)
                    ? inventory.potionInput().getSlotLimit(0)
                    : inventory.items().getSlotLimit(innerSlot(inventory, slot));
        }

        @Override public boolean isItemValid(int slot, ItemStack stack) {
            MachineInventory inventory = machine.inventoryStorage();
            if (isPotionSlot(inventory, slot)) return inventory.potionInput().isItemValid(0, stack);
            return slot < inventory.inputCount() && inventory.items().isItemValid(slot, stack);
        }

        private static boolean isPotionSlot(MachineInventory inventory, int slot) {
            return inventory.potionInput() != null && slot == inventory.inputCount();
        }

        private static int innerSlot(MachineInventory inventory, int slot) {
            return slot < inventory.inputCount() ? slot : slot - inventory.potionCount();
        }
    }
}
