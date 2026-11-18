package gregtech.engine.recipe;

import static gregtech.engine.recipe.RecipeMap.empty;
import static gregtech.engine.recipe.RecipeMap.fluidKey;

import gregtech.engine.stack.StackCompat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Duplicate registration checks for a recipe map, independent of runtime recipe selection. */
final class RecipeCollisionIndex {
    private final RecipeMap map;
    private final RecipeLookupTable lookupTable;
    /** Anvil rows treat an empty input slot as a required empty work area. */
    private final boolean anvilRowMap;

    RecipeCollisionIndex(RecipeMap map, RecipeLookupTable lookupTable, boolean anvilRowMap) {
        this.map = map;
        this.lookupTable = lookupTable;
        this.anvilRowMap = anvilRowMap;
    }

    Batch startBatch() {
        return new Batch();
    }

    /** Temporary required-item anchors for the lifetime of a single ordered bulk registration. */
    final class Batch {
        private final Map<Item, List<Recipe>> anchors = new IdentityHashMap<>();
        private final List<Recipe> unanchored = new ArrayList<>();

        private Batch() {
            Set<Recipe> indexed = Collections.newSetFromMap(new IdentityHashMap<>());
            for (var pool : map.recipesByInputItem.values()) indexed.addAll(pool);
            for (var pool : map.recipesByInputFluid.values()) indexed.addAll(pool);
            for (Recipe row : indexed) add(row);
        }

        Recipe findCollision(Recipe candidate) {
            if (candidate.hasTaggedInputs()) return map.findCollision(candidate);
            for (ItemStack input : candidate.itemInputs) {
                if (empty(input)) continue;
                var pool = anchors.get(input.getItem());
                if (pool == null) continue;
                for (Recipe existing : pool)
                    if (collisionReachable(existing, candidate) && inputsSatisfiedBy(existing, candidate))
                        return existing;
            }
            // A zero-amount non-presence fluid is indexed but not required by the matcher.
            // Keep the key reachability test for rows that have no required item anchor.
            for (Recipe existing : unanchored)
                if (collisionReachable(existing, candidate) && inputsSatisfiedBy(existing, candidate))
                    return existing;
            return null;
        }

        void add(Recipe row) {
            if (row.hasTaggedInputs()) {
                // covers() cannot prove guarded domains; do not rescan them for every concrete row.
                if (!row.hasItemInputGuards() && !row.hasSameItemInputPredicates()) unanchored.add(row);
                return;
            }
            for (ItemStack required : row.itemInputs) if (!empty(required)) {
                anchors.computeIfAbsent(required.getItem(), key -> new ArrayList<>()).add(row);
                return;
            }
            unanchored.add(row);
        }
    }

    private boolean collisionReachable(Recipe existing, Recipe candidate) {
        if (existing.hasTaggedInputs() || candidate.hasTaggedInputs()) return true;
        for (ItemStack input : candidate.itemInputs) if (!empty(input)) {
            var pool = map.recipesByInputItem.get(input.getItem());
            if (pool != null && pool.contains(existing)) return true;
        }
        for (FluidStack input : candidate.fluidInputs) if (input != null && input.getFluid() != Fluids.EMPTY) {
            var pool = map.recipesByInputFluid.get(fluidKey(input));
            if (pool != null && pool.contains(existing)) return true;
        }
        return false;
    }

    /**
     * Returns an already-registered recipe whose inputs would also be satisfied by the given
     * recipe's inputs. Only rows whose required identities are all offered by the candidate can
     * qualify, so the candidate enumerates the subsets of its own identities instead of walking
     * the shared input pools; rows with more identities than {@link #COLLISION_SIGNATURE_KEYS}
     * keep the pool scan. Both paths end in {@link #inputsSatisfiedBy}, which decides alone.
     */
    Recipe findCollision(Recipe recipe) {
        // Guarded rows narrow their domain with an opaque predicate; no other row can prove it
        // covers or is covered by that domain, so they coexist instead of one being rejected.
        if (recipe.hasItemInputGuards() || recipe.hasSameItemInputPredicates()) return null;
        if (recipe.hasTaggedInputs()) {
            for (Recipe existing : map.recipeRows)
                if (conditionsEqual(existing, recipe) && RecipeTagInputs.covers(existing, recipe)) return existing;
            return null;
        }
        for (Recipe existing : lookupTable.tagged())
            if (conditionsEqual(existing, recipe) && RecipeTagInputs.covers(existing, recipe)) return existing;
        if (collisionDirty || indexedCollisionRows != map.recipeRows.size()) rebuildCollisionIndex();
        int[] keys = availableCollisionIds(recipe);
        if (keys.length > COLLISION_SIGNATURE_KEYS) return findCollisionByPools(recipe);
        for (int mask = 1; mask < (1 << keys.length); mask++) {
            int[] subset = new int[Integer.bitCount(mask)];
            int next = 0;
            for (int bit = 0; bit < keys.length; bit++) if ((mask & (1 << bit)) != 0) subset[next++] = keys[bit];
            var bucket = collisionBuckets.get(new CollisionSignature(subset));
            if (bucket == null) continue;
            for (Recipe existing : bucket) if (inputsSatisfiedBy(existing, recipe)) return existing;
        }
        // A row that requires nothing is reachable only through an input the candidate also has.
        for (Recipe existing : collisionDegenerate)
            if (sharesReachKey(existing, keys) && inputsSatisfiedBy(existing, recipe)) return existing;
        return null;
    }

