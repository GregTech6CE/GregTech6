package gregtech.emi;

import gregtech.api.recipe.RecipeMap;
import gregtech.jei.RecipeMachines;
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
    private static int overlayGeneration;

    /** The client overlay calls this before EMI re-registers, so this pass reads the live maps. */
    public static void overlayChanged() { overlayGeneration++; }
    public static int overlayGeneration() { return overlayGeneration; }

    @Override
    public void register(EmiRegistry registry) {
        gregtech.api.fluid.FluidContainerDisplayBinding.invalidate();
        FluidScreenStackProvider.register(registry);
        WoodenBucketLookupRecipe.register(registry);
        registry.addRecipeHandler(gregtech.registry.GTMenuTypes.ADVANCED_CRAFTING.get(),new AdvancedCraftingEmiHandler());
        for(var table:net.minecraft.core.registries.BuiltInRegistries.BLOCK)
            if(table instanceof gregtech.block.misc.AdvancedCraftingTableBlock)
                registry.addWorkstation(dev.emi.emi.api.recipe.VanillaEmiRecipeCategories.CRAFTING,EmiStack.of(new ItemStack(table)));
        EmiCatalogRepair.apply();
        for(var item:gregtech.api.recipe.UsbRecipeDisplayBinding.sticks())
            registry.setDefaultComparison(item,dev.emi.emi.api.stack.Comparison.compareData(
                    stack->gregtech.api.recipe.UsbRecipeDisplayBinding.subtype(stack.getItemStack())));
        ToolAssemblyEmiRecipe.register(registry);
        LootEmiRecipe.register(registry);
        StructureEmiRecipe.register(registry);
        WorldgenEmiRecipe.register(registry);
        // Rebuild aliases from this reload's compositions before creating ingredient templates.
        gregtech.api.material.MaterialDisplayBinding.invalidate();
        var machines = RecipeMachines.collect();
        var ingredients = new MachineEmiIngredients();
        long started = System.nanoTime();
        int rows = 0, categories = 0;
        for (RecipeMap map : RecipeMap.RECIPE_MAP_LIST) {
            if (!map.mViewerAllowed || map.mRecipeList.stream().noneMatch(r -> r.mEnabled && !r.mHidden)) continue;
            var workstations = machines.getOrDefault(map, java.util.List.of());
            ItemStack icon = workstations.isEmpty() ? new ItemStack(Items.ANVIL) : workstations.get(0);
            var id = ResourceLocation.fromNamespaceAndPath("gt6ce", map.mNameInternal.toLowerCase(Locale.ROOT));
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
                        ResourceLocation.fromNamespaceAndPath("gt6ce", "/machine/" + id.getPath() + "/" + recipeIndex), ingredients));
                rows++;
            }
            categories++;
            if (rows - before >= 1000) com.mojang.logging.LogUtils.getLogger().info(
                    "[gregtech] EMI registered {} recipes for {} in {} ms", rows - before, map.mNameInternal,
                    (System.nanoTime() - mapStarted) / 1_000_000);
        }
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] EMI registered {} machine recipes in {} categories in {} ms (overlay {})",
                rows, categories, (System.nanoTime() - started) / 1_000_000, overlayGeneration);
        if (Integer.getInteger("gregtech.materialScale", 0) > 0) {
            Runtime runtime = Runtime.getRuntime();
            com.mojang.logging.LogUtils.getLogger().info("EMI_SCALE recipes {} ms {} items {} heapMb {}",
                    rows, (System.nanoTime() - started) / 1_000_000L,
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.size(),
                    (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576L);
        }
    }
}
