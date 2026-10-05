package com.gregtech.gregtech.api.recipe;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.slf4j.Logger;

import java.util.*;

/**
 * GT6 RecipeMap — recipe registry keyed by unique internal name.
 * Each machine type binds to one RecipeMap which defines its slot/tank counts.
 *
 * <p>1.20.1 port of {@code gregapi.recipes.Recipe.RecipeMap}. GT6 subclasses that override
 * {@code findRecipe()} to build a row from the <em>contents</em> of the inputs (the scanner, the
 * printer and the replicator read the NBT of a USB stick) are ported through
 * {@link #dynamicRecipes(DynamicRecipes)}: the port keeps the table-driven lookup and asks the
 * provider only when no static row matches.</p>
 */
public class RecipeMap {

    /**
     * GT6's subclass {@code findRecipe()} overrides: a row computed from the actual input stacks,
     * used for recipes whose inputs only differ by NBT (e.g. the material data a scanner wrote onto a
     * USB stick). Called on every failed lookup, so implementations must be cheap or cache.
     */
    @FunctionalInterface
    public interface DynamicRecipes {
        /** The matching row, or null when the inputs carry no such data. */
        @javax.annotation.Nullable
        Recipe find(List<ItemStack> items, List<FluidStack> fluids);
    }

    @javax.annotation.Nullable
    private DynamicRecipes mDynamicRecipes;

    /** Original findRecipe overrides that also need the machine world and special slot. */
    @FunctionalInterface
    public interface ContextRecipes {
        Recipe find(net.minecraft.world.level.Level level,
                net.minecraft.world.level.block.entity.BlockEntity machine, ItemStack special,
                List<ItemStack> items, List<FluidStack> fluids);
    }
    private ContextRecipes mContextRecipes;
    public RecipeMap contextualRecipes(ContextRecipes provider) { mContextRecipes = provider; return this; }


    /**
     * Installs GT6's per-map {@code findRecipe()} override. One provider per map (GT6 has one
     * subclass per map); calling it again replaces the previous provider.
     */
    public RecipeMap dynamicRecipes(DynamicRecipes provider) {
        mDynamicRecipes = provider;
        return this;
    }

    /** Whether this map computes rows from input NBT (reported by the start-up log line). */
    public boolean hasDynamicRecipes() { return mDynamicRecipes != null || mContextRecipes != null; }
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Map<String, RecipeMap> RECIPE_MAPS = new LinkedHashMap<>();
    public static final List<RecipeMap> RECIPE_MAP_LIST = new ArrayList<>();

    /**
     * Rows {@link #make} dropped because optimizing cancelled them down to nothing (GT6 keeps such a
     * row; see the note in {@code make}). Reported by {@code StoneExtrusionTests} as evidence that no
     * degenerate row reaches a recipe map.
     */
    public static int COLLAPSED_RECIPES_DROPPED = 0;

    private static final List<String> COLLAPSED_SAMPLES = new ArrayList<>();

    private static final Map<String, Integer> COLLAPSED_BY_MAP = new LinkedHashMap<>();

    /** Set by {@link #make} when it drops a collapsed row and consumed by the next {@link #addRecipe} call. */
    private static boolean tLastMakeCollapsed = false;

    /** The first few dropped rows, for the start-up log line. */
    public static List<String> collapsedSamples() {
        return List.copyOf(COLLAPSED_SAMPLES);
    }

    /** Which recipe map the dropped rows came from, for the start-up log line. */
    public static Map<String, Integer> collapsedByMap() {
        return Map.copyOf(COLLAPSED_BY_MAP);
    }

    /** Zero-length convenience constants (GT6 {@code CS.ZL_IS} / {@code CS.ZL_FS} / {@code CS.ZL_LONG}). */
    public static final ItemStack[] ZL_IS = new ItemStack[0];
    public static final FluidStack[] ZL_FS = new FluidStack[0];
    public static final long[] ZL_LONG = new long[0];

    // ── Slot / tank counts (set by constructor, immutable) ──────────────
    public final int mInputItemsCount, mOutputItemsCount;
    public final int mInputFluidCount, mOutputFluidCount;
    public final int mMinimalInputItems, mMinimalInputFluids, mMinimalInputs;

    // ── Identity ─────────────────────────────────────────────────────────
    public final String mNameInternal;
    public final String mNameLocal;
    public final String mGUIPath;

    // ── Recipe storage ───────────────────────────────────────────────────
    public final Collection<Recipe> mRecipeList;
    /** Index: input {@link Item} -> recipes using it as an input. */
    public final Map<Item, Collection<Recipe>> mRecipeItemMap = new HashMap<>();
    public final Map<String, Collection<Recipe>> mRecipeFluidMap = new HashMap<>();

    /** Minimum tank size per fluid key, populated when recipes are added. */
    public final Map<String, Long> mMinInputTankSizes = new HashMap<>();

    /** Largest single fluid input amount seen so far (min 1000). */
    public int mMaxFluidInputSize = 1000;
    /** Largest single fluid output amount seen so far (min 1000). */
    public int mMaxFluidOutputSize = 1000;

    /** Whether {@link #addRecipe(Recipe)} logs errors by default. */
    public boolean mLogErrors = true;

    // ── Convenience flags ────────────────────────────────────────────────
    /** GT6 recipe power divisor; all current non-crafting maps use one. */
    public long mPower = 1;
    /** Original NEI eligibility and caption flags; recipe lookup/storage remain independent. */
    public boolean mViewerAllowed=true, mShowVoltageAmperage=true;
    public RecipeMap viewer(boolean allowed,boolean showVoltageAmperage) {
        mViewerAllowed=allowed;mShowVoltageAmperage=showVoltageAmperage;return this;
    }
    public final boolean mNeedsOutputs;
    public final boolean mCombinePower;
    public final boolean mUseBucketSizeIn;
    public final boolean mUseBucketSizeOut;

    // ── NEI/JEI special value labels (GT6 aNEISpecialValuePre/Multiplier/Post) ──
    /**
     * Prefix of the recipe viewer's "special value" line, e.g. {@code "Temperature: "} for the
     * crucible maps, whose {@link Recipe#mSpecialValue} is a temperature in Kelvin
     * ({@code RM.CrucibleAlloying} / {@code RM.CrucibleSmelting} in the original). An empty
     * prefix means the map has no special value line.
     */
    public String mSpecialValuePre = "";
    /** Multiplier applied to {@link Recipe#mSpecialValue} before display (GT6 uses 1). */
    public long mSpecialValueMultiplier = 0;
    /** Suffix of the special value line, e.g. {@code " K"}. */
    public String mSpecialValuePost = "";