    private boolean sharesReachKey(Recipe existing, int[] candidateKeys) {
        for (FluidStack fluid : existing.fluidInputs) {
            if (fluid == null || fluid.getFluid() == Fluids.EMPTY) continue;
            int id = collisionFluidId(fluidKey(fluid));
            for (int key : candidateKeys) if (key == id) return true;
        }
        return false;
    }

    /** The original input-index scan, kept for candidates with too many identities. */
    private Recipe findCollisionByPools(Recipe recipe) {
        Set<Recipe> checked = null;
        for (ItemStack in : recipe.itemInputs) {
            if (empty(in)) continue;
            Collection<Recipe> pool = map.recipesByInputItem.get(in.getItem());
            if (pool == null) continue;
            if (checked == null) checked = new it.unimi.dsi.fastutil.objects.ObjectOpenHashSet<>();
            for (Recipe t : pool) if (collisionItemTypesPossible(t, recipe)
                    && checked.add(t) && inputsSatisfiedBy(t, recipe)) return t;
        }
        for (FluidStack f : recipe.fluidInputs) {
            if (f == null || f.getFluid() == Fluids.EMPTY) continue;
            Collection<Recipe> pool = map.recipesByInputFluid.get(fluidKey(f));
            if (pool == null) continue;
            if (checked == null) checked = new it.unimi.dsi.fastutil.objects.ObjectOpenHashSet<>();
            for (Recipe t : pool) if (collisionItemTypesPossible(t, recipe)
                    && checked.add(t) && inputsSatisfiedBy(t, recipe)) return t;
        }
        return null;
    }

    /** Reject unrelated mold/tool buckets before allocating entries in the collision visited set. */
    private static boolean collisionItemTypesPossible(Recipe existingRecipe, Recipe candidateRecipe) {
        if (existingRecipe.hasTaggedInputs() || candidateRecipe.hasTaggedInputs()) return true;
        for (ItemStack required : existingRecipe.itemInputs) {
            if (empty(required)) continue;
            boolean present = false;
            for (ItemStack candidate : candidateRecipe.itemInputs) {
                if (!empty(candidate) && candidate.getItem() == required.getItem()) {
                    present = true;
                    break;
                }
            }
            if (!present) return false;
        }
        return true;
    }

    /**
     * Whether two rows that may see the same inputs also have the same execution conditions.
     * Rows that differ in energy, duration, probability, special value, priority, catalyst role or
     * exactness are legitimate variants of one another and may coexist: the machine's own matcher
     * decides between them, and {@link RecipeMap#findRecipe} prefers the higher priority. Only rows whose
     * conditions are identical are duplicates, which is the case the collision check rejects.
     */
    static boolean conditionsEqual(Recipe a, Recipe b) {
        return a.specialValue == b.specialValue
                && a.priority == b.priority
                && a.energyPerTick == b.energyPerTick
                && a.durationTicks == b.durationTicks
                && a.needsEmptyOutput == b.needsEmptyOutput
                && a.exactItemInputs == b.exactItemInputs
                && a.explicitCatalystsOnly == b.explicitCatalystsOnly
                && a.requiresEmptyContainerInputs == b.requiresEmptyContainerInputs
                && a.materialRecovery == b.materialRecovery
                && Arrays.equals(a.outputChances, b.outputChances)
                && Arrays.equals(a.explicitCatalystInputs(), b.explicitCatalystInputs())
                && Arrays.equals(a.explicitCatalystFluidInputs(), b.explicitCatalystFluidInputs())
                && Arrays.equals(a.explicitPresenceFluidInputs(), b.explicitPresenceFluidInputs());
    }

