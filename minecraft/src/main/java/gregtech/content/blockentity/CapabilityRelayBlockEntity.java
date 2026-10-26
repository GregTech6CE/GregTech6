package gregtech.content.blockentity;

import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.capabilities.*;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}
import java.util.*;
import java.util.function.Function;

/**
 * Live, sided capability forwarding without local storage.
 * The destination lookup does not load its chunk; subclass route resolution may scan other chunks.
 * A shared visit set guards nested relay operations against cycles.
 */
public abstract class CapabilityRelayBlockEntity extends BlockEntity {

    public record Target(BlockPos position, Direction side, net.minecraft.world.level.Level level) {

        public Target(BlockPos position, Direction side) {
            this(position, side, null);
        }
    }

    protected final net.minecraft.world.level.Level targetLevel(Target target) {
        return target.level() == null ? level : target.level();
    }
    private static final gregtech.engine.logistics.RelayVisitSet<CapabilityRelayBlockEntity> ACTIVE =
            new gregtech.engine.logistics.RelayVisitSet<>();
//? if >=1.21.1 {
    /*private final Map<Direction, IItemHandler> itemCaps = new EnumMap<>(Direction.class);
    private final Map<Direction, IFluidHandler> fluidCaps = new EnumMap<>(Direction.class);
*///?} else {
    private final Map<Direction, LazyOptional<IItemHandler>> itemCaps = new EnumMap<>(Direction.class);
    private final Map<Direction, LazyOptional<IFluidHandler>> fluidCaps = new EnumMap<>(Direction.class);
//?}
    protected CapabilityRelayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract Target target(Direction side);

    protected boolean permitsItem(ItemStack stack) {
        return true;
    }

    protected boolean permitsFluid(FluidStack stack) {
        return true;
    }

    protected boolean permitsItem(Direction side, ItemStack stack) {
        return permitsItem(stack);
    }

    protected boolean permitsFluid(Direction side, FluidStack stack) {
        return permitsFluid(stack);
    }

    protected boolean supportsItems() {
        return true;
    }

    protected boolean supportsFluids() {
        return true;
    }

    protected boolean supportsFluidExtraction() {
        return true;
    }

    protected boolean exposes(Direction side) {
        return side != null;
    }
//? if >=1.21.1 {
    /*protected final <T, R> R forward(Direction side, BlockCapability<T, Direction> capability, R unavailable, Function<T, R> operation) {
*///?} else {
    protected final <T, R> R forward(Direction side, Capability<T> capability, R unavailable, Function<T, R> operation) {
//?}
        var visited = ACTIVE.get();
        if (level == null || isRemoved() || !visited.add(this)) return unavailable;
        try {
            var target = target(side);
            if (target == null) return unavailable;
            var destinationLevel = targetLevel(target);
            if (!destinationLevel.hasChunkAt(target.position())) return unavailable;
            var entity = destinationLevel.getBlockEntity(target.position());
            if (entity == null || entity == this || entity.isRemoved()) return unavailable;
//? if >=1.21.1 {
            /*var handler = destinationLevel.getCapability(capability, target.position(), target.side());
            return handler == null ? unavailable : operation.apply(handler);
*///?} else {
            return entity.getCapability(capability, target.side()).map(operation::apply).orElse(unavailable);
//?}
        } finally {
            visited.remove(this);
            if (visited.isEmpty()) ACTIVE.remove();
        }
    }
//? if >=1.21.1 {
    /*public IItemHandler itemHandler(Direction side) {
        return !isRemoved() && exposes(side) && supportsItems() ? itemCaps.computeIfAbsent(side, ItemRelay::new) : null;
    }

    public IFluidHandler fluidHandler(Direction side) {
        return !isRemoved() && exposes(side) && supportsFluids() ? fluidCaps.computeIfAbsent(side, FluidRelay::new) : null;
    }
*///?} else {
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (!isRemoved() && exposes(side)) {
            if (cap == ForgeCapabilities.ITEM_HANDLER && supportsItems()) return itemCaps.computeIfAbsent(side,
                    key -> LazyOptional.of(() -> new ItemRelay(key))).cast();
            if (cap == ForgeCapabilities.FLUID_HANDLER && supportsFluids()) return fluidCaps.computeIfAbsent(side,
                    key -> LazyOptional.of(() -> new FluidRelay(key))).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCaps.values().forEach(LazyOptional::invalidate);
        fluidCaps.values().forEach(LazyOptional::invalidate);
        itemCaps.clear();
        fluidCaps.clear();
    }
//?}
    /** Platform capabilities stay here; the relay operations below are shared. */
    private <R> R forwardItems(Direction side, R unavailable, Function<IItemHandler, R> operation) {
//? if >=1.21.1 {
        /*return forward(side, Capabilities.ItemHandler.BLOCK, unavailable, operation);
*///?} else {
        return forward(side, ForgeCapabilities.ITEM_HANDLER, unavailable, operation);
//?}
    }

