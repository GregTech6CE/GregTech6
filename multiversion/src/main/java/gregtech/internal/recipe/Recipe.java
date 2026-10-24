package gregtech.internal.recipe;

import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

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
        for (int i = 0; i < mOutputs.length; i++) if (mOutputs[i] != null)
            mOutputs[i] = gregtech.content.block.misc.LegacyConstructionBlockItem.canonical(mOutputs[i]);
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
        if (!viewerInputAlternatives.containsKey(index))
            return gregtech.content.block.misc.LegacyConstructionBlockItem.ingredientAlternatives(mInputs[index]);
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
        boolean marked = catalystInputs.get(index);
        boolean itemCatalyst = !marked && !mExplicitCatalystsOnly
                && mInputs[index].getItem() instanceof RecipeCatalystLike tool && tool.isCatalyst();
        return RecipeCatalystInputs.isCatalyst(mExplicitCatalystsOnly, marked, itemCatalyst);
    }

    /** Indexes passed to {@link #withCatalystInputs(int...)}, in ascending order. Implicit catalyst items are not listed. */
    public int[] explicitCatalystInputs() {
        int[] indices = new int[catalystInputs.cardinality()];
        int cursor = 0;
        for (int bit = catalystInputs.nextSetBit(0); bit >= 0; bit = catalystInputs.nextSetBit(bit + 1))
            indices[cursor++] = bit;
        return indices;
    }

    private final java.util.BitSet catalystFluidInputs = new java.util.BitSet();

    /** Mark fluid inputs that are checked at their declared amount and not drained. */
    public Recipe withCatalystFluidInputs(int... indices) {
        for (int index : indices) {
            if (index < 0 || index >= mFluidInputs.length) throw new IllegalArgumentException("Catalyst fluid input " + index);
            if (presenceFluidInputs.get(index)) throw new IllegalArgumentException("Fluid input " + index + " is presence-only");
            catalystFluidInputs.set(index);
        }
        return this;
    }

    public boolean isCatalystFluidInput(int index) {
        return index >= 0 && index < mFluidInputs.length && catalystFluidInputs.get(index);
    }

    /** Indexes passed to {@link #withCatalystFluidInputs(int...)}, in ascending order. */
    public int[] explicitCatalystFluidInputs() {
        int[] indices = new int[catalystFluidInputs.cardinality()];
        int cursor = 0;
        for (int bit = catalystFluidInputs.nextSetBit(0); bit >= 0; bit = catalystFluidInputs.nextSetBit(bit + 1))
            indices[cursor++] = bit;
        return indices;
    }

    private final java.util.BitSet presenceFluidInputs = new java.util.BitSet();

    /** Presence-only gates use positive native representatives, never zero-sized native stacks. */
    public Recipe withPresenceFluidInputs(int... indices) {
        for (int index : gregtech.api.addon.RecipeKeptInputs.normalizeFluids(indices, mFluidInputs.length)) {
            if (mFluidInputs[index] == null || mFluidInputs[index].isEmpty())
                throw new IllegalArgumentException("Presence fluid input " + index + " needs a positive representative");
            if (catalystFluidInputs.get(index))
                throw new IllegalArgumentException("Fluid input " + index + " cannot be both kept and presence-only");
            mFluidInputs[index] = mFluidInputs[index].copy();
            mFluidInputs[index].setAmount(1);
            presenceFluidInputs.set(index);
        }
        return this;
    }

    public boolean isPresenceFluidInput(int index) {
        return index >= 0 && index < mFluidInputs.length && presenceFluidInputs.get(index);
    }

    public int[] explicitPresenceFluidInputs() {
        return presenceFluidInputs.stream().toArray();
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
