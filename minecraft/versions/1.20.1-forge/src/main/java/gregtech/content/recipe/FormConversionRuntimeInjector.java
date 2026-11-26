package gregtech.content.recipe;

import gregtech.engine.material.ItemMaterialForms;
import gregtech.platform.Mc;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

/** Forge 1.20.1 runtime entry that strips replaced plain recipes before regenerating form conversions. */
final class FormConversionRuntimeInjector {
    private FormConversionRuntimeInjector() {}

    static void apply(RecipeManager manager, RegistryAccess access) {
        synchronized (FormConversionCraftingRecipeInjector.class) {
            var previousGenerated = FormConversionCraftingRecipeInjector.snapshotRegistered();
            List<Recipe<?>> recipes = new ArrayList<>(manager.getRecipes());
            int before = recipes.size();
            recipes.removeIf(recipe -> recipe instanceof CraftingRecipe crafting
                    && !ConfiguredRecipeRules.keepVanillaStorageBlockRecipe(recipe.getId())
                    && FormConversionCraftingRecipeInjector.disallowsPlainRecipe(recipe.getId(), crafting, access));
            int removedPlain = before - recipes.size();
            // Regenerate only the rows this injector owns; other GT rows must survive the pass above.
            recipes.removeIf(recipe -> previousGenerated.contains(recipe.getId().toString()));
            Mc.logger().info("Replaced {} plain source-equivalent crafting rows", removedPlain);
            FormConversionCraftingRecipeInjector.add(
                    GeneratedCraftingSink.runtime(manager, recipes), ItemMaterialForms.aliases());
        }
    }
}
