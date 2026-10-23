package com.gregtech.gregtech.api.addon.platform;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

import com.gregtech.gregtech.api.addon.RecipeKeptInputs;
import com.gregtech.gregtech.api.addon.RecipeOutputChances;

import java.util.Arrays;
import java.util.List;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/** Handles for machine recipe maps that already exist, including maps an addon declared. */
public final class AddonRecipes {
    private AddonRecipes() {}

    /** The named map, or an exception when GregTech has no such map. */
    public static Map map(String name) {
        Map map = AddonLoader.services().map(name);
        if (map == null) throw new IllegalArgumentException("Unknown recipe map: " + name);
        return map;
    }

    public interface Dynamic {
        /**
         * The matching row, or null when this provider does not handle the input.
         * Each call receives unmodifiable lists containing detached stack copies. A provider may
         * inspect or locally modify a stack, but cannot alter the machine or another provider's input.
         * Retaining these snapshots does not give access to later inventory changes.
         */
        Line find(List<ItemStack> items, List<FluidStack> fluids);
    }

    public interface Map {
        String name();

        /** Adds a static row. Null means the map rejected it. */
        Line add(Line line);

        /** A display row for JEI and EMI. Machines do not craft it. Null means the map rejected it. */
        Line addDisplay(Line line);

        /** An existing static row whose inputs this line would cover, or null. */
        Line collision(Line line);

        /** True when some static row already consumes this item. */
        boolean hasItemInput(ItemStack stack);

        /** Appends a provider behind the built-in one. A null provider is rejected. */
        void addDynamic(Dynamic provider);

        /**
         * The first matching row, including dynamic providers. Null when nothing matches.
         * This query does not consume or modify the supplied stacks or lists.
         */
        Line find(List<ItemStack> items, List<FluidStack> fluids, boolean needsOutputs, int inputSlots, int outputSlots);
    }