    /** Declares the recipe viewer's special value line; see {@link #mSpecialValuePre}. */
    public RecipeMap specialValueLabel(String aPre, long aMultiplier, String aPost) {
        mSpecialValuePre = aPre == null ? "" : aPre;
        mSpecialValueMultiplier = aMultiplier;
        mSpecialValuePost = aPost == null ? "" : aPost;
        return this;
    }

    /** Whether this map shows a special value line for its recipes. */
    public boolean hasSpecialValueLabel() {
        return !mSpecialValuePre.isEmpty() && mSpecialValueMultiplier != 0;
    }

    /**
     * Recipes of this map finish instantly, so the viewer must not print a duration.
     * <p>
     * GT6's crucibles convert the moment the input reaches its melting point
     * ({@code TileEntityBase08Crucible}: the conversion is a state change, not a timed machine
     * process), which is why "Time: 1 tick" on the crucible recipe pages is meaningless.
     * </p>
     */
    public boolean mInstantRecipes = false;

    /** Marks the map as instant; see {@link #mInstantRecipes}. */
    public RecipeMap instantRecipes() {
        mInstantRecipes = true;
        return this;
    }

    /**
     * Full constructor matching the original GT6 parameter list.
     *
     * @param aRecipeList         backing recipe collection (null = use HashSet)
     * @param aNameInternal       unique registry key, e.g. {@code "gt.recipe.crusher"}
     * @param aNameLocal          human-readable display name
     * @param aGUIPath            GUI texture path (auto-appends {@code .png} if missing)
     * @param aInputItemsCount    max input item slots
     * @param aOutputItemsCount   max output item slots
     * @param aMinimalInputItems  minimum non-empty input slots required
     * @param aInputFluidCount    max input fluid tanks
     * @param aOutputFluidCount   max output fluid tanks
     * @param aMinimalInputFluids minimum non-empty fluid inputs required
     * @param aMinimalInputs      minimum total inputs (items + fluids) required
     * @param aNeedsOutputs       recipes require output slots to be available
     * @param aCombinePower       combine EU/t from parallel recipes
     * @param aUseBucketSizeIn    input fluids use bucket-based scaling (144 * n)
     * @param aUseBucketSizeOut   output fluids use bucket-based scaling
     */
    public RecipeMap(Collection<Recipe> aRecipeList,
                     String aNameInternal, String aNameLocal, String aGUIPath,
                     int aInputItemsCount, int aOutputItemsCount, int aMinimalInputItems,
                     int aInputFluidCount, int aOutputFluidCount, int aMinimalInputFluids,
                     int aMinimalInputs,
                     boolean aNeedsOutputs, boolean aCombinePower,
                     boolean aUseBucketSizeIn, boolean aUseBucketSizeOut) {
        mRecipeList = (aRecipeList != null) ? aRecipeList : new HashSet<>();
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

        if (RECIPE_MAPS.containsKey(mNameInternal))
            throw new IllegalArgumentException("Duplicate RecipeMap key: " + mNameInternal);
        RECIPE_MAPS.put(mNameInternal, this);
        RECIPE_MAP_LIST.add(this);
    }

    @Override
    public String toString() { return mNameInternal; }

    // ── Recipe registration ──────────────────────────────────────────────

    /**
     * Add a single recipe to this map (default: collision check on, not fake, not hidden).
     * Returns the recipe on success or {@code null} if it was rejected/skipped.
     */
    public Recipe addRecipe(Recipe aRecipe) {
        return addRecipe(aRecipe, true, false, false, mLogErrors);
    }

    public Recipe addRecipe(Recipe aRecipe, boolean aCheckForCollisions, boolean aFakeRecipe, boolean aHidden) {
        return addRecipe(aRecipe, aCheckForCollisions, aFakeRecipe, aHidden, mLogErrors);
    }

    public Recipe addRecipe(Recipe aRecipe, boolean aCheckForCollisions, boolean aFakeRecipe, boolean aHidden, boolean aLogErrors) {
        if (aRecipe == null) {
            if (tLastMakeCollapsed) {
                tLastMakeCollapsed = false;
                COLLAPSED_BY_MAP.merge(mNameInternal, 1, Integer::sum);
            }
            return null;
        }
        for(int i=0;i<aRecipe.mOutputs.length;i++)
            aRecipe.mOutputs[i]=com.gregtech.gregtech.api.material.MaterialUnification.canonical(aRecipe.mOutputs[i]);
        aRecipe.mHidden = aHidden;
        aRecipe.mFakeRecipe = aFakeRecipe;
        if (!aFakeRecipe && !validate(aRecipe, aLogErrors && mLogErrors)) return null;
        if (aCheckForCollisions && findCollision(aRecipe) != null) return null;
        return add(aRecipe);
    }

    /** Raw GT6-style addRecipe taking all arrays at once. */
    public Recipe addRecipe(boolean aOptimize, ItemStack[] aInputs, ItemStack[] aOutputs, Object aSpecial, long[] aOutputChances, FluidStack[] aFluidInputs, FluidStack[] aFluidOutputs, long aDuration, long aEUt, long aSpecialValue) {
        return addRecipe(make(aOptimize, aInputs, aOutputs, aSpecial, aOutputChances, aFluidInputs, aFluidOutputs, aDuration, aEUt, aSpecialValue));
    }

    /** Fake recipes are only shown in recipe viewers; machines never match them. */
    public Recipe addFakeRecipe(boolean aCheckForCollisions, ItemStack[] aInputs, ItemStack[] aOutputs, Object aSpecial, long[] aOutputChances, FluidStack[] aFluidInputs, FluidStack[] aFluidOutputs, long aDuration, long aEUt, long aSpecialValue) {
        return addFakeRecipe(aCheckForCollisions, make(false, aInputs, aOutputs, aSpecial, aOutputChances, aFluidInputs, aFluidOutputs, aDuration, aEUt, aSpecialValue));
    }

    public Recipe addFakeRecipe(boolean aCheckForCollisions, ItemStack[] aInputs, ItemStack[] aOutputs, Object aSpecial, FluidStack[] aFluidInputs, FluidStack[] aFluidOutputs, long aDuration, long aEUt, long aSpecialValue) {
        return addFakeRecipe(aCheckForCollisions, make(false, aInputs, aOutputs, aSpecial, null, aFluidInputs, aFluidOutputs, aDuration, aEUt, aSpecialValue));
    }

    public Recipe addFakeRecipe(boolean aCheckForCollisions, Recipe aRecipe) {
        return addRecipe(aRecipe, aCheckForCollisions, true, false, mLogErrors);
    }

    // ── GT6 convenience overloads ────────────────────────────────────────
    // Argument order: (optimize, EU/t, duration in ticks, [chance(s)], inputs..., [fluids...], outputs...)
    // Chances are in 1/100 percent: 10000 = 100%. A chance <= 0 means 100%.

