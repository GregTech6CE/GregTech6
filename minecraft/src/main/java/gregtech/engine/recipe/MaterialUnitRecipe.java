package gregtech.engine.recipe;

import gregtech.api.addon.recipe.MaterialUnitIngredient.Rounding;
import gregtech.api.material.GTMaterial;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialEquivalence;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** A compact material rule. Only the requested physical batch is instantiated, never the form matrix. */
public final class MaterialUnitRecipe {
    private record Source(ItemStack stack, long units, Predicate<ItemStack> guard, long ticks, int fluidAmount) {}
    private static final Predicate<ItemStack> UNRESTRICTED = ignored -> true;
    private Map<Item, Source> sources = new LinkedHashMap<>();

    /** Bootstrap-scoped interning; only immutable proofs with identical eligibility are shared. */
    public static final class SourcePool {
        private record Key(Item item, int count, long units, Predicate<ItemStack> guard, long ticks, int fluidAmount) {}
        private record Shared(Map<Item, Source> sources, List<ItemStack> stacks, Set<Item> keys) {}
        private final Map<Key, Source> values = new HashMap<>();
        private final Map<Set<Key>, Shared> sets = new HashMap<>();
        private Shared share(Map<Item, Source> source) {
            var keys = new HashSet<Key>();
            for (var entry : source.entrySet()) {
                var value = entry.getValue();
                // Stack data is part of a proof and must not be discarded by an identity-only pool.
                if (!ItemStack.matches(value.stack.copyWithCount(1), new ItemStack(entry.getKey())))
                    return freeze(source);
                var key = new Key(entry.getKey(), value.stack.getCount(), value.units, value.guard, value.ticks, value.fluidAmount);
                keys.add(key); values.putIfAbsent(key, value);
            }
            return sets.computeIfAbsent(Set.copyOf(keys), ignored -> {
                var canonical = new LinkedHashMap<Item, Source>();
                for (var key : keys) canonical.put(key.item, values.get(key));
                return freeze(canonical);
            });
        }
        private static Shared freeze(Map<Item, Source> source) {
            var frozen = Collections.unmodifiableMap(new LinkedHashMap<>(source));
            return new Shared(frozen, frozen.values().stream().map(Source::stack).toList(), Set.copyOf(frozen.keySet()));
        }
    }
    public final Recipe template;
    public final GTMaterial material;
    public final int inputIndex;
    public final long units;
    public final Rounding rounding;
    private RecipeMap owner;

    public MaterialUnitRecipe(Recipe template, int inputIndex, GTMaterial material, long units, Rounding rounding) {
        if (template == null || material == null || units <= 0 || rounding == null
                || inputIndex < 0 || inputIndex >= template.mInputs.length || template.isCatalystInput(inputIndex))
            throw new IllegalArgumentException("Material unit recipe");
        this.template = template; this.inputIndex = inputIndex;
        this.material = material.resolve(); this.units = units; this.rounding = rounding;
    }

    public MaterialUnitRecipe source(ItemStack stack, long weight) {
        return source(stack, weight, UNRESTRICTED, 0, 0);
    }

    /** Existing thermal proofs and batch timings survive compression instead of broadening eligibility. */
    public MaterialUnitRecipe source(ItemStack stack, long weight, Predicate<ItemStack> guard, long ticks, int fluidAmount) {
        if (stack.isEmpty() || weight <= 0 || guard == null) throw new IllegalArgumentException("Material source");
        if (owner != null) throw new IllegalStateException("Configure sources before publishing");
        sources.putIfAbsent(stack.getItem(), new Source(stack.copy(), weight, guard, ticks, fluidAmount));
        return this;
    }

    public Recipe publish(RecipeMap map) { return publish(map, null); }
    public Recipe publish(RecipeMap map, SourcePool pool) {
        if (owner != null || sources.isEmpty()) throw new IllegalStateException("Empty or already published material rule");
        owner = map;
        var shared = pool == null ? SourcePool.freeze(sources) : pool.share(sources);
        sources = shared.sources;
        template.withMaterialInputDisplay(inputIndex, units, shared.stacks, rounding);
        map.addFakeRecipe(false, template);
        MaterialUnitRecipeIndex.of(map).add(this, shared.keys);
        return template;
    }

    public void remove() {
        if (owner == null) return;
        owner.unlink(template);
        MaterialUnitRecipeIndex.of(owner).remove(this);
        owner = null;
    }

    boolean active() { return owner != null && template.mEnabled && owner.mRecipeList.contains(template); }

    private Source source(ItemStack stack) {
        Source direct = sources.get(stack.getItem());
        if (direct != null) return direct;
        for (Source source : sources.values())
            if (MaterialEquivalence.matches(source.stack, stack)) return source;
        return null;
    }

