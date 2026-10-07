package com.gregtech.gregtech.jei;


import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JEI integration for GT6: one generic {@link RecipeMapCategory} per {@link RecipeMap}
 * that contains recipes, populated from the map's recipe list.
 *
 * <p>{@code FluidItem} proxies appear in JEI's item panel automatically
 * (they are registered in the {@code GregTech Fluids} creative tab).</p>
 */
@JeiPlugin
public final class GregTechJEIPlugin implements IModPlugin {

    /** Categories created in {@link #registerCategories}, reused in {@link #registerRecipes}. */
    private final Map<RecipeMap, RecipeMapCategory> categories = new LinkedHashMap<>();

    private Map<RecipeMap,List<ItemStack>> machines = Map.of();

    @Override
    public ResourceLocation getPluginUid() {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        categories.clear();
        machines = RecipeMachines.collect();
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        for (RecipeMap map : RecipeMap.RECIPE_MAP_LIST) {
            if (!map.mViewerAllowed || map.mRecipeList.stream().noneMatch(recipe -> recipe.mEnabled && !recipe.mHidden)) continue;
            var category = new RecipeMapCategory(guiHelper, map, iconFor(map));
            registration.addRecipeCategories(category);
            categories.put(map, category);
        }
        registration.addRecipeCategories(new LootInfoCategories.Category(guiHelper, false), new LootInfoCategories.Category(guiHelper, true));
        registration.addRecipeCategories(new MultiblockInfoCategory(guiHelper));
        registration.addRecipeCategories(new ToolAssemblyCategory.Category(guiHelper));
        registration.addRecipeCategories(
                new WorldgenInfoCategories.VeinCategory(guiHelper),
                new WorldgenInfoCategories.SmallOreCategory(guiHelper),
                new WorldgenInfoCategories.LayerCategory(guiHelper),
                new WorldgenInfoCategories.BedrockCategory(guiHelper));
    }

    @Override
    public void registerRecipeCatalysts(mezz.jei.api.registration.IRecipeCatalystRegistration registration) {
        for (var info : MultiblockInfoCategory.recipes())
            registration.addRecipeCatalyst(info.controller(), MultiblockInfoCategory.TYPE);
        machines.forEach((map, stacks) -> {
            if (!categories.containsKey(map)) return;
            for (var stack : stacks) registration.addRecipeCatalyst(stack, RecipeMapCategory.recipeType(map));
        });
    }

    @Override public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration registration) {
        for(var item:com.gregtech.gregtech.api.recipe.UsbRecipeDisplayBinding.sticks())
            registration.registerSubtypeInterpreter(item,(stack,context)->com.gregtech.gregtech.api.recipe.UsbRecipeDisplayBinding.subtype(stack));
        for(var entry:com.gregtech.gregtech.platform.neoforge.fluid.FluidRegistries.ITEMS.getEntries()) if(entry.isBound())
            registration.registerSubtypeInterpreter(entry.get(),(stack,context) -> com.gregtech.gregtech.api.fluid.FluidDisplayBinding.subtype(net.minecraft.client.Minecraft.getInstance().level.registryAccess(),stack));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // EMI has its own native pages. Its JEI bridge must not import a second copy.
        if (!net.neoforged.fml.ModList.get().isLoaded("emi")) {
            registration.addRecipes(LootInfoCategories.LOOT, com.gregtech.gregtech.content.loot.LootViewerData.lootGroups());
            registration.addRecipes(LootInfoCategories.MOBS, com.gregtech.gregtech.content.loot.LootViewerData.mobGroups());
        }
        if (!net.neoforged.fml.ModList.get().isLoaded("emi")) {
        registration.addRecipes(MultiblockInfoCategory.TYPE,MultiblockInfoCategory.recipes());
        registration.addRecipes(WorldgenInfoCategories.VEIN_TYPE, WorldgenInfoCategories.buildVeins());
        registration.addRecipes(WorldgenInfoCategories.SMALL_ORE_TYPE, WorldgenInfoCategories.buildSmallOres());
        registration.addRecipes(WorldgenInfoCategories.LAYER_TYPE, WorldgenInfoCategories.buildLayers());
        registration.addRecipes(WorldgenInfoCategories.BEDROCK_TYPE, WorldgenInfoCategories.buildBedrockOres());
        }
        registration.addRecipes(ToolAssemblyCategory.TYPE, com.gregtech.gregtech.recipe.ToolAssemblyCatalog.build());
        // EMI registers these same rows natively; keep JEI's bridge from duplicating them.
        if (net.neoforged.fml.ModList.get().isLoaded("emi")) return;
        for (Map.Entry<RecipeMap, RecipeMapCategory> entry : categories.entrySet()) {
            List<Recipe> recipes = new ArrayList<>();
            for (Recipe recipe : entry.getKey().mRecipeList) {
                if (recipe.mEnabled && !recipe.mHidden) recipes.add(recipe);
            }
            if (!recipes.isEmpty()) {
                registration.addRecipes(entry.getValue().getRecipeType(),recipes);
            }
        }
    }

    /** Prefer the machine itself; fall back to a recipe output for maps without a registered machine. */
    private ItemStack iconFor(RecipeMap map) {
        var catalysts=machines.get(map);
        if (catalysts!=null && !catalysts.isEmpty()) return catalysts.get(0).copy();
        for (Recipe recipe : map.mRecipeList) {
            for (ItemStack out : recipe.mOutputs) {
                if (out != null && !out.isEmpty()) return out;
            }
        }
        return new ItemStack(Items.ANVIL);
    }
}
