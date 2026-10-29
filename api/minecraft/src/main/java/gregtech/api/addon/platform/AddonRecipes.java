/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

import gregtech.api.addon.recipe.RecipeKeptInputs;
import gregtech.api.addon.recipe.RecipeOutputChances;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/** Handles for machine recipe maps that already exist, including maps an addon declared. */
public final class AddonRecipes {
    private AddonRecipes() {}

    /** The named map. An unknown name throws {@link IllegalArgumentException}. */
    public static RecipeMap map(String name) {
        RecipeMap map = AddonLoader.services().map(name);
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
        @Nullable Line find(List<ItemStack> items, List<FluidStack> fluids);
    }

    /** One machine recipe map. Named {@code RecipeMap} so it does not read as {@link java.util.Map}. */
    public interface RecipeMap {
        String name();

        /** Adds a static row. Null means the map rejected it. */
        @Nullable Line add(Line line);

        /** A display row for JEI and EMI. Machines do not craft it. Null means the map rejected it. */
        @Nullable Line addDisplay(Line line);

        /** An existing static row whose inputs this line would cover, or null. */
        @Nullable Line collision(Line line);

        /** True when some static row already consumes this item. */
        boolean hasItemInput(ItemStack stack);

        /** Appends a provider behind the built-in one. A null provider is rejected. */
        void addDynamic(Dynamic provider);

        /**
         * The first matching row, including dynamic providers. Null when nothing matches.
         * This query does not consume or modify the supplied stacks or lists.
         */
        @Nullable Line find(List<ItemStack> items, List<FluidStack> fluids, boolean needsOutputs,
                            int inputSlots, int outputSlots);
    }

    /**
     * An immutable recipe snapshot. Construction and array accessors copy arrays and their stacks;
     * changing a supplied or returned object never changes this row.
     * {@code keptInputs} lists item-input indexes the machine checks for and does not consume.
     * {@code keptFluidInputs} does the same for fluid-input indexes: the declared amount is checked and not drained.
     * {@code presenceFluidInputs} checks only that the original tanks contain that fluid, without reserving any amount.
     * Use positive native representatives for presence gates; native zero-sized stacks lose their fluid identity.
     * Null marks nothing. Kept and presence indexes must not overlap.
     * <p>
     * Every constructor checks that each input amount is positive. {@link #read} is the only path that keeps an
     * empty slot from a row already stored. Array accessors return copies; changing a returned array or stack
     * does not change this row. A duration below one tick is stored as one, and a presence-only fluid input's
     * stored amount is rewritten to one; the presence gate only checks that the fluid is there.
     * <p>
     * New code should build rows through {@link #builder()}; the public constructors and {@code read}
     * overloads exist for the host's own recipe readers.
     */
    @NullUnmarked
    public static final class Line {
        private final ItemStack[] itemInputs;
        private final ItemStack[] itemOutputs;
        private final FluidStack[] fluidInputs;
        private final FluidStack[] fluidOutputs;
        private final long duration;
        private final long euPerTick;
        private final long special;
        private final int[] outputChances;
        private final int[] keptInputs;
        private final int[] keptFluidInputs;
        private final int[] presenceFluidInputs;

        /** Named construction with the same validation and defaults as the public constructors. */
        public static Builder builder() {
            return new Builder();
        }

        /** Detached copies, including each stack's native item data. */
        public ItemStack[] itemInputs() { return copyItems(itemInputs); }

        /** Detached copies, including each stack's native item data. */
        public ItemStack[] itemOutputs() { return copyItems(itemOutputs); }

        /** Detached copies, including each stack's native fluid data. */
        public FluidStack[] fluidInputs() { return copyFluids(fluidInputs); }

        /** Detached copies, including each stack's native fluid data. */
        public FluidStack[] fluidOutputs() { return copyFluids(fluidOutputs); }

        public long duration() { return duration; }
        public long euPerTick() { return euPerTick; }
        public long special() { return special; }

        public int[] outputChances() { return outputChances.clone(); }
        public int[] keptInputs() { return keptInputs.clone(); }
        public int[] keptFluidInputs() { return keptFluidInputs.clone(); }
        public int[] presenceFluidInputs() { return presenceFluidInputs.clone(); }

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
            return new Line(true, itemInputs, itemOutputs, fluidInputs, fluidOutputs, duration, euPerTick, special,
                    outputChances, keptInputs, keptFluidInputs, presenceFluidInputs);
        }