    /** Whether every input of {@code existingRecipe} is covered by the inputs of {@code candidateRecipe}. */
    private boolean inputsSatisfiedBy(Recipe existingRecipe, Recipe candidateRecipe) {
        if (existingRecipe.fakeRecipe) return false;
        if (existingRecipe.hasItemInputGuards() || existingRecipe.hasSameItemInputPredicates()) return false;
        if (!conditionsEqual(existingRecipe, candidateRecipe)) return false;
        // In an anvil recipe an empty entry is a required empty work area, not a wildcard.
        if (anvilRowMap) {
            long requiredEmpty = java.util.Arrays.stream(existingRecipe.itemInputs).filter(RecipeMap::empty).count();
            long candidateEmpty = java.util.Arrays.stream(candidateRecipe.itemInputs).filter(RecipeMap::empty).count();
            if (requiredEmpty != candidateEmpty) return false;
        }
        if (existingRecipe.hasTaggedInputs() || candidateRecipe.hasTaggedInputs()) return RecipeTagInputs.covers(existingRecipe, candidateRecipe);
        // Common inputs such as molds put many unrelated recipes in the same index bucket.
        // Reject an impossible item match before copying any stacks, while retaining the full
        // count and NBT/component comparison below for every possible match.
        for (ItemStack required : existingRecipe.itemInputs) {
            if (empty(required)) continue;
            boolean present = false;
            for (ItemStack candidate : candidateRecipe.itemInputs) {
                if (!empty(candidate) && candidate.getItem() == required.getItem()) {
                    present = true;
                    break;
                }
            }
            if (!present) return false;
        }
        var items = java.util.Arrays.stream(candidateRecipe.itemInputs)
                .map(s -> empty(s) ? ItemStack.EMPTY : s.copy()).toArray(ItemStack[]::new);
        for (ItemStack in : existingRecipe.itemInputs) {
            if (empty(in)) continue;
            int needed = in.getCount();
            for (ItemStack s : items) {

                if (!empty(s) && StackCompat.sameItem(in, s)) {

                    int take = Math.min(needed, s.getCount());
                    s.shrink(take); needed -= take;
                    if (needed == 0) break;
                }
            }
            if (needed > 0) return false;
        }
        var fluids = java.util.Arrays.stream(candidateRecipe.fluidInputs)
                .map(f -> f == null ? FluidStack.EMPTY : f.copy()).toArray(FluidStack[]::new);
        // Presence representatives are identities, not one extra mB available to satisfy consumption.
        for (int i = 0; i < fluids.length; i++) if (candidateRecipe.isPresenceFluidInput(i)) fluids[i] = FluidStack.EMPTY;
        for (int inputIndex = 0; inputIndex < existingRecipe.fluidInputs.length; inputIndex++) {
            FluidStack in = existingRecipe.fluidInputs[inputIndex];
            if (in == null || in.getFluid() == Fluids.EMPTY) continue;
            if (existingRecipe.isPresenceFluidInput(inputIndex)) {
                boolean present = false;
                for (FluidStack f : candidateRecipe.fluidInputs) {
                    if (f != null && !f.isEmpty() && RecipeInputs.sameFluid(f, in)) { present = true; break; }
                }
                if (!present) return false;
                continue;
            }
            if (in.isEmpty()) continue;
            int needed = in.getAmount();
            for (FluidStack f : fluids) {

                if (!f.isEmpty() && StackCompat.sameFluid(f,in)) {

                    int take = Math.min(needed, f.getAmount());
                    f.shrink(take); needed -= take;
                    if (needed == 0) break;
                }
            }
            if (needed > 0) return false;
        }
        return true;
    }

    /**
     * Ceiling on the distinct input identities a new row may have before the collision check falls
     * back to the input-index scan. Candidates are found by enumerating the identity subsets, which
     * costs 2^n lookups; the shipped content peaks at four, so this only bounds addon rows.
     */
    private static final int COLLISION_SIGNATURE_KEYS = 10;

    /** Strict required-input identities of one stored row, in ascending identity order. */
    private static final class CollisionSignature {
        private final int[] ids;
        private final int hash;

        CollisionSignature(int[] ids) {
            this.ids = ids;
            this.hash = Arrays.hashCode(ids);
        }

        @Override public int hashCode() { return hash; }

        @Override public boolean equals(Object other) {
            return other instanceof CollisionSignature signature && hash == signature.hash
                    && Arrays.equals(ids, signature.ids);
        }
    }

