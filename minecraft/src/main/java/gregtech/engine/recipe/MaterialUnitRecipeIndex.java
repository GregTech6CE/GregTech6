package gregtech.engine.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.engine.material.MaterialEquivalence;
import java.util.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Per-map candidate index, with bounded batches rather than eagerly expanded recipes. */
final class MaterialUnitRecipeIndex {
    private static final Map<RecipeMap, MaterialUnitRecipeIndex> MAPS = new IdentityHashMap<>();
    private final Map<Item, List<MaterialUnitRecipe>> byItem = new IdentityHashMap<>();
    private final Map<MaterialUnitRecipe, Set<Item>> sourceKeys = new IdentityHashMap<>();
    private final Map<GTMaterial, List<MaterialUnitRecipe>> byMaterial = new IdentityHashMap<>();
    private final Map<Item, List<MaterialUnitRecipe>> catalysts = new IdentityHashMap<>();
    private record Key(MaterialUnitRecipe rule, Item item) {}
    private final Map<Key, Recipe> batches = new LinkedHashMap<>(256, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Key, Recipe> entry) { return size() > 256; }
    };
    static MaterialUnitRecipeIndex of(RecipeMap map) {
        return MAPS.computeIfAbsent(map, key -> {
            var index = new MaterialUnitRecipeIndex();
            key.addDynamicRecipes(index::find);
            key.dynamicInputAdmission(index::accepts);
            return index;
        });
    }
    void add(MaterialUnitRecipe rule, Set<Item> items) {
        sourceKeys.put(rule, items);
        for (Item item : items) byItem.computeIfAbsent(item, ignored -> new ArrayList<>()).add(rule);
        byMaterial.computeIfAbsent(rule.material, ignored -> new ArrayList<>()).add(rule);
        for (int i = 0; i < rule.template.mInputs.length; i++) if (i != rule.inputIndex)
            catalysts.computeIfAbsent(rule.template.mInputs[i].getItem(), ignored -> new ArrayList<>()).add(rule);
    }
    static void discardUnregistered() {
        for (var entry : MAPS.entrySet()) {
            var rules = new java.util.HashSet<MaterialUnitRecipe>();
            entry.getValue().byMaterial.values().forEach(rules::addAll);
            for (var rule : rules) if (!entry.getKey().mRecipeList.contains(rule.template)) entry.getValue().remove(rule);
        }
    }
    void remove(MaterialUnitRecipe rule) {
        var keys = sourceKeys.remove(rule);
        if (keys != null) for (Item key : keys) {
            var list = byItem.get(key);
            if (list != null) { list.remove(rule); if (list.isEmpty()) byItem.remove(key); }
        }
        var materialRules = byMaterial.get(rule.material);
        if (materialRules != null) { materialRules.remove(rule); if (materialRules.isEmpty()) byMaterial.remove(rule.material); }
        for (int i = 0; i < rule.template.mInputs.length; i++) if (i != rule.inputIndex) {
            var item = rule.template.mInputs[i].getItem();
            var list = catalysts.get(item);
            if (list != null) { list.remove(rule); if (list.isEmpty()) catalysts.remove(item); }
        }
        batches.keySet().removeIf(key -> key.rule == rule);
    }
    private long revision = -1;
    private List<MaterialUnitRecipe> candidates(ItemStack stack) {
        var direct = byItem.get(stack.getItem());
        if (direct != null) return direct;
        var form = MaterialEquivalence.form(stack);
        return form == null ? List.of() : byMaterial.getOrDefault(form.material().resolve(), List.of());
    }
    private boolean accepts(ItemStack stack) {
        for (var rule : candidates(stack)) if (rule.accepts(stack)) return true;
        for (var rule : catalysts.getOrDefault(stack.getItem(), List.of())) if (rule.active())
            for (int i = 0; i < rule.template.mInputs.length; i++)
                if (i != rule.inputIndex && rule.template.matchesItemInput(i, stack)) return true;
        return false;
    }
    static boolean acceptsFluid(RecipeMap map, FluidStack fluid) {
        var index = MAPS.get(map);
        if (index == null) return false;
        for (var rules : index.byMaterial.values()) for (var rule : rules) if (rule.active())
            for (int i = 0; i < rule.template.mFluidInputs.length; i++)
                if (rule.template.matchesFluidInput(i, fluid)) return true;
        return false;
    }
    private Recipe find(List<ItemStack> items, List<FluidStack> fluids) {
        long currentRevision = gregtech.engine.material.ItemMaterialRegistry.compositionRevision();
        if (revision != currentRevision) { batches.clear(); revision = currentRevision; }
        var matcher = new RecipeInputs.Matcher(items, fluids);
        for (ItemStack stack : items) if (!stack.isEmpty()) for (var rule : candidates(stack)) {
            if (!rule.accepts(stack)) continue;
            boolean otherInputs = true;
            for (int i = 0; i < rule.template.mInputs.length; i++) if (i != rule.inputIndex && !rule.template.mInputs[i].isEmpty()) {
                final int slot = i;
                if (items.stream().noneMatch(item -> rule.template.matchesItemInput(slot, item))) { otherInputs = false; break; }
            }
            if (!otherInputs) continue;
            var key = new Key(rule, stack.getItem());
//? if >=1.21.1 {
            /*boolean cacheable = stack.getComponentsPatch().isEmpty();
*///?} else {
            boolean cacheable = !stack.hasTag();
//?}
            Recipe row = cacheable ? batches.get(key) : null;
            if (row == null) {
                row = rule.resolve(stack);
                if (row != null && cacheable) batches.put(key, row);
            }
            if (row != null && matcher.matches(row, 1)) return row;
        }
        return null;
    }
}
