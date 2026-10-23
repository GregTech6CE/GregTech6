package com.gregtech.gregtech.api.recipe;

import java.util.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Ordered exact-input candidates. Mutations through the public collection invalidate the snapshot. */
final class RecipeLookupTable extends AbstractCollection<Recipe> {
    private final Collection<Recipe> rows;
    private Recipe[] snapshot;
    private final Map<Item, List<Integer>> byFirstItem = new IdentityHashMap<>();
    private BitSet noItems;
    private final Deque<Candidates> recent = new ArrayDeque<>();

    record Candidates(List<ItemStack> items, List<FluidStack> fluids,
                      List<Recipe> exact, List<Recipe> alternatives) {}

    RecipeLookupTable(Collection<Recipe> rows) { this.rows = rows; }

    void invalidate() { snapshot = null; recent.clear(); }
    @Override public int size() { return rows.size(); }
    @Override public boolean add(Recipe row) { invalidate(); return rows.add(row); }
    @Override public void clear() { invalidate(); rows.clear(); }
    @Override public boolean remove(Object row) { invalidate(); return rows.remove(row); }
    @Override public boolean removeAll(Collection<?> removed) { invalidate(); return rows.removeAll(removed); }
    @Override public boolean retainAll(Collection<?> kept) { invalidate(); return rows.retainAll(kept); }
    @Override public Iterator<Recipe> iterator() {
        var source = rows.iterator();
        return new Iterator<>() {
            public boolean hasNext() { return source.hasNext(); }
            public Recipe next() { return source.next(); }
            public void remove() { invalidate(); source.remove(); }
        };
    }

    /** Cache candidate identities, never the match result: counts, enabled flags and outputs stay live. */
    Candidates candidates(List<ItemStack> items, List<FluidStack> fluids) {
        if (snapshot == null || snapshot.length != rows.size()) rebuild();
        for (var cached : recent) if (sameInputs(cached, items, fluids)) return cached;
        var exact = new ArrayList<Recipe>();
        var alternatives = new ArrayList<Recipe>();
        for (var row : exactCandidates(items)) {
            if (RecipeMap.exactItems(row, items) && possibleInputs(row, items, fluids)) exact.add(row);
        }
        for (var row : snapshot) {
            if (!RecipeMap.exactItems(row, items) && possibleInputs(row, items, fluids)) alternatives.add(row);
        }
        var itemKey = items.stream().map(s -> s.isEmpty() ? ItemStack.EMPTY : s.copyWithCount(1)).toList();
        var fluidKey = fluids.stream().map(s -> {
            var copy = s.copy();
            if (!copy.isEmpty()) copy.setAmount(1);
            return copy;
        }).toList();
        var result = new Candidates(itemKey, fluidKey, List.copyOf(exact), List.copyOf(alternatives));
        if (recent.size() == 16) recent.removeLast();
        recent.addFirst(result);
        return result;
    }

    private static boolean possibleInputs(Recipe row, List<ItemStack> items, List<FluidStack> fluids) {
        for (var required : row.mInputs) {
            if (required == null || required.isEmpty()) continue;
            boolean found = false;
            // Same-item container capabilities may depend on stack count. Keep these candidates;
            // the live matcher checks contents/components rather than caching that admission result.
            for (var available : items) {
                if (!available.isEmpty() && (required.is(available.getItem()) || RecipeInputs.matches(required, available))) {
                    found = true; break;
                }
            }
            if (!found) return false;
        }
        for (var required : row.mFluidInputs) {
            if (required == null || required.isEmpty()) continue;
            boolean found = false;
            for (var available : fluids) {
                if (!available.isEmpty() && RecipeInputs.sameFluid(available, required)) { found = true; break; }
            }
            if (!found) return false;
        }
        return true;
    }

    private static boolean sameInputs(Candidates cached, List<ItemStack> items, List<FluidStack> fluids) {
        if (cached.items.size() != items.size() || cached.fluids.size() != fluids.size()) return false;
        for (int i = 0; i < items.size(); i++) {
            var a = cached.items.get(i); var b = items.get(i);
            if (a.isEmpty() != b.isEmpty()) return false;
//? if >=1.21.1 {
            /*if (!ItemStack.isSameItemSameComponents(a, b)) return false;
*///?} else {
            if (!ItemStack.isSameItemSameTags(a, b)) return false;
//?}
        }
        for (int i = 0; i < fluids.size(); i++) {
            var a = cached.fluids.get(i); var b = fluids.get(i);
            if (a.isEmpty() != b.isEmpty()) return false;
            if (!a.isEmpty() && !RecipeInputs.sameFluid(a, b)) return false;
        }
        return true;
    }

    Iterable<Recipe> exactCandidates(List<ItemStack> inputs) {
        if (snapshot == null || snapshot.length != rows.size()) rebuild();
        // Every exact match must contain its first required item. Rows without items are always candidates.
        var selected = (BitSet) noItems.clone();
        for (var input : inputs) {
            var matching = byFirstItem.get(input.getItem());
            if (matching != null) for (int index : matching) selected.set(index);
        }
        Recipe[] ordered = snapshot;
        return () -> new Iterator<>() {
            private int index = selected.nextSetBit(0);
            public boolean hasNext() { return index >= 0; }
            public Recipe next() {
                if (index < 0) throw new NoSuchElementException();
                Recipe row = ordered[index];
                index = selected.nextSetBit(index + 1);
                return row;
            }
        };
    }

    private void rebuild() {
        recent.clear();
        snapshot = rows.toArray(Recipe[]::new);
        byFirstItem.clear();
        noItems = new BitSet();
        for (int i = 0; i < snapshot.length; i++) {
            var inputs = snapshot[i].mInputs;
            if (inputs.length == 0) noItems.set(i);
            else byFirstItem.computeIfAbsent(inputs[0].getItem(), item -> new ArrayList<>()).add(i);
        }
    }
}