    /** Stored rows grouped by the exact set of inputs they require. */
    private final Map<CollisionSignature, List<Recipe>> collisionBuckets = new HashMap<>();
    private final Map<Item, Integer> collisionItemIds = new IdentityHashMap<>();
    private final Map<String, Integer> collisionFluidIds = new HashMap<>();
    /** Rows that require nothing but are still reachable through a zero-amount fluid. */
    private final List<Recipe> collisionDegenerate = new ArrayList<>();
    private int nextCollisionId;
    /** Rows this index has seen; a different count means the public collection was changed directly. */
    private volatile int indexedCollisionRows;
    /** Set when registered rows may have changed identity in place; the next check rebuilds. */
    private volatile boolean collisionDirty;

    private int collisionItemId(Item item) {
        return collisionItemIds.computeIfAbsent(item, unused -> nextCollisionId++);
    }

    private int collisionFluidId(String key) {
        Integer id = collisionFluidIds.get(key);
        if (id == null) collisionFluidIds.put(key, id = nextCollisionId++);
        return id;
    }

    /** Identities a stored row needs from a candidate before {@link #inputsSatisfiedBy} can pass. */
    private int[] requiredCollisionIds(Recipe recipe) {
        int[] ids = new int[recipe.itemInputs.length + recipe.fluidInputs.length];
        int count = 0;
        for (ItemStack required : recipe.itemInputs) {
            if (empty(required)) continue;
            ids[count++] = collisionItemId(required.getItem());
        }
        for (int index = 0; index < recipe.fluidInputs.length; index++) {
            FluidStack required = recipe.fluidInputs[index];
            if (required == null || required.getFluid() == Fluids.EMPTY) continue;
            // A zero-amount non-presence fluid is indexed by add() but not required by the matcher.
            if (!recipe.isPresenceFluidInput(index) && required.getAmount() <= 0) continue;
            ids[count++] = collisionFluidId(fluidKey(required));
        }
        return distinctSorted(ids, count);
    }

    /** Identities a candidate offers, matching the pools {@link RecipeMap#add} would index it under. */
    private int[] availableCollisionIds(Recipe recipe) {
        int[] ids = new int[recipe.itemInputs.length + recipe.fluidInputs.length];
        int count = 0;
        for (ItemStack available : recipe.itemInputs) {
            if (empty(available)) continue;
            ids[count++] = collisionItemId(available.getItem());
        }
        for (FluidStack available : recipe.fluidInputs) {
            if (available == null || available.getFluid() == Fluids.EMPTY) continue;
            ids[count++] = collisionFluidId(fluidKey(available));
        }
        return distinctSorted(ids, count);
    }

    private static int[] distinctSorted(int[] ids, int count) {
        Arrays.sort(ids, 0, count);
        int size = 0;
        for (int index = 0; index < count; index++)
            if (size == 0 || ids[size - 1] != ids[index]) ids[size++] = ids[index];
        return Arrays.copyOf(ids, size);
    }

    void add(Recipe recipe) {
        indexedCollisionRows++;
        int[] ids = requiredCollisionIds(recipe);
        if (ids.length == 0) {
            if (reachableFluid(recipe)) collisionDegenerate.add(recipe);
            return;
        }
        collisionBuckets.computeIfAbsent(new CollisionSignature(ids), unused -> new ArrayList<>()).add(recipe);
    }

    void remove(Recipe recipe) {
        indexedCollisionRows--;
        int[] ids = requiredCollisionIds(recipe);
        if (ids.length == 0) {
            collisionDegenerate.remove(recipe);
            return;
        }
        var signature = new CollisionSignature(ids);
        var bucket = collisionBuckets.get(signature);
        if (bucket == null) return;
        bucket.remove(recipe);
        if (bucket.isEmpty()) collisionBuckets.remove(signature);
    }

    private static boolean reachableFluid(Recipe recipe) {
        for (FluidStack fluid : recipe.fluidInputs)
            if (fluid != null && fluid.getFluid() != Fluids.EMPTY) return true;
        return false;
    }

    void clear() {
        collisionBuckets.clear();
        collisionDegenerate.clear();
        indexedCollisionRows = 0;
        collisionDirty = false;
    }

    void invalidate() {
        collisionDirty = true;
    }

    private void rebuildCollisionIndex() {
        synchronized (map) {
            clear();
            for (Recipe recipe : map.recipeRows) add(recipe);
        }
    }
}
