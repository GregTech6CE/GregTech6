package com.gregtech.gregtech.loaders;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.recipe.GTFlintAndTinderRecipe;
import com.gregtech.gregtech.recipe.GTToolAssemblyRecipe;
import com.gregtech.gregtech.recipe.GTToolCraftingRecipe;
import com.gregtech.gregtech.recipe.GTToolHeadRecipe;
import com.gregtech.gregtech.recipe.GTToolRecipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers GT6's tool crafting rows ({@link GTToolRecipes}) as real crafting recipes.
 *
 * <p>Before §29 the port described every tool with one data-pack {@code gregtech:tool_assembly} row
 * that matched <em>shapelessly</em> and had no ingredients, so a wrench or a knife could be thrown
 * together in any arrangement and none of it appeared in the crafting-table recipe list. GT6's rows
 * are shaped for the one-piece tools and for tool heads; only the head + handle tools are shapeless
 * (GT6 {@code AdvancedCraftingTool extends ShapelessOreRecipe}). All of them are registered here so
 * the shape lives in one place, {@link GTToolRecipes}.</p>
 */
public final class Loader_ToolCraftingRecipes {

    private static final List<String> REGISTERED = new ArrayList<>();

    private Loader_ToolCraftingRecipes() {}

    /** Recipe ids the last server start added, for tests and reports. */
    public static List<String> registeredIds() { return List.copyOf(REGISTERED); }

    public static void apply(RecipeManager manager, net.minecraft.core.RegistryAccess access) {
        List<Recipe<?>> recipes = new ArrayList<>(manager.getRecipes());
        REGISTERED.clear();

        int shaped = 0;
        int heads = 0;
        int assemblies = 0;
        List<String> withoutRecipe = new ArrayList<>();
        for (GTToolType type : GTToolType.values()) {
            List<GTToolRecipes.Pattern> patterns = GTToolRecipes.shaped(type);
            for (int i = 0; i < patterns.size(); i++) {
                if (!com.gregtech.gregtech.recipe.GTToolPatternRecipe.hasMaterials(type, patterns.get(i))) continue;
                ResourceLocation id = id("tools/" + type.id() + (i == 0 ? "" : "_" + i));
                recipes.add(type == GTToolType.FLINT_AND_TINDER
                        ? new GTFlintAndTinderRecipe(id)
                        : new GTToolCraftingRecipe(id, type, i));
                REGISTERED.add(id.toString());
                shaped++;
            }
            boolean assembly = type.requiresHeadAssembly() || type == GTToolType.MAGNIFYING_GLASS;
            if (assembly) {
                ResourceLocation id = id("tools/" + type.id() + "_assembly");
                recipes.add(new GTToolAssemblyRecipe(id, type));
                REGISTERED.add(id.toString());
                assemblies++;
            }
            if (patterns.isEmpty() && !assembly) withoutRecipe.add(type.id());
            List<GTToolRecipes.Pattern> headPatterns = GTToolRecipes.heads(type);
            for (int i = 0; i < headPatterns.size(); i++) {
                if (!com.gregtech.gregtech.recipe.GTToolPatternRecipe.hasMaterials(type, headPatterns.get(i))) continue;
                ResourceLocation id = id("tool_heads/" + type.id() + (i == 0 ? "" : "_" + i));
                recipes.add(new GTToolHeadRecipe(id, type, i));
                REGISTERED.add(id.toString());
                heads++;
            }
        }
        var electric=com.gregtech.gregtech.content.tool.ElectricToolAssembly.build();
        recipes.addAll(electric);
        electric.forEach(recipe->REGISTERED.add(recipe.getId().toString()));
        GregTech.LOGGER.info("Registered {} material-specific GT6 LV electric tool crafting rows",electric.size());
        com.gregtech.gregtech.recipe.RuntimeRecipeLifecycle.replaceGenerated(manager, recipes);
        GregTech.LOGGER.info("Registered GT6 tool crafting rows: {} shaped tools, {} head + handle"
                        + " assemblies, {} tool heads ({} tools without a manual recipe: {})",
                shaped, assemblies, heads, withoutRecipe.size(), withoutRecipe);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE, path);
    }
}
