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
        /** The matching row, or null when this provider does not handle the input. */
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

        /** The first matching row, including dynamic providers. Null when nothing matches. */
        Line find(List<ItemStack> items, List<FluidStack> fluids, boolean needsOutputs, int inputSlots, int outputSlots);
    }

    /**
     * One recipe row. {@code keptInputs} lists item-input indexes the machine checks for and does not consume.
     * {@code keptFluidInputs} does the same for fluid-input indexes: the declared amount is checked and not drained.
     * Null marks nothing. A count of 0, or 0 mB, is rejected; leave the amount positive and name the index here.
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
            int[] keptFluidInputs
    ) {
        private static final ThreadLocal<Boolean> READING = ThreadLocal.withInitial(() -> Boolean.FALSE);

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
            READING.set(Boolean.TRUE);
            try {
                return new Line(itemInputs, itemOutputs, fluidInputs, fluidOutputs, duration, euPerTick, special,
                        outputChances, keptInputs, keptFluidInputs);
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

        public Line {
            itemInputs = copyItemInputs(itemInputs);
            itemOutputs = copyItems(itemOutputs);
            fluidInputs = copyFluidInputs(fluidInputs);
            fluidOutputs = copyFluids(fluidOutputs);
            duration = Math.max(1, duration);
            outputChances = RecipeOutputChances.normalize(outputChances, itemOutputs.length);
            keptInputs = RecipeKeptInputs.normalize(keptInputs, itemInputs.length);
            keptFluidInputs = RecipeKeptInputs.normalizeFluids(keptFluidInputs, fluidInputs.length);
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
