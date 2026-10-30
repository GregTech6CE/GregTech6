package gregtech.content.block.misc;

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

/** A single craft's reservations; previewing never consumes a neighbouring inventory or tank. */
final class ConnectedCraftingInputs {
    private record Items(IItemHandler handler, int slot, ItemStack stack) {}
    private record Fluid(IFluidHandler handler, FluidStack stack) {}
    private final List<IItemHandler> inventories = new ArrayList<>();
    private final List<IFluidHandler> tanks = new ArrayList<>();
    private final List<Items> items = new ArrayList<>();
    private final List<Fluid> fluids = new ArrayList<>();

    ConnectedCraftingInputs(Level level, BlockPos pos) {
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
        var vessel = FluidUtil.getFluidHandler(remainder.copy()).orElse(null);
        if (vessel == null || vessel.fill(contained.copy(), IFluidHandler.FluidAction.EXECUTE) != contained.getAmount()
                || !AdvancedCraftingTableBlockEntity.same(vessel.getContainer(), used)) return false;
        for (var handler : tanks) {
            int index = -1;
            for (int i = 0; i < fluids.size(); i++)
                if (fluids.get(i).handler == handler && sameFluid(fluids.get(i).stack, contained)) { index = i; break; }
            var total = contained.copy();
            if (index >= 0) total.grow(fluids.get(index).stack.getAmount());
            if (!equalFluid(handler.drain(total, IFluidHandler.FluidAction.SIMULATE), total)) continue;
            var reservation = new Fluid(handler, total);
            if (index < 0) fluids.add(reservation); else fluids.set(index, reservation);
            return true;
        }
        return false;
    }

    boolean commit() {
        // Recheck all sources together, including repeated ingredients, before taking anything.
        for (var input : items) {
            var available = input.handler.extractItem(input.slot, input.stack.getCount(), true);
            if (available.getCount() != input.stack.getCount()
                    || !AdvancedCraftingTableBlockEntity.same(available, input.stack)) return false;
        }
        for (var input : fluids)
            if (!equalFluid(input.handler.drain(input.stack, IFluidHandler.FluidAction.SIMULATE), input.stack)) return false;
        for (var input : items) {
            var taken = input.handler.extractItem(input.slot, input.stack.getCount(), false);
            if (taken.getCount() != input.stack.getCount() || !AdvancedCraftingTableBlockEntity.same(taken, input.stack))
                throw new IllegalStateException("Connected crafting inventory did not honour its extraction simulation");
        }
        for (var input : fluids)
            if (!equalFluid(input.handler.drain(input.stack, IFluidHandler.FluidAction.EXECUTE), input.stack))
                throw new IllegalStateException("Connected crafting tank did not honour its drain simulation");
        return true;
    }

    private static boolean equalFluid(FluidStack a, FluidStack b) {
        return a.getAmount() == b.getAmount() && sameFluid(a, b);
    }
    private static boolean sameFluid(FluidStack a, FluidStack b) {
//? if >=1.21.1 {
        /*return FluidStack.isSameFluidSameComponents(a, b);
*///?} else {
        return a.isFluidEqual(b);
//?}
    }
}
