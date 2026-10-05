package com.gregtech.gregtech.api.recipe;
import java.util.Collection;
/** Single original machine-map identity, slot/tank limits and presentation flags. */
public final class RecipeMapSpec {
    public final String mNameInternal,mNameLocal,mGUIPath;
    public final int mInputItemsCount,mOutputItemsCount,mMinimalInputItems,mInputFluidCount,mOutputFluidCount,mMinimalInputFluids,mMinimalInputs;
    public final boolean mNeedsOutputs,mCombinePower,mUseBucketSizeIn,mUseBucketSizeOut;
    public String mSpecialValuePre="",mSpecialValuePost="";
    public long mSpecialValueMultiplier;
    public boolean mInstantRecipes;
    /** Original NEI category eligibility and tier/amperage captions, used by both modern viewers. */
    public boolean mViewerAllowed=true, mShowVoltageAmperage=true;
    public RecipeMapSpec(Collection<?> aRecipeList,
                     String aNameInternal, String aNameLocal, String aGUIPath,
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
    public RecipeMapSpec specialValueLabel(String prefix,long multiplier,String suffix) {
        mSpecialValuePre=prefix==null?"":prefix;mSpecialValueMultiplier=multiplier;mSpecialValuePost=suffix==null?"":suffix;return this;
    }
    public RecipeMapSpec viewer(boolean allowed, boolean showVoltageAmperage) {
        mViewerAllowed=allowed;mShowVoltageAmperage=showVoltageAmperage;return this;
    }
    public RecipeMapSpec instantRecipes() { mInstantRecipes=true;return this; }
}
