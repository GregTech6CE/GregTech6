package gregtech.compat.jei;

import gregtech.client.gui.MachineContainerScreen;
import gregtech.client.gui.RecipeViewerNavigation;
import gregtech.compat.common.MaterialUnitTagDisplay;
import gregtech.compat.common.MoldChiselDisplay;
import gregtech.compat.common.OreGenerationClient;
import gregtech.compat.common.RecipeMachines;
import gregtech.compat.common.RecipeViewerIcons;
import gregtech.content.block.misc.AdvancedCraftingTableBlock;
import gregtech.content.item.CatalogVisibility;
import gregtech.content.loot.LootViewerData;
import gregtech.content.multiblock.MultiblockInfoData;
import gregtech.content.recipe.MachineRecipeOverlay;
import gregtech.content.recipe.ToolAssemblyCatalog;
import gregtech.content.registry.GTFluidItems;
import gregtech.engine.fluid.FluidContainerDisplayBinding;
import gregtech.engine.fluid.FluidDisplayBinding;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.recipe.UsbRecipeDisplayBinding;
import gregtech.platform.LoaderHooks;
import gregtech.platform.Mc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI integration for GT6: one generic {@link RecipeMapCategory} per {@link RecipeMap}
 * that contains recipes, populated from the map's recipe list.
 *
 * <p>{@code FluidItem} proxies appear in JEI's item panel automatically
 * (they are registered in the {@code GregTech Fluids} creative tab).</p>
 */