    boolean accepts(ItemStack stack) {
        if (!active() || stack.isEmpty() || ItemMaterialRegistry.hasStoredContents(stack)) return false;
        Source source = source(stack);
        return source != null && source.guard.test(stack) && RecipeInputs.matches(source.stack, stack);
    }

    Recipe resolve(ItemStack offered) {
        if (!accepts(offered)) return null;
        Source source = source(offered);
        try {
            var quantities = new ArrayList<Long>();
            for (var output : template.mOutputs) if (!output.isEmpty()) quantities.add((long) output.getCount());
            for (var output : template.mFluidOutputs) if (!output.isEmpty()) quantities.add((long) output.getAmount());
            for (int i = 0; i < template.mInputs.length; i++)
                if (i != inputIndex && !template.isCatalystInput(i)) quantities.add((long) template.mInputs[i].getCount());
            for (int i = 0; i < template.mFluidInputs.length; i++)
                if (!template.isCatalystFluidInput(i) && !template.isPresenceFluidInput(i)) quantities.add((long) template.mFluidInputs[i].getAmount());
            MaterialUnitBatch batch = rounding == Rounding.WHOLE_OUTPUTS
                    ? MaterialUnitBatch.wholeOutputs(source.units, units)
                    : MaterialUnitBatch.exactRatio(source.units, units, quantities.stream().mapToLong(Long::longValue).toArray());
            if (batch == null) return null;
            // A reviewed thermal input can keep its original batch and rounded duration exactly.
            boolean thermal = source.fluidAmount > 0;
            var inputs = new ItemStack[template.mInputs.length];
            for (int i = 0; i < inputs.length; i++) {
                var original = template.mInputs[i];
                inputs[i] = i == inputIndex ? offered.copyWithCount(thermal ? source.stack.getCount() : batch.inputCount())
                        : original.copyWithCount(template.isCatalystInput(i) ? original.getCount()
                        : Math.toIntExact(batch.scale(original.getCount(), false)));
            }
            var outputs = new ArrayList<ItemStack>();
            var chances = new ArrayList<Long>();
            for (int i = 0; i < template.mOutputs.length; i++) {
                var original = template.mOutputs[i];
                long count = batch.scale(original.getCount(), false);
                if (count > 128) return null;
                while (count > 0) {
                    int part = (int) Math.min(original.getMaxStackSize(), count);
                    outputs.add(original.copyWithCount(part)); chances.add((long) template.getOutputChance(i)); count -= part;
                }
            }
            if (outputs.size() > owner.mOutputItemsCount) return null;
            var fluidInputs = new FluidStack[template.mFluidInputs.length];
            for (int i = 0; i < fluidInputs.length; i++) {
                fluidInputs[i] = template.mFluidInputs[i].copy();
                if (!template.isCatalystFluidInput(i) && !template.isPresenceFluidInput(i))
                    fluidInputs[i].setAmount(Math.toIntExact(batch.scale(fluidInputs[i].getAmount(), false)));
            }
            var fluidOutputs = new FluidStack[template.mFluidOutputs.length];
            for (int i = 0; i < fluidOutputs.length; i++) {
                fluidOutputs[i] = template.mFluidOutputs[i].copy();
                fluidOutputs[i].setAmount(thermal ? source.fluidAmount
                        : Math.toIntExact(batch.scale(fluidOutputs[i].getAmount(), false)));
            }
            long ticks = thermal ? source.ticks : Math.max(1, batch.scale(template.mDuration, true));
            var result = new Recipe(inputs, outputs.toArray(ItemStack[]::new), template.mSpecialItems,
                    chances.stream().mapToLong(Long::longValue).toArray(), fluidInputs, fluidOutputs,
                    ticks, template.mEUt, template.mSpecialValue)
                    .withCatalystInputs(template.explicitCatalystInputs())
                    .withCatalystFluidInputs(template.explicitCatalystFluidInputs())
                    .withPresenceFluidInputs(template.explicitPresenceFluidInputs()).withEmptyContainerInputs();
            result.mExplicitCatalystsOnly = template.mExplicitCatalystsOnly;
            result.mExactItemInputs = template.mExactItemInputs;
            result.mNeedsEmptyOutput = template.mNeedsEmptyOutput;
            result.mMaterialRecovery = template.mMaterialRecovery;
            result.withItemInputGuard(inputIndex, stack -> active() && source.guard.test(stack)
                    && RecipeInputs.matches(source.stack, stack));
            for (int i = 0; i < inputs.length; i++) if (i != inputIndex) {
                final int index = i;
                result.withItemInputGuard(i, stack -> template.matchesItemInput(index, stack));
                if (template.itemTag(i) != null) result.withItemTagInput(i, template.itemTag(i).location());
            }
            for (int i = 0; i < fluidInputs.length; i++)
                if (template.fluidTag(i) != null) result.withFluidTagInput(i, template.fluidTag(i).location());
            return result;
        } catch (ArithmeticException tooLarge) { return null; }
    }
}