        /** Every item output is certain. No input is marked kept. */
        public Line(ItemStack[] itemInputs, ItemStack[] itemOutputs, FluidStack[] fluidInputs, FluidStack[] fluidOutputs,
                    long duration, long euPerTick, long special) {
            this(false, itemInputs, itemOutputs, fluidInputs, fluidOutputs, duration, euPerTick, special, null, null, null, null);
        }

        /** No input is marked kept. */
        public Line(ItemStack[] itemInputs, ItemStack[] itemOutputs, FluidStack[] fluidInputs, FluidStack[] fluidOutputs,
                    long duration, long euPerTick, long special, int[] outputChances) {
            this(false, itemInputs, itemOutputs, fluidInputs, fluidOutputs, duration, euPerTick, special, outputChances, null, null, null);
        }

        /** No fluid input is marked kept. */
        public Line(ItemStack[] itemInputs, ItemStack[] itemOutputs, FluidStack[] fluidInputs, FluidStack[] fluidOutputs,
                    long duration, long euPerTick, long special, int[] outputChances, int[] keptInputs) {
            this(false, itemInputs, itemOutputs, fluidInputs, fluidOutputs, duration, euPerTick, special,
                    outputChances, keptInputs, null, null);
        }

        /** Compatibility overload: positive kept amounts, no presence-only gates. */
        public Line(ItemStack[] itemInputs, ItemStack[] itemOutputs, FluidStack[] fluidInputs, FluidStack[] fluidOutputs,
                    long duration, long euPerTick, long special, int[] outputChances, int[] keptInputs, int[] keptFluidInputs) {
            this(false, itemInputs, itemOutputs, fluidInputs, fluidOutputs, duration, euPerTick, special,
                    outputChances, keptInputs, keptFluidInputs, null);
        }

        /** Positive input amounts, including presence-only gates. */
        public Line(ItemStack[] itemInputs, ItemStack[] itemOutputs, FluidStack[] fluidInputs, FluidStack[] fluidOutputs,
                    long duration, long euPerTick, long special, int[] outputChances, int[] keptInputs,
                    int[] keptFluidInputs, int[] presenceFluidInputs) {
            this(false, itemInputs, itemOutputs, fluidInputs, fluidOutputs, duration, euPerTick, special,
                    outputChances, keptInputs, keptFluidInputs, presenceFluidInputs);
        }

