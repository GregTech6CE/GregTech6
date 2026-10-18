package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.recipe.CraftingMaterialForms;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;

/** Tag-bound source prefix handlers. GT registers metadata for these forms, never new owned items. */
public final class ExternalOreProcessing {
    private ExternalOreProcessing() {}
    private static final Map<RecipeMap, Set<Recipe>> OWNED = new IdentityHashMap<>();
    /** Distinct from ore-prefix compositions so callers can tell tag-derived ingots, dusts and rods apart. */
    public static final String TAGGED_COMPOSITION_SOURCE = "GT6 common form tags";
    private static volatile Map<Item, ItemComposition> compositions = Map.of();
    private static volatile int taggedCompositions;
    private static volatile int ambiguousTaggedItems;

    private static volatile Map<Item, FurnaceSmeltingRecipes.CookingData> cooking = Map.of();
    public static int taggedCompositions() { return taggedCompositions; }
    public static int ambiguousTaggedItems() { return ambiguousTaggedItems; }
    public static Optional<FurnaceSmeltingRecipes.CookingData> cooking(Item item) { return Optional.ofNullable(cooking.get(item)); }

    public static Optional<ItemComposition> composition(Item item) { return Optional.ofNullable(compositions.get(item)); }

    /** Remove only this adapter's exact rows; other mods' processing rows remain registered. */
    public static synchronized void clear() {
        // Restore displaced source rows before removing them; otherwise an old mirror can resurrect this reload's predecessors.
        com.gregtech.gregtech.loaders.Loader_OvenRecipes.clearMirrors();
        OWNED.forEach((map, rows) -> {
            map.mRecipeList.removeAll(rows);
            map.mRecipeItemMap.values().forEach(values -> values.removeAll(rows));
            map.mRecipeFluidMap.values().forEach(values -> values.removeAll(rows));
        });
        OWNED.clear();
        compositions = Map.of();
        cooking = Map.of();
        taggedCompositions = 0;
        ambiguousTaggedItems = 0;
    }

    public static synchronized int rebuild() {
        clear(); // Old tag compositions must not bias native identity when indexing this reload.
        CraftingMaterialForms.rebuild(true);
        var forms = CraftingMaterialForms.nativeAliases();
        var materialData = new IdentityHashMap<Item, ItemComposition>();
        for (var prefix : ExternalOreProcessingRules.COMPOSITION_FORMS) for (var entry : forms.entrySet()) {
            if (!entry.getKey().prefix().equals(prefix.getName())) continue;
            var material = GTMaterialRegistry.get(entry.getKey().material()).resolve();
            if (!material.isValid()) continue;
            for (var item : entry.getValue()) if (ItemMaterialRegistry.base(item).isEmpty())
                materialData.put(item, new ItemComposition(prefix,
                        List.of(MaterialComponent.of(material, prefix.getMaterialWeight())),
                        "GT6 OP external ore-prefix tags", true));
        }
        int oreCompositions = materialData.size();
        taggedCompositions = bindCommonForms(materialData);
        compositions = Collections.unmodifiableMap(materialData);
        int added = 0;
        for (var route : ExternalOreProcessingRules.ROUTES) {
            var map = map(route.map());
            for (var entry : forms.entrySet()) {
                if (!entry.getKey().prefix().equals(route.input().getName())) continue;
                var material = GTMaterialRegistry.get(entry.getKey().material()).resolve();
                if (!material.isValid()) continue;
                var output = output(route.output(), material, route.count());
                for (var item : entry.getValue()) {
                    if (!ExternalOreProcessingRules.allows(material) || output.isEmpty()) continue;
                    // Prefix handlers disable optimizing; costs use the larger input/output unit amount.
                    var recipe = map.addRecipe1(false, 16, route.duration(material.getToolQuality()),
                            new ItemStack(item), output.copy());
                    if (recipe != null) {
                        OWNED.computeIfAbsent(map, unused -> Collections.newSetFromMap(new IdentityHashMap<>())).add(recipe);
                        added++;
                    }
                }
            }
        }
        for (var route : ExternalOreProcessingRules.GRINDING) {
            var map = map(route.map());
            for (var entry : forms.entrySet()) {
                if (!entry.getKey().prefix().equals(route.input().getName())) continue;
                var material = GTMaterialRegistry.get(entry.getKey().material()).resolve();
                if (!route.allows(material)) continue;
                var target = route.outputMaterial(material);
                var dust = route.pulverizedRemains() ? MortarGrindingRecipes.pulverize(material, route.input().getMaterialWeight())
                        : output(MaterialPrefix.dust, target, route.dustCount());
                var fines = route.fines() ? output(MaterialPrefix.dustTiny, target, route.fineCount()) : ItemStack.EMPTY;
                if (dust.isEmpty() || (route.fines() && fines.isEmpty())) continue;
                for (var item : entry.getValue()) {
                    var outputs = fines.isEmpty() ? new ItemStack[]{dust.copy()} : new ItemStack[]{dust.copy(), fines.copy()};
                    var recipe = route.requiresEmptySlot()
                            ? map.addRecipe(new Recipe(new ItemStack[]{new ItemStack(item), ItemStack.EMPTY},
                                    outputs, null, null, null, null, route.duration(material), 16, 0))
                            : map.addRecipe1(false, 16, route.duration(material), new ItemStack(item), outputs);
                    if (recipe != null) {
                        OWNED.computeIfAbsent(map, unused -> Collections.newSetFromMap(new IdentityHashMap<>())).add(recipe);
                        added++;
                    }
                }
            }
        }
        var cookingData = new IdentityHashMap<Item, FurnaceSmeltingRecipes.CookingData>();
        for (var row : FurnaceSmeltingRules.EXTERNAL) for (var entry : forms.entrySet()) {
            if (!entry.getKey().prefix().equals(row.input().getName())) continue;
            var material = GTMaterialRegistry.get(entry.getKey().material()).resolve();
            if (!FurnaceSmeltingRules.allows(material)) continue;
            var target = material.getTargetSmeltingMaterial();
            if (target == null || !target.isValid()) continue;
            long amount = FurnaceSmeltingRules.amount(row.amount(), material.getTargetSmeltingAmount());
            var output = com.gregtech.gregtech.util.OM.ingot(target, amount);
            if (output.isEmpty()) continue;
            for (var item : entry.getValue()) {
                var map = MachineRecipeMaps.Furnace;
                var recipe = map.addRecipe1(true, 16, 16, new ItemStack(item), MachineRecipeMaps.neverFurnaceOutput(output.copy()));
                if (recipe == null) continue;
                OWNED.computeIfAbsent(map, unused -> Collections.newSetFromMap(new IdentityHashMap<>())).add(recipe);
                cookingData.put(item, new FurnaceSmeltingRecipes.CookingData(
                        row.experience() ? FurnaceSmeltingRules.experience(amount, material.getToolQuality()) : 0,
                        com.gregtech.gregtech.data.generated.MaterialWorkability.isFood(material)));
                added++;
            }
        }
        cooking = Collections.unmodifiableMap(cookingData);
        for (var prefix : ExternalOreProcessingRules.CRUCIBLE_FORMS) for (var entry : forms.entrySet()) {
            if (!entry.getKey().prefix().equals(prefix.getName())) continue;
            var material = GTMaterialRegistry.get(entry.getKey().material()).resolve();
            var preview = com.gregtech.gregtech.api.machine.crucible.CrucibleInputRules.smeltingPreview(material, prefix);
            if (preview == null) continue;
            var output = com.gregtech.gregtech.util.OM.ingotOrDust(preview.material(), preview.amount());
            if (output.isEmpty()) continue;
            for (var item : entry.getValue()) {
                var map = MachineRecipeMaps.CrucibleSmelting;
                var recipe = map.addFakeRecipe(false, new ItemStack[]{new ItemStack(item)}, new ItemStack[]{output.copy()},
                        null, null, null, null, 0, 0, preview.temperatureK());
                if (recipe == null) continue;
                OWNED.computeIfAbsent(map, unused -> Collections.newSetFromMap(new IdentityHashMap<>())).add(recipe);
                added++;
            }
        }
        LogUtils.getLogger().info("[gregtech] Bound {} external ore processing rows and {} material compositions from loaded tags ({} ore prefix, {} common form); skipped {} ambiguous common-form items",
                added, compositions.size(), oreCompositions, taggedCompositions, ambiguousTaggedItems);
        return added;
    }