@JeiPlugin
public final class GregTechJEIPlugin implements IModPlugin {
    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
        this.runtime = runtime;
        active = this;
        RecipeViewerNavigation.registerJei(map -> {
            if (!categories.containsKey(map)) return false;
            runtime.getRecipesGui().showTypes(List.of(RecipeMapCategory.recipeType(map)));
            return true;
        });
        var legacy = BuiltInRegistries.ITEM.stream()
                .filter(item -> !CatalogVisibility.visible(new ItemStack(item)))
                .map(ItemStack::new)
                .toList();
        runtime.getIngredientManager()
                .removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, legacy);
    }

    @Override
    public void onRuntimeUnavailable() {
        if (active == this) active = null;
        runtime = null;
        RecipeViewerNavigation.clearJei();
    }

    @Override
    public void registerGuiHandlers(mezz.jei.api.registration.IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(
                MachineContainerScreen.class,
                new mezz.jei.api.gui.handlers.IGuiContainerHandler<MachineContainerScreen<?>>() {
                    @Override
                    public List<Rect2i> getGuiExtraAreas(MachineContainerScreen<?> screen) {
                        var tab = screen.titleBounds();
                        return tab.getWidth() > 0 ? List.of(tab) : List.of();
                    }
                });
    }

    @Override
    public void registerVanillaCategoryExtensions(
            mezz.jei.api.registration.IVanillaCategoryExtensionRegistration registration) {
        ToolCraftingDisplay.register(registration);
    }

    /** Categories created in {@link #registerCategories}, reused in {@link #registerRecipes}. */
    private final Map<RecipeMap, RecipeMapCategory> categories = new LinkedHashMap<>();

    private static volatile GregTechJEIPlugin active;
    private mezz.jei.api.runtime.IJeiRuntime runtime;
    private final Map<RecipeMap, List<Recipe>> shownRows = new LinkedHashMap<>();
    private final Map<RecipeMap, Set<Recipe>> knownRows = new LinkedHashMap<>();

    /** Normal JEI reload discovers the current maps through registerRecipes. */
    public static void overlayChanged() {}

    /** Replace old machine rows when an overlay has fallen back to its baseline. */
    public static void overlayFailed() {
        var plugin = active;
        long generation = MachineRecipeOverlay.generation();
        if (plugin == null) return;
        Minecraft.getInstance().execute(() -> {
            if (active != plugin || plugin.runtime == null || generation != MachineRecipeOverlay.generation()) return;
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

    private Map<RecipeMap, List<ItemStack>> machines = Map.of();

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
        registration.addRecipeCategories(
                new LootInfoCategories.Category(guiHelper, false), new LootInfoCategories.Category(guiHelper, true));
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
        for (var mold : MoldChiselDisplay.molds()) registration.addRecipeCatalyst(mold, MoldChiselCategory.TYPE);
        for (var table : BuiltInRegistries.BLOCK)
            if (table instanceof AdvancedCraftingTableBlock)
                registration.addRecipeCatalyst(new ItemStack(table), mezz.jei.api.constants.RecipeTypes.CRAFTING);
        // Assembly controllers are recipe outputs, not catalysts for every structure in the category.
        // JEI indexes each controller and its construction parts in MultiblockInfoCategory.setRecipe.
        machines.forEach((map, stacks) -> {
            if (!categories.containsKey(map)) return;
            for (var stack : stacks) registration.addRecipeCatalyst(stack, RecipeMapCategory.recipeType(map));
        });
    }

    @Override
    public void registerRecipeTransferHandlers(mezz.jei.api.registration.IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new AdvancedCraftingTransferInfo());
    }

    @Override
    public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration registration) {
        for (var item : UsbRecipeDisplayBinding.sticks())
            registration.registerSubtypeInterpreter(item, (stack, context) -> UsbRecipeDisplayBinding.subtype(stack));

        for (var entry : GTFluidItems.allEntries())
            if (entry.isPresent())
                registration.registerSubtypeInterpreter(entry.get(), (stack, context) -> {
                    var level = Minecraft.getInstance().level;
                    return FluidDisplayBinding.subtype(stack, level == null ? null : level.registryAccess());
                });
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        shownRows.clear();
        knownRows.clear();
        FluidContainerDisplayBinding.invalidate();
        long started = System.nanoTime();
        int shown = 0;
        try {
            // EMI registers its own pages and otherwise imports a second copy through the JEI bridge.
            if (LoaderHooks.isModLoaded("emi")) return;
            registration.addRecipes(LootInfoCategories.LOOT, LootViewerData.lootGroups());
            registration.addRecipes(LootInfoCategories.MOBS, LootViewerData.mobGroups());
            registration.addRecipes(MultiblockInfoCategory.TYPE, MultiblockInfoData.recipes());
            var atlas = OreGenerationClient.beginBuild();
            registration.addRecipes(WorldgenInfoCategories.VEIN_TYPE, WorldgenInfoCategories.buildVeins());
            registration.addRecipes(WorldgenInfoCategories.SMALL_ORE_TYPE, WorldgenInfoCategories.buildSmallOres());
            registration.addRecipes(WorldgenInfoCategories.LAYER_TYPE, WorldgenInfoCategories.buildLayers());
            registration.addRecipes(WorldgenInfoCategories.BEDROCK_TYPE, WorldgenInfoCategories.buildBedrockOres());
            OreGenerationClient.finishBuild(atlas);
            registration.addRecipes(ToolAssemblyCategory.TYPE, ToolAssemblyCatalog.build());
            registration.addRecipes(MoldChiselCategory.TYPE, MoldChiselDisplay.recipes());
            var unitTags = MaterialUnitTagDisplay.entries();
            for (var family : MaterialUnitTagCategory.FAMILIES)
                registration.addRecipes(
                        MaterialUnitTagCategory.type(family), MaterialUnitTagCategory.pages(family, unitTags));
            for (Map.Entry<RecipeMap, RecipeMapCategory> entry : categories.entrySet()) {
                List<Recipe> recipes = new ArrayList<>();
                for (Recipe recipe : entry.getKey().allRecipes()) {
                    if (recipe.enabled && !recipe.hidden) recipes.add(recipe);
                }
                shownRows.put(entry.getKey(), List.copyOf(recipes));
                var known = Collections.newSetFromMap(new IdentityHashMap<Recipe, Boolean>());
                known.addAll(recipes);
                knownRows.put(entry.getKey(), known);
                if (!recipes.isEmpty()) {
                    entry.getValue().fitRecipes(recipes);
                    registration.addRecipes(entry.getValue().getRecipeType(), recipes);
                    shown += recipes.size();
                }
            }
        } finally {
            if (Integer.getInteger("gregtech.materialScale", 0) > 0) {
                Runtime runtime = Runtime.getRuntime();
                Mc.logger()
                        .info(
                                "JEI_SCALE recipes {} ms {} items {} heapMb {}",
                                shown,
                                (System.nanoTime() - started) / 1_000_000L,
                                BuiltInRegistries.ITEM.size(),
                                (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576L);
            }
        }
    }

    /** Prefer the machine itself; fall back to a recipe output for maps without a registered machine. */
    private ItemStack iconFor(RecipeMap map) {
        return RecipeViewerIcons.category(map, machines);
    }
}
