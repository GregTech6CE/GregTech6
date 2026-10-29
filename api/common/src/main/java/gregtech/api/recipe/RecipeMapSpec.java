package gregtech.api.recipe;

import gregtech.api.addon.AddonApiRemoval;

import java.util.Collection;

/**
 * Single original machine-map identity, slot/tank limits and presentation flags.
 *
 * <p>This is the host-facing model for built-in recipe maps. Addon declarations use
 * {@link gregtech.api.addon.recipe.RecipeTable} instead; this type is planned to move back
 * into the host, so new addon code should not depend on it.</p>
 */
public final class RecipeMapSpec {
    public final String mNameInternal, mNameLocal, mGUIPath;
    public final int mInputItemsCount, mOutputItemsCount, mMinimalInputItems,
            mInputFluidCount, mOutputFluidCount, mMinimalInputFluids, mMinimalInputs;
    public final boolean mNeedsOutputs, mCombinePower, mUseBucketSizeIn, mUseBucketSizeOut;
    public String mSpecialValuePre = "", mSpecialValuePost = "";
    public long mSpecialValueMultiplier;
    public boolean mInstantRecipes;

    /** Original NEI category eligibility and tier/amperage captions, used by both modern viewers. */
    public boolean mViewerAllowed = true, mShowVoltageAmperage = true;

    /**
     * @deprecated {@code aRecipeList} is ignored; use the overload without it.
     * Removal is 0.5.
     */
    @Deprecated(since = "0.3", forRemoval = true)
    @AddonApiRemoval(id = "gregtech.api.recipe.RecipeMapSpec#RecipeMapSpec", removeAt = "0.5")
    public RecipeMapSpec(Collection<?> aRecipeList,
                         String aNameInternal, String aNameLocal, String aGUIPath,
                         int aInputItemsCount, int aOutputItemsCount, int aMinimalInputItems,
                         int aInputFluidCount, int aOutputFluidCount, int aMinimalInputFluids,
                         int aMinimalInputs,
                         boolean aNeedsOutputs, boolean aCombinePower,
                         boolean aUseBucketSizeIn, boolean aUseBucketSizeOut) {
        this(aNameInternal, aNameLocal, aGUIPath,
                aInputItemsCount, aOutputItemsCount, aMinimalInputItems,
                aInputFluidCount, aOutputFluidCount, aMinimalInputFluids,
                aMinimalInputs, aNeedsOutputs, aCombinePower, aUseBucketSizeIn, aUseBucketSizeOut);
    }

    /**
     * Creates a spec without the ignored recipe-list argument.
     *
     * <p>When {@code aMinimalInputItems} exceeds {@code aInputItemsCount}, the stored input slot
     * count is widened to the minimum instead of being rejected; the same applies to
     * {@code aMinimalInputFluids} and {@code aInputFluidCount}. This is the original GT6
     * behaviour and is kept for existing maps.</p>
     */
    public RecipeMapSpec(String aNameInternal, String aNameLocal, String aGUIPath,
                         int aInputItemsCount, int aOutputItemsCount, int aMinimalInputItems,
                         int aInputFluidCount, int aOutputFluidCount, int aMinimalInputFluids,
                         int aMinimalInputs,
                         boolean aNeedsOutputs, boolean aCombinePower,
                         boolean aUseBucketSizeIn, boolean aUseBucketSizeOut) {
        mNameInternal = aNameInternal;
        mNameLocal = aNameLocal;
        mGUIPath = aGUIPath.endsWith(".png") ? aGUIPath : aGUIPath + ".png";
        mInputItemsCount   = Math.max(aInputItemsCount, aMinimalInputItems);
        mOutputItemsCount  = aOutputItemsCount;
        mMinimalInputItems = aMinimalInputItems;
        mInputFluidCount   = Math.max(aInputFluidCount, aMinimalInputFluids);
        mOutputFluidCount  = aOutputFluidCount;
        mMinimalInputFluids = aMinimalInputFluids;
        mMinimalInputs     = aMinimalInputs;
        mNeedsOutputs      = aNeedsOutputs;
        mCombinePower      = aCombinePower;
        mUseBucketSizeIn   = aUseBucketSizeIn;
        mUseBucketSizeOut  = aUseBucketSizeOut;
    }

    public RecipeMapSpec specialValueLabel(String prefix, long multiplier, String suffix) {
        mSpecialValuePre = prefix == null ? "" : prefix;
        mSpecialValueMultiplier = multiplier;
        mSpecialValuePost = suffix == null ? "" : suffix;
        return this;
    }

    public RecipeMapSpec viewer(boolean allowed, boolean showVoltageAmperage) {
        mViewerAllowed = allowed;
        mShowVoltageAmperage = showVoltageAmperage;
        return this;
    }

    public RecipeMapSpec instantRecipes() {
        mInstantRecipes = true;
        return this;
    }
}
