package gregtech.content.blockentity.machine;

import gregtech.api.addon.machine.BehaviorMachine;
import gregtech.api.addon.platform.MachineBehavior;
import gregtech.api.addon.platform.MachineBehaviorContext;
import gregtech.api.machine.MachineFace;
import gregtech.api.multiblock.PortChannel;
import gregtech.content.blockentity.machine.BehaviorMachineTanks.Tank;
import gregtech.engine.energy.FaceConfig;
import gregtech.engine.multiblock.MultiblockLayout;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
*///?} else {
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
//?}

/** The item and fluid handlers for a behavior machine and its structure ports. */
final class BehaviorMachineIOPorts {
    private final BehaviorMachineBlockEntity machine;
    private final BehaviorMachine declaration;
    private final MachineBehavior behavior;
    private final MachineBehaviorContext context;
    private final ItemStackHandler items;
    private final BehaviorMachineTanks fluidAccess;
    private final Tank[] tanks;
    private final PipeItems pipeItems = new PipeItems();
    private final PipeFluids pipeFluids = new PipeFluids();
    private final IItemHandler[] itemSides = new IItemHandler[6];
    private final IFluidHandler[] fluidSides = new IFluidHandler[6];
//? if <1.21.1 {
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> pipeItems);
    private final LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> pipeFluids);
    @SuppressWarnings("unchecked")
    private final LazyOptional<IItemHandler>[] sidedItemCaps = new LazyOptional[6];
    @SuppressWarnings("unchecked")
    private final LazyOptional<IFluidHandler>[] sidedFluidCaps = new LazyOptional[6];
//?}

    BehaviorMachineIOPorts(BehaviorMachineBlockEntity machine, BehaviorMachine declaration, MachineBehavior behavior,
                           MachineBehaviorContext context, ItemStackHandler items, BehaviorMachineTanks fluidAccess) {
        this.machine = machine;
        this.declaration = declaration;
        this.behavior = behavior;
        this.context = context;
        this.items = items;
        this.fluidAccess = fluidAccess;
        this.tanks = fluidAccess.entries();
    }

    IItemHandler items() { return pipeItems; }
    IFluidHandler fluids() { return pipeFluids; }

    /** Unsided when {@code side} is null or the machine declares no {@code MachineIo}. A closed face is null. */
    @Nullable
    IItemHandler itemHandler(@Nullable Direction side) {
        if (side == null || declaration.io() == null) return pipeItems;
        int rel = machine.relative(side);
        boolean insert = FaceConfig.has(declaration.itemInputFaces(), rel);
        boolean extract = FaceConfig.has(declaration.itemOutputFaces(), rel);
        if (!insert && !extract) return null;
        IItemHandler cached = itemSides[rel];
        if (cached == null) itemSides[rel] = cached = new PipeItems(faceAt(rel), insert, extract);
        return cached;
    }

    /** Unsided when {@code side} is null or the machine declares no {@code MachineIo}. A closed face is null. */
    @Nullable
    IFluidHandler fluidHandler(@Nullable Direction side) {
        if (tanks.length == 0) return null;
        if (side == null || declaration.io() == null) return pipeFluids;
        int rel = machine.relative(side);
        boolean fill = FaceConfig.has(declaration.fluidInputFaces(), rel);
        boolean drain = FaceConfig.has(declaration.fluidOutputFaces(), rel);
        if (!fill && !drain) return null;
        IFluidHandler cached = fluidSides[rel];
        if (cached == null) fluidSides[rel] = cached = new PipeFluids(faceAt(rel), fill, drain);
        return cached;
    }

    private static MachineFace faceAt(int relative) {
        for (MachineFace face : MachineFace.values()) if (face.index() == relative) return face;
        return MachineFace.FRONT;
    }

//? if <1.21.1 {
    LazyOptional<IItemHandler> itemCapability(@Nullable Direction side) {
        if (side == null || declaration.io() == null) return itemCap;
        int rel = machine.relative(side);
        if (machine.itemHandler(side) == null) return LazyOptional.empty();
        if (sidedItemCaps[rel] == null) {
            IItemHandler handler = itemSides[rel];
            sidedItemCaps[rel] = LazyOptional.of(() -> handler);
        }
        return sidedItemCaps[rel];
    }

    @Nullable
    LazyOptional<IFluidHandler> fluidCapability(@Nullable Direction side) {
        if (machine.fluidHandler(side) == null) return null;
        if (side == null || declaration.io() == null) return fluidCap;
        int rel = machine.relative(side);
        if (sidedFluidCaps[rel] == null) {
            IFluidHandler handler = fluidSides[rel];
            sidedFluidCaps[rel] = LazyOptional.of(() -> handler);
        }
        return sidedFluidCaps[rel];
    }

    void invalidateCaps() {
        itemCap.invalidate();
        fluidCap.invalidate();
        for (int i = 0; i < sidedItemCaps.length; i++) {
            if (sidedItemCaps[i] != null) sidedItemCaps[i].invalidate();
            if (sidedFluidCaps[i] != null) sidedFluidCaps[i].invalidate();
        }
    }

