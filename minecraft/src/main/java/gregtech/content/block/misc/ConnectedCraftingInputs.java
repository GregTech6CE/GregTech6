package gregtech.content.block.misc;

import gregtech.engine.stack.StackCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
//?}
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** A single craft's reservations; previewing never consumes a neighbouring inventory or tank. */
final class ConnectedCraftingInputs {
    private record Items(IItemHandler handler, int slot, ItemStack stack) {}
    private record Fluid(IFluidHandler handler, FluidStack stack) {}
    private final List<IItemHandler> inventories = new ArrayList<>();
    private final List<IFluidHandler> tanks = new ArrayList<>();
    private static final int MAX_REFUND_TYPES = 9; // One fluid identity per crafting-grid cell.
    private final List<FluidStack> refundTypes = new ArrayList<>();
    private final List<Items> items = new ArrayList<>();
    private final List<Fluid> fluids = new ArrayList<>();

    ConnectedCraftingInputs(List<? extends IItemHandler> inventories, List<? extends IFluidHandler> tanks) {
        this.inventories.addAll(inventories);
        this.tanks.addAll(tanks);
    }

    ConnectedCraftingInputs(Level level, BlockPos pos, List<? extends IFluidHandler> recovered) {
        this(List.of(), recovered);
        for (var tank : recovered) {
            var stored = tank.getFluidInTank(0);
            if (!stored.isEmpty() && !knownRefundType(stored)) refundTypes.add(stored);
        }
        for (var side : Direction.values()) {
            var adjacent = pos.relative(side);
            if (!level.hasChunkAt(adjacent)) continue;
//? if >=1.21.1 {
            /*var inventory = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, adjacent, side.getOpposite());
            var tank = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, adjacent, side.getOpposite());
*///?} else {
            var entity = level.getBlockEntity(adjacent);
            if (entity == null) continue;
            var inventory = entity.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, side.getOpposite()).orElse(null);
            var tank = entity.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, side.getOpposite()).orElse(null);
//?}
            if (inventory != null && !inventories.contains(inventory)) inventories.add(inventory);
            if (tank != null && !tanks.contains(tank)) tanks.add(tank);
        }
    }

    ItemStack take(ItemStack wanted) {
        for (var handler : inventories) for (int slot = 0; slot < handler.getSlots(); slot++) {
            int index = -1;
            for (int i = 0; i < items.size(); i++)
                if (items.get(i).handler == handler && items.get(i).slot == slot) { index = i; break; }
            int count = index < 0 ? 1 : items.get(index).stack.getCount() + 1;
            var available = handler.extractItem(slot, count, true);
            if (available.getCount() != count || !AdvancedCraftingTableBlockEntity.same(available, wanted)) continue;
            var reservation = new Items(handler, slot, available.copy());
            if (index < 0) items.add(reservation); else items.set(index, reservation);
            return available.copyWithCount(1);
        }
        return ItemStack.EMPTY;
    }

    /** Refill the returned vessel, as MultiTileEntityAdvancedCraftingTable.consumeSlot does. */
    boolean refill(ItemStack used, ItemStack remainder) {
        if (remainder.getCount() != 1) return false;
        var contained = FluidUtil.getFluidContained(used).orElse(FluidStack.EMPTY);
        if (contained.isEmpty()) return false;
        // An executed reservation can become a refund. Include both the stored credits and
        // this craft's planned types when checking if a new identity would exceed the limit.
        boolean known = knownRefundType(contained);
        if (!known && refundTypes.size() >= MAX_REFUND_TYPES) return false;
        var vessel = FluidUtil.getFluidHandler(remainder.copy()).orElse(null);
        if (vessel == null || vessel.fill(contained.copy(), IFluidHandler.FluidAction.EXECUTE) != contained.getAmount()
                || !AdvancedCraftingTableBlockEntity.same(vessel.getContainer(), used)) return false;
        // A previous partial return may hold less than one vessel. Combine it with neighbouring
        // sources; publish the new reservations only when the whole vessel can be refilled.
        var proposed = new ArrayList<>(fluids);
        int missing = contained.getAmount();
        for (var handler : tanks) {
            int index = -1;
            for (int i = 0; i < proposed.size(); i++)
                if (proposed.get(i).handler == handler && sameFluid(proposed.get(i).stack, contained)) { index = i; break; }
            int reserved = index < 0 ? 0 : proposed.get(index).stack.getAmount();
            var request = contained.copy();
            request.setAmount(reserved + Math.min(missing, Integer.MAX_VALUE - reserved));
            var available = handler.drain(request, IFluidHandler.FluidAction.SIMULATE);
            if (!sameFluid(available, contained) || available.getAmount() <= reserved) continue;
            int take = Math.min(missing, available.getAmount() - reserved);
            request.setAmount(reserved + take);
            var reservation = new Fluid(handler, request);
            if (index < 0) proposed.add(reservation); else proposed.set(index, reservation);
            missing -= take;
            if (missing == 0) {
                if (!known) refundTypes.add(contained);
                fluids.clear();
                fluids.addAll(proposed);
                return true;
            }
        }
        return false;
    }

    private boolean knownRefundType(FluidStack candidate) {
        return refundTypes.stream().anyMatch(stored -> sameFluid(stored, candidate));
    }

    boolean commit(BooleanSupplier tableUnchanged, Consumer<ItemStack> recoverItem, Consumer<FluidStack> recoverFluid) {
        var takenItems = new ArrayList<Items>();
        var takenFluids = new ArrayList<Fluid>();
        boolean complete = false;
        try {
            for (var input : items) {
                int missing = extract(input.handler, input.slot, input.stack, input.stack.getCount(), takenItems);
                // Two views of one chest can both promise its last item. Other connected
                // inventories may still have the identical ingredient, even in the same tick.
                if (missing > 0) {
                    for (var handler : inventories) {
                        for (int slot = 0; slot < handler.getSlots() && missing > 0; slot++) {
                            if (handler == input.handler && slot == input.slot) continue;
                            var offered = handler.extractItem(slot, missing, true);
                            if (offered.isEmpty() || !StackCompat.sameItem(offered, input.stack)) continue;
                            missing = extract(handler, slot, input.stack, missing, takenItems);
                        }
                        if (missing <= 0) break;
                    }
                }
                if (missing != 0) return false;
            }
            for (var input : fluids) {
                var taken = input.handler.drain(input.stack.copy(), IFluidHandler.FluidAction.EXECUTE);
                if (!taken.isEmpty()) takenFluids.add(new Fluid(input.handler, taken.copy()));
                if (!equalFluid(taken, input.stack)) return false;
            }
            // A relay may even point back into this table. Applying its old local snapshot
            // after such an extraction would recreate the item we just took.
            complete = tableUnchanged.getAsBoolean();
            return complete;
        } catch (RuntimeException failure) {
            com.mojang.logging.LogUtils.getLogger().warn("Connected crafting source failed during extraction", failure);
            return false;
        } finally {
            if (!complete) {
                for (var input : takenFluids) {
                    int restored = 0;
                    try {
                        restored = input.handler.fill(input.stack.copy(), IFluidHandler.FluidAction.EXECUTE);
                    } catch (RuntimeException failure) {
                        com.mojang.logging.LogUtils.getLogger().warn("Connected crafting tank refused a refund", failure);
                    }
                    input.stack.shrink(Math.max(0, restored));
                    if (!input.stack.isEmpty()) recoverFluid.accept(input.stack);
                }
                for (var input : takenItems) {
                    var rest = input.stack;
                    try {
                        if (input.slot < input.handler.getSlots()) rest = input.handler.insertItem(input.slot, rest.copy(), false);
                    } catch (RuntimeException failure) {
                        com.mojang.logging.LogUtils.getLogger().warn("Connected crafting inventory refused a refund", failure);
                    }
                    if (!rest.isEmpty()) recoverItem.accept(rest);
                }
            }
        }
    }

    private static int extract(IItemHandler handler, int slot, ItemStack wanted, int missing, List<Items> takenItems) {
        if (slot >= handler.getSlots()) return missing;
        var taken = handler.extractItem(slot, missing, false);
        if (taken.isEmpty()) return missing;
        takenItems.add(new Items(handler, slot, taken.copy()));
        return taken.getCount() <= missing && StackCompat.sameItem(taken, wanted) ? missing - taken.getCount() : -1;
    }

    private static boolean equalFluid(FluidStack a, FluidStack b) {
        return a.getAmount() == b.getAmount() && sameFluid(a, b);
    }
    private static boolean sameFluid(FluidStack a, FluidStack b) {
        return StackCompat.sameFluid(a, b);
    }
}
