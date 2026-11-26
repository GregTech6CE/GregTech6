package gregtech.compat.jei;

import gregtech.content.recipe.RecipeMachines;
import gregtech.content.multiblock.MultiblockInfoData;
import gregtech.compat.common.MaterialAtlas;
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
 * <p>Fluid lookups use JEI's native fluid ingredients. Display items remain available in the creative tab.</p>
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
        var ingredients = runtime.getIngredientManager();
        var type = mezz.jei.api.constants.VanillaTypes.ITEM_STACK;
        var legacy = ingredients.getAllIngredients(type).stream()
                .filter(stack -> stack.getItem() instanceof gregtech.content.item.FluidDisplayItemLike
                        || !gregtech.content.item.CatalogVisibility.visible(stack))
                .toList();
        ingredients.removeIngredientsAtRuntime(type, legacy);
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

    public static mezz.jei.api.runtime.IIngredientManager ingredients() {
        return active.runtime.getIngredientManager();
    }
    private final Map<RecipeMap, List<Recipe>> shownRows = new LinkedHashMap<>();
    private final Map<RecipeMap, java.util.Set<Recipe>> knownRows = new LinkedHashMap<>();
    private final java.util.Set<MaterialAtlas.Page> knownOrePages = new java.util.LinkedHashSet<>();
    private MaterialAtlas atlas;

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
            if (plugin.atlas == null) return;
            plugin.atlas.clearMachineRecipes();
            for (var entry : plugin.shownRows.entrySet()) {
                var map = entry.getKey();
                var type = RecipeMapCategory.recipeType(map);
                List<Recipe> now = new ArrayList<>();
                map.visitDisplayRows((row, index) -> {
                    now.add(row);
                    plugin.atlas.accept(map, row, index);
                });
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
            var type = AtlasCategory.type(MaterialAtlas.Kind.ORE);
            var pages = plugin.atlas.pages(MaterialAtlas.Kind.ORE);
            var current = java.util.Set.copyOf(pages);
            // JEI indexes ingredients at registration. Hide or restore the original indexed page.
            manager.hideRecipes(type, plugin.knownOrePages.stream().filter(p -> !current.contains(p)).toList());
            manager.unhideRecipes(type, plugin.knownOrePages.stream().filter(current::contains).toList());
            var added = pages.stream().filter(p -> !plugin.knownOrePages.contains(p)).toList();
            if (!added.isEmpty()) manager.addRecipes(type, added);
            plugin.knownOrePages.addAll(added);
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
            if (map == gregtech.content.data.MachineRecipeMaps.ByProductList || !map.viewerAllowed
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
        for (var kind : gregtech.compat.common.MaterialAtlas.Kind.values())
            registration.addRecipeCategories(new AtlasCategory(guiHelper, kind, () -> runtime));
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
                return gregtech.engine.fluid.FluidDisplayBinding.subtype(stack, level == null ? null : level.registryAccess());
            });

    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        atlas = null;
        knownOrePages.clear();
        shownRows.clear();
        knownRows.clear();
        gregtech.engine.fluid.FluidContainerDisplayBinding.invalidate();
        long started = System.nanoTime();
        int shown = 0;
        try {
        // Reads the live RecipeMap lists, including a datapack overlay applied earlier in this reload.
        // EMI has its own native pages. Its JEI bridge must not import a second copy.

//? if >=1.21.1 {
/*        if (net.neoforged.fml.ModList.get().isLoaded("emi")) return;
*///?} else {
        if (net.minecraftforge.fml.ModList.get().isLoaded("emi")) return;
//?}

        registration.addRecipes(LootInfoCategories.LOOT, gregtech.content.loot.LootViewerData.lootGroups());
        registration.addRecipes(LootInfoCategories.MOBS, gregtech.content.loot.LootViewerData.mobGroups());
        registration.addRecipes(MultiblockInfoCategory.TYPE,MultiblockInfoData.recipes());
        registration.addRecipes(WorldgenInfoCategories.VEIN_TYPE, WorldgenInfoCategories.buildVeins());
        registration.addRecipes(WorldgenInfoCategories.SMALL_ORE_TYPE, WorldgenInfoCategories.buildSmallOres());
        registration.addRecipes(WorldgenInfoCategories.LAYER_TYPE, WorldgenInfoCategories.buildLayers());
        registration.addRecipes(WorldgenInfoCategories.BEDROCK_TYPE, WorldgenInfoCategories.buildBedrockOres());
        var assemblies = gregtech.content.recipe.ToolAssemblyCatalog.build();
        registration.addRecipes(ToolAssemblyCategory.TYPE, assemblies);
        atlas = new MaterialAtlas(assemblies, machines);
        registration.addRecipes(MoldChiselCategory.TYPE, gregtech.compat.common.MoldChiselDisplay.recipes());
        var unitTags = gregtech.compat.common.MaterialUnitTagDisplay.entries();
        for (var family : MaterialUnitTagCategory.FAMILIES)
            registration.addRecipes(MaterialUnitTagCategory.type(family), MaterialUnitTagCategory.pages(family, unitTags));
        for (Map.Entry<RecipeMap, RecipeMapCategory> entry : categories.entrySet()) {
            List<Recipe> recipes = new ArrayList<>();
            entry.getKey().visitDisplayRows((row, index) -> {
                recipes.add(row);
                atlas.accept(entry.getKey(), row, index);
            });
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
        for (var kind : MaterialAtlas.Kind.values()) {
            var pages = atlas.pages(kind);
            registration.addRecipes(AtlasCategory.type(kind), pages);
            if (kind == MaterialAtlas.Kind.ORE) knownOrePages.addAll(pages);
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