//?}

    private boolean insertable(int slot) {
        var role = declaration.items().get(slot).role();
        return role == BehaviorMachine.SlotRole.INPUT || role == BehaviorMachine.SlotRole.ANY;
    }

    private boolean extractable(int slot) {
        var role = declaration.items().get(slot).role();
        return role == BehaviorMachine.SlotRole.OUTPUT || role == BehaviorMachine.SlotRole.ANY;
    }

    private boolean fillable(int tank) {
        var role = declaration.fluids().get(tank).role();
        return role == BehaviorMachine.SlotRole.INPUT || role == BehaviorMachine.SlotRole.ANY;
    }

    private boolean drainable(int tank) {
        var role = declaration.fluids().get(tank).role();
        return role == BehaviorMachine.SlotRole.OUTPUT || role == BehaviorMachine.SlotRole.ANY;
    }

    private final class PipeItems implements IItemHandler {
        @Nullable private final MachineFace face;
        private final boolean allowInsert;
        private final boolean allowExtract;

        private PipeItems() { this(null, true, true); }

        private PipeItems(@Nullable MachineFace face, boolean allowInsert, boolean allowExtract) {
            this.face = face;
            this.allowInsert = allowInsert;
            this.allowExtract = allowExtract;
        }

        @Override public int getSlots() { return items.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!allowInsert || !insertable(slot) || stack == null || stack.isEmpty()
                    || !behavior.canInsert(context, slot, stack, face)) return stack;
            return items.insertItem(slot, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return allowExtract && extractable(slot) ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return allowInsert && insertable(slot) && behavior.canInsert(context, slot, stack, face);
        }
    }

    private final class PipeFluids implements IFluidHandler {
        @Nullable private final MachineFace face;
        private final boolean allowFill;
        private final boolean allowDrain;

        private PipeFluids() { this(null, true, true); }

        private PipeFluids(@Nullable MachineFace face, boolean allowFill, boolean allowDrain) {
            this.face = face;
            this.allowFill = allowFill;
            this.allowDrain = allowDrain;
        }

        @Override public int getTanks() { return tanks.length; }
        @Override public FluidStack getFluidInTank(int tank) { return tanks[tank].copy(); }
        @Override public int getTankCapacity(int tank) { return tanks[tank].capacity; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return allowFill && fillable(tank) && behavior.canFill(context, tank, stack, face);
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            if (!allowFill || resource == null || resource.isEmpty()) return 0;
            for (int tank = 0; tank < tanks.length; tank++) {
                if (!fillable(tank) || !behavior.canFill(context, tank, resource, face)) continue;
                int taken = behavior.acceptFluid(context, tank, resource, action.simulate());
                if (taken > 0) return taken;
            }
            return 0;
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!allowDrain || resource == null || resource.isEmpty()) return FluidStack.EMPTY;
            for (int tank = 0; tank < tanks.length; tank++) {
                if (!drainable(tank) || tanks[tank].fluid != resource.getFluid()) continue;
                FluidStack drained = tanks[tank].drain(resource.getAmount(), action.simulate());
                if (!action.simulate() && !drained.isEmpty()) machine.setChanged();
                return drained;
            }
            return FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            if (!allowDrain) return FluidStack.EMPTY;
            for (int tank = 0; tank < tanks.length; tank++) {
                if (!drainable(tank)) continue;
                FluidStack drained = tanks[tank].drain(maxDrain, action.simulate());
                if (!drained.isEmpty()) {
                    if (!action.simulate()) machine.setChanged();
                    return drained;
                }
            }
            return FluidStack.EMPTY;
        }
    }

    private static boolean itemInput(MultiblockLayout.Role role) {
        return role != null && role.allows(PortChannel.ITEM_INPUT);
    }

    private static boolean itemOutput(MultiblockLayout.Role role) {
        return role != null && role.allows(PortChannel.ITEM_OUTPUT);
    }

    private static boolean fluidInput(MultiblockLayout.Role role) {
        return role != null && role.allows(PortChannel.FLUID_INPUT);
    }

    private static boolean fluidOutput(MultiblockLayout.Role role) {
        return role != null && role.allows(PortChannel.FLUID_OUTPUT);
    }

    private boolean itemSlotAllows(int slot, boolean input) {
        if (slot < 0 || slot >= declaration.items().size()) return false;
        var role = declaration.items().get(slot).role();
        return role == BehaviorMachine.SlotRole.ANY
                || role == (input ? BehaviorMachine.SlotRole.INPUT : BehaviorMachine.SlotRole.OUTPUT);
    }

    private boolean tankAllows(int tank, boolean input) {
        if (tank < 0 || tank >= declaration.fluids().size()) return false;
        var role = declaration.fluids().get(tank).role();
        return role == BehaviorMachine.SlotRole.ANY
                || role == (input ? BehaviorMachine.SlotRole.INPUT : BehaviorMachine.SlotRole.OUTPUT);
    }

    IItemHandler portItems(MultiblockLayout.Role role, MachineFace face) {
        if (!itemInput(role) && !itemOutput(role)) return null;
        return new IItemHandler() {
            @Override public int getSlots() { return items.getSlots(); }
            @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return isItemValid(slot, stack) ? items.insertItem(slot, stack, simulate) : stack;
            }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return itemOutput(role) && itemSlotAllows(slot, false)
                        ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }
            @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return itemInput(role) && itemSlotAllows(slot, true) && items.isItemValid(slot, stack)
                        && behavior.canInsert(context, slot, stack)
                        && behavior.canInsert(context, slot, stack, face);
            }
        };
    }

    IFluidHandler portFluids(MultiblockLayout.Role role, MachineFace face) {
        if ((!fluidInput(role) && !fluidOutput(role)) || tanks.length == 0) return null;
        return new IFluidHandler() {
            @Override public int getTanks() { return tanks.length; }
            @Override public FluidStack getFluidInTank(int tank) { return tanks[tank].copy(); }
            @Override public int getTankCapacity(int tank) { return tanks[tank].capacity; }
            @Override public boolean isFluidValid(int tank, FluidStack stack) {
                return fluidInput(role) && tankAllows(tank, true) && behavior.canFill(context, tank, stack)
                        && behavior.canFill(context, tank, stack, face);
            }
            @Override public int fill(FluidStack stack, FluidAction action) {
                if (!fluidInput(role) || stack == null || stack.isEmpty()) return 0;
                int remaining = stack.getAmount();
                int filled = 0;
                for (int tank = 0; tank < tanks.length && remaining > 0; tank++) {
                    if (!isFluidValid(tank, stack)) continue;
                    var offer = stack.copy();
                    offer.setAmount(remaining);
                    int taken = behavior.acceptFluid(context, tank, offer, action.simulate());
                    filled += taken;
                    remaining -= taken;
                }
                return filled;
            }
            @Override public FluidStack drain(FluidStack stack, FluidAction action) {
                if (!fluidOutput(role) || stack == null || stack.isEmpty()) return FluidStack.EMPTY;
                return drainMatching(stack.getFluid(), stack.getAmount(), action.simulate());
            }
            @Override public FluidStack drain(int amount, FluidAction action) {
                if (!fluidOutput(role) || amount <= 0) return FluidStack.EMPTY;
                return drainMatching(null, amount, action.simulate());
            }
            private FluidStack drainMatching(Fluid fluid, int amount, boolean simulate) {
                FluidStack out = FluidStack.EMPTY;
                int remaining = amount;
                for (int tank = 0; tank < tanks.length && remaining > 0; tank++) {
                    if (!tankAllows(tank, false)) continue;
                    if (fluid != null && tanks[tank].fluid != fluid) continue;
                    if (!out.isEmpty() && tanks[tank].fluid != out.getFluid()) continue;
                    FluidStack got = tanks[tank].drain(remaining, simulate);
                    if (got.isEmpty()) continue;
                    if (out.isEmpty()) out = got.copy();
                    else out.grow(got.getAmount());
                    remaining -= got.getAmount();
                }
                if (!simulate && !out.isEmpty()) machine.setChanged();
                return out;
            }
        };
    }

    IFluidHandler portFluidTank(MultiblockLayout.Role role, int tank, MachineFace face) {
        if (tank < 0 || tank >= tanks.length) return null;
        return new IFluidHandler() {
            public int getTanks() { return 1; }
            public FluidStack getFluidInTank(int index) { return index == 0 ? tanks[tank].copy() : FluidStack.EMPTY; }
            public int getTankCapacity(int index) { return index == 0 ? tanks[tank].capacity : 0; }
            public boolean isFluidValid(int index, FluidStack stack) {
                return index == 0 && fluidInput(role) && tankAllows(tank, true) && behavior.canFill(context, tank, stack)
                        && behavior.canFill(context, tank, stack, face);
            }
            public int fill(FluidStack stack, FluidAction action) {
                return isFluidValid(0, stack) ? behavior.acceptFluid(context, tank, stack, action.simulate()) : 0;
            }
            public FluidStack drain(FluidStack stack, FluidAction action) {
                return !stack.isEmpty() && stack.getFluid() == tanks[tank].fluid ? drain(stack.getAmount(), action) : FluidStack.EMPTY;
            }
            public FluidStack drain(int amount, FluidAction action) {
                return fluidOutput(role) && tankAllows(tank, false) ? fluidAccess.drain(tank, amount, action.simulate()) : FluidStack.EMPTY;
            }
        };
    }

}
