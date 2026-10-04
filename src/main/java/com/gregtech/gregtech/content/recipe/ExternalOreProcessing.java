package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.recipe.CraftingMaterialForms;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Tag-bound source prefix handlers. GT registers metadata for these forms, never new owned items. */
public final class ExternalOreProcessing {
    private ExternalOreProcessing() {}
    private static final Map<RecipeMap, Set<Recipe>> OWNED = new IdentityHashMap<>();
    private static volatile Map<Item, ItemComposition> compositions = Map.of();

    public static Optional<ItemComposition> composition(Item item) { return Optional.ofNullable(compositions.get(item)); }

    /** Remove only this adapter's exact rows; other mods' processing rows remain registered. */
    public static synchronized void clear() {
        OWNED.forEach((map, rows) -> {
            map.mRecipeList.removeAll(rows);
            map.mRecipeItemMap.values().forEach(values -> values.removeAll(rows));
            map.mRecipeFluidMap.values().forEach(values -> values.removeAll(rows));
        });
        OWNED.clear();
        compositions = Map.of();
    }

    public static synchronized int rebuild() {
        clear(); // Old tag compositions must not bias native identity when indexing this reload.
        CraftingMaterialForms.rebuild(true);
        var forms = CraftingMaterialForms.nativeAliases();
        var materialData = new IdentityHashMap<Item, ItemComposition>();
        for (var prefix : ExternalOreProcessingRules.EXTERNAL_FORMS) for (var entry : forms.entrySet()) {
            if (!entry.getKey().prefix().equals(prefix.getName())) continue;
            var material = GTMaterialRegistry.get(entry.getKey().material()).resolve();
            if (!material.isValid()) continue;
            for (var item : entry.getValue()) if (ItemMaterialRegistry.base(item).isEmpty())
                materialData.put(item, new ItemComposition(prefix,
                        List.of(MaterialComponent.of(material, prefix.getMaterialWeight())),
                        "GT6 OP external ore-prefix tags", true));
        }
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
                var fines = route.fines() ? output(MaterialPrefix.dustTiny, target, 1) : ItemStack.EMPTY;
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
        LogUtils.getLogger().info("[gregtech] Bound {} external ore processing rows and {} material compositions from loaded tags",
                added, compositions.size());
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
