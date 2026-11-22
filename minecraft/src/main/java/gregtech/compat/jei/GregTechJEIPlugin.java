package gregtech.compat.jei;

import gregtech.compat.common.RecipeMachines;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.platform.Mc;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

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
        this.runtime = runtime;
        active = this;
        gregtech.client.gui.RecipeViewerNavigation.registerJei(map -> {
            if (!categories.containsKey(map)) return false;
            runtime.getRecipesGui().showTypes(java.util.List.of(RecipeMapCategory.recipeType(map)));
            return true;
        });
        var legacy = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                .filter(item -> !gregtech.content.item.CatalogVisibility.visible(new ItemStack(item)))
                .map(ItemStack::new).toList();
        runtime.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, legacy);
    }

    @Override public void onRuntimeUnavailable() {
        if (active == this) active = null;
        runtime = null;
        gregtech.client.gui.RecipeViewerNavigation.clearJei();
    }

    @Override
    public void registerGuiHandlers(mezz.jei.api.registration.IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(gregtech.client.gui.MachineContainerScreen.class,
                new mezz.jei.api.gui.handlers.IGuiContainerHandler<gregtech.client.gui.MachineContainerScreen<?>>() {
                    @Override public java.util.List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(
                            gregtech.client.gui.MachineContainerScreen<?> screen) {
                        var tab = screen.titleBounds();
                        return tab.getWidth() > 0 ? java.util.List.of(tab) : java.util.List.of();
                    }
                });
    }

    @Override public void registerVanillaCategoryExtensions(mezz.jei.api.registration.IVanillaCategoryExtensionRegistration registration) {
        ToolCraftingDisplay.register(registration);
    }

    /** Categories created in {@link #registerCategories}, reused in {@link #registerRecipes}. */
    private final Map<RecipeMap, RecipeMapCategory> categories = new LinkedHashMap<>();
    private static volatile GregTechJEIPlugin active;
    private mezz.jei.api.runtime.IJeiRuntime runtime;
    private final Map<RecipeMap, List<Recipe>> shownRows = new LinkedHashMap<>();
    private final Map<RecipeMap, java.util.Set<Recipe>> knownRows = new LinkedHashMap<>();

    /** Normal JEI reload discovers the current maps through registerRecipes. */
    public static void overlayChanged() {}

    /** Replace old machine rows when an overlay has fallen back to its baseline. */
    public static void overlayFailed() {
        var plugin = active;
        long generation = gregtech.content.recipe.MachineRecipeOverlay.generation();
        if (plugin == null) return;
        net.minecraft.client.Minecraft.getInstance().execute(() -> {
            if (active != plugin || plugin.runtime == null
                    || generation != gregtech.content.recipe.MachineRecipeOverlay.generation()) return;
            var manager = plugin.runtime.getRecipeManager();
            for (var entry : plugin.shownRows.entrySet()) {
                var map = entry.getKey();
                var type = RecipeMapCategory.recipeType(map);
                List<Recipe> now = new ArrayList<>();
                for (Recipe row : map.allRecipes()) if (row.enabled && !row.hidden) now.add(row);
                var removed = new ArrayList<>(entry.getValue());
                removed.removeAll(now);
                manager.hideRecipes(type, removed);
                var known = plugin.knownRows.get(map);
                var added = now.stream().filter(row -> !known.contains(row)).toList();
                if (!added.isEmpty()) manager.addRecipes(type, added);
                manager.unhideRecipes(type, now);
                known.addAll(now);
                entry.setValue(now);
            }
        });
    }

    private Map<RecipeMap,List<ItemStack>> machines = Map.of();

    @Override
    public ResourceLocation getPluginUid() {

        return Mc.gregtechId("jei_plugin");

    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        categories.clear();
        machines = RecipeMachines.collect();
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        for (var family : MaterialUnitTagCategory.FAMILIES)
            registration.addRecipeCategories(new MaterialUnitTagCategory(guiHelper, family));
        for (RecipeMap map : RecipeMap.RECIPE_MAP_LIST) {
            if (!map.viewerAllowed
                    || map.recipeRows.stream().noneMatch(recipe -> recipe.enabled && !recipe.hidden)
                            && !map.hasDynamicEnumeration()) continue;
            var category = new RecipeMapCategory(guiHelper, map, iconFor(map));
            registration.addRecipeCategories(category);
            categories.put(map, category);
        }
        registration.addRecipeCategories(new LootInfoCategories.Category(guiHelper, false), new LootInfoCategories.Category(guiHelper, true));
        registration.addRecipeCategories(new MultiblockInfoCategory(guiHelper));
        registration.addRecipeCategories(new ToolAssemblyCategory.Category(guiHelper));
        registration.addRecipeCategories(new MoldChiselCategory(guiHelper));
        registration.addRecipeCategories(
                new WorldgenInfoCategories.VeinCategory(guiHelper),
                new WorldgenInfoCategories.SmallOreCategory(guiHelper),
                new WorldgenInfoCategories.LayerCategory(guiHelper),
                new WorldgenInfoCategories.BedrockCategory(guiHelper));
    }

    @Override
    public void registerRecipeCatalysts(mezz.jei.api.registration.IRecipeCatalystRegistration registration) {
        for (var mold : gregtech.compat.common.MoldChiselDisplay.molds())
            registration.addRecipeCatalyst(mold, MoldChiselCategory.TYPE);
        for (var table:net.minecraft.core.registries.BuiltInRegistries.BLOCK)
            if (table instanceof gregtech.content.block.misc.AdvancedCraftingTableBlock)
                registration.addRecipeCatalyst(new ItemStack(table),mezz.jei.api.constants.RecipeTypes.CRAFTING);
        // Assembly controllers are recipe outputs, not catalysts for every structure in the category.
        // JEI indexes each controller and its construction parts in MultiblockInfoCategory.setRecipe.
        machines.forEach((map, stacks) -> {
            if (!categories.containsKey(map)) return;
            for (var stack : stacks) registration.addRecipeCatalyst(stack, RecipeMapCategory.recipeType(map));
        });
    }

    @Override public void registerRecipeTransferHandlers(mezz.jei.api.registration.IRecipeTransferRegistration registration){
        registration.addRecipeTransferHandler(new AdvancedCraftingTransferInfo());
    }

    @Override public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration registration) {
        for (var item:gregtech.engine.recipe.UsbRecipeDisplayBinding.sticks())
            registration.registerSubtypeInterpreter(item,(stack,context)->gregtech.engine.recipe.UsbRecipeDisplayBinding.subtype(stack));

        for (var entry : gregtech.content.registry.GTFluidItems.allEntries()) if (entry.isPresent())
            registration.registerSubtypeInterpreter(entry.get(), (stack, context) -> {
                var level = net.minecraft.client.Minecraft.getInstance().level;
                return gregtech.engine.fluid.FluidDisplayBinding.subtype(level == null ? null : level.registryAccess(), stack);
            });

    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        shownRows.clear();
        knownRows.clear();
        gregtech.engine.fluid.FluidContainerDisplayBinding.invalidate();
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

            registration.addRecipes(ToolAssemblyCategory.TYPE, gregtech.content.recipe.ToolAssemblyCatalog.build());
        // EMI registers these same rows natively; keep JEI's bridge from duplicating them.

//? if >=1.21.1 {
/*        if (net.neoforged.fml.ModList.get().isLoaded("emi")) return;
*///?} else {
        if (net.minecraftforge.fml.ModList.get().isLoaded("emi")) return;
//?}

        registration.addRecipes(MoldChiselCategory.TYPE, gregtech.compat.common.MoldChiselDisplay.recipes());
        var unitTags = gregtech.compat.common.MaterialUnitTagDisplay.entries();
        for (var family : MaterialUnitTagCategory.FAMILIES)
            registration.addRecipes(MaterialUnitTagCategory.type(family), MaterialUnitTagCategory.pages(family, unitTags));
        for (Map.Entry<RecipeMap, RecipeMapCategory> entry : categories.entrySet()) {
            List<Recipe> recipes = new ArrayList<>();
            for (Recipe recipe : entry.getKey().allRecipes()) {
                if (recipe.enabled && !recipe.hidden) recipes.add(recipe);
            }
            shownRows.put(entry.getKey(), List.copyOf(recipes));
            var known = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Recipe, Boolean>());
            known.addAll(recipes);
            knownRows.put(entry.getKey(), known);
            if (!recipes.isEmpty()) {
                entry.getValue().fitRecipes(recipes);
                registration.addRecipes(entry.getValue().getRecipeType(),recipes);
                shown += recipes.size();
            }
        }
        } finally {
            if (Integer.getInteger("gregtech.materialScale", 0) > 0) {
                Runtime runtime = Runtime.getRuntime();
                Mc.logger().info("JEI_SCALE recipes {} ms {} items {} heapMb {}",
                        shown, (System.nanoTime() - started) / 1_000_000L,
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.size(),
                        (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576L);
            }
        }
    }

    /** Prefer the machine itself; fall back to a recipe output for maps without a registered machine. */
    private ItemStack iconFor(RecipeMap map) {
        return gregtech.compat.common.RecipeViewerIcons.category(map, machines);
    }

}
