package com.gregtech.gregtech.api.addon.platform;

import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

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

    public record Line(
            ItemStack[] itemInputs,
            ItemStack[] itemOutputs,
            FluidStack[] fluidInputs,
            FluidStack[] fluidOutputs,
            long duration,
            long euPerTick,
            long special,
            int[] outputChances
    ) {
        /** Every item output is certain. */
        public Line(ItemStack[] itemInputs, ItemStack[] itemOutputs, FluidStack[] fluidInputs, FluidStack[] fluidOutputs,
                    long duration, long euPerTick, long special) {
            this(itemInputs, itemOutputs, fluidInputs, fluidOutputs, duration, euPerTick, special, null);
        }

        public Line {
            itemInputs = copyItems(itemInputs);
            itemOutputs = copyItems(itemOutputs);
            fluidInputs = copyFluids(fluidInputs);
            fluidOutputs = copyFluids(fluidOutputs);
            duration = Math.max(1, duration);
            outputChances = RecipeOutputChances.normalize(outputChances, itemOutputs.length);
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
