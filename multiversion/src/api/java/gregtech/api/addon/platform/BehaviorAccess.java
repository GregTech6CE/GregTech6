/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Shared adapters, not an addon entry point. */
final class BehaviorAccess {
    private BehaviorAccess() {}

    static InventoryAccess inventory(IntSupplier slots, IntFunction<ItemStack> read,
                                     BiConsumer<Integer, ItemStack> write) {
        return new InventoryAccess() {
            @Override public int slots() { return slots.getAsInt(); }

            @Override public ItemStack snapshot(int slot) {
                checkIndex(slot, slots(), "slot");
                ItemStack stack = read.apply(slot);
                return stack == null ? ItemStack.EMPTY : stack.copy();
            }

            @Override public void set(int slot, ItemStack stack) {
                checkIndex(slot, slots(), "slot");
                if (stack != null && stack.getCount() < 0)
                    throw new IllegalArgumentException("negative item count");
                write.accept(slot, stack == null ? ItemStack.EMPTY : stack.copy());
            }
        };
    }

    static FluidAccess fluids(IntSupplier tanks, IntFunction<FluidStack> read,
                              IntUnaryOperator capacity, Fill fill, Drain drain) {
        return new FluidAccess() {
            @Override public int tanks() { return tanks.getAsInt(); }

            @Override public FluidStack snapshot(int tank) {
                checkIndex(tank, tanks(), "tank");
                return copy(read.apply(tank));
            }

            @Override public int capacity(int tank) {
                checkIndex(tank, tanks(), "tank");
                return capacity.applyAsInt(tank);
            }

            @Override public int fill(int tank, FluidStack stack, boolean simulate) {
                checkIndex(tank, tanks(), "tank");
                if (stack == null) return 0;
                if (stack.getAmount() < 0) throw new IllegalArgumentException("negative fluid amount");
                return stack.isEmpty() ? 0 : fill.apply(tank, stack.copy(), simulate);
            }

            @Override public FluidStack drain(int tank, int amount, boolean simulate) {
                checkIndex(tank, tanks(), "tank");
                if (amount < 0) throw new IllegalArgumentException("negative fluid amount");
                return amount == 0 ? FluidStack.EMPTY : copy(drain.apply(tank, amount, simulate));
            }
        };
    }

    private static FluidStack copy(FluidStack stack) {
        return stack == null ? FluidStack.EMPTY : stack.copy();
    }

    private static void checkIndex(int index, int count, String kind) {
        if (index < 0 || index >= count)
            throw new IndexOutOfBoundsException(kind + " " + index + " outside 0.." + (count - 1));
    }

    @FunctionalInterface interface Fill { int apply(int tank, FluidStack stack, boolean simulate); }
    @FunctionalInterface interface Drain { FluidStack apply(int tank, int amount, boolean simulate); }
}
