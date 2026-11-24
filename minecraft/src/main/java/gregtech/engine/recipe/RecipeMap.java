package gregtech.engine.recipe;

import gregtech.content.recipe.RecipeMapSpec;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

import org.slf4j.Logger;

import java.util.*;
import java.util.function.Predicate;

/**
 * GT6 RecipeMap — recipe registry keyed by unique internal name.
 * Each machine type binds to one RecipeMap which defines its slot/tank counts.
 *
 * <p>1.20.1 port of {@code gregapi.recipes.Recipe.RecipeMap}. GT6 subclasses that override
 * {@code findRecipe()} to build a row from the <em>contents</em> of the inputs (the scanner, the
 * printer and the replicator read the NBT of a USB stick) are ported through
 * {@link #dynamicRecipes(DynamicRecipes)}: the port keeps the table-driven lookup and asks the
 * providers only when no static row matches. The built-in provider stays first; addon providers are
 * ordered by the explicit priority passed to {@link #addDynamicRecipes(long, DynamicRecipes)}, not
 * by the order they were registered.</p>
 *
 * <p>Static rows carry an explicit {@link Recipe#priority}; a lookup returns the highest-priority
 * matching row and uses the row published first only to break a tie. Rows that share inputs but
 * differ in conditions (energy, duration, probability, special value, catalyst role, priority) are
 * legitimate variants and coexist instead of one being rejected as a duplicate; see
 * {@link #conditionsEqual(Recipe, Recipe)}.</p>
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
    private DynamicRecipes dynamicRecipes;
    private static final DynamicRecipes[] NO_ADDON_DYNAMIC_RECIPES = new DynamicRecipes[0];
    private static final long[] NO_ADDON_DYNAMIC_PRIORITIES = new long[0];
    private DynamicRecipes[] addonDynamicRecipes = NO_ADDON_DYNAMIC_RECIPES;
    /** Parallel to {@link #addonDynamicRecipes}; descending, ties in registration order. */
    private long[] addonDynamicPriorities = NO_ADDON_DYNAMIC_PRIORITIES;

    /** Original findRecipe overrides that also need the machine world and special slot. */
    @FunctionalInterface
    public interface ContextRecipes {
        Recipe find(net.minecraft.world.level.Level level,
                net.minecraft.world.level.block.entity.BlockEntity machine, ItemStack special,
                List<ItemStack> items, List<FluidStack> fluids);
    }
    private java.util.function.Predicate<ItemStack> dynamicInputAdmission;
    /** Rule families can admit ingredients without enumerating every physical input/output pair. */
    public RecipeMap dynamicInputAdmission(java.util.function.Predicate<ItemStack> admission) {
        dynamicInputAdmission = dynamicInputAdmission == null ? admission : dynamicInputAdmission.or(admission);
        return this;
    }
    private ContextRecipes contextRecipes;
    public RecipeMap contextualRecipes(ContextRecipes provider) {
        contextRecipes = provider;
        lookupTable.invalidate();
        return this;
    }


    /**
     * Installs the built-in {@code findRecipe()} override at the front of this map.
     * Calling it again replaces that front provider and leaves providers added by
     * {@link #addDynamicRecipes(DynamicRecipes)} where they are.
     */
    public RecipeMap dynamicRecipes(DynamicRecipes provider) {
        dynamicRecipes = provider;
        lookupTable.invalidate();
        return this;
    }

    /**
     * Registers a provider at neutral priority; see {@link #addDynamicRecipes(long, DynamicRecipes)}.
     */
    public RecipeMap addDynamicRecipes(DynamicRecipes provider) {
        return addDynamicRecipes(0L, provider);
    }

    /**
     * Registers a provider behind the built-in one, ordered by explicit priority instead of call
     * order. A null answer means this provider does not handle the input; the next provider in
     * priority order is asked. The first answer that yields a row the machine can actually use is
     * the dynamic row, so a provider returning a row that fails the normal matcher does not mask a
     * later provider that would have matched. Equal priorities keep registration order.
     */
    public RecipeMap addDynamicRecipes(long priority, DynamicRecipes provider) {
        if (provider == null) throw new IllegalArgumentException("dynamic recipe provider");
        int length = addonDynamicRecipes.length;
        int at = 0;
        while (at < length && addonDynamicPriorities[at] >= priority) at++;
        var providers = new DynamicRecipes[length + 1];
        var priorities = new long[length + 1];
        System.arraycopy(addonDynamicRecipes, 0, providers, 0, at);
        System.arraycopy(addonDynamicPriorities, 0, priorities, 0, at);
        providers[at] = provider;
        priorities[at] = priority;
        System.arraycopy(addonDynamicRecipes, at, providers, at + 1, length - at);
        System.arraycopy(addonDynamicPriorities, at, priorities, at + 1, length - at);
        addonDynamicRecipes = providers;
        addonDynamicPriorities = priorities;
        lookupTable.invalidate();
        return this;
    }

    /** Priorities of the addon providers, in lookup order; the built-in provider is not listed. */
    public long[] dynamicProviderPriorities() {
        return addonDynamicPriorities.clone();
    }

    /**
     * GT6's NEI pass over a handler: the rows a rule family generates on demand. The family must
     * produce exactly the rows its {@link DynamicRecipes} provider matches, so the viewers, the
     * dump and the addon query surface enumerate the same conversions the machine performs.
     */
    @FunctionalInterface
    public interface DynamicEnumeration {
        Iterable<Recipe> rows();
    }

    @javax.annotation.Nullable
    private DynamicEnumeration dynamicEnumeration;

    public RecipeMap dynamicEnumeration(DynamicEnumeration provider) {
        dynamicEnumeration = provider;
        return this;
    }

    /** Whether this map has a rule family whose rows only exist when something enumerates them. */
    public boolean hasDynamicEnumeration() {
        return dynamicEnumeration != null;
    }

    /**
     * Registered rows followed by the rule family's generated rows. Iterating this can build the
     * family's rows again, so only viewers and development tools should use it.
     */
    public Iterable<Recipe> allRecipes() {
        Iterable<Recipe> statics = recipeRows;
        DynamicEnumeration generated = dynamicEnumeration;
        if (generated == null) return statics;
        return () -> concat(statics.iterator(), generated.rows().iterator());
    }

    private static <T> java.util.Iterator<T> concat(java.util.Iterator<T> first, java.util.Iterator<T> second) {
        return new java.util.Iterator<>() {
            @Override public boolean hasNext() { return first.hasNext() || second.hasNext(); }
            @Override public T next() { return first.hasNext() ? first.next() : second.next(); }
        };
    }

    /** Whether this map computes rows from input NBT (reported by the start-up log line). */
    public boolean hasDynamicRecipes() {
        return dynamicRecipes != null || addonDynamicRecipes.length != 0 || contextRecipes != null;
    }
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<String, RecipeMap> RECIPE_MAPS_INTERNAL = new LinkedHashMap<>();
    private static final List<RecipeMap> RECIPE_MAP_LIST_INTERNAL = new ArrayList<>();
    /** Read-only view. Use {@link #unregister(RecipeMap)} / {@link #restore(RecipeMap)} to change it. */
    public static final Map<String, RecipeMap> RECIPE_MAPS = Collections.unmodifiableMap(RECIPE_MAPS_INTERNAL);
    /** Read-only view of every registered map, in registration order. */
    public static final List<RecipeMap> RECIPE_MAP_LIST = Collections.unmodifiableList(RECIPE_MAP_LIST_INTERNAL);

    /** Rows {@link RecipeBuilder} dropped after optimization left no usable inputs or results. */
    private static int collapsedRecipesDropped;

    private static final List<String> COLLAPSED_SAMPLES = new ArrayList<>();

    private static final Map<String, Integer> COLLAPSED_BY_MAP = new LinkedHashMap<>();

    /** The first few dropped rows, for the start-up log line. */
    public static List<String> collapsedSamples() {
        return List.copyOf(COLLAPSED_SAMPLES);
    }

    /** Which recipe map the dropped rows came from, for the start-up log line. */
    public static Map<String, Integer> collapsedByMap() {
        return Map.copyOf(COLLAPSED_BY_MAP);
    }

    /** Zero-length stack arrays (GT6 {@code CS.ZL_IS} and {@code CS.ZL_FS}). */
    public static final ItemStack[] ZL_IS = new ItemStack[0];
    public static final FluidStack[] ZL_FS = new FluidStack[0];

    // ── Slot / tank counts (set by constructor, immutable) ──────────────
    public final int itemInputSlots, itemOutputSlots;
    /** Dedicated potion input slots; {@link MachineInventory} keeps them beside the item slots. */
    public final int potionInputSlots;
    public final int fluidInputTanks, fluidOutputTanks;
    public final int minimumItemInputs, minimumFluidInputs, minimumInputs;

    // ── Identity ─────────────────────────────────────────────────────────
    public final String internalName;
    public final String displayName;
    public final String guiPath;
    /** Direct fuel converters read fluid amounts instead of presence gates. */
    private final boolean directFuelConverter;

    // ── Recipe storage ───────────────────────────────────────────────────
    public final Collection<Recipe> recipeRows;
    private final RecipeLookupTable lookupTable;
    private final RecipeCollisionIndex collisionIndex;
    /** Publication counter behind {@link Recipe#registrationOrder}; survives removals and reloads. */
    private long nextRegistrationOrder;
    /** Index: input {@link Item} -> recipes using it as an input. */
    public final Map<Item, Collection<Recipe>> recipesByInputItem = new HashMap<>();
    public final Map<String, Collection<Recipe>> recipesByInputFluid = new HashMap<>();

    /** Minimum tank size per fluid key, populated when recipes are added. */
    public final Map<String, Long> minimumInputTankSizes = new HashMap<>();

    /** Largest single fluid input amount seen so far (min 1000). */
    public int maxFluidInputSize = 1000;
    /** Largest single fluid output amount seen so far (min 1000). */
    public int maxFluidOutputSize = 1000;

    /** Whether {@link #addRecipe(Recipe)} logs errors by default. */
    public boolean logRecipeErrors = true;

    // ── Convenience flags ────────────────────────────────────────────────
    /** GT6 recipe power divisor; all current non-crafting maps use one. */
    public long powerMultiplier = 1;
    /** Original NEI eligibility and caption flags; recipe lookup/storage remain independent. */
    public final boolean viewerAllowed, showVoltageAmperage;
    public final boolean requiresOutputs;
    public final boolean combinePower;
    public final boolean bucketSizedFluidInputs;
    public final boolean bucketSizedFluidOutputs;

    // ── NEI/JEI special value labels (GT6 aNEISpecialValuePre/Multiplier/Post) ──
    /**
     * Prefix of the recipe viewer's "special value" line, e.g. {@code "Temperature: "} for the
     * crucible maps, whose {@link Recipe#specialValue} is a temperature in Kelvin
     * ({@code RM.CrucibleAlloying} / {@code RM.CrucibleSmelting} in the original). An empty
     * prefix means the map has no special value line.
     */
    public final String specialValuePrefix;
    /** Multiplier applied to {@link Recipe#specialValue} before display (GT6 uses 1). */
    public final long specialValueMultiplier;
    /** Suffix of the special value line, e.g. {@code " K"}. */
    public final String specialValueSuffix;

    /** Whether this map shows a special value line for its recipes. */
    public boolean hasSpecialValueLabel() {
        return !specialValuePrefix.isEmpty() && specialValueMultiplier != 0;
    }

    /**
     * Recipes of this map finish instantly, so the viewer must not print a duration.
     * <p>
     * GT6's crucibles convert the moment the input reaches its melting point
     * ({@code TileEntityBase08Crucible}: the conversion is a state change, not a timed machine
     * process), which is why "Time: 1 tick" on the crucible recipe pages is meaningless.
     * </p>
     */
    public final boolean instantRecipes;

    /** Creates an empty recipe map and registers it under the specified internal name. */
    public RecipeMap(RecipeMapSpec spec) {
        internalName = spec.internalName;
        lookupTable = new RecipeLookupTable(new HashSet<>(), internalName);
        recipeRows = lookupTable;
        directFuelConverter = switch (internalName) {
            case "gt.recipe.fuels.burn", "gt.recipe.fuels.gas", "gt.recipe.fuels.fluidbed",
                    "gt.recipe.fuels.engine", "gt.recipe.fuels.hot" -> true;
            default -> false;
        };
        collisionIndex = new RecipeCollisionIndex(this, lookupTable, internalName.startsWith("gt.recipe.anvil"));
        displayName = spec.displayName;
        guiPath = spec.guiPath;
        itemInputSlots = spec.itemInputSlots;
        itemOutputSlots = spec.itemOutputSlots;
        minimumItemInputs = spec.minimumItemInputs;
        fluidInputTanks = spec.fluidInputTanks;
        fluidOutputTanks = spec.fluidOutputTanks;
        minimumFluidInputs = spec.minimumFluidInputs;
        minimumInputs = spec.minimumInputs;
        requiresOutputs = spec.requiresOutputs;
        combinePower = spec.combinePower;
        bucketSizedFluidInputs = spec.bucketSizedFluidInputs;
        bucketSizedFluidOutputs = spec.bucketSizedFluidOutputs;
        viewerAllowed = spec.viewerAllowed;
        showVoltageAmperage = spec.showVoltageAmperage;
        potionInputSlots = spec.potionInputSlots;
        specialValuePrefix = spec.specialValuePrefix;
        specialValueMultiplier = spec.specialValueMultiplier;
        specialValueSuffix = spec.specialValueSuffix;
        instantRecipes = spec.instantRecipes;

        if (RECIPE_MAPS_INTERNAL.containsKey(internalName))
            throw new IllegalArgumentException("Duplicate RecipeMap key: " + internalName);
        RECIPE_MAPS_INTERNAL.put(internalName, this);
        RECIPE_MAP_LIST_INTERNAL.add(this);
    }

    /** Number of rows {@link RecipeBuilder} dropped as empty after optimization. */
    public static int collapsedRecipesDropped() {
        return collapsedRecipesDropped;
    }

    /** Drops a map from the registry. Tests use this so the public views stay read-only. */
    public static RecipeMap unregister(String name) {
        RecipeMap map = RECIPE_MAPS_INTERNAL.remove(name);
        if (map != null) RECIPE_MAP_LIST_INTERNAL.remove(map);
        return map;
    }

    /** Drops {@code map} from the registry when it is the map stored under its name. */
    public static void unregister(RecipeMap map) {
        if (map != null && RECIPE_MAPS_INTERNAL.remove(map.internalName, map))
            RECIPE_MAP_LIST_INTERNAL.remove(map);
    }

    /** Puts a previously unregistered map back. */
    public static void restore(RecipeMap map) {
        if (map == null) return;
        if (RECIPE_MAPS_INTERNAL.putIfAbsent(map.internalName, map) != null)
            throw new IllegalArgumentException("Duplicate RecipeMap key: " + map.internalName);
        RECIPE_MAP_LIST_INTERNAL.add(map);
    }

    @Override
    public String toString() { return internalName; }

    // ── Recipe registration ──────────────────────────────────────────────

    /** Controls how a built row is published to a map. Display rows are never executable. */
    public static final class Registration {
        public static final Registration DEFAULT = new Registration(true, false, false, true);
        public static final Registration DISPLAY = new Registration(false, true, false, true);
        public static final Registration DISPLAY_CHECKED = new Registration(true, true, false, true);

        private final boolean checkForCollisions;
        private final boolean fakeRecipe;
        private final boolean hidden;
        private final boolean logErrors;

        private Registration(boolean checkForCollisions, boolean fakeRecipe, boolean hidden, boolean logErrors) {
            this.checkForCollisions = checkForCollisions;
            this.fakeRecipe = fakeRecipe;
            this.hidden = hidden;
            this.logErrors = logErrors;
        }

        public Registration withCollisionCheck(boolean checkForCollisions) {
            return new Registration(checkForCollisions, fakeRecipe, hidden, logErrors);
        }

        public Registration withFakeRecipe(boolean fakeRecipe) {
            return new Registration(checkForCollisions, fakeRecipe, hidden, logErrors);
        }

        public Registration withHidden(boolean hidden) {
            return new Registration(checkForCollisions, fakeRecipe, hidden, logErrors);
        }

        public Registration withLogErrors(boolean logErrors) {
            return new Registration(checkForCollisions, fakeRecipe, hidden, logErrors);
        }
    }

    /** Normalizes and registers a recipe under the map's default registration rules. */
    public Recipe register(RecipeBuilder builder) {
        return register(builder, Registration.DEFAULT);
    }

    public Recipe register(RecipeBuilder builder, Registration registration) {
        RecipeBuilder.BuildResult result = builder.buildResult();
        if (result.collapsed()) COLLAPSED_BY_MAP.merge(internalName, 1, Integer::sum);
        return addRecipe(result.recipe(), registration);
    }

    /** Registers a prepared row. Returns null when it is missing, invalid or rejected. */
    public Recipe addRecipe(Recipe recipe) {
        return addRecipe(recipe, Registration.DEFAULT);
    }

    public Recipe addRecipe(Recipe recipe, Registration registration) {
        if (recipe == null) return null;
        canonicalInputs(recipe);
        for (int i = 0; i < recipe.itemOutputs.length; i++) {
            recipe.itemOutputs[i] = gregtech.engine.material.MaterialUnification.canonical(recipe.itemOutputs[i]);
        }
        recipe.hidden = registration.hidden;
        recipe.fakeRecipe = registration.fakeRecipe;
        if (!registration.fakeRecipe && !validate(recipe, registration.logErrors && logRecipeErrors)) return null;
        if (registration.checkForCollisions) {
            Recipe collision = findCollision(recipe);
            if (collision != null) {
                RecipeDiagnostics.rejected(this, collision, recipe);
                return null;
            }
        }
        return add(recipe);
    }

    /**
     * Ordered bulk registration with the same validation and collision predicate as addRecipe.
     * A temporary required-item anchor avoids scanning enormous shared catalyst buckets. It is
     * intentionally not a persistent index: public collection mutations and reloads remain unchanged.
     */
    public synchronized List<Recipe> addRecipes(Collection<Recipe> candidates) {
        var batch = collisionIndex.startBatch();
        List<Recipe> accepted = new ArrayList<>();
        for (Recipe row : candidates) {
            if (row == null) continue;
            canonicalInputs(row);
            for (int i = 0; i < row.itemOutputs.length; i++)
                row.itemOutputs[i] = gregtech.engine.material.MaterialUnification.canonical(row.itemOutputs[i]);
            row.hidden = false;
            row.fakeRecipe = false;
            if (!validate(row, logRecipeErrors)) continue;
            Recipe collision = batch.findCollision(row);
            if (collision != null) {
                RecipeDiagnostics.rejected(this, collision, row);
            } else if (add(row) == row) {
                accepted.add(row);
                batch.add(row);
            }
        }
        return accepted;
    }

    /** Unify explicit vanilla replacements before collision lookup, as viewers and live aliases do.
     * Exact recipes and identity-sensitive predicates keep their original representatives. */
    private static void canonicalInputs(Recipe recipe) {
        if (recipe.exactItemInputs || recipe.hasRuntimeResolver()) return;
        for (int i = 0; i < recipe.itemInputs.length; i++) {
            if (recipe.itemTag(i) == null && !recipe.hasItemInputGuard(i) && !recipe.hasSameItemInputPredicate(i))
                recipe.itemInputs[i] = gregtech.engine.material.MaterialUnification.canonical(recipe.itemInputs[i]);
        }
    }

    static boolean empty(ItemStack stack) {
        return stack == null || stack.isEmpty();
    }

    static void recordCollapsedRecipe(ItemStack[] inputs, ItemStack[] outputs) {
        collapsedRecipesDropped++;
        if (COLLAPSED_SAMPLES.size() < 64) {
            COLLAPSED_SAMPLES.add(Arrays.toString(inputs) + " -> " + Arrays.toString(outputs));
        }
    }

    // ── Validation / collision / storage ─────────────────────────────────

    private boolean validate(Recipe recipe, boolean logErrors) {
        boolean invalid = false;
        if (recipe.itemInputs.length + recipe.fluidInputs.length <= 0) {
            if (logErrors) LOGGER.error("[{}] Recipe has no Inputs!", internalName);
            invalid = true;
        } else {
            if (requiresOutputs && recipe.itemOutputs.length + recipe.fluidOutputs.length <= 0) {if (logErrors) LOGGER.error("[{}] Recipe has no Outputs!", internalName); invalid = true;}
            if (recipe.itemInputs.length      < minimumItemInputs)  {if (logErrors) LOGGER.error("[{}] Recipe has less than the minimal amount of Input ItemStacks!", internalName); invalid = true;}
            if (recipe.fluidInputs.length < minimumFluidInputs) {if (logErrors) LOGGER.error("[{}] Recipe has less than the minimal amount of Input FluidStacks!", internalName); invalid = true;}
            if (recipe.itemInputs.length + recipe.fluidInputs.length < minimumInputs) {if (logErrors) LOGGER.error("[{}] Recipe has less than the minimal amount of general Inputs!", internalName); invalid = true;}
            if (recipe.itemInputs.length      > itemInputSlots + potionInputSlots) {if (logErrors) LOGGER.error("[{}] Recipe has more than the maximum amount of Input ItemStacks!", internalName); invalid = true;}
            if (recipe.fluidInputs.length > fluidInputTanks)    {if (logErrors) LOGGER.error("[{}] Recipe has more than the maximum amount of Input FluidStacks!", internalName); invalid = true;}
        }
        return !invalid;
    }

    /** Returns an already-registered row whose matching inputs and execution conditions cover the candidate. */
    public Recipe findCollision(Recipe recipe) {
        return collisionIndex.findCollision(recipe);
    }

    static boolean conditionsEqual(Recipe a, Recipe b) {
        return RecipeCollisionIndex.conditionsEqual(a, b);
    }

    static String fluidKey(FluidStack f) {
        return f.getFluid().builtInRegistryHolder().key().location().toString();
    }

    /** These builtin converters still interpret fluid input amounts directly, not as presence gates. */
    private boolean supportsFluidPresence(Recipe recipe) {
        return !recipe.hasPresenceFluidInputs() || !directFuelConverter;
    }

    /** Stores the recipe and updates lookup indices, rejecting unsupported presence-only fuel rows. */
    public synchronized Recipe add(Recipe recipe) {
        if (recipe == null) return null;
        if (!supportsFluidPresence(recipe)) {
            LOGGER.error("[{}] Presence-only fluid inputs are unsupported by this map's direct fuel converters; recipe rejected", internalName);
            return null;
        }
        boolean inserted = recipeRows.add(recipe);
        if (inserted) {
            // The first publication order is kept when a baseline row is put back, so an equal-priority
            // tie resolves the same way after a reload.
            if (recipe.registrationOrder < 0) recipe.registrationOrder = nextRegistrationOrder++;
            for (var capture : additionCaptures) capture.record(recipe);
            collisionIndex.add(recipe);
        }
        // Index by input items
        for (ItemStack in : recipe.itemInputs) {
            if (empty(in)) continue;
            recipesByInputItem.computeIfAbsent(in.getItem(), k -> new HashSet<>()).add(recipe);
            for (var color : gregtech.content.block.misc.LegacyConstructionBlockItem.ingredientAlternatives(in))
                recipesByInputItem.computeIfAbsent(color.getItem(), k -> new HashSet<>()).add(recipe);
        }
        // Index by input fluids; a 0 mB catalyst still needs the fluid present.
        for (FluidStack f : recipe.fluidInputs) {
            if (f == null || f.getFluid() == Fluids.EMPTY) continue;
            String fKey = fluidKey(f);
            recipesByInputFluid.computeIfAbsent(fKey, k -> new HashSet<>()).add(recipe);
            minimumInputTankSizes.merge(fKey, (long) Math.max(1, f.getAmount()), Math::max);
            maxFluidInputSize = Math.max(maxFluidInputSize, f.getAmount());
        }
        for (FluidStack f : recipe.fluidOutputs) {
            if (f != null) maxFluidOutputSize = Math.max(maxFluidOutputSize, f.getAmount());
        }
        return recipe;
    }

    private List<Recipe> baseline;

    public boolean hasBaseline() { return baseline != null; }
    public int baselineSize() { return baseline == null ? 0 : baseline.size(); }

    /** The first capture is kept. Rows in {@code exclude} are rebuilt from tags after every restore. */
    public synchronized void captureBaseline(Set<Recipe> exclude) {
        if (baseline != null) return;
        var rows = new ArrayList<Recipe>(recipeRows.size());
        for (var recipe : recipeRows) if (exclude == null || !exclude.contains(recipe)) rows.add(recipe);
        baseline = List.copyOf(rows);
    }

    /** Drops rows outside the baseline and puts missing baseline rows back. Returns how many rows moved. */
    public synchronized int restoreBaseline() {
        if (baseline == null) throw new IllegalStateException("Recipe baseline was not captured for " + internalName);
        var keep = Collections.newSetFromMap(new IdentityHashMap<Recipe, Boolean>());
        keep.addAll(baseline);
        var drop = new ArrayList<Recipe>();
        for (var recipe : recipeRows) if (!keep.contains(recipe)) drop.add(recipe);
        removeAll(drop);
        var present = Collections.newSetFromMap(new IdentityHashMap<Recipe, Boolean>());
        present.addAll(recipeRows);
        int restored = 0;
        for (var recipe : baseline) if (present.add(recipe)) {
            add(recipe);
            restored++;
        }
        return drop.size() + restored;
    }

    /** Clears the indexes and inserts the baseline again, for timing a full rebuild. */
    public synchronized int reindexBaseline() {
        if (baseline == null) throw new IllegalStateException("Recipe baseline was not captured for " + internalName);
        recipeRows.clear();
        recipesByInputItem.clear();
        recipesByInputFluid.clear();
        collisionIndex.clear();
        for (var recipe : baseline) add(recipe);
        return baseline.size();
    }

    /** Removes one stored row and its input indexes. */
    public synchronized boolean unlink(Recipe recipe) {
        if (recipe == null || !recipeRows.remove(recipe)) return false;
        unlinkIndexes(recipe);
        return true;
    }

    /**
     * Removes {@code rows} from this map and from every index entry {@link #add} created for them.
     * The one deletion entry for overlay markers, reload producers and baseline restores: the
     * stored rows and the auxiliary indexes cannot drift apart.
     *
     * @return how many of the given rows were stored here
     */
    public synchronized int removeAll(Collection<Recipe> rows) {
        if (rows == null || rows.isEmpty()) return 0;
        // Removing one row at a time keeps the membership test on the stored set's own hash.
        var present = new ArrayList<Recipe>(rows.size());
        for (Recipe recipe : rows) if (recipe != null && recipeRows.remove(recipe)) present.add(recipe);
        for (Recipe recipe : present) unlinkIndexes(recipe);
        return present.size();
    }

    /** Drops every index entry {@link #add} created for an already-unstored row. */
    private void unlinkIndexes(Recipe recipe) {
        for (ItemStack in : recipe.itemInputs) {
            if (empty(in)) continue;
            removeItemIndex(in.getItem(), recipe);
            for (var color : gregtech.content.block.misc.LegacyConstructionBlockItem.ingredientAlternatives(in))
                removeItemIndex(color.getItem(), recipe);
        }
        for (FluidStack fluid : recipe.fluidInputs) {
            // add() indexes every non-empty fluid, amount 0 included; the removal must mirror it.
            if (fluid == null || fluid.getFluid() == Fluids.EMPTY) continue;
            String key = fluidKey(fluid);
            var pool = recipesByInputFluid.get(key);
            if (pool == null) continue;
            pool.remove(recipe);
            if (pool.isEmpty()) recipesByInputFluid.remove(key);
        }
        collisionIndex.remove(recipe);
    }

    private void removeItemIndex(Item item, Recipe recipe) {
        var pool = recipesByInputItem.get(item);
        if (pool == null) return;
        pool.remove(recipe);
        if (pool.isEmpty()) recipesByInputItem.remove(item);
    }

    /** Removes all recipes that use the given item as one of their inputs. */
    public synchronized boolean removeRecipesByInput(ItemStack input) {
        if (empty(input)) return false;
        var removed = new HashSet<Recipe>(recipesByInputItem.getOrDefault(input.getItem(), List.of()));
        for (Recipe recipe : lookupTable.tagged()) if (containsInput(recipe, input, false)) removed.add(recipe);
        if (removed.isEmpty()) return false;
        removeAll(removed);
        return true;
    }

    // ── Added-row capture ────────────────────────────────────────────────

    /** One in-flight {@link #captureAdditions(List, Runnable)} scope, owned by its calling thread. */
    private static final class AdditionCapture {
        private final Thread owner = Thread.currentThread();
        private final List<Recipe> rows = new ArrayList<>();

        void record(Recipe recipe) {
            if (Thread.currentThread() == owner) rows.add(recipe);
        }
    }

    private final List<AdditionCapture> additionCaptures = new ArrayList<>();

    /**
     * Replaces a before/after snapshot of the whole collection by the rows this thread adds to this
     * map inside {@code body} that are still stored when it returns, in registration order.
     * Adding a row that is already stored is not an addition, and a row dropped again before the
     * scope ended is left out. Nested scopes each see the rows added anywhere inside them; a scope
     * on another thread never sees these rows. When {@code body} throws the list stays empty and
     * the failure propagates unchanged.
     */
    public void captureAdditions(List<Recipe> out, Runnable body) {
        var capture = new AdditionCapture();
        synchronized (this) { additionCaptures.add(capture); }
        boolean completed = false;
        try {
            body.run();
            completed = true;
        } finally {
            synchronized (this) { additionCaptures.remove(capture); }
            out.clear();
            if (completed) {
                var seen = Collections.newSetFromMap(new IdentityHashMap<Recipe, Boolean>());
                for (Recipe recipe : capture.rows)
                    if (seen.add(recipe) && recipeRows.contains(recipe)) out.add(recipe);
            }
        }
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
        var matcher = new RecipeInputs.Matcher(inputItems, inputFluids);
        boolean emptyOutputs = !needsOutputs || allOutputSlotsEmpty(items, inputSlotCount, outputSlotCount);
        Recipe best = findStaticRecipe(inputItems, inputFluids, matcher, emptyOutputs, row -> true);
        if (best != null) return best;
        // GT6's subclass overrides run only when the table had no answer, exactly like GT6's
        // `if (rRecipe != null) return rRecipe;` guards at the top of those findRecipe() bodies.
        if (dynamicRecipes != null || addonDynamicRecipes.length != 0) {
            Recipe dynamic = firstMatchingDynamicRecipe(matcher, inputItems, inputFluids, emptyOutputs);
            if (dynamic != null) return dynamic;
        }
        if (contextRecipes != null && level != null) {
            Recipe dynamic = contextRecipes.find(level, machine, special, inputItems, inputFluids);
            if (dynamic != null && dynamic.enabled && !dynamic.fakeRecipe && supportsFluidPresence(dynamic)
                    && (!needsOutputs || !dynamic.needsEmptyOutput
                            || allOutputSlotsEmpty(items, inputSlotCount, outputSlotCount))
                    && matcher.matches(dynamic, 1)) return dynamic;
        }
        return null;
    }

    /** Selects a registered row for a device that checks its own power and output limits. */
    @javax.annotation.Nullable
    public Recipe findStaticRecipe(List<ItemStack> items, List<FluidStack> fluids, Predicate<Recipe> canRun) {
        return findStaticRecipe(items, fluids, new RecipeInputs.Matcher(items, fluids), true, canRun);
    }

    @javax.annotation.Nullable
    private Recipe findStaticRecipe(List<ItemStack> items, List<FluidStack> fluids, RecipeInputs.Matcher matcher,
                                    boolean emptyOutputs, Predicate<Recipe> canRun) {
        var candidates = lookupTable.candidates(items, fluids);
        Recipe exact = bestStaticMatch(candidates.exact(), matcher, items, emptyOutputs, canRun);
        if (exact != null) return exact;
        return bestStaticMatch(candidates.alternatives(), matcher, items, emptyOutputs, canRun);
    }

    /**
     * The row a machine should use from one lookup stage: highest explicit priority first, then the
     * earliest publication. Only rows the machine can actually run are considered, so a disabled,
     * fake, presence-incompatible or non-matching row never shadows a usable variant.
     */
    @javax.annotation.Nullable
    private Recipe bestStaticMatch(List<Recipe> stage, RecipeInputs.Matcher matcher, List<ItemStack> inputs,
                                   boolean emptyOutputs, Predicate<Recipe> canRun) {
        if (stage.isEmpty()) return null;
        // Explicit priority first; equal priorities keep the row published first. The matcher then
        // runs in that order and the first usable row answers, so ordinary content (one priority
        // everywhere) does not pay for the rest of a shared mold/tool bucket.
        List<Recipe> ordered = stage;
        if (stage.size() > 1 && !isSelectionOrdered(stage)) {
            ordered = new java.util.ArrayList<>(stage);
            ordered.sort(RecipeMap::compareSelectionOrder);
        }
        for (Recipe candidate : ordered) {
            if (!usableRecipe(candidate, matcher, emptyOutputs)) continue;
            Recipe row = candidate.hasRuntimeResolver() ? candidate.resolveRuntime(inputs) : candidate;
            if (row != candidate && !usableRecipe(row, matcher, emptyOutputs)) continue;
            if (canRun.test(row)) return row;
        }
        return null;
    }

    /**
     * Whether the candidate stage already follows {@link #compareSelectionOrder}. The lookup index
     * keeps rows in snapshot order, which is not guaranteed to be publication order, so the order
     * is verified here instead of assuming uniform priorities arrive in registration order.
     */
    private static boolean isSelectionOrdered(List<Recipe> stage) {
        Recipe previous = stage.get(0);
        for (int i = 1; i < stage.size(); i++) {
            Recipe candidate = stage.get(i);
            if (compareSelectionOrder(previous, candidate) > 0) return false;
            previous = candidate;
        }
        return true;
    }

    private static int compareSelectionOrder(Recipe a, Recipe b) {
        int byPriority = Long.compare(b.priority, a.priority);
        if (byPriority != 0) return byPriority;
        long first = a.registrationOrder < 0 ? Long.MAX_VALUE : a.registrationOrder;
        long second = b.registrationOrder < 0 ? Long.MAX_VALUE : b.registrationOrder;
        return Long.compare(first, second);
    }

    /**
     * The built-in handler, then addon providers in priority order. A provider whose row cannot be
     * used does not consume the query: the next provider is still asked, so one provider's partial
     * match cannot hide another's real one. This is the same row set {@link #dynamicEnumeration}
     * must expose.
     */
    @javax.annotation.Nullable
    private Recipe firstMatchingDynamicRecipe(RecipeInputs.Matcher matcher, List<ItemStack> inputItems,
                                              List<FluidStack> inputFluids, boolean emptyOutputs) {
        if (dynamicRecipes != null) {
            Recipe recipe = dynamicRecipes.find(inputItems, inputFluids);
            if (usableRecipe(recipe, matcher, emptyOutputs)) return recipe;
        }
        for (int i = 0; i < addonDynamicRecipes.length; i++) {
            Recipe recipe = addonDynamicRecipes[i].find(inputItems, inputFluids);
            if (usableRecipe(recipe, matcher, emptyOutputs)) return recipe;
        }
        return null;
    }

    private boolean usableRecipe(Recipe recipe, RecipeInputs.Matcher matcher, boolean emptyOutputs) {
        return recipe != null && recipe.enabled && !recipe.fakeRecipe && supportsFluidPresence(recipe)
                && (!recipe.needsEmptyOutput || emptyOutputs)
                && matcher.matches(recipe, 1);
    }

    static boolean exactItems(Recipe recipe, List<ItemStack> inputs) {
        for (int input = 0; input < recipe.itemInputs.length; input++) {
            var required = recipe.itemInputs[input];
            boolean found = false;
            for (var available : inputs) if (recipe.itemTag(input) != null ? available.is(recipe.itemTag(input))
                    : required.is(available.getItem())) { found = true; break; }
            if (!found) return false;
        }
        return true;
    }

    /** Call after mutating the ingredients of an already registered row in place. */
    public void invalidateLookup() {
        lookupTable.invalidate();
        collisionIndex.invalidate();
    }

    /** Whether a null {@link #findRecipe} answer can only come from the static row table. */
    public boolean hasOnlyStaticLookup() {
        return dynamicRecipes == null && addonDynamicRecipes.length == 0 && contextRecipes == null;
    }

    /** Whether any published row answers through a live item guard or predicate; unknown means true. */
    public boolean hasLiveItemPredicates() { return lookupTable.hasLiveItemPredicates(); }

    /** Whether any published row is skipped while output slots are occupied; unknown means true. */
    public boolean hasEmptyOutputRows() { return lookupTable.hasEmptyOutputRows(); }

    /** Increments on every lookup invalidation; caches keyed on recipe state must compare it. */
    public long lookupRevision() { return lookupTable.revision(); }

    /** Builds the candidate index for every map at a registration or reload boundary. */
    public static void prebuildLookupIndexes() {
        for (var map : RECIPE_MAP_LIST) map.lookupTable.rebuildNow();
    }

    /**
     * Ordered rows whose item inputs may accept {@code stack}, or null when no usable index is
     * published yet. Callers must still apply their original admission test to each row; the
     * lookup only narrows the candidates and never builds an index on the caller's thread.
     */
    @javax.annotation.Nullable
    public List<Recipe> admissionCandidates(ItemStack stack) { return lookupTable.admissionCandidates(stack); }

    /**
     * Ordered rows that declare {@code fluid} as an input, presence gates included, or null when no
     * usable index is published yet. Callers must still apply their original fluid test to each row;
     * the index narrows candidates by fluid identity only and never builds on the caller's thread.
     */
    @javax.annotation.Nullable
    public List<Recipe> fluidInputCandidates(FluidStack fluid) { return lookupTable.fluidInputCandidates(fluid); }

    @javax.annotation.Nullable
    private Recipe firstDynamicRecipe(List<ItemStack> inputItems, List<FluidStack> inputFluids) {
        if (dynamicRecipes != null) {
            Recipe recipe = dynamicRecipes.find(inputItems, inputFluids);
            if (recipe != null && supportsFluidPresence(recipe)) return recipe;
        }
        for (int i = 0; i < addonDynamicRecipes.length; i++) {
            Recipe recipe = addonDynamicRecipes[i].find(inputItems, inputFluids);
            if (recipe != null && supportsFluidPresence(recipe)) return recipe;
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
        return RecipeInputs.matches(recipe, input) && input.getCount() >= recipe.getCount();
    }

    // ── Fluid helpers ────────────────────────────────────────────────────

    /** Minimum recommended tank size for a given fluid, or 0 if unknown. */
    public long minTankSize(String fluidKey) {
        long size = minimumInputTankSizes.getOrDefault(fluidKey, 0L);
        var id = net.minecraft.resources.ResourceLocation.tryParse(fluidKey);
        var fluid = id == null ? Fluids.EMPTY : net.minecraft.core.registries.BuiltInRegistries.FLUID.get(id);
        if (fluid != Fluids.EMPTY) for (var recipe : lookupTable.tagged())
            for (int i = 0; i < recipe.fluidInputs.length; i++)
                if (recipe.fluidTag(i) != null && fluid.builtInRegistryHolder().is(recipe.fluidTag(i)))
                    size = Math.max(size, recipe.fluidInputs[i].getAmount());
        return size;
    }

    /** Whether any registered recipe, including a rule family's generated row, accepts the item. */
    public boolean containsInput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        // First stage: the legacy item index answers marked and unmarked rows by stack identity.
        var recipes = recipesByInputItem.get(stack.getItem());
        if (recipes != null && recipes.stream().anyMatch(r -> containsInput(r, stack, true))) return true;
        // Second stage keeps the original per-row test; the admission index only narrows the rows.
        List<Recipe> candidates = lookupTable.admissionCandidates(stack);
        boolean found;
        if (candidates == null) {
            found = fullAdmissionScan(stack);
        } else {
            found = false;
            for (int index = 0; index < candidates.size(); index++)
                if (containsInput(candidates.get(index), stack, false)) { found = true; break; }
        }
        if (RecipeLookupTable.verifying()) {
            boolean expected = fullAdmissionScan(stack);
            verifyAdmissionCandidates(stack, expected);
            found = expected;
        }
        if (found) return true;
        if (dynamicInputAdmission != null && dynamicInputAdmission.test(stack)) return true;
        // Only a rule family with a published enumeration answers an item it has not indexed yet.
        if (dynamicEnumeration == null) return false;
        Recipe generated = firstDynamicRecipe(List.of(stack), List.of());
        return generated != null && generated.enabled && !generated.fakeRecipe;
    }

    private boolean fullAdmissionScan(ItemStack stack) {
        for (Recipe recipe : recipeRows) if (containsInput(recipe, stack, false)) return true;
        return false;
    }

    /** Development check only: the full-scan answer above stays authoritative. */
    private void verifyAdmissionCandidates(ItemStack stack, boolean expected) {
        List<Recipe> candidates = lookupTable.admissionCandidates(stack);
        if (candidates == null) return;
        boolean indexed = false;
        for (Recipe recipe : candidates) if (containsInput(recipe, stack, false)) { indexed = true; break; }
        if (indexed != expected) RecipeLookupTable.admissionMismatch(internalName, expected, indexed);
    }

    private static boolean containsInput(Recipe recipe, ItemStack stack, boolean indexed) {
        if (!recipe.enabled || recipe.fakeRecipe) return false;
        if (indexed && !recipe.hasTaggedInputs() && !recipe.hasSameItemInputPredicates() && !recipe.hasItemInputGuards()) return true;
        for (int input = 0; input < recipe.itemInputs.length; input++) {
            ItemStack required = recipe.itemInputs[input];
            if (empty(required)) continue;
            if (recipe.hasTaggedInputs() || recipe.hasSameItemInputPredicate(input) || recipe.hasItemInputGuard(input) || !indexed) {
                if (recipe.matchesItemInput(input, stack)) return true;
            } else {
                // Preserve the old index-only admission for unmarked inputs, including palette aliases.
                // An unrelated unmarked slot must not bypass a marked slot's predicate.
                if (required.is(stack.getItem())) return true;
                for (var color : gregtech.content.block.misc.LegacyConstructionBlockItem.matchingAlternatives(required))
                    if (color.is(stack.getItem())) return true;
            }
        }
        return false;
    }

    /** Whether any registered recipe accepts the given fluid as input. */
    public boolean containsInput(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) return false;
        if (MaterialUnitRecipeIndex.acceptsFluid(this, fluid)) return true;
        var recipes = recipesByInputFluid.get(fluidKey(fluid));
        if (recipes != null && recipes.stream().anyMatch(r -> acceptsFluid(r, fluid))) return true;
        return lookupTable.tagged().stream().anyMatch(r -> acceptsFluid(r, fluid));
    }

    private static boolean acceptsFluid(Recipe recipe, FluidStack fluid) {
        if (!recipe.enabled || recipe.fakeRecipe) return false;
        for (int i = 0; i < recipe.fluidInputs.length; i++)
            if (recipe.matchesFluidInput(i, fluid)) return true;
        return false;
    }

    /** Concrete index plus tag rows, without materializing a recipe for each tag member. */
    public Collection<Recipe> recipesForFluid(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) return List.of();
        var rows = new LinkedHashSet<Recipe>();
        for (var recipe : recipesByInputFluid.getOrDefault(fluidKey(fluid), List.of()))
            if (acceptsFluid(recipe, fluid)) rows.add(recipe);
        for (var recipe : lookupTable.tagged()) if (acceptsFluid(recipe, fluid)) rows.add(recipe);
        return rows;
    }
}
