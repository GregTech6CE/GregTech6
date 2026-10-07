package com.gregtech.gregtech.api.recipe;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * GT6 recipe data container.
 *
 * <p>1.20.1 port of {@code gregapi.recipes.Recipe}. Holds input/output items
 * and fluids, processing duration, energy cost, and output chances.</p>
 */
public class Recipe {
    public final ItemStack[] mInputs;
    public final ItemStack[] mOutputs;
    public final FluidStack[] mFluidInputs;
    public final FluidStack[] mFluidOutputs;
    public final long[] mChances;
    public final long[] mMaxChances;
    public final long mDuration;
    public final long mEUt;
    public final long mSpecialValue;
    public final Object mSpecialItems;

    public boolean mEnabled = true;
    public boolean mHidden;
    public boolean mFakeRecipe;
    public boolean mCanBeBuffered = true;
    public boolean mNeedsEmptyOutput;
    public boolean mMaterialRecovery;
    public boolean mRequiresEmptyContainerInputs;
    /** Blueprint recipes require exact stack data and only their explicit retained inputs. */
    public boolean mExactItemInputs, mExplicitCatalystsOnly;
    public Recipe withMaterialRecovery() { mMaterialRecovery = true; mRequiresEmptyContainerInputs = true; return this; }
    public Recipe withEmptyContainerInputs() { mRequiresEmptyContainerInputs = true; return this; }

    public Recipe(ItemStack[] aInputs, ItemStack[] aOutputs, Object aSpecialItems,
                  long[] aChances, FluidStack[] aFluidInputs, FluidStack[] aFluidOutputs,
                  long aDuration, long aEUt, long aSpecialValue) {
        mInputs       = aInputs      != null ? aInputs      : new ItemStack[0];
        mOutputs      = aOutputs     != null ? aOutputs     : new ItemStack[0];
        mFluidInputs  = aFluidInputs != null ? aFluidInputs : new FluidStack[0];
        mFluidOutputs = aFluidOutputs!= null ? aFluidOutputs: new FluidStack[0];
        mChances      = aChances     != null ? aChances     : new long[mOutputs.length];
        mMaxChances   = new long[mChances.length];
        for (int i = 0; i < mChances.length; i++) {
            if (mChances[i] <= 0) mChances[i] = 10000;
            mMaxChances[i] = 10000;
        }
        mDuration     = Math.max(1, aDuration);
        mEUt          = aEUt;
        mSpecialValue = aSpecialValue;
        mSpecialItems = aSpecialItems;
    }

    private final java.util.BitSet catalystInputs = new java.util.BitSet();

    private final java.util.Map<Integer, java.util.List<ItemStack>> viewerInputAlternatives = new java.util.HashMap<>();
    /** Informational choices for one displayed slot; this never broadens machine matching. */
    public Recipe withViewerInputAlternatives(int index, java.util.List<ItemStack> alternatives) {
        if (index < 0 || index >= mInputs.length || alternatives.isEmpty()) throw new IllegalArgumentException("Viewer input " + index);
        viewerInputAlternatives.put(index, alternatives.stream().map(ItemStack::copy).toList());
        return this;
    }
    public java.util.List<ItemStack> viewerInputAlternatives(int index) {
        return viewerInputAlternatives.getOrDefault(index, java.util.List.of()).stream().map(ItemStack::copy).toList();
    }

    /** Mark non-consumable lenses/etc. before publishing this recipe to a map. */
    public Recipe withCatalystInputs(int... indices) {
        for (int index : indices) {
            if (index < 0 || index >= mInputs.length) throw new IllegalArgumentException("Catalyst input " + index);
            catalystInputs.set(index);
        }
        return this;
    }
    public boolean isCatalystInput(int index) {
        return catalystInputs.get(index) || !mExplicitCatalystsOnly && mInputs[index].getItem() instanceof com.gregtech.gregtech.api.recipe.RecipeCatalystLike tool && tool.isCatalyst();
    }

    // ── Output helpers ───────────────────────────────────────────────────

    public int getOutputChance(int idx) {
        if (idx < 0 || idx >= mChances.length) return 10000;
        return (int) mChances[idx];
    }

    /** GT6 rolls each output item independently, including every parallel operation. */
    public long rollOutputCount(int index, int processes, java.util.function.IntUnaryOperator random) {
        if (index < 0 || index >= mOutputs.length || processes <= 0) return 0;
        ItemStack output = mOutputs[index];
        if (output == null || output.isEmpty()) return 0;
        return RecipeChanceRules.rollOutputCount(output.getCount(),getOutputChance(index),processes,random);
    }

    public ItemStack getOutput(int idx) {
        if (idx < 0 || idx >= mOutputs.length) return ItemStack.EMPTY;
        ItemStack s = mOutputs[idx];
        return s != null ? s.copy() : ItemStack.EMPTY;
    }

    public FluidStack getFluidOutput(int idx) {
        if (idx < 0 || idx >= mFluidOutputs.length) return FluidStack.EMPTY;
        FluidStack f = mFluidOutputs[idx];
        return f != null ? f.copy() : FluidStack.EMPTY;
    }

    public FluidStack getRepresentativeFluidInput(int idx) {
        if (idx < 0 || idx >= mFluidInputs.length) return FluidStack.EMPTY;
        FluidStack f = mFluidInputs[idx];
        return f != null ? f.copy() : FluidStack.EMPTY;
    }
}
