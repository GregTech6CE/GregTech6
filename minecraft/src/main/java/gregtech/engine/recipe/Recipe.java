package gregtech.engine.recipe;

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
    private static final ItemStack[] NO_ITEMS = new ItemStack[0];
    private static final FluidStack[] NO_FLUIDS = new FluidStack[0];
    private static final long[] NO_CHANCES = new long[0];

    private java.util.Map<Integer, net.minecraft.tags.TagKey<net.minecraft.world.item.Item>> itemTags = java.util.Map.of();
    private java.util.Map<Integer, net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid>> fluidTags = java.util.Map.of();

    /** Representatives retain amounts/layout; the tag, not its representative, defines acceptance. */
    public Recipe withItemTagInput(int index, net.minecraft.resources.ResourceLocation id) {
        if (index < 0 || index >= itemInputs.length || itemInputs[index].isEmpty()) throw new IllegalArgumentException("Item tag input " + index);
        var tags = new java.util.HashMap<>(itemTags);
        tags.put(index, net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, id));
        itemTags = java.util.Map.copyOf(tags);
        return this;
    }

    public Recipe withFluidTagInput(int index, net.minecraft.resources.ResourceLocation id) {
        if (index < 0 || index >= fluidInputs.length || fluidInputs[index].isEmpty()) throw new IllegalArgumentException("Fluid tag input " + index);
        var tags = new java.util.HashMap<>(fluidTags);
        tags.put(index, net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.FLUID, id));
        fluidTags = java.util.Map.copyOf(tags);
        return this;
    }

    public net.minecraft.tags.TagKey<net.minecraft.world.item.Item> itemTag(int index) { return itemTags.get(index); }
    public net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid> fluidTag(int index) { return fluidTags.get(index); }
    public boolean hasTaggedInputs() { return !itemTags.isEmpty() || !fluidTags.isEmpty(); }

    public boolean matchesFluidInput(int index, FluidStack available) {
        if (index < 0 || index >= fluidInputs.length || available == null || available.isEmpty()
                || fluidInputs[index] == null || fluidInputs[index].isEmpty()) return false;
        var tag = fluidTags.get(index);
        return tag == null ? RecipeInputs.sameFluid(available, fluidInputs[index])
                : available.getFluid().builtInRegistryHolder().is(tag);
    }

    public java.util.List<FluidStack> viewerFluidInputAlternatives(int index) {
        var tag = fluidTags.get(index);
        if (tag == null) return java.util.List.of(fluidInputs[index]);
        var choices = new java.util.ArrayList<FluidStack>();
        for (var holder : net.minecraft.core.registries.BuiltInRegistries.FLUID.getTagOrEmpty(tag))
            if (holder.value() != net.minecraft.world.level.material.Fluids.EMPTY)
                choices.add(new FluidStack(holder.value(), fluidInputs[index].getAmount()));
        return java.util.List.copyOf(choices);
    }
    public record MaterialInputDisplay(long amount, java.util.List<ItemStack> alternatives, gregtech.api.addon.recipe.MaterialUnitIngredient.Rounding rounding, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag) {
        public MaterialInputDisplay(long amount, java.util.List<ItemStack> alternatives, gregtech.api.addon.recipe.MaterialUnitIngredient.Rounding rounding) {
            this(amount, alternatives, rounding, null);
        }
        public MaterialInputDisplay {
            if (amount <= 0) throw new IllegalArgumentException("Material input amount");
            alternatives = java.util.List.copyOf(alternatives);
        }
    }
    private java.util.Map<Integer, MaterialInputDisplay> materialInputDisplays = java.util.Map.of();
    /** A unit icon describes a rule; its physical input is resolved to a whole-item batch at runtime. */
    public Recipe withMaterialInputDisplay(int index, long amount, java.util.List<ItemStack> alternatives, gregtech.api.addon.recipe.MaterialUnitIngredient.Rounding rounding) {
        if (index < 0 || index >= itemInputs.length || itemInputs[index].isEmpty()) throw new IllegalArgumentException("Material display slot");
        var displays = new java.util.HashMap<>(materialInputDisplays);
        displays.put(index, new MaterialInputDisplay(amount, alternatives, rounding));
        materialInputDisplays = java.util.Map.copyOf(displays);
        return this;
    }
    public MaterialInputDisplay materialInputDisplay(int index) { return materialInputDisplays.get(index); }
    /** Viewer membership is separate from quantity-aware physical matching. */
    public Recipe withMaterialInputTag(int index, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag) {
        var display = java.util.Objects.requireNonNull(materialInputDisplays.get(index));
        var displays = new java.util.HashMap<>(materialInputDisplays);
        displays.put(index, new MaterialInputDisplay(display.amount(), display.alternatives(), display.rounding(), tag));
        materialInputDisplays = java.util.Map.copyOf(displays);
        return this;
    }

    public java.util.Map<Integer, gregtech.api.addon.recipe.MaterialUnitIngredient> materialDeclarations = java.util.Map.of();

    public final ItemStack[] itemInputs;
    public final ItemStack[] itemOutputs;
    public final FluidStack[] fluidInputs;
    public final FluidStack[] fluidOutputs;
    public final long[] outputChances;
    public final long[] maxOutputChances;
    public final long durationTicks;
    public final long energyPerTick;
    public final long specialValue;

    public boolean enabled = true;
    public boolean hidden;
    public boolean fakeRecipe;
    public boolean canBeBuffered = true;
    public boolean needsEmptyOutput;
    public boolean materialRecovery;
    public boolean requiresEmptyContainerInputs;
    /** Blueprint recipes require exact stack data and only their explicit retained inputs. */
    public boolean exactItemInputs, explicitCatalystsOnly;

    /**
     * Explicit selection order among rows that could otherwise match the same machine input.
     * A higher value wins regardless of registration order; equal priorities keep the row that the
     * map registered first. Every row defaults to zero, so built-in content is unaffected until a
     * producer states its intent.
     */
    public long priority;

    /**
     * Structured recovery ownership. Generic rows are fallback rows for an input family; explicit
     * rows are returned by the producer that owns the more specific receiver or form.
     */
    public RecoveryOwnership recoveryOwnership;

    public record RecoveryOwnership(String family, Scope scope) {
        public enum Scope {
            GENERIC,
            EXPLICIT
        }

        public RecoveryOwnership {
            if (family == null || family.isBlank() || scope == null)
                throw new IllegalArgumentException("Recovery ownership");
        }
    }

    /**
     * Which producer published this row ({@code datapack:<id>}, {@code addon}); empty means the
     * built-in content tables. Diagnostics and the recipe dump read it; it never participates in
     * matching.
     */
    public String source = "";

    /**
     * Map-assigned publication order. Used only as the deterministic tie-breaker between rows of
     * equal priority, replacing the accidental ordering of the backing collection.
     */
    long registrationOrder = -1L;

    public Recipe withMaterialRecovery() { materialRecovery = true; requiresEmptyContainerInputs = true; return this; }
    public Recipe withEmptyContainerInputs() { requiresEmptyContainerInputs = true; return this; }

    /** Marks the explicit selection order; see {@link #priority}. */
    public Recipe withPriority(long priority) { this.priority = priority; return this; }

    /** Marks generic or explicit recovery ownership; see {@link #recoveryOwnership}. */
    public Recipe withRecoveryOwnership(String family, RecoveryOwnership.Scope scope) {
        recoveryOwnership = new RecoveryOwnership(family, scope);
        return this;
    }

    /** Records the producing entry point; see {@link #source}. */
    public Recipe withSource(String source) { this.source = source == null ? "" : source; return this; }

    public Recipe(ItemStack[] itemInputs, ItemStack[] itemOutputs, long[] outputChances,
                  FluidStack[] fluidInputs, FluidStack[] fluidOutputs,
                  long durationTicks, long energyPerTick, long specialValue) {
        this.itemInputs = itemInputs != null ? itemInputs : NO_ITEMS;
        this.itemOutputs = itemOutputs != null ? itemOutputs : NO_ITEMS;
        for (int i = 0; i < this.itemOutputs.length; i++) {
            if (this.itemOutputs[i] != null)
                this.itemOutputs[i] = gregtech.content.block.misc.LegacyConstructionBlockItem.canonical(this.itemOutputs[i]);
        }
        this.fluidInputs = fluidInputs != null ? fluidInputs : NO_FLUIDS;
        this.fluidOutputs = fluidOutputs != null ? fluidOutputs : NO_FLUIDS;
        this.outputChances = outputChances != null ? outputChances
                : this.itemOutputs.length == 0 ? NO_CHANCES : new long[this.itemOutputs.length];
        this.maxOutputChances = this.outputChances.length == 0 ? NO_CHANCES : new long[this.outputChances.length];
        for (int i = 0; i < this.outputChances.length; i++) {
            if (this.outputChances[i] <= 0) this.outputChances[i] = 10000;
            this.maxOutputChances[i] = 10000;
        }
        this.durationTicks = Math.max(1, durationTicks);
        this.energyPerTick = energyPerTick;
        this.specialValue = specialValue;
    }

    /** Allocated on the first mark; unmarked recipes keep every group absent. */
    private RecipeInputMarks catalystInputs;

    private java.util.Map<Integer, java.util.function.Predicate<ItemStack>> sameItemInputPredicates = java.util.Map.of();
    private java.util.Map<Integer, java.util.function.Predicate<ItemStack>> itemInputGuards = java.util.Map.of();

    /** Builds the executable row for a state-dependent stack, currently damaged recovery items. */
    @FunctionalInterface
    public interface RuntimeResolver {
        @javax.annotation.Nullable
        Recipe resolve(java.util.List<ItemStack> inputs);
    }

    private RuntimeResolver runtimeResolver;

    /**
     * Adds a runtime row builder without changing static viewer/map identity. The caller keeps the
     * template's ownership, priority and source on the returned row.
     */
    public Recipe withRuntimeResolver(RuntimeResolver resolver) {
        runtimeResolver = java.util.Objects.requireNonNull(resolver, "runtime resolver");
        return this;
    }

    public boolean hasRuntimeResolver() {
        return runtimeResolver != null;
    }

    @javax.annotation.Nullable
    public Recipe resolveRuntime(java.util.List<ItemStack> inputs) {
        return runtimeResolver == null ? null : runtimeResolver.resolve(inputs);
    }

    /**
     * Adds a restriction after ordinary ingredient matching, retaining material/tag aliases and any
     * same-item predicate. Configure the normalized recipe before publishing it. Guards must not
     * mutate stacks and are evaluated live for admission, lookup and consumption, never cached.
     * Repeated calls for a slot compose restrictions rather than replacing an earlier guard.
     */
    public Recipe withItemInputGuard(int index, java.util.function.Predicate<ItemStack> guard) {
        if (index < 0 || index >= itemInputs.length || itemInputs[index] == null || itemInputs[index].isEmpty()
                || guard == null) throw new IllegalArgumentException("Item input guard " + index);
        var guards = new java.util.HashMap<>(itemInputGuards);
        guards.merge(index, guard, (previous, added) -> previous.and(added));
        itemInputGuards = java.util.Map.copyOf(guards);
        return this;
    }

    public boolean hasItemInputGuard(int index) { return itemInputGuards.containsKey(index); }

    boolean hasItemInputGuards() { return !itemInputGuards.isEmpty(); }

    /**
     * Replace only this input's stack-data comparison, never its Item identity or consumption gates.
     * Configure the final, normalized recipe before publishing it to a map. Predicates must not mutate
     * the offered stack; they are evaluated again for each candidate slot, including consumption.
     */
    public Recipe withSameItemInputPredicate(int index, java.util.function.Predicate<ItemStack> predicate) {
        if (index < 0 || index >= itemInputs.length || itemInputs[index] == null || itemInputs[index].isEmpty()
                || predicate == null) throw new IllegalArgumentException("Item input predicate " + index);
        var predicates = new java.util.HashMap<>(sameItemInputPredicates);
        predicates.put(index, predicate);
        sameItemInputPredicates = java.util.Map.copyOf(predicates);
        return this;
    }

    public boolean hasSameItemInputPredicate(int index) {
        return sameItemInputPredicates.containsKey(index);
    }

    boolean hasSameItemInputPredicates() {
        return !sameItemInputPredicates.isEmpty();
    }

    /** Count-independent ingredient match. Exact-input, catalyst and recovery rules remain in Matcher. */
    public boolean matchesItemInput(int index, ItemStack available) {
        if (index < 0 || index >= itemInputs.length || available == null || available.isEmpty()) return false;
        ItemStack required = itemInputs[index];
        if (required == null || required.isEmpty()) return false;
        var predicate = sameItemInputPredicates.get(index);
        var tag = itemTags.get(index);
        boolean matches = tag != null ? available.is(tag) && RecipeInputs.emptyContainerMatches(required, available)
                && (predicate == null || predicate.test(available))
                : predicate == null ? RecipeInputs.matches(required, available)
                : required.is(available.getItem()) && RecipeInputs.emptyContainerMatches(required, available)
                && predicate.test(available);
        var guard = itemInputGuards.get(index);
        return matches && (guard == null || guard.test(available));
    }

    private java.util.Map<Integer, java.util.List<ItemStack>> viewerInputAlternatives = java.util.Map.of();
    /** Informational choices for one displayed slot; this never broadens machine matching. */
    public Recipe withViewerInputAlternatives(int index, java.util.List<ItemStack> alternatives) {
        if (index < 0 || index >= itemInputs.length || alternatives.isEmpty()) throw new IllegalArgumentException("Viewer input " + index);
        if (viewerInputAlternatives.isEmpty()) viewerInputAlternatives = new java.util.HashMap<>();
        viewerInputAlternatives.put(index, alternatives.stream().map(ItemStack::copy).toList());
        return this;
    }
    public java.util.List<ItemStack> viewerInputAlternatives(int index) {
        var tag = itemTags.get(index);
        if (tag != null) {
            var choices = new java.util.ArrayList<ItemStack>();
            for (var holder : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(tag))
                if (holder.value() != net.minecraft.world.item.Items.AIR)
                    choices.add(new ItemStack(holder.value(), itemInputs[index].getCount()));
            return java.util.List.copyOf(choices);
        }
        var stored = viewerInputAlternatives.get(index);
        if (stored == null)
            return gregtech.content.block.misc.LegacyConstructionBlockItem.ingredientAlternatives(itemInputs[index]);
        return stored.stream().map(ItemStack::copy).toList();
    }

    private java.util.Map<Integer, String> viewerInputNotes = java.util.Map.of();

    /** Viewer-only translation key; never stored on an ingredient or used for matching/collisions. */
    public Recipe withViewerInputNote(int index, String translationKey) {
        if (index < 0 || index >= itemInputs.length || translationKey == null || translationKey.isBlank())
            throw new IllegalArgumentException("Viewer input note " + index);
        var notes = new java.util.HashMap<>(viewerInputNotes);
        notes.put(index, translationKey);
        viewerInputNotes = java.util.Map.copyOf(notes);
        return this;
    }

    public String viewerInputNote(int index) {
        return viewerInputNotes.getOrDefault(index, "");
    }

    /** Mark non-consumable lenses/etc. before publishing this recipe to a map. */
    public Recipe withCatalystInputs(int... indices) {
        for (int index : indices) {
            if (index < 0 || index >= itemInputs.length) throw new IllegalArgumentException("Catalyst input " + index);
            if (catalystInputs == null) catalystInputs = new RecipeInputMarks();
            catalystInputs.set(index);
        }
        return this;
    }
    public boolean isCatalystInput(int index) {
        boolean marked = catalystInputs != null && catalystInputs.get(index);
        boolean itemCatalyst = !marked && !explicitCatalystsOnly
                && itemInputs[index].getItem() instanceof RecipeCatalystLike tool && tool.isCatalyst();
        return RecipeCatalystInputs.isCatalyst(explicitCatalystsOnly, marked, itemCatalyst);
    }

    /** Indexes passed to {@link #withCatalystInputs(int...)}, in ascending order. Implicit catalyst items are not listed. */
    public int[] explicitCatalystInputs() {
        return catalystInputs == null ? RecipeInputMarks.NO_INDEXES : catalystInputs.toArray();
    }

    private RecipeInputMarks catalystFluidInputs;

    /** Mark fluid inputs that are checked at their declared amount and not drained. */
    public Recipe withCatalystFluidInputs(int... indices) {
        for (int index : indices) {
            if (index < 0 || index >= fluidInputs.length) throw new IllegalArgumentException("Catalyst fluid input " + index);
            if (isPresenceFluidInput(index)) throw new IllegalArgumentException("Fluid input " + index + " is presence-only");
            if (catalystFluidInputs == null) catalystFluidInputs = new RecipeInputMarks();
            catalystFluidInputs.set(index);
        }
        return this;
    }

    public boolean isCatalystFluidInput(int index) {
        return index >= 0 && index < fluidInputs.length && catalystFluidInputs != null && catalystFluidInputs.get(index);
    }

    /** Indexes passed to {@link #withCatalystFluidInputs(int...)}, in ascending order. */
    public int[] explicitCatalystFluidInputs() {
        return catalystFluidInputs == null ? RecipeInputMarks.NO_INDEXES : catalystFluidInputs.toArray();
    }

    private RecipeInputMarks presenceFluidInputs;

    /** Presence-only gates use positive native representatives, never zero-sized native stacks. */
    public Recipe withPresenceFluidInputs(int... indices) {
        for (int index : RecipeKeptInputs.normalizeFluids(indices, fluidInputs.length)) {
            if (fluidInputs[index] == null || fluidInputs[index].isEmpty())
                throw new IllegalArgumentException("Presence fluid input " + index + " needs a positive representative");
            if (isCatalystFluidInput(index))
                throw new IllegalArgumentException("Fluid input " + index + " cannot be both kept and presence-only");
            fluidInputs[index] = fluidInputs[index].copy();
            fluidInputs[index].setAmount(1);
            if (presenceFluidInputs == null) presenceFluidInputs = new RecipeInputMarks();
            presenceFluidInputs.set(index);
        }
        return this;
    }

    public boolean isPresenceFluidInput(int index) {
        return index >= 0 && index < fluidInputs.length && presenceFluidInputs != null && presenceFluidInputs.get(index);
    }

    /** Whether any fluid input is a presence-only gate. */
    boolean hasPresenceFluidInputs() { return presenceFluidInputs != null && !presenceFluidInputs.isEmpty(); }

    public int[] explicitPresenceFluidInputs() {
        return presenceFluidInputs == null ? RecipeInputMarks.NO_INDEXES : presenceFluidInputs.toArray();
    }

    // ── Output helpers ───────────────────────────────────────────────────

    public int getOutputChance(int idx) {
        if (idx < 0 || idx >= outputChances.length) return 10000;
        return (int) outputChances[idx];
    }

    /** GT6 rolls each output item independently, including every parallel operation. */
    public long rollOutputCount(int index, int processes, java.util.function.IntUnaryOperator random) {
        if (index < 0 || index >= itemOutputs.length || processes <= 0) return 0;
        ItemStack output = itemOutputs[index];
        if (output == null || output.isEmpty()) return 0;
        return RecipeChanceRules.rollOutputCount(output.getCount(),getOutputChance(index),processes,random);
    }

    public ItemStack getOutput(int idx) {
        if (idx < 0 || idx >= itemOutputs.length) return ItemStack.EMPTY;
        ItemStack s = itemOutputs[idx];
        return s != null ? s.copy() : ItemStack.EMPTY;
    }

    public FluidStack getFluidOutput(int idx) {
        if (idx < 0 || idx >= fluidOutputs.length) return FluidStack.EMPTY;
        FluidStack f = fluidOutputs[idx];
        return f != null ? f.copy() : FluidStack.EMPTY;
    }

    public FluidStack getRepresentativeFluidInput(int idx) {
        if (idx < 0 || idx >= fluidInputs.length) return FluidStack.EMPTY;
        FluidStack f = fluidInputs[idx];
        return f != null ? f.copy() : FluidStack.EMPTY;
    }
}
