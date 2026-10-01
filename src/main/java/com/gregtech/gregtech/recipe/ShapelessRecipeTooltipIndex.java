package com.gregtech.gregtech.recipe;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.item.MaterialItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Mirrors GT6 {@code AdvancedCrafting1ToY}/{@code AdvancedCraftingXToY} tooltip indexing.
 * <p>
 * When an item is shown in tooltip, GT6 lists input amounts for shapeless prefix conversions
 * (e.g. 1 crushed  -> 9 crushedTiny). The GT workbench uses the same managers at craft time.
 */
public final class ShapelessRecipeTooltipIndex {
    private static final Map<String, List<Integer>> INPUT_COUNTS = new HashMap<>();

    private ShapelessRecipeTooltipIndex() {}

    public static void rebuild(RecipeManager recipes) {
        INPUT_COUNTS.clear();
        PrefixRegistry.ensurePrefixesLoaded();
        registerBuiltInPrefixChains();
        if (recipes != null) {
            scanShapelessRecipes(recipes);
        }
        INPUT_COUNTS.replaceAll((k, v) -> List.copyOf(new TreeSet<>(v)));
    }

    /** GT6 {@code Loader_Recipes_Handlers} 1-input prefix chains. */
    private static void registerBuiltInPrefixChains() {
        chain(MaterialPrefix.crushed, MaterialPrefix.crushedTiny, 1);
        chain(MaterialPrefix.crushedPurified, MaterialPrefix.crushedPurifiedTiny, 1);
        chain(MaterialPrefix.crushedCentrifuged, MaterialPrefix.crushedCentrifugedTiny, 1);
        chain(MaterialPrefix.ingot, MaterialPrefix.nugget, 1);
        chain(MaterialPrefix.ingot, MaterialPrefix.chunkGt, 1);
        chain(MaterialPrefix.billet, MaterialPrefix.nugget, 1);
        chain(MaterialPrefix.dust, MaterialPrefix.dustTiny, 1);
        chain(MaterialPrefix.dust, MaterialPrefix.dustSmall, 1);
        chain(MaterialPrefix.dustTiny, MaterialPrefix.dustDiv72, 1);
        chain(MaterialPrefix.dustSmall, MaterialPrefix.dustDiv72, 1);
        chain(MaterialPrefix.gem, MaterialPrefix.gemChipped, 1);
        chain(MaterialPrefix.gem, MaterialPrefix.gemFlawed, 1);
        chain(MaterialPrefix.plate, MaterialPrefix.plateTiny, 1);
        chain(MaterialPrefix.plateGem, MaterialPrefix.plateGemTiny, 1);
        chain(MaterialPrefix.stick, MaterialPrefix.bolt, 1);
        chain(MaterialPrefix.stickLong, MaterialPrefix.bolt, 1);
        chain(MaterialPrefix.oreRaw, MaterialPrefix.gem, 1);
    }

    private static void chain(MaterialPrefix input, MaterialPrefix output, int inputCount) {
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (!input.isValidFor(material) || !output.isValidFor(material)) {
                continue;
            }
            add(output, material, inputCount);
        }
    }

    private static void scanShapelessRecipes(RecipeManager recipes) {
        for (var recipe : recipes.getAllRecipesFor(RecipeType.CRAFTING)) {
            if (!(recipe instanceof ShapelessRecipe shapeless)) {
                continue;
            }
            ItemStack result = shapeless.getResultItem(null);
            var outputItem=com.gregtech.gregtech.api.material.MaterialEquivalence.form(result);
            if (result.isEmpty() || outputItem==null) {
                continue;
            }

            List<ItemStack> inputs = new ArrayList<>();
            for (Ingredient ingredient : shapeless.getIngredients()) {
                for (ItemStack stack : ingredient.getItems()) {
                    if (!stack.isEmpty()) {
                        inputs.add(stack);
                        break;
                    }
                }
            }
            var inputItem=inputs.size()==1?com.gregtech.gregtech.api.material.MaterialEquivalence.form(inputs.get(0)):null;
            if (inputItem==null) {
                continue;
            }
            if (inputItem.material().resolve() != outputItem.material().resolve()) {
                continue;
            }
            add(outputItem.prefix(), outputItem.material(), inputs.get(0).getCount());
        }
    }

    private static void add(MaterialPrefix outputPrefix, GTMaterial material, int inputCount) {
        if (outputPrefix == null || material == null || inputCount <= 0) {
            return;
        }
        INPUT_COUNTS.computeIfAbsent(key(outputPrefix, material), k -> new ArrayList<>()).add(inputCount);
    }

    public static List<Integer> inputAmounts(MaterialPrefix outputPrefix, GTMaterial material) {
        if (outputPrefix == null || material == null) {
            return List.of();
        }
        GTMaterial resolved = material.resolve();
        List<Integer> amounts = INPUT_COUNTS.get(key(outputPrefix, resolved));
        return amounts == null ? List.of() : amounts;
    }

    private static String key(MaterialPrefix outputPrefix, GTMaterial material) {
        return outputPrefix.getName() + "/" + material.getName();
    }

    public static Map<String, List<Integer>> debugView() {
        return Collections.unmodifiableMap(INPUT_COUNTS);
    }
}
