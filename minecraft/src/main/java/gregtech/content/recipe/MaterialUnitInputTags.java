package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.recipe.Recipe;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Machine-specific physical membership; unit icons themselves never belong to these tags. */
public final class MaterialUnitInputTags {
    private static Map<String, Set<String>> fixed;

    private MaterialUnitInputTags() {}

    public static TagKey<Item> smeltable(GTMaterial material) {
        return tag("smeltable", material);
    }

    public static TagKey<Item> extrudable(GTMaterial material) {
        return tag("extrudable", material);
    }

    private static TagKey<Item> tag(String family, GTMaterial material) {
        return TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(
                        "gt6ce", family + "/" + MaterialEquivalence.materialName(material)));
    }

    static boolean isBullet(ItemStack stack) {
        var form = MaterialEquivalence.form(stack);
        if (form != null && form.prefix().getName().toLowerCase(Locale.ROOT).startsWith("bullet")) return true;
        var path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.startsWith("bullet_") || path.startsWith("bulletgt");
    }

    /** Build from the same native proofs as admission, before the registry binds the datapack tags. */
    public static synchronized void tags(BiConsumer<String, String> output) {
        if (fixed == null) {
            var initial = new LinkedHashMap<String, Set<String>>();
            for (Recipe row : MachineRecipeMaps.Smelter.mRecipeList) {
                if (!row.mEnabled
                        || row.mHidden
                        || row.mFakeRecipe
                        || row.mInputs.length != 1
                        || row.mOutputs.length != 0
                        || row.mFluidInputs.length != 0
                        || row.mFluidOutputs.length != 1
                        || row.hasTaggedInputs()
                        || row.isCatalystInput(0)
                        || row.mSpecialItems != null
                        || row.mSpecialValue != 0) continue;
                var material = MoltenMetalUnitRecipes.inputMaterial(row.mInputs[0], row.mFluidOutputs[0]);
                if (material != null) add(initial, smeltable(material), row.mInputs[0]);
            }
            // Equivalent immutable input lists are shared across molds; enumerate each only once.
            Set<List<ItemStack>> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            for (var row : MachineRecipeMaps.Extruder.mRecipeList) {
                var display = row.materialInputDisplay(0);
                if (display != null && display.tag() != null && seen.add(display.alternatives()))
                    for (var stack : display.alternatives()) add(initial, display.tag(), stack);
            }
            initial.replaceAll((key, values) -> Set.copyOf(values));
            fixed = Map.copyOf(initial);
        }
        var live = new LinkedHashMap<String, Set<String>>();
        fixed.forEach((key, values) -> live.put(key, new LinkedHashSet<>(values)));
        SharedThermalInputRows.smeltingInputs((input, fluid) -> {
            var material = MoltenMetalUnitRecipes.inputMaterial(input, fluid);
            if (material != null) add(live, smeltable(material), input);
        });
        SharedThermalRecyclingRows.smeltingInputs((input, fluid) -> {
            var material = MoltenMetalUnitRecipes.inputMaterial(input, fluid);
            if (material != null) add(live, smeltable(material), input);
        });
        live.forEach((tag, members) -> members.forEach(item -> output.accept(tag, item)));
    }

    private static void add(Map<String, Set<String>> target, TagKey<Item> tag, ItemStack stack) {
        if (stack.isEmpty()) return;
        target.computeIfAbsent(tag.location().getPath(), ignored -> new LinkedHashSet<>())
                .add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
}