    /**
     * An immutable recipe snapshot. Construction and array accessors copy arrays and their stacks;
     * changing a supplied or returned object never changes this row.
     * {@code keptInputs} lists item-input indexes the machine checks for and does not consume.
     * {@code keptFluidInputs} does the same for fluid-input indexes: the declared amount is checked and not drained.
     * {@code presenceFluidInputs} checks only that the original tanks contain that fluid, without reserving any amount.
     * Use positive native representatives for presence gates; native zero-sized stacks lose their fluid identity.
     * Null marks nothing. Kept and presence indexes must not overlap.
     */
    public record Line(
            ItemStack[] itemInputs,
            ItemStack[] itemOutputs,
            FluidStack[] fluidInputs,
            FluidStack[] fluidOutputs,
            long duration,
            long euPerTick,
            long special,
            int[] outputChances,
            int[] keptInputs,
            int[] keptFluidInputs,
            int[] presenceFluidInputs
    ) {
        private static final ThreadLocal<Boolean> READING = ThreadLocal.withInitial(() -> Boolean.FALSE);

        /** Named construction with the same validation and defaults as the public constructors. */
        public static Builder builder() {
            return new Builder();
        }

        /** Detached copies, including each stack's native item data. */
        @Override public ItemStack[] itemInputs() { return copyItems(itemInputs); }

        /** Detached copies, including each stack's native item data. */
        @Override public ItemStack[] itemOutputs() { return copyItems(itemOutputs); }

        /** Detached copies, including each stack's native fluid data. */
        @Override public FluidStack[] fluidInputs() { return copyFluids(fluidInputs); }

        /** Detached copies, including each stack's native fluid data. */
        @Override public FluidStack[] fluidOutputs() { return copyFluids(fluidOutputs); }

        @Override public int[] outputChances() { return outputChances.clone(); }
        @Override public int[] keptInputs() { return keptInputs.clone(); }
        @Override public int[] keptFluidInputs() { return keptFluidInputs.clone(); }
        @Override public int[] presenceFluidInputs() { return presenceFluidInputs.clone(); }

        /**
         * A stored row, including an empty slot a built-in recipe already kept.
         * Adding that row again still rejects an input whose amount is not positive.
         */
        public static Line read(ItemStack[] itemInputs, ItemStack[] itemOutputs, FluidStack[] fluidInputs,
                                FluidStack[] fluidOutputs, long duration, long euPerTick, long special,
                                int[] outputChances, int[] keptInputs) {
            return read(itemInputs, itemOutputs, fluidInputs, fluidOutputs, duration, euPerTick, special,
                    outputChances, keptInputs, null);
        }

        public static Line read(ItemStack[] itemInputs, ItemStack[] itemOutputs, FluidStack[] fluidInputs,
                                FluidStack[] fluidOutputs, long duration, long euPerTick, long special,
                                int[] outputChances, int[] keptInputs, int[] keptFluidInputs) {
            return read(itemInputs, itemOutputs, fluidInputs, fluidOutputs, duration, euPerTick, special,
                    outputChances, keptInputs, keptFluidInputs, null);
        }

        public static Line read(ItemStack[] itemInputs, ItemStack[] itemOutputs, FluidStack[] fluidInputs,
                                FluidStack[] fluidOutputs, long duration, long euPerTick, long special,
                                int[] outputChances, int[] keptInputs, int[] keptFluidInputs, int[] presenceFluidInputs) {
            READING.set(Boolean.TRUE);
            try {
                return new Line(itemInputs, itemOutputs, fluidInputs, fluidOutputs, duration, euPerTick, special,
                        outputChances, keptInputs, keptFluidInputs, presenceFluidInputs);
            } finally {
                READING.remove();
            }
        }

        /** Every item output is certain. No input is marked kept. */
        public Line(ItemStack[] itemInputs, ItemStack[] itemOutputs, FluidStack[] fluidInputs, FluidStack[] fluidOutputs,
                    long duration, long euPerTick, long special) {
            this(itemInputs, itemOutputs, fluidInputs, fluidOutputs, duration, euPerTick, special, null, null);
        }

        /** No input is marked kept. */
        public Line(ItemStack[] itemInputs, ItemStack[] itemOutputs, FluidStack[] fluidInputs, FluidStack[] fluidOutputs,
                    long duration, long euPerTick, long special, int[] outputChances) {
            this(itemInputs, itemOutputs, fluidInputs, fluidOutputs, duration, euPerTick, special, outputChances, null);
        }

        /** No fluid input is marked kept. */
        public Line(ItemStack[] itemInputs, ItemStack[] itemOutputs, FluidStack[] fluidInputs, FluidStack[] fluidOutputs,
                    long duration, long euPerTick, long special, int[] outputChances, int[] keptInputs) {
            this(itemInputs, itemOutputs, fluidInputs, fluidOutputs, duration, euPerTick, special,
                    outputChances, keptInputs, null);
        }

        /** Compatibility overload: positive kept amounts, no presence-only gates. */
        public Line(ItemStack[] itemInputs, ItemStack[] itemOutputs, FluidStack[] fluidInputs, FluidStack[] fluidOutputs,
                    long duration, long euPerTick, long special, int[] outputChances, int[] keptInputs, int[] keptFluidInputs) {
            this(itemInputs, itemOutputs, fluidInputs, fluidOutputs, duration, euPerTick, special,
                    outputChances, keptInputs, keptFluidInputs, null);
        }

        public Line {
            itemInputs = copyItemInputs(itemInputs);
            itemOutputs = copyItems(itemOutputs);
            fluidInputs = copyFluidInputs(fluidInputs);
            fluidOutputs = copyFluids(fluidOutputs);
            duration = Math.max(1, duration);
            outputChances = RecipeOutputChances.normalize(outputChances, itemOutputs.length);
            keptInputs = RecipeKeptInputs.normalize(keptInputs, itemInputs.length);
            keptFluidInputs = RecipeKeptInputs.normalizeFluids(keptFluidInputs, fluidInputs.length);
            presenceFluidInputs = RecipeKeptInputs.normalizeFluids(presenceFluidInputs, fluidInputs.length);
            for (int index : presenceFluidInputs) {
                if (fluidInputs[index].isEmpty()) throw new IllegalArgumentException("Presence fluid input " + index + " is empty");
                for (int kept : keptFluidInputs) if (kept == index)
                    throw new IllegalArgumentException("Fluid input " + index + " cannot be both kept and presence-only");
                fluidInputs[index].setAmount(1);
            }
        }

        /**
         * Reusable named recipe construction. Setters replace the named field and copy mutable
         * arguments immediately. Defaults are no inputs/outputs, one tick, zero power/special value,
         * certain item outputs, and no kept or presence-only inputs. Validation happens in build().
         * This mutable builder is not intended to be shared between threads.
         */
        public static final class Builder {
            private ItemStack[] itemInputs;
            private ItemStack[] itemOutputs;
            private FluidStack[] fluidInputs;
            private FluidStack[] fluidOutputs;
            private long duration = 1;
            private long euPerTick;
            private long special;
            private int[] outputChances;
            private int[] keptInputs;
            private int[] keptFluidInputs;
            private int[] presenceFluidInputs;

            private Builder() {}

            /** Item requirements in slot order. Each declared input must have a positive count. */
            public Builder itemInputs(ItemStack... stacks) {
                itemInputs = copyItems(stacks);
                return this;
            }

            public Builder itemOutputs(ItemStack... stacks) {
                itemOutputs = copyItems(stacks);
                return this;
            }

            /** Fluid requirements in tank order. Each declared input must have a positive amount. */
            public Builder fluidInputs(FluidStack... stacks) {
                fluidInputs = copyFluids(stacks);
                return this;
            }

            public Builder fluidOutputs(FluidStack... stacks) {
                fluidOutputs = copyFluids(stacks);
                return this;
            }

            /** Duration in game ticks; values below one normalize to one, as in the constructors. */
            public Builder duration(long ticks) {
                duration = ticks;
                return this;
            }

            /** The recipe map's existing EU/t work value; its sign and interpretation are unchanged. */
            public Builder euPerTick(long value) {
                euPerTick = value;
                return this;
            }

            /** Map-specific value, such as the required temperature. */
            public Builder special(long value) {
                special = value;
                return this;
            }

            /** Item output chances in parts per 10,000. Null or empty means all outputs are certain. */
            public Builder outputChances(int... chances) {
                outputChances = copyIndexes(chances);
                return this;
            }

            /** Zero-based item input indexes that are checked but not consumed. */
            public Builder keptInputs(int... indexes) {
                keptInputs = copyIndexes(indexes);
                return this;
            }

            /** Zero-based fluid input indexes whose declared amounts are checked but not consumed. */
            public Builder keptFluidInputs(int... indexes) {
                keptFluidInputs = copyIndexes(indexes);
                return this;
            }

            /** Zero-based fluid input indexes checked for identity without reserving an amount. */
            public Builder presenceFluidInputs(int... indexes) {
                presenceFluidInputs = copyIndexes(indexes);
                return this;
            }

            /** Validates the fields and creates an independent immutable row. */
            public Line build() {
                return new Line(itemInputs, itemOutputs, fluidInputs, fluidOutputs, duration, euPerTick,
                        special, outputChances, keptInputs, keptFluidInputs, presenceFluidInputs);
            }

            private static int[] copyIndexes(int[] indexes) {
                return indexes == null ? null : indexes.clone();
            }
        }

        private static ItemStack[] copyItemInputs(ItemStack[] stacks) {
            if (stacks == null) return new ItemStack[0];
            if (READING.get()) return copyItems(stacks);
            ItemStack[] copy = new ItemStack[stacks.length];
            for (int i = 0; i < stacks.length; i++) {
                ItemStack stack = stacks[i];
                int count = stack == null || stack.isEmpty() ? 0 : stack.getCount();
                RecipeKeptInputs.requirePositiveCount(count, i);
                copy[i] = stack.copy();
            }
            return copy;
        }

        private static FluidStack[] copyFluidInputs(FluidStack[] stacks) {
            if (stacks == null) return new FluidStack[0];
            if (READING.get()) return copyFluids(stacks);
            FluidStack[] copy = new FluidStack[stacks.length];
            for (int i = 0; i < stacks.length; i++) {
                FluidStack stack = stacks[i];
                if (stack == null || stack.getFluid() == Fluids.EMPTY)
                    throw new IllegalArgumentException("Fluid input " + i + " is empty");
                RecipeKeptInputs.requirePositiveFluid(stack.getAmount(), i);
                copy[i] = stack.copy();
            }
            return copy;
        }

        private static ItemStack[] copyItems(ItemStack[] stacks) {
            if (stacks == null) return new ItemStack[0];
            return Arrays.stream(stacks).map(stack -> stack == null ? ItemStack.EMPTY : stack.copy()).toArray(ItemStack[]::new);
        }

        private static FluidStack[] copyFluids(FluidStack[] stacks) {
            if (stacks == null) return new FluidStack[0];
            return Arrays.stream(stacks).map(stack -> stack == null || stack.isEmpty() ? FluidStack.EMPTY : stack.copy()).toArray(FluidStack[]::new);
        }
    }
}