    // Items only
    public Recipe addRecipe1(boolean aOptimize, long aEUt, long aDuration, long   aChance , ItemStack aInput                                                                            , ItemStack    aOutput ) {return addRecipe(make(aOptimize, ar(aInput)         , ar(aOutput), null, new long[] {aChance}, ZL_FS         , ZL_FS          , aDuration, aEUt, 0));}
    public Recipe addRecipe2(boolean aOptimize, long aEUt, long aDuration, long   aChance , ItemStack aInput1, ItemStack aInput2                                                        , ItemStack    aOutput ) {return addRecipe(make(aOptimize, ar(aInput1, aInput2), ar(aOutput), null, new long[] {aChance}, ZL_FS         , ZL_FS          , aDuration, aEUt, 0));}
    public Recipe addRecipeX(boolean aOptimize, long aEUt, long aDuration, long   aChance , ItemStack[] aInputs                                                                         , ItemStack    aOutput ) {return addRecipe(make(aOptimize, aInputs            , ar(aOutput), null, new long[] {aChance}, ZL_FS         , ZL_FS          , aDuration, aEUt, 0));}
    public Recipe addRecipe1(boolean aOptimize, long aEUt, long aDuration                 , ItemStack aInput                                                                            , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ar(aInput)         , aOutputs   , null, ZL_LONG             , ZL_FS         , ZL_FS          , aDuration, aEUt, 0));}
    public Recipe addRecipe2(boolean aOptimize, long aEUt, long aDuration                 , ItemStack aInput1, ItemStack aInput2                                                        , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ar(aInput1, aInput2), aOutputs   , null, ZL_LONG             , ZL_FS         , ZL_FS          , aDuration, aEUt, 0));}
    public Recipe addRecipeX(boolean aOptimize, long aEUt, long aDuration                 , ItemStack[] aInputs                                                                         , ItemStack... aOutputs) {return addRecipe(make(aOptimize, aInputs            , aOutputs   , null, ZL_LONG             , ZL_FS         , ZL_FS          , aDuration, aEUt, 0));}
    public Recipe addRecipe1(boolean aOptimize, long aEUt, long aDuration, long[] aChances, ItemStack aInput                                                                            , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ar(aInput)         , aOutputs   , null, aChances            , ZL_FS         , ZL_FS          , aDuration, aEUt, 0));}
    public Recipe addRecipe2(boolean aOptimize, long aEUt, long aDuration, long[] aChances, ItemStack aInput1, ItemStack aInput2                                                        , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ar(aInput1, aInput2), aOutputs   , null, aChances            , ZL_FS         , ZL_FS          , aDuration, aEUt, 0));}
    public Recipe addRecipeX(boolean aOptimize, long aEUt, long aDuration, long[] aChances, ItemStack[] aInputs                                                                         , ItemStack... aOutputs) {return addRecipe(make(aOptimize, aInputs            , aOutputs   , null, aChances            , ZL_FS         , ZL_FS          , aDuration, aEUt, 0));}
    // Single fluid input + single fluid output
    public Recipe addRecipe0(boolean aOptimize, long aEUt, long aDuration, long   aChance                                       , FluidStack   aFluidInput , FluidStack    aFluidOutput , ItemStack    aOutput ) {return addRecipe(make(aOptimize, ZL_IS              , ar(aOutput), null, new long[] {aChance}, fl(aFluidInput), fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipe1(boolean aOptimize, long aEUt, long aDuration, long   aChance , ItemStack aInput                    , FluidStack   aFluidInput , FluidStack    aFluidOutput , ItemStack    aOutput ) {return addRecipe(make(aOptimize, ar(aInput)         , ar(aOutput), null, new long[] {aChance}, fl(aFluidInput), fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipe2(boolean aOptimize, long aEUt, long aDuration, long   aChance , ItemStack aInput1, ItemStack aInput2, FluidStack   aFluidInput , FluidStack    aFluidOutput , ItemStack    aOutput ) {return addRecipe(make(aOptimize, ar(aInput1, aInput2), ar(aOutput), null, new long[] {aChance}, fl(aFluidInput), fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipeX(boolean aOptimize, long aEUt, long aDuration, long   aChance , ItemStack[] aInputs                 , FluidStack   aFluidInput , FluidStack    aFluidOutput , ItemStack    aOutput ) {return addRecipe(make(aOptimize, aInputs            , ar(aOutput), null, new long[] {aChance}, fl(aFluidInput), fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipe0(boolean aOptimize, long aEUt, long aDuration                                                       , FluidStack   aFluidInput , FluidStack    aFluidOutput , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ZL_IS              , aOutputs   , null, ZL_LONG             , fl(aFluidInput), fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipe1(boolean aOptimize, long aEUt, long aDuration                 , ItemStack aInput                    , FluidStack   aFluidInput , FluidStack    aFluidOutput , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ar(aInput)         , aOutputs   , null, ZL_LONG             , fl(aFluidInput), fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipe2(boolean aOptimize, long aEUt, long aDuration                 , ItemStack aInput1, ItemStack aInput2, FluidStack   aFluidInput , FluidStack    aFluidOutput , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ar(aInput1, aInput2), aOutputs   , null, ZL_LONG             , fl(aFluidInput), fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipeX(boolean aOptimize, long aEUt, long aDuration                 , ItemStack[] aInputs                 , FluidStack   aFluidInput , FluidStack    aFluidOutput , ItemStack... aOutputs) {return addRecipe(make(aOptimize, aInputs            , aOutputs   , null, ZL_LONG             , fl(aFluidInput), fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipe0(boolean aOptimize, long aEUt, long aDuration, long[] aChances                                      , FluidStack   aFluidInput , FluidStack    aFluidOutput , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ZL_IS              , aOutputs   , null, aChances            , fl(aFluidInput), fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipe1(boolean aOptimize, long aEUt, long aDuration, long[] aChances, ItemStack aInput                    , FluidStack   aFluidInput , FluidStack    aFluidOutput , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ar(aInput)         , aOutputs   , null, aChances            , fl(aFluidInput), fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipe2(boolean aOptimize, long aEUt, long aDuration, long[] aChances, ItemStack aInput1, ItemStack aInput2, FluidStack   aFluidInput , FluidStack    aFluidOutput , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ar(aInput1, aInput2), aOutputs   , null, aChances            , fl(aFluidInput), fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipeX(boolean aOptimize, long aEUt, long aDuration, long[] aChances, ItemStack[] aInputs                 , FluidStack   aFluidInput , FluidStack    aFluidOutput , ItemStack... aOutputs) {return addRecipe(make(aOptimize, aInputs            , aOutputs   , null, aChances            , fl(aFluidInput), fl(aFluidOutput), aDuration, aEUt, 0));}
    // Single fluid input + multiple fluid outputs
    public Recipe addRecipe0(boolean aOptimize, long aEUt, long aDuration                                                       , FluidStack   aFluidInput , FluidStack... aFluidOutputs                       ) {return addRecipe(make(aOptimize, ZL_IS              , ZL_IS      , null, ZL_LONG             , fl(aFluidInput), aFluidOutputs  , aDuration, aEUt, 0));}
    public Recipe addRecipe1(boolean aOptimize, long aEUt, long aDuration                 , ItemStack aInput                    , FluidStack   aFluidInput , FluidStack... aFluidOutputs                       ) {return addRecipe(make(aOptimize, ar(aInput)         , ZL_IS      , null, ZL_LONG             , fl(aFluidInput), aFluidOutputs  , aDuration, aEUt, 0));}
    public Recipe addRecipe2(boolean aOptimize, long aEUt, long aDuration                 , ItemStack aInput1, ItemStack aInput2, FluidStack   aFluidInput , FluidStack... aFluidOutputs                       ) {return addRecipe(make(aOptimize, ar(aInput1, aInput2), ZL_IS      , null, ZL_LONG             , fl(aFluidInput), aFluidOutputs  , aDuration, aEUt, 0));}
    public Recipe addRecipeX(boolean aOptimize, long aEUt, long aDuration                 , ItemStack[] aInputs                 , FluidStack   aFluidInput , FluidStack... aFluidOutputs                       ) {return addRecipe(make(aOptimize, aInputs            , ZL_IS      , null, ZL_LONG             , fl(aFluidInput), aFluidOutputs  , aDuration, aEUt, 0));}
    // Multiple fluid inputs + single fluid output
    public Recipe addRecipe0(boolean aOptimize, long aEUt, long aDuration, long   aChance                                       , FluidStack[] aFluidInputs, FluidStack    aFluidOutput , ItemStack    aOutput ) {return addRecipe(make(aOptimize, ZL_IS              , ar(aOutput), null, new long[] {aChance}, aFluidInputs   , fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipe1(boolean aOptimize, long aEUt, long aDuration, long   aChance , ItemStack aInput                    , FluidStack[] aFluidInputs, FluidStack    aFluidOutput , ItemStack    aOutput ) {return addRecipe(make(aOptimize, ar(aInput)         , ar(aOutput), null, new long[] {aChance}, aFluidInputs   , fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipe2(boolean aOptimize, long aEUt, long aDuration, long   aChance , ItemStack aInput1, ItemStack aInput2, FluidStack[] aFluidInputs, FluidStack    aFluidOutput , ItemStack    aOutput ) {return addRecipe(make(aOptimize, ar(aInput1, aInput2), ar(aOutput), null, new long[] {aChance}, aFluidInputs   , fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipeX(boolean aOptimize, long aEUt, long aDuration, long   aChance , ItemStack[] aInputs                 , FluidStack[] aFluidInputs, FluidStack    aFluidOutput , ItemStack    aOutput ) {return addRecipe(make(aOptimize, aInputs            , ar(aOutput), null, new long[] {aChance}, aFluidInputs   , fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipe0(boolean aOptimize, long aEUt, long aDuration                                                       , FluidStack[] aFluidInputs, FluidStack    aFluidOutput , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ZL_IS              , aOutputs   , null, ZL_LONG             , aFluidInputs   , fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipe1(boolean aOptimize, long aEUt, long aDuration                 , ItemStack aInput                    , FluidStack[] aFluidInputs, FluidStack    aFluidOutput , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ar(aInput)         , aOutputs   , null, ZL_LONG             , aFluidInputs   , fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipe2(boolean aOptimize, long aEUt, long aDuration                 , ItemStack aInput1, ItemStack aInput2, FluidStack[] aFluidInputs, FluidStack    aFluidOutput , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ar(aInput1, aInput2), aOutputs   , null, ZL_LONG             , aFluidInputs   , fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipeX(boolean aOptimize, long aEUt, long aDuration                 , ItemStack[] aInputs                 , FluidStack[] aFluidInputs, FluidStack    aFluidOutput , ItemStack... aOutputs) {return addRecipe(make(aOptimize, aInputs            , aOutputs   , null, ZL_LONG             , aFluidInputs   , fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipe0(boolean aOptimize, long aEUt, long aDuration, long[] aChances                                      , FluidStack[] aFluidInputs, FluidStack    aFluidOutput , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ZL_IS              , aOutputs   , null, aChances            , aFluidInputs   , fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipe1(boolean aOptimize, long aEUt, long aDuration, long[] aChances, ItemStack aInput                    , FluidStack[] aFluidInputs, FluidStack    aFluidOutput , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ar(aInput)         , aOutputs   , null, aChances            , aFluidInputs   , fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipe2(boolean aOptimize, long aEUt, long aDuration, long[] aChances, ItemStack aInput1, ItemStack aInput2, FluidStack[] aFluidInputs, FluidStack    aFluidOutput , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ar(aInput1, aInput2), aOutputs   , null, aChances            , aFluidInputs   , fl(aFluidOutput), aDuration, aEUt, 0));}
    public Recipe addRecipeX(boolean aOptimize, long aEUt, long aDuration, long[] aChances, ItemStack[] aInputs                 , FluidStack[] aFluidInputs, FluidStack    aFluidOutput , ItemStack... aOutputs) {return addRecipe(make(aOptimize, aInputs            , aOutputs   , null, aChances            , aFluidInputs   , fl(aFluidOutput), aDuration, aEUt, 0));}
    // Multiple fluid inputs + multiple fluid outputs
    public Recipe addRecipe0(boolean aOptimize, long aEUt, long aDuration                                                       , FluidStack[] aFluidInputs, FluidStack... aFluidOutputs                       ) {return addRecipe(make(aOptimize, ZL_IS              , ZL_IS      , null, ZL_LONG             , aFluidInputs   , aFluidOutputs  , aDuration, aEUt, 0));}
    public Recipe addRecipe1(boolean aOptimize, long aEUt, long aDuration                 , ItemStack aInput                    , FluidStack[] aFluidInputs, FluidStack... aFluidOutputs                       ) {return addRecipe(make(aOptimize, ar(aInput)         , ZL_IS      , null, ZL_LONG             , aFluidInputs   , aFluidOutputs  , aDuration, aEUt, 0));}
    public Recipe addRecipe2(boolean aOptimize, long aEUt, long aDuration                 , ItemStack aInput1, ItemStack aInput2, FluidStack[] aFluidInputs, FluidStack... aFluidOutputs                       ) {return addRecipe(make(aOptimize, ar(aInput1, aInput2), ZL_IS      , null, ZL_LONG             , aFluidInputs   , aFluidOutputs  , aDuration, aEUt, 0));}
    public Recipe addRecipeX(boolean aOptimize, long aEUt, long aDuration                 , ItemStack[] aInputs                 , FluidStack[] aFluidInputs, FluidStack... aFluidOutputs                       ) {return addRecipe(make(aOptimize, aInputs            , ZL_IS      , null, ZL_LONG             , aFluidInputs   , aFluidOutputs  , aDuration, aEUt, 0));}
    public Recipe addRecipe0(boolean aOptimize, long aEUt, long aDuration, long   aChance                                       , FluidStack[] aFluidInputs, FluidStack[]  aFluidOutputs, ItemStack    aOutput ) {return addRecipe(make(aOptimize, ZL_IS              , ar(aOutput), null, new long[] {aChance}, aFluidInputs   , aFluidOutputs  , aDuration, aEUt, 0));}
    public Recipe addRecipe1(boolean aOptimize, long aEUt, long aDuration, long   aChance , ItemStack aInput                    , FluidStack[] aFluidInputs, FluidStack[]  aFluidOutputs, ItemStack    aOutput ) {return addRecipe(make(aOptimize, ar(aInput)         , ar(aOutput), null, new long[] {aChance}, aFluidInputs   , aFluidOutputs  , aDuration, aEUt, 0));}
    public Recipe addRecipe2(boolean aOptimize, long aEUt, long aDuration, long   aChance , ItemStack aInput1, ItemStack aInput2, FluidStack[] aFluidInputs, FluidStack[]  aFluidOutputs, ItemStack    aOutput ) {return addRecipe(make(aOptimize, ar(aInput1, aInput2), ar(aOutput), null, new long[] {aChance}, aFluidInputs   , aFluidOutputs  , aDuration, aEUt, 0));}
    public Recipe addRecipeX(boolean aOptimize, long aEUt, long aDuration, long   aChance , ItemStack[] aInputs                 , FluidStack[] aFluidInputs, FluidStack[]  aFluidOutputs, ItemStack    aOutput ) {return addRecipe(make(aOptimize, aInputs            , ar(aOutput), null, new long[] {aChance}, aFluidInputs   , aFluidOutputs  , aDuration, aEUt, 0));}
    public Recipe addRecipe0(boolean aOptimize, long aEUt, long aDuration                                                       , FluidStack[] aFluidInputs, FluidStack[]  aFluidOutputs, ItemStack... aOutputs) {return addRecipe(make(aOptimize, ZL_IS              , aOutputs   , null, ZL_LONG             , aFluidInputs   , aFluidOutputs  , aDuration, aEUt, 0));}
    public Recipe addRecipe1(boolean aOptimize, long aEUt, long aDuration                 , ItemStack aInput                    , FluidStack[] aFluidInputs, FluidStack[]  aFluidOutputs, ItemStack... aOutputs) {return addRecipe(make(aOptimize, ar(aInput)         , aOutputs   , null, ZL_LONG             , aFluidInputs   , aFluidOutputs  , aDuration, aEUt, 0));}
    public Recipe addRecipe2(boolean aOptimize, long aEUt, long aDuration                 , ItemStack aInput1, ItemStack aInput2, FluidStack[] aFluidInputs, FluidStack[]  aFluidOutputs, ItemStack... aOutputs) {return addRecipe(make(aOptimize, ar(aInput1, aInput2), aOutputs   , null, ZL_LONG             , aFluidInputs   , aFluidOutputs  , aDuration, aEUt, 0));}
    public Recipe addRecipeX(boolean aOptimize, long aEUt, long aDuration                 , ItemStack[] aInputs                 , FluidStack[] aFluidInputs, FluidStack[]  aFluidOutputs, ItemStack... aOutputs) {return addRecipe(make(aOptimize, aInputs            , aOutputs   , null, ZL_LONG             , aFluidInputs   , aFluidOutputs  , aDuration, aEUt, 0));}
    public Recipe addRecipe0(boolean aOptimize, long aEUt, long aDuration, long[] aChances                                      , FluidStack[] aFluidInputs, FluidStack[]  aFluidOutputs, ItemStack... aOutputs) {return addRecipe(make(aOptimize, ZL_IS              , aOutputs   , null, aChances            , aFluidInputs   , aFluidOutputs  , aDuration, aEUt, 0));}
    public Recipe addRecipe1(boolean aOptimize, long aEUt, long aDuration, long[] aChances, ItemStack aInput                    , FluidStack[] aFluidInputs, FluidStack[]  aFluidOutputs, ItemStack... aOutputs) {return addRecipe(make(aOptimize, ar(aInput)         , aOutputs   , null, aChances            , aFluidInputs   , aFluidOutputs  , aDuration, aEUt, 0));}
    public Recipe addRecipe2(boolean aOptimize, long aEUt, long aDuration, long[] aChances, ItemStack aInput1, ItemStack aInput2, FluidStack[] aFluidInputs, FluidStack[]  aFluidOutputs, ItemStack... aOutputs) {return addRecipe(make(aOptimize, ar(aInput1, aInput2), aOutputs   , null, aChances            , aFluidInputs   , aFluidOutputs  , aDuration, aEUt, 0));}
    public Recipe addRecipeX(boolean aOptimize, long aEUt, long aDuration, long[] aChances, ItemStack[] aInputs                 , FluidStack[] aFluidInputs, FluidStack[]  aFluidOutputs, ItemStack... aOutputs) {return addRecipe(make(aOptimize, aInputs            , aOutputs   , null, aChances            , aFluidInputs   , aFluidOutputs  , aDuration, aEUt, 0));}
    // Variants with explicit collision/fake/hidden/log flags
    public Recipe addRecipe1(boolean aOptimize, boolean aCheckForCollisions, boolean aFakeRecipe, boolean aHidden, boolean aLogErrors, long aEUt, long aDuration, ItemStack aInput                                                       , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ar(aInput)         , aOutputs, null, ZL_LONG, ZL_FS          , ZL_FS           , aDuration, aEUt, 0), aCheckForCollisions, aFakeRecipe, aHidden, aLogErrors);}
    public Recipe addRecipe2(boolean aOptimize, boolean aCheckForCollisions, boolean aFakeRecipe, boolean aHidden, boolean aLogErrors, long aEUt, long aDuration, ItemStack aInput1, ItemStack aInput2                                   , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ar(aInput1, aInput2), aOutputs, null, ZL_LONG, ZL_FS          , ZL_FS           , aDuration, aEUt, 0), aCheckForCollisions, aFakeRecipe, aHidden, aLogErrors);}
    public Recipe addRecipeX(boolean aOptimize, boolean aCheckForCollisions, boolean aFakeRecipe, boolean aHidden, boolean aLogErrors, long aEUt, long aDuration, ItemStack[] aInputs                                                    , ItemStack... aOutputs) {return addRecipe(make(aOptimize, aInputs            , aOutputs, null, ZL_LONG, ZL_FS          , ZL_FS           , aDuration, aEUt, 0), aCheckForCollisions, aFakeRecipe, aHidden, aLogErrors);}
    public Recipe addRecipe0(boolean aOptimize, boolean aCheckForCollisions, boolean aFakeRecipe, boolean aHidden, boolean aLogErrors, long aEUt, long aDuration                  , FluidStack aFluidInput, FluidStack aFluidOutput      , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ZL_IS              , aOutputs, null, ZL_LONG, fl(aFluidInput), fl(aFluidOutput), aDuration, aEUt, 0), aCheckForCollisions, aFakeRecipe, aHidden, aLogErrors);}
    public Recipe addRecipe1(boolean aOptimize, boolean aCheckForCollisions, boolean aFakeRecipe, boolean aHidden, boolean aLogErrors, long aEUt, long aDuration, ItemStack aInput, FluidStack aFluidInput, FluidStack aFluidOutput      , ItemStack... aOutputs) {return addRecipe(make(aOptimize, ar(aInput)         , aOutputs, null, ZL_LONG, fl(aFluidInput), fl(aFluidOutput), aDuration, aEUt, 0), aCheckForCollisions, aFakeRecipe, aHidden, aLogErrors);}

    // ── Recipe construction (normalisation + GT6 "optimize") ─────────────

    private static ItemStack[] ar(ItemStack a) {return new ItemStack[] {a};}
    private static ItemStack[] ar(ItemStack a, ItemStack b) {return new ItemStack[] {a, b};}
    private static FluidStack[] fl(FluidStack a) {return a == null || a.isEmpty() ? ZL_FS : new FluidStack[] {a};}

    private static boolean empty(ItemStack s) {return s == null || s.isEmpty();}

    /**
     * Builds a Recipe from the given (possibly sparse) arrays.
     * Mirrors the original GT6 Recipe constructor, with ItemStack.EMPTY treated like null:
     * <ul>
     * <li>Returns null if any declared item input is empty ("ONLY_IF_HAS_RESULT" semantics)</li>
     * <li>Returns null if there is no remaining input or no remaining output at all</li>
     * <li>Trims trailing empty outputs, drops empty fluids, copies all stacks</li>
     * <li>If aOptimize, cancels equal input/output items and divides the recipe by common factors</li>
     * <li>After optimizing, drops input slots cancelled down to nothing and returns null if the recipe
     * no longer has any real input or any real output</li>
     * </ul>
     */
    public static Recipe make(boolean aOptimize, ItemStack[] aInputs, ItemStack[] aOutputs, Object aSpecial,
                              long[] aChances, FluidStack[] aFluidInputs, FluidStack[] aFluidOutputs,
                              long aDuration, long aEUt, long aSpecialValue) {
        if (aInputs == null) aInputs = ZL_IS;
        if (aOutputs == null) aOutputs = ZL_IS;
        if (aFluidInputs == null) aFluidInputs = ZL_FS;
        if (aFluidOutputs == null) aFluidOutputs = ZL_FS;
        tLastMakeCollapsed = false;
        // Skip recipe if any declared item input failed to resolve (empty stack).
        for (ItemStack s : aInputs) if (empty(s)) return null;
        // Copy inputs.
        ItemStack[] tInputs = new ItemStack[aInputs.length];
        for (int i = 0; i < aInputs.length; i++) tInputs[i] = aInputs[i].copy();
        // Copy outputs, trimming trailing empties but keeping interior empties (chance alignment).
        int outLen = aOutputs.length;
        while (outLen > 0 && empty(aOutputs[outLen - 1])) outLen--;
        ItemStack[] tOutputs = new ItemStack[outLen];
        for (int i = 0; i < outLen; i++) tOutputs[i] = empty(aOutputs[i]) ? ItemStack.EMPTY : aOutputs[i].copy();
        // Copy fluids, dropping null/empty entries entirely.
        FluidStack[] tFluidInputs = copyFluids(aFluidInputs);
        FluidStack[] tFluidOutputs = copyFluids(aFluidOutputs);
        // Require at least one input and at least one output overall.
        if (tInputs.length + tFluidInputs.length <= 0) return null;
        int tRealOutputs = tFluidOutputs.length;
        for (ItemStack s : tOutputs) if (!empty(s)) tRealOutputs++;
        if (tRealOutputs <= 0) return null;
        // Align chance array with output array.
        long[] tChances = (aChances == null || aChances.length == 0) ? null : Arrays.copyOf(aChances, tOutputs.length);

        if (aOptimize && (tChances == null || allDefault(tChances))) {
            long l = Math.max(1, aDuration / 16);
            // Cancel identical items appearing on both sides.
            for (ItemStack in : tInputs) {
                if (in.isEmpty()) continue;
                for (int j = 0; j < tOutputs.length; j++) {
                    ItemStack out = tOutputs[j];
                    if (!out.isEmpty() && ItemStack.isSameItemSameTags(in, out)) {
                        if (in.getCount() >= out.getCount()) {
                            in.shrink(out.getCount());
                            tOutputs[j] = ItemStack.EMPTY;
                        } else {
                            out.shrink(in.getCount());
                            in.setCount(0);
                        }
                    }
                }
            }
            for (ItemStack s : tInputs) if (s.getCount() != 0) l = Math.min(l, s.getCount());
            for (ItemStack s : tOutputs) if (s.getCount() != 0) l = Math.min(l, s.getCount());
            for (FluidStack f : tFluidInputs) if (f.getAmount() != 0) l = Math.min(l, f.getAmount());
            for (FluidStack f : tFluidOutputs) if (f.getAmount() != 0) l = Math.min(l, f.getAmount());
            // Divide the whole recipe by the largest evenly-dividing factor.
            for (; l > 1; l--) {
                boolean ok = true;
                for (ItemStack s : tInputs) if (s.getCount() % l != 0) {ok = false; break;}
                if (ok) for (ItemStack s : tOutputs) if (!s.isEmpty() && s.getCount() % l != 0) {ok = false; break;}
                if (ok) for (FluidStack f : tFluidInputs) if (f.getAmount() % l != 0) {ok = false; break;}
                if (ok) for (FluidStack f : tFluidOutputs) if (f.getAmount() % l != 0) {ok = false; break;}
                if (ok) {
                    for (ItemStack s : tInputs) s.setCount(s.getCount() / (int) l);
                    for (ItemStack s : tOutputs) if (!s.isEmpty()) s.setCount(s.getCount() / (int) l);
                    for (FluidStack f : tFluidInputs) f.setAmount(f.getAmount() / (int) l);
                    for (FluidStack f : tFluidOutputs) f.setAmount(f.getAmount() / (int) l);
                    aDuration /= l;
                    break;
                }
            }
        }
        // The optimization above can cancel an input against an output of the same item and leave the
        // input slot at size 0 ("1 X + mould -> 1 X"). GT6 keeps such a collapsed recipe; the port drops
        // it and compacts emptied input slots away instead, because a recipe whose only input slot is
        // empty matches every later candidate sharing its mould and makes findCollision reject unrelated
        // recipes — the failure that broke the extruder table when the stone rows were first added.
        int tKeptInputs = 0;
        for (ItemStack s : tInputs) if (!empty(s)) tKeptInputs++;
        if (tKeptInputs != tInputs.length) {
            ItemStack[] tCompacted = new ItemStack[tKeptInputs];
            int tNext = 0;
            for (ItemStack s : tInputs) if (!empty(s)) tCompacted[tNext++] = s;
            tInputs = tCompacted;
        }
        int tRealOutputsAfterOptimize = tFluidOutputs.length;
        for (ItemStack s : tOutputs) if (!empty(s)) tRealOutputsAfterOptimize++;
        if (tInputs.length + tFluidInputs.length <= 0 || tRealOutputsAfterOptimize <= 0) {
            COLLAPSED_RECIPES_DROPPED++;
            tLastMakeCollapsed = true;
            if (COLLAPSED_SAMPLES.size() < 64) {
                COLLAPSED_SAMPLES.add(Arrays.toString(aInputs) + " -> " + Arrays.toString(aOutputs));
            }
            return null;
        }

        for (ItemStack s : tInputs) if (s.getCount() > 64) s.setCount(64);
        for (ItemStack s : tOutputs) if (!s.isEmpty() && s.getCount() > 64) s.setCount(64);

        return new Recipe(tInputs, tOutputs, aSpecial, tChances, tFluidInputs, tFluidOutputs, aDuration, aEUt, aSpecialValue);
    }

    private static boolean allDefault(long[] aChances) {
        for (long c : aChances) if (c > 0 && c < 10000) return false;
        return true;
    }

    private static FluidStack[] copyFluids(FluidStack[] aFluids) {
        int n = 0;
        for (FluidStack f : aFluids) if (f != null && !f.isEmpty()) n++;
        if (n == 0) return ZL_FS;
        FluidStack[] r = new FluidStack[n];
        int i = 0;
        for (FluidStack f : aFluids) if (f != null && !f.isEmpty()) r[i++] = f.copy();
        return r;
    }

    // ── Validation / collision / storage ─────────────────────────────────

    private boolean validate(Recipe aRecipe, boolean aLogErrors) {
        boolean tFailed = false;
        if (aRecipe.mInputs.length + aRecipe.mFluidInputs.length <= 0) {
            if (aLogErrors) LOGGER.error("[{}] Recipe has no Inputs!", mNameInternal);
            tFailed = true;
        } else {
            if (mNeedsOutputs && aRecipe.mOutputs.length + aRecipe.mFluidOutputs.length <= 0) {if (aLogErrors) LOGGER.error("[{}] Recipe has no Outputs!", mNameInternal); tFailed = true;}
            if (aRecipe.mInputs.length      < mMinimalInputItems)  {if (aLogErrors) LOGGER.error("[{}] Recipe has less than the minimal amount of Input ItemStacks!", mNameInternal); tFailed = true;}
            if (aRecipe.mFluidInputs.length < mMinimalInputFluids) {if (aLogErrors) LOGGER.error("[{}] Recipe has less than the minimal amount of Input FluidStacks!", mNameInternal); tFailed = true;}
            if (aRecipe.mInputs.length + aRecipe.mFluidInputs.length < mMinimalInputs) {if (aLogErrors) LOGGER.error("[{}] Recipe has less than the minimal amount of general Inputs!", mNameInternal); tFailed = true;}
            if (aRecipe.mInputs.length      > mInputItemsCount)    {if (aLogErrors) LOGGER.error("[{}] Recipe has more than the maximum amount of Input ItemStacks!", mNameInternal); tFailed = true;}
            if (aRecipe.mFluidInputs.length > mInputFluidCount)    {if (aLogErrors) LOGGER.error("[{}] Recipe has more than the maximum amount of Input FluidStacks!", mNameInternal); tFailed = true;}
        }
        return !tFailed;
    }

    /** Returns an already-registered recipe whose inputs would also be satisfied by the given recipe's inputs. */
    public Recipe findCollision(Recipe aRecipe) {
        Set<Recipe> tChecked = null;
        for (ItemStack in : aRecipe.mInputs) {
            if (empty(in)) continue;
            Collection<Recipe> tPool = mRecipeItemMap.get(in.getItem());
            if (tPool == null) continue;
            if (tChecked == null) tChecked = new HashSet<>();
            for (Recipe t : tPool) if (tChecked.add(t) && inputsSatisfiedBy(t, aRecipe)) return t;
        }
        for (FluidStack f : aRecipe.mFluidInputs) {
            if (f == null || f.isEmpty()) continue;
            Collection<Recipe> tPool = mRecipeFluidMap.get(fluidKey(f));
            if (tPool == null) continue;
            if (tChecked == null) tChecked = new HashSet<>();
            for (Recipe t : tPool) if (tChecked.add(t) && inputsSatisfiedBy(t, aRecipe)) return t;
        }
        return null;
    }

    /** Whether every input of {@code aExisting} is covered by the inputs of {@code aCandidate}. */
    private boolean inputsSatisfiedBy(Recipe aExisting, Recipe aCandidate) {
        if (aExisting.mFakeRecipe) return false;
        // In an anvil recipe an empty entry is a required empty work area, not a wildcard.
        if (mNameInternal.startsWith("gt.recipe.anvil")) {
            long requiredEmpty = java.util.Arrays.stream(aExisting.mInputs).filter(RecipeMap::empty).count();
            long candidateEmpty = java.util.Arrays.stream(aCandidate.mInputs).filter(RecipeMap::empty).count();
            if (requiredEmpty != candidateEmpty) return false;
        }
        var items = java.util.Arrays.stream(aCandidate.mInputs)
                .map(s -> empty(s) ? ItemStack.EMPTY : s.copy()).toArray(ItemStack[]::new);
        for (ItemStack in : aExisting.mInputs) {
            if (empty(in)) continue;
            int needed = in.getCount();
            for (ItemStack s : items) {
                if (!empty(s) && ItemStack.isSameItemSameTags(in, s)) {
                    int take = Math.min(needed, s.getCount());
                    s.shrink(take); needed -= take;
                    if (needed == 0) break;
                }
            }
            if (needed > 0) return false;
        }
        var fluids = java.util.Arrays.stream(aCandidate.mFluidInputs)
                .map(f -> f == null ? FluidStack.EMPTY : f.copy()).toArray(FluidStack[]::new);
        for (FluidStack in : aExisting.mFluidInputs) {
            if (in == null || in.isEmpty()) continue;
            int needed = in.getAmount();
            for (FluidStack f : fluids) {
                if (!f.isEmpty() && f.isFluidEqual(in)) {
                    int take = Math.min(needed, f.getAmount());
                    f.shrink(take); needed -= take;
                    if (needed == 0) break;
                }
            }
            if (needed > 0) return false;
        }
        return true;
    }

    private static String fluidKey(FluidStack f) {
        return f.getFluid().builtInRegistryHolder().key().location().toString();
    }

    /** Unconditionally stores the recipe and updates the lookup indices. */
    public synchronized Recipe add(Recipe aRecipe) {
        if (aRecipe == null) return null;
        mRecipeList.add(aRecipe);
        // Index by input items
        for (ItemStack in : aRecipe.mInputs) {
            if (empty(in)) continue;
            mRecipeItemMap.computeIfAbsent(in.getItem(), k -> new HashSet<>()).add(aRecipe);
        }
        // Index by input fluids
        for (FluidStack f : aRecipe.mFluidInputs) {
            if (f == null || f.isEmpty()) continue;
            String fKey = fluidKey(f);
            mRecipeFluidMap.computeIfAbsent(fKey, k -> new HashSet<>()).add(aRecipe);
            mMinInputTankSizes.merge(fKey, (long) f.getAmount(), Math::max);
            mMaxFluidInputSize = Math.max(mMaxFluidInputSize, f.getAmount());
        }
        for (FluidStack f : aRecipe.mFluidOutputs) {
            if (f != null) mMaxFluidOutputSize = Math.max(mMaxFluidOutputSize, f.getAmount());
        }
        return aRecipe;
    }

    /** Removes all recipes that use the given item as one of their inputs. */
    public boolean removeRecipesByInput(ItemStack aInput) {
        if (empty(aInput)) return false;
        Collection<Recipe> tPool = mRecipeItemMap.remove(aInput.getItem());
        if (tPool == null || tPool.isEmpty()) return false;
        mRecipeList.removeAll(tPool);
        for (Collection<Recipe> c : mRecipeItemMap.values()) c.removeAll(tPool);
        for (Collection<Recipe> c : mRecipeFluidMap.values()) c.removeAll(tPool);
        return true;
    }

    // ── Recipe lookup ────────────────────────────────────────────────────

    /** Find the first recipe matching the given inputs. Returns null if none matches. */
    @javax.annotation.Nullable
    public Recipe findRecipe(Iterable<ItemStack> items, Iterable<FluidStack> fluids,
                             boolean needsOutputs, int inputSlotCount, int outputSlotCount) {
        return findRecipe(items, fluids, needsOutputs, inputSlotCount, outputSlotCount, null, null, ItemStack.EMPTY);
    }

    @javax.annotation.Nullable
    public Recipe findRecipe(Iterable<ItemStack> items, Iterable<FluidStack> fluids,
            boolean needsOutputs, int inputSlotCount, int outputSlotCount,
            net.minecraft.world.level.Level level, net.minecraft.world.level.block.entity.BlockEntity machine,
            ItemStack special) {
        var inputItems = new ArrayList<ItemStack>();
        for (var item : items) { if (inputItems.size() >= inputSlotCount) break; inputItems.add(item); }
        var inputFluids = new ArrayList<FluidStack>();
        for (var fluid : fluids) inputFluids.add(fluid == null ? FluidStack.EMPTY : fluid);
        for(int pass=0;pass<2;pass++) for (Recipe r : mRecipeList) {
            boolean exact=java.util.Arrays.stream(r.mInputs).allMatch(required->inputItems.stream().anyMatch(available->required.is(available.getItem())));
            if((pass==0)!=exact)continue;
            if (!r.mEnabled || r.mFakeRecipe) continue;
            if (needsOutputs && r.mNeedsEmptyOutput && !allOutputSlotsEmpty(items, inputSlotCount, outputSlotCount)) continue;
            if (RecipeInputs.consume(r, inputItems, inputFluids, 1) != null) return r;
        }
        // GT6's subclass overrides run only when the table had no answer, exactly like GT6's
        // `if (rRecipe != null) return rRecipe;` guards at the top of those findRecipe() bodies.
        if (mDynamicRecipes != null) {
            Recipe dynamic = mDynamicRecipes.find(inputItems, inputFluids);
            if (dynamic != null && dynamic.mEnabled && !dynamic.mFakeRecipe) {
                if (needsOutputs && dynamic.mNeedsEmptyOutput
                        && !allOutputSlotsEmpty(items, inputSlotCount, outputSlotCount)) return null;
                if (RecipeInputs.consume(dynamic, inputItems, inputFluids, 1) != null) return dynamic;
            }
        }
        if (mContextRecipes != null && level != null) {
            Recipe dynamic = mContextRecipes.find(level, machine, special, inputItems, inputFluids);
            if (dynamic != null && dynamic.mEnabled && !dynamic.mFakeRecipe
                    && (!needsOutputs || !dynamic.mNeedsEmptyOutput
                            || allOutputSlotsEmpty(items, inputSlotCount, outputSlotCount))
                    && RecipeInputs.consume(dynamic, inputItems, inputFluids, 1) != null) return dynamic;
        }
        return null;
    }

    private static boolean allOutputSlotsEmpty(Iterable<ItemStack> items, int inputCount, int outputCount) {
        int slot = 0;
        for (ItemStack s : items) {
            if (slot >= inputCount && slot < inputCount + outputCount && !s.isEmpty()) return false;
            slot++;
        }
        return true;
    }

    private static boolean itemMatches(ItemStack recipe, ItemStack input) {
        return recipe.getItem() == input.getItem() && input.getCount() >= recipe.getCount();
    }

    // ── Fluid helpers ────────────────────────────────────────────────────

    /** Minimum recommended tank size for a given fluid, or 0 if unknown. */
    public long minTankSize(String fluidKey) {
        return mMinInputTankSizes.getOrDefault(fluidKey, 0L);
    }

    /** Whether any registered recipe accepts the given item as input. */
    public boolean containsInput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var recipes = mRecipeItemMap.get(stack.getItem());
        if(recipes != null && recipes.stream().anyMatch(r -> r.mEnabled && !r.mFakeRecipe)) return true;
        return mRecipeList.stream().anyMatch(r->r.mEnabled&&!r.mFakeRecipe&&Arrays.stream(r.mInputs).anyMatch(required->RecipeInputs.matches(required,stack)));
    }

    /** Whether any registered recipe accepts the given fluid as input. */
    public boolean containsInput(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) return false;
        var recipes = mRecipeFluidMap.get(fluidKey(fluid));
        return recipes != null && recipes.stream().anyMatch(r -> r.mEnabled && !r.mFakeRecipe
                && Arrays.stream(r.mFluidInputs).anyMatch(f -> f != null && f.isFluidEqual(fluid)));
    }
}