    private <R> R forwardFluids(Direction side, R unavailable, Function<IFluidHandler, R> operation) {
//? if >=1.21.1 {
        /*return forward(side, Capabilities.FluidHandler.BLOCK, unavailable, operation);
*///?} else {
        return forward(side, ForgeCapabilities.FLUID_HANDLER, unavailable, operation);
//?}
    }

    private final class ItemRelay implements IItemHandler {
        private final Direction side;

        ItemRelay(Direction side) {
            this.side = side;
        }

        @Override
        public int getSlots() {
            return forwardItems(side, 0, IItemHandler::getSlots);
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return forwardItems(side, ItemStack.EMPTY,
                    targetHandler -> slot >= 0 && slot < targetHandler.getSlots() ? targetHandler.getStackInSlot(slot).copy() : ItemStack.EMPTY);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return forwardItems(side, stack,
                    targetHandler -> slot >= 0 && slot < targetHandler.getSlots() && permitsItem(side, stack)
                            // Vanilla InvWrapper can retain the offered instance in an empty slot.
                            // The receiver must own its stack; later extraction must not mutate the caller's offer.
                            ? targetHandler.insertItem(slot, stack.copy(), simulate) : stack);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return forwardItems(side, ItemStack.EMPTY,
                    targetHandler -> slot >= 0 && slot < targetHandler.getSlots() && permitsItem(side, targetHandler.getStackInSlot(slot))
                            ? targetHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY);
        }

        @Override
        public int getSlotLimit(int slot) {
            return forwardItems(side, 0,
                    targetHandler -> slot >= 0 && slot < targetHandler.getSlots() ? targetHandler.getSlotLimit(slot) : 0);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return forwardItems(side, false,
                    targetHandler -> slot >= 0 && slot < targetHandler.getSlots() && permitsItem(side, stack) && targetHandler.isItemValid(slot, stack));
        }
    }

    private final class FluidRelay implements IFluidHandler {
        private final Direction side;

        FluidRelay(Direction side) {
            this.side = side;
        }

        @Override
        public int getTanks() {
            return forwardFluids(side, 0, IFluidHandler::getTanks);
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return forwardFluids(side, FluidStack.EMPTY,
                    targetHandler -> tank >= 0 && tank < targetHandler.getTanks() ? targetHandler.getFluidInTank(tank).copy() : FluidStack.EMPTY);
        }

        @Override
        public int getTankCapacity(int tank) {
            return forwardFluids(side, 0,
                    targetHandler -> tank >= 0 && tank < targetHandler.getTanks() ? targetHandler.getTankCapacity(tank) : 0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return forwardFluids(side, false,
                    targetHandler -> tank >= 0 && tank < targetHandler.getTanks() && permitsFluid(side, stack) && targetHandler.isFluidValid(tank, stack));
        }

        @Override
        public int fill(FluidStack stack, FluidAction action) {
            return forwardFluids(side, 0, targetHandler -> permitsFluid(side, stack) ? targetHandler.fill(stack, action) : 0);
        }

        @Override
        public FluidStack drain(FluidStack stack, FluidAction action) {
            return supportsFluidExtraction()
            ? forwardFluids(side, FluidStack.EMPTY,
                    targetHandler -> permitsFluid(side, stack) ? targetHandler.drain(stack, action) : FluidStack.EMPTY)
            : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int amount, FluidAction action) {
            return supportsFluidExtraction() ? forwardFluids(side, FluidStack.EMPTY, targetHandler -> {
                var candidate = targetHandler.drain(amount, FluidAction.SIMULATE);
                return permitsFluid(side, candidate) ? targetHandler.drain(candidate, action) : FluidStack.EMPTY;
            }) : FluidStack.EMPTY;
        }
    }
}
