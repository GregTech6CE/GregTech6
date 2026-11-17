package gregtech.engine.recipe;

import gregtech.engine.stack.StackCompat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

import java.util.Arrays;

/** Builds normalized machine recipes from item and fluid stacks. */
public final class RecipeBuilder {
    private final long energyPerTick;
    private final long duration;
    private ItemStack[] itemInputs = RecipeMap.ZL_IS;
    private ItemStack[] itemOutputs = RecipeMap.ZL_IS;
    private FluidStack[] fluidInputs = RecipeMap.ZL_FS;
    private FluidStack[] fluidOutputs = RecipeMap.ZL_FS;
    private long[] chances;
    private long specialValue;
    private boolean optimize;

    private RecipeBuilder(long energyPerTick, long duration) {
        this.energyPerTick = energyPerTick;
        this.duration = duration;
    }

    public static RecipeBuilder of(long energyPerTick, long duration) {
        return new RecipeBuilder(energyPerTick, duration);
    }

    public RecipeBuilder itemInputs(ItemStack... inputs) {
        itemInputs = inputs == null ? RecipeMap.ZL_IS : inputs;
        return this;
    }

    public RecipeBuilder itemOutputs(ItemStack... outputs) {
        itemOutputs = outputs == null ? RecipeMap.ZL_IS : outputs;
        return this;
    }

    public RecipeBuilder fluidInputs(FluidStack... inputs) {
        fluidInputs = inputs == null ? RecipeMap.ZL_FS : inputs;
        return this;
    }

    public RecipeBuilder fluidOutputs(FluidStack... outputs) {
        fluidOutputs = outputs == null ? RecipeMap.ZL_FS : outputs;
        return this;
    }

    /** Output chances in hundredths of a percent. Nonpositive values use the guaranteed default. */
    public RecipeBuilder chances(long... chances) {
        this.chances = chances;
        return this;
    }

    public RecipeBuilder specialValue(long specialValue) {
        this.specialValue = specialValue;
        return this;
    }

    public RecipeBuilder optimize(boolean optimize) {
        this.optimize = optimize;
        return this;
    }

    /** Returns null if a required input is missing or the recipe has no valid result. */
    public Recipe build() {
        return buildResult().recipe();
    }

    /** A collapsed row had usable inputs and outputs before optimization. */
    record BuildResult(Recipe recipe, boolean collapsed) {}

