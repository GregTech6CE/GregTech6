package com.gregtech.gregtech.emi;

import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.jei.RecipeMachines;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Locale;

/** Native EMI integration: machine maps are independent of the vanilla RecipeManager and JEI. */
@EmiEntrypoint
public final class GregTechEMIPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        for(var item:com.gregtech.gregtech.api.recipe.UsbRecipeDisplayBinding.sticks())
            registry.setDefaultComparison(item,dev.emi.emi.api.stack.Comparison.compareData(
                    stack->com.gregtech.gregtech.api.recipe.UsbRecipeDisplayBinding.subtype(stack.getItemStack())));
        LootEmiRecipe.register(registry);
        StructureEmiRecipe.register(registry);
        WorldgenEmiRecipe.register(registry);
        // Rebuild aliases from this reload's compositions before creating ingredient templates.
        com.gregtech.gregtech.api.material.MaterialDisplayBinding.invalidate();
        var machines = RecipeMachines.collect();
        var ingredients = new MachineEmiIngredients();
        long started = System.nanoTime();
        int rows = 0, categories = 0;
        for (RecipeMap map : RecipeMap.RECIPE_MAP_LIST) {
            if (!map.mViewerAllowed || map.mRecipeList.stream().noneMatch(r -> r.mEnabled && !r.mHidden)) continue;
            var workstations = machines.getOrDefault(map, java.util.List.of());
            ItemStack icon = workstations.isEmpty() ? new ItemStack(Items.ANVIL) : workstations.get(0);
            var id = ResourceLocation.fromNamespaceAndPath("gregtech", map.mNameInternal.toLowerCase(Locale.ROOT));
            var category = new EmiRecipeCategory(id, EmiStack.of(icon)) {
                @Override public Component getName() { return Component.literal(map.mNameLocal); }
            };
            registry.addCategory(category);
            for (var station : workstations) registry.addWorkstation(category, EmiStack.of(station));
            int i = 0;
            int before = rows;
            long mapStarted = System.nanoTime();
            com.mojang.logging.LogUtils.getLogger().debug("[gregtech] EMI registering {}", map.mNameInternal);
            for (var recipe : map.mRecipeList) {
                int recipeIndex = i++;
                if (!recipe.mEnabled || recipe.mHidden) continue;
                registry.addRecipe(new MachineEmiRecipe(category, map, recipe,
                        // EMI reserves leading '/' for recipes outside vanilla RecipeManager.
                        ResourceLocation.fromNamespaceAndPath("gregtech", "/machine/" + id.getPath() + "/" + recipeIndex), ingredients));
                rows++;
            }
            categories++;
            if (rows - before >= 1000) com.mojang.logging.LogUtils.getLogger().info(
                    "[gregtech] EMI registered {} recipes for {} in {} ms", rows - before, map.mNameInternal,
                    (System.nanoTime() - mapStarted) / 1_000_000);
        }
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] EMI registered {} machine recipes in {} categories in {} ms", rows, categories,
                (System.nanoTime() - started) / 1_000_000);
    }
}
