package gregtech.content.recipe;

/**
 * Machine-map identity, slot and tank limits, behavior, and presentation.
 * Addon declarations use {@link gregtech.api.addon.recipe.RecipeTable}.
 */
public final class RecipeMapSpec {
    public final String mNameInternal, mNameLocal, mGUIPath;
    public final int mInputItemsCount,
            mOutputItemsCount,
            mMinimalInputItems,
            mInputFluidCount,
            mOutputFluidCount,
            mMinimalInputFluids,
            mMinimalInputs;
    public final boolean mNeedsOutputs, mCombinePower, mUseBucketSizeIn, mUseBucketSizeOut;
    public final String mSpecialValuePre, mSpecialValuePost;
    public final long mSpecialValueMultiplier;
    public final boolean mInstantRecipes;
    public final int mInputPotionCount;
    public final boolean mViewerAllowed, mShowVoltageAmperage;

    private RecipeMapSpec(Builder builder) {
        mNameInternal = builder.name;
        mNameLocal = builder.title;
        mGUIPath = builder.gui.endsWith(".png") ? builder.gui : builder.gui + ".png";
        mInputItemsCount = Math.max(builder.inputItems, builder.minimumInputItems);
        mOutputItemsCount = builder.outputItems;
        mMinimalInputItems = builder.minimumInputItems;
        mInputFluidCount = Math.max(builder.inputFluids, builder.minimumInputFluids);
        mOutputFluidCount = builder.outputFluids;
        mMinimalInputFluids = builder.minimumInputFluids;
        mMinimalInputs = builder.minimumInputs;
        mNeedsOutputs = builder.needsOutputs;
        mCombinePower = builder.combinePower;
        mUseBucketSizeIn = builder.bucketInput;
        mUseBucketSizeOut = builder.bucketOutput;
        mSpecialValuePre = builder.specialValuePrefix;
        mSpecialValuePost = builder.specialValueSuffix;
        mSpecialValueMultiplier = builder.specialValueMultiplier;
        mInstantRecipes = builder.instantRecipes;
        mInputPotionCount = builder.potionInputs;
        mViewerAllowed = builder.viewerAllowed;
        mShowVoltageAmperage = builder.showVoltageAmperage;
    }

    public static Builder builder(String name, String title, String gui) {
        return new Builder(name, title, gui);
    }

    public static final class Builder {
        private final String name, title, gui;
        private int inputItems, outputItems, minimumInputItems;
        private int inputFluids, outputFluids, minimumInputFluids, minimumInputs;
        private boolean needsOutputs, combinePower, bucketInput, bucketOutput;
        private String specialValuePrefix = "", specialValueSuffix = "";
        private long specialValueMultiplier;
        private boolean instantRecipes;
        private int potionInputs;
        private boolean viewerAllowed = true, showVoltageAmperage = true;

        private Builder(String name, String title, String gui) {
            this.name = name;
            this.title = title;
            this.gui = gui;
        }

        /** The minimum can widen the input capacity to preserve the original map layout. */
        public Builder items(int inputs, int outputs, int minimum) {
            inputItems = inputs;
            outputItems = outputs;
            minimumInputItems = minimum;
            return this;
        }

        /** The minimum can widen the input tank count to preserve the original map layout. */
        public Builder fluids(int inputs, int outputs, int minimum) {
            inputFluids = inputs;
            outputFluids = outputs;
            minimumInputFluids = minimum;
            return this;
        }

        public Builder minimumInputs(int count) {
            minimumInputs = count;
            return this;
        }

        public Builder needsOutputs(boolean required) {
            needsOutputs = required;
            return this;
        }

        public Builder combinePower(boolean combine) {
            combinePower = combine;
            return this;
        }

        public Builder bucketInput(boolean enabled) {
            bucketInput = enabled;
            return this;
        }

        public Builder bucketOutput(boolean enabled) {
            bucketOutput = enabled;
            return this;
        }

        public Builder specialValueLabel(String prefix, long multiplier, String suffix) {
            specialValuePrefix = prefix == null ? "" : prefix;
            specialValueMultiplier = multiplier;
            specialValueSuffix = suffix == null ? "" : suffix;
            return this;
        }

        public Builder viewerAllowed(boolean allowed) {
            viewerAllowed = allowed;
            return this;
        }

        public Builder showVoltageAmperage(boolean show) {
            showVoltageAmperage = show;
            return this;
        }

        public Builder instantRecipes() {
            instantRecipes = true;
            return this;
        }

        public Builder potionInputs(int count) {
            potionInputs = Math.max(0, count);
            return this;
        }

        public RecipeMapSpec build() {
            return new RecipeMapSpec(this);
        }
    }
}