    BuildResult buildResult() {
        long ticks = duration;
        // Skip recipe if any declared item input failed to resolve (empty stack).
        for (ItemStack s : itemInputs) if (empty(s)) return new BuildResult(null, false);
        // Copy inputs.
        ItemStack[] inputs = new ItemStack[itemInputs.length];
        for (int i = 0; i < itemInputs.length; i++) inputs[i] = itemInputs[i].copy();
        // Copy outputs, trimming trailing empties but keeping interior empties (chance alignment).
        int outLen = itemOutputs.length;
        while (outLen > 0 && empty(itemOutputs[outLen - 1])) outLen--;
        ItemStack[] outputs = new ItemStack[outLen];
        for (int i = 0; i < outLen; i++) outputs[i] = empty(itemOutputs[i]) ? ItemStack.EMPTY : itemOutputs[i].copy();
        // Native zero-sized stacks have no usable identity. Presence gates use Recipe.withPresenceFluidInputs.
        FluidStack[] normalizedFluidInputs = copyFluidInputs(fluidInputs);
        FluidStack[] normalizedFluidOutputs = copyFluids(fluidOutputs);
        // Fuel may produce energy alone (original Blaze hot fuel). Ordinary work still needs a product.
        if (inputs.length + normalizedFluidInputs.length <= 0) return new BuildResult(null, false);
        int realOutputs = normalizedFluidOutputs.length;
        for (ItemStack s : outputs) if (!empty(s)) realOutputs++;
        if (realOutputs <= 0 && energyPerTick >= 0) return new BuildResult(null, false);
        // Align chance array with output array.
        long[] normalizedChances = chances == null || chances.length == 0
                ? null : Arrays.copyOf(chances, outputs.length);

        if (optimize && (normalizedChances == null || allDefault(normalizedChances))) {
            long factor = Math.max(1, ticks / 16);
            // Cancel identical items appearing on both sides.
            for (ItemStack in : inputs) {
                if (in.isEmpty()) continue;
                for (int j = 0; j < outputs.length; j++) {
                    ItemStack out = outputs[j];
                    if (!out.isEmpty() && StackCompat.sameItem(in, out)) {
                        if (in.getCount() >= out.getCount()) {
                            in.shrink(out.getCount());
                            outputs[j] = ItemStack.EMPTY;
                        } else {
                            out.shrink(in.getCount());
                            in.setCount(0);
                        }
                    }
                }
            }
            for (ItemStack s : inputs) if (s.getCount() != 0) factor = Math.min(factor, s.getCount());
            for (ItemStack s : outputs) if (s.getCount() != 0) factor = Math.min(factor, s.getCount());
            for (FluidStack f : normalizedFluidInputs) if (f.getAmount() != 0) factor = Math.min(factor, f.getAmount());
            for (FluidStack f : normalizedFluidOutputs) if (f.getAmount() != 0) factor = Math.min(factor, f.getAmount());
            // Divide the whole recipe by the largest evenly-dividing factor.
            for (; factor > 1; factor--) {
                boolean dividesEvenly = true;
                for (ItemStack s : inputs) if (s.getCount() % factor != 0) {dividesEvenly = false; break;}
                if (dividesEvenly) for (ItemStack s : outputs) if (!s.isEmpty() && s.getCount() % factor != 0) {dividesEvenly = false; break;}
                if (dividesEvenly) for (FluidStack f : normalizedFluidInputs) if (f.getAmount() % factor != 0) {dividesEvenly = false; break;}
                if (dividesEvenly) for (FluidStack f : normalizedFluidOutputs) if (f.getAmount() % factor != 0) {dividesEvenly = false; break;}
                if (dividesEvenly) {
                    for (ItemStack s : inputs) s.setCount(s.getCount() / (int) factor);
                    for (ItemStack s : outputs) if (!s.isEmpty()) s.setCount(s.getCount() / (int) factor);
                    for (FluidStack f : normalizedFluidInputs) f.setAmount(f.getAmount() / (int) factor);
                    for (FluidStack f : normalizedFluidOutputs) f.setAmount(f.getAmount() / (int) factor);
                    ticks /= factor;
                    break;
                }
            }
        }
        // Remove input slots emptied by optimization. Drop an unproductive row so a mold
        // left behind by cancellation does not match and reject unrelated recipes.
        int keptInputs = 0;
        for (ItemStack s : inputs) if (!empty(s)) keptInputs++;
        if (keptInputs != inputs.length) {
            ItemStack[] compacted = new ItemStack[keptInputs];
            int next = 0;
            for (ItemStack s : inputs) if (!empty(s)) compacted[next++] = s;
            inputs = compacted;
        }
        int realOutputsAfterOptimize = normalizedFluidOutputs.length;
        for (ItemStack s : outputs) if (!empty(s)) realOutputsAfterOptimize++;
        if (inputs.length + normalizedFluidInputs.length <= 0 || realOutputsAfterOptimize <= 0 && energyPerTick >= 0) {
            RecipeMap.recordCollapsedRecipe(itemInputs, itemOutputs);
            return new BuildResult(null, true);
        }

        for (ItemStack s : inputs) if (s.getCount() > 64) s.setCount(64);
        for (ItemStack s : outputs) if (!s.isEmpty() && s.getCount() > 64) s.setCount(64);

        Recipe recipe = new Recipe(inputs, outputs, normalizedChances, normalizedFluidInputs, normalizedFluidOutputs,
                ticks, energyPerTick, specialValue);
        return new BuildResult(recipe, false);
    }

    private static boolean allDefault(long[] chances) {
        for (long c : chances) if (c > 0 && c < 10000) return false;
        return true;
    }

    private static FluidStack[] copyFluids(FluidStack[] aFluids) {
        int n = 0;
        for (FluidStack f : aFluids) if (f != null && !f.isEmpty()) n++;
        if (n == 0) return RecipeMap.ZL_FS;
        FluidStack[] r = new FluidStack[n];
        int i = 0;
        for (FluidStack f : aFluids) if (f != null && !f.isEmpty()) r[i++] = f.copy();
        return r;
    }

    /** Copy positive native representatives; legacy empty placeholders are dropped. */
    private static FluidStack[] copyFluidInputs(FluidStack[] aFluids) {
        int n = 0;
        for (FluidStack f : aFluids) if (f != null && f.getFluid() != Fluids.EMPTY) n++;
        if (n == 0) return RecipeMap.ZL_FS;
        FluidStack[] r = new FluidStack[n];
        int i = 0;
        for (FluidStack f : aFluids) if (f != null && f.getFluid() != Fluids.EMPTY) r[i++] = f.copy();
        return r;
    }

    private static boolean empty(ItemStack stack) {
        return stack == null || stack.isEmpty();
    }
}
