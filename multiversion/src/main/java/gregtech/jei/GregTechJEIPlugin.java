package gregtech.jei;



//? if >=1.21.1 {
/**///?} else {
import gregtech.GregTech;
//?}

import gregtech.api.recipe.Recipe;
import gregtech.api.recipe.RecipeMap;
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
    @Override public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
        var legacy = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof gregtech.block.misc.LegacyConstructionBlockItem)
                .map(ItemStack::new).toList();
        runtime.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, legacy);
    }

    @Override public void registerVanillaCategoryExtensions(mezz.jei.api.registration.IVanillaCategoryExtensionRegistration registration) {
        ToolCraftingDisplay.register(registration);
    }

    /** Categories created in {@link #registerCategories}, reused in {@link #registerRecipes}. */
    private final Map<RecipeMap, RecipeMapCategory> categories = new LinkedHashMap<>();
    private static int overlayGeneration;

    /** The client overlay calls this before JEI re-registers, so the next pass reads the live maps. */
    public static void overlayChanged() { overlayGeneration++; }
    public static int overlayGeneration() { return overlayGeneration; }

    private Map<RecipeMap,List<ItemStack>> machines = Map.of();

    @Override
    public ResourceLocation getPluginUid() {

//? if >=1.21.1 {
/*        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce","jei_plugin");
*///?} else {
        return GregTech.id("jei_plugin");
//?}

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
        for(var table:net.minecraft.core.registries.BuiltInRegistries.BLOCK)
            if(table instanceof gregtech.block.misc.AdvancedCraftingTableBlock)
                registration.addRecipeCatalyst(new ItemStack(table),mezz.jei.api.constants.RecipeTypes.CRAFTING);
        for (var info : MultiblockInfoCategory.recipes())
            registration.addRecipeCatalyst(info.controller(), MultiblockInfoCategory.TYPE);
        machines.forEach((map, stacks) -> {
            if (!categories.containsKey(map)) return;
            for (var stack : stacks) registration.addRecipeCatalyst(stack, RecipeMapCategory.recipeType(map));
        });
    }

    @Override public void registerRecipeTransferHandlers(mezz.jei.api.registration.IRecipeTransferRegistration registration){
        registration.addRecipeTransferHandler(new AdvancedCraftingTransferInfo());
    }

    @Override public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration registration) {
        for(var item:gregtech.api.recipe.UsbRecipeDisplayBinding.sticks())
            registration.registerSubtypeInterpreter(item,(stack,context)->gregtech.api.recipe.UsbRecipeDisplayBinding.subtype(stack));

//? if >=1.21.1 {
/*        for(var entry:gregtech.platform.neoforge.fluid.FluidRegistries.allItems().toList()) if(entry.isBound())
            registration.registerSubtypeInterpreter(entry.get(),(stack,context) -> gregtech.api.fluid.FluidDisplayBinding.subtype(net.minecraft.client.Minecraft.getInstance().level.registryAccess(),stack));
*///?} else {
        for(var entry:gregtech.registry.GTFluidItems.allEntries()) if(entry.isPresent())
            registration.registerSubtypeInterpreter(entry.get(),(stack,context) -> gregtech.api.fluid.FluidDisplayBinding.subtype(stack));
//?}

    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        gregtech.api.fluid.FluidContainerDisplayBinding.invalidate();
        long started = System.nanoTime();
        int shown = 0;
        try {
        // Reads the live RecipeMap lists, including a datapack overlay applied earlier in this reload.
        // EMI has its own native pages. Its JEI bridge must not import a second copy.

//? if >=1.21.1 {
/*        if (!net.neoforged.fml.ModList.get().isLoaded("emi")) {
*///?} else {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("emi")) {
//?}

            registration.addRecipes(LootInfoCategories.LOOT, gregtech.content.loot.LootViewerData.lootGroups());
            registration.addRecipes(LootInfoCategories.MOBS, gregtech.content.loot.LootViewerData.mobGroups());
        }

//? if >=1.21.1 {
/*        if (!net.neoforged.fml.ModList.get().isLoaded("emi")) {
*///?} else {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("emi")) {
//?}

        registration.addRecipes(MultiblockInfoCategory.TYPE,MultiblockInfoCategory.recipes());
        registration.addRecipes(WorldgenInfoCategories.VEIN_TYPE, WorldgenInfoCategories.buildVeins());
        registration.addRecipes(WorldgenInfoCategories.SMALL_ORE_TYPE, WorldgenInfoCategories.buildSmallOres());
        registration.addRecipes(WorldgenInfoCategories.LAYER_TYPE, WorldgenInfoCategories.buildLayers());
        registration.addRecipes(WorldgenInfoCategories.BEDROCK_TYPE, WorldgenInfoCategories.buildBedrockOres());
        }

//? if >=1.21.1 {
/*        if (!net.neoforged.fml.ModList.get().isLoaded("emi"))
*///?} else {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("emi"))
//?}

            registration.addRecipes(ToolAssemblyCategory.TYPE, gregtech.recipe.ToolAssemblyCatalog.build());
        // EMI registers these same rows natively; keep JEI's bridge from duplicating them.

//? if >=1.21.1 {
/*        if (net.neoforged.fml.ModList.get().isLoaded("emi")) return;
*///?} else {
        if (net.minecraftforge.fml.ModList.get().isLoaded("emi")) return;
//?}

        for (Map.Entry<RecipeMap, RecipeMapCategory> entry : categories.entrySet()) {
            List<Recipe> recipes = new ArrayList<>();
            for (Recipe recipe : entry.getKey().mRecipeList) {
                if (recipe.mEnabled && !recipe.mHidden) recipes.add(recipe);
            }
            if (!recipes.isEmpty()) {
                registration.addRecipes(entry.getValue().getRecipeType(),recipes);
                shown += recipes.size();
            }
        }
        } finally {
            if (Integer.getInteger("gregtech.materialScale", 0) > 0) {
                Runtime runtime = Runtime.getRuntime();
//? if >=1.21.1 {
/*                com.mojang.logging.LogUtils.getLogger().info("JEI_SCALE recipes {} ms {} items {} heapMb {}",
*///?} else {
                GregTech.LOGGER.info("JEI_SCALE recipes {} ms {} items {} heapMb {}",
//?}
                        shown, (System.nanoTime() - started) / 1_000_000L,
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.size(),
                        (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576L);
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