        private Line(boolean reading, ItemStack[] itemInputs, ItemStack[] itemOutputs, FluidStack[] fluidInputs,
                     FluidStack[] fluidOutputs, long duration, long euPerTick, long special, int[] outputChances,
                     int[] keptInputs, int[] keptFluidInputs, int[] presenceFluidInputs) {
            this.itemInputs = reading ? copyItems(itemInputs) : copyItemInputs(itemInputs);
            this.itemOutputs = copyItems(itemOutputs);
            this.fluidInputs = reading ? copyFluids(fluidInputs) : copyFluidInputs(fluidInputs);
            this.fluidOutputs = copyFluids(fluidOutputs);
            this.duration = Math.max(1, duration);
            this.euPerTick = euPerTick;
            this.special = special;
            this.outputChances = RecipeOutputChances.normalize(outputChances, this.itemOutputs.length);
            this.keptInputs = RecipeKeptInputs.normalize(keptInputs, this.itemInputs.length);
            this.keptFluidInputs = RecipeKeptInputs.normalizeFluids(keptFluidInputs, this.fluidInputs.length);
            this.presenceFluidInputs = RecipeKeptInputs.normalizeFluids(presenceFluidInputs, this.fluidInputs.length);
            for (int index : this.presenceFluidInputs) {
                if (this.fluidInputs[index].isEmpty()) throw new IllegalArgumentException("Presence fluid input " + index + " is empty");
                for (int kept : this.keptFluidInputs) if (kept == index)
                    throw new IllegalArgumentException("Fluid input " + index + " cannot be both kept and presence-only");
                this.fluidInputs[index].setAmount(1);
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

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Line line)) return false;
            return sameItems(itemInputs, line.itemInputs)
                    && sameItems(itemOutputs, line.itemOutputs)
                    && sameFluids(fluidInputs, line.fluidInputs)
                    && sameFluids(fluidOutputs, line.fluidOutputs)
                    && duration == line.duration
                    && euPerTick == line.euPerTick
                    && special == line.special
                    && Arrays.equals(outputChances, line.outputChances)
                    && Arrays.equals(keptInputs, line.keptInputs)
                    && Arrays.equals(keptFluidInputs, line.keptFluidInputs)
                    && Arrays.equals(presenceFluidInputs, line.presenceFluidInputs);
        }

        @Override public int hashCode() {
            int result = hashItems(itemInputs);
            result = 31 * result + hashItems(itemOutputs);
            result = 31 * result + hashFluids(fluidInputs);
            result = 31 * result + hashFluids(fluidOutputs);
            result = 31 * result + Long.hashCode(duration);
            result = 31 * result + Long.hashCode(euPerTick);
            result = 31 * result + Long.hashCode(special);
            result = 31 * result + Arrays.hashCode(outputChances);
            result = 31 * result + Arrays.hashCode(keptInputs);
            result = 31 * result + Arrays.hashCode(keptFluidInputs);
            return 31 * result + Arrays.hashCode(presenceFluidInputs);
        }

        private static boolean sameItems(ItemStack[] left, ItemStack[] right) {
            if (left.length != right.length) return false;
            for (int i = 0; i < left.length; i++) {
                if (!ItemStack.matches(left[i], right[i])) return false;
            }
            return true;
        }

        private static boolean sameFluids(FluidStack[] left, FluidStack[] right) {
            if (left.length != right.length) return false;
            for (int i = 0; i < left.length; i++) {
//? if >=1.21.1 {
                /*if (!FluidStack.matches(left[i], right[i])) return false;
*///?} else {
                if (!left[i].isFluidStackIdentical(right[i])) return false;
//?}
            }
            return true;
        }

        private static int hashItems(ItemStack[] stacks) {
            int hash = 1;
            for (ItemStack stack : stacks) {
                hash = 31 * hash + (stack == null || stack.isEmpty() ? 0 : itemHash(stack));
            }
            return hash;
        }

        private static int itemHash(ItemStack stack) {
//? if >=1.21.1 {
            /*int hash = ItemStack.hashItemAndComponents(stack);
            return 31 * hash + stack.getCount();
*///?} else {
            return Objects.hash(stack.getItem(), stack.getCount(), stack.getTag());
//?}
        }

        private static int hashFluids(FluidStack[] stacks) {
            int hash = 1;
            for (FluidStack stack : stacks) {
                hash = 31 * hash + (stack == null || stack.isEmpty() ? 0 : fluidHash(stack));
            }
            return hash;
        }

        private static int fluidHash(FluidStack stack) {
//? if >=1.21.1 {
            /*int hash = FluidStack.hashFluidAndComponents(stack);
            return 31 * hash + stack.getAmount();
*///?} else {
            int hash = 31 * stack.getFluid().hashCode()
                    + (stack.getTag() == null ? 0 : stack.getTag().hashCode());
            return 31 * hash + stack.getAmount();
//?}
        }

        @Override public String toString() {
            return "Line[itemInputs=" + Arrays.toString(itemInputs)
                    + ", itemOutputs=" + Arrays.toString(itemOutputs)
                    + ", fluidInputs=" + Arrays.toString(fluidInputs)
                    + ", fluidOutputs=" + Arrays.toString(fluidOutputs)
                    + ", duration=" + duration
                    + ", euPerTick=" + euPerTick
                    + ", special=" + special
                    + ", outputChances=" + Arrays.toString(outputChances)
                    + ", keptInputs=" + Arrays.toString(keptInputs)
                    + ", keptFluidInputs=" + Arrays.toString(keptFluidInputs)
                    + ", presenceFluidInputs=" + Arrays.toString(presenceFluidInputs) + "]";
        }

        private static ItemStack[] copyItemInputs(ItemStack[] stacks) {
            if (stacks == null) return new ItemStack[0];
            ItemStack[] copy = new ItemStack[stacks.length];
            for (int i = 0; i < stacks.length; i++) {
                ItemStack stack = stacks[i];
                if (stack == null)
                    throw new IllegalArgumentException("Item input " + i + " is null");
                if (stack.isEmpty())
                    throw new IllegalArgumentException("Item input " + i + " is empty");
                RecipeKeptInputs.requirePositiveCount(stack.getCount(), i);
                copy[i] = stack.copy();
            }
            return copy;
        }

        private static FluidStack[] copyFluidInputs(FluidStack[] stacks) {
            if (stacks == null) return new FluidStack[0];
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