    /** One unambiguous common-form tag becomes one unit of that GT prefix. Conflicting tags are left untouched. */
    private static int bindCommonForms(Map<Item, ItemComposition> materialData) {
        var allowed = new HashSet<String>();
        for (var prefix : ExternalOreProcessingRules.TAGGED_MATERIAL_FORMS) allowed.add(prefix.getName());
        int added = 0;
        int ambiguous = 0;
        for (var item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || item == Items.AIR) continue;
            String namespace = id.getNamespace();
            if (namespace.equals("minecraft") || namespace.equals("gregtech")) continue;
            if (ItemMaterialRegistry.explicit(item).isPresent() || materialData.containsKey(item)) continue;
            var forms = CraftingMaterialForms.taggedForms(new ItemStack(item));
            boolean common = false;
            for (var form : forms) if (allowed.contains(form.prefix())) { common = true; break; }
            if (!common) continue;
            if (forms.size() != 1) { ambiguous++; continue; }
            var prefix = com.gregtech.gregtech.api.prefix.PrefixRegistry.byName(forms.get(0).prefix());
            var material = GTMaterialRegistry.get(forms.get(0).material()).resolve();
            if (prefix == null || !allowed.contains(prefix.getName()) || !material.isValid() || !prefix.isValidFor(material)) continue;
            materialData.put(item, new ItemComposition(prefix,
                    List.of(MaterialComponent.of(material, prefix.getMaterialWeight())),
                    TAGGED_COMPOSITION_SOURCE, true));
            added++;
        }
        ambiguousTaggedItems = ambiguous;
        return added;
    }

    private static RecipeMap map(String name) {
        return switch (name) {
            case "Crusher" -> MachineRecipeMaps.Crusher;
            case "Sifting" -> MachineRecipeMaps.Sifting;
            case "Shredder" -> MachineRecipeMaps.Shredder;
            case "Anvil" -> MachineRecipeMaps.Anvil;
            case "Mortar" -> MachineRecipeMaps.Mortar;
            default -> throw new IllegalArgumentException("Unknown source external ore map " + name);
        };
    }

    private static ItemStack output(MaterialPrefix prefix, GTMaterial material, int count) {
        var output = com.gregtech.gregtech.registry.GTItems.getStack(prefix, material, count);
        return output.isEmpty() ? CraftingMaterialForms.stack(prefix.getName(), material, count) : output;
    }
}
