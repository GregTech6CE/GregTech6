package gregtech.compat.jei;

import gregtech.content.recipe.MaterialProcessSnapshot;
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
    private mezz.jei.api.runtime.IJeiRuntime runtime;
    private MaterialProcessSnapshot processSnapshot;
    private MaterialProcessCategory processCategory;

    @Override public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
        this.runtime = runtime;
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
        runtime = null;
        processSnapshot = null;
        gregtech.client.gui.RecipeViewerNavigation.clearJei();
    }

    @Override
    public void registerGuiHandlers(mezz.jei.api.registration.IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(gregtech.client.MaterialProcessScreen.class, screen ->
                new mezz.jei.api.gui.handlers.IGuiProperties() {
                    // JEI 19 renamed GUI property accessors; the occupied full-window rectangle is identical.
//? if >=1.21.1 {
                    /*@Override public Class<? extends net.minecraft.client.gui.screens.Screen> screenClass() {
                        return gregtech.client.MaterialProcessScreen.class;
                    }
                    @Override public int guiLeft() { return 0; }
                    @Override public int guiTop() { return 0; }
                    @Override public int guiXSize() { return screen.width; }
                    @Override public int guiYSize() { return screen.height; }
                    @Override public int screenWidth() { return screen.width; }
                    @Override public int screenHeight() { return screen.height; }
*///?} else {
                    @Override public Class<? extends net.minecraft.client.gui.screens.Screen> getScreenClass() {
                        return gregtech.client.MaterialProcessScreen.class;
                    }
                    @Override public int getGuiLeft() { return 0; }
                    @Override public int getGuiTop() { return 0; }
                    @Override public int getGuiXSize() { return screen.width; }
                    @Override public int getGuiYSize() { return screen.height; }
                    @Override public int getScreenWidth() { return screen.width; }
                    @Override public int getScreenHeight() { return screen.height; }
//?}
                });
        registration.addGlobalGuiHandler(new mezz.jei.api.gui.handlers.IGlobalGuiHandler() {
            @Override
            public java.util.Optional<mezz.jei.api.runtime.IClickableIngredient<?>> getClickableIngredientUnderMouse(
                    double x, double y) {
                if (runtime == null || !(net.minecraft.client.Minecraft.getInstance().screen
                        instanceof gregtech.client.MaterialProcessScreen screen)) return java.util.Optional.empty();
                var stack = screen.stackAt(x, y);
                if (stack.isEmpty()) return java.util.Optional.empty();
                Object ingredient = stack;
                if (gregtech.engine.fluid.FluidDisplayBinding.hasPayload(stack)) {
//? if >=1.21.1 {
                    /*var fluid = gregtech.engine.fluid.FluidDisplayBinding.resolveCurrent(stack);
*///?} else {
                    var fluid = gregtech.engine.fluid.FluidDisplayBinding.resolve(stack);
//?}
                    if (fluid.isEmpty()) return java.util.Optional.empty();
                    ingredient = fluid;
                }
                return runtime.getIngredientManager().createClickableIngredient(ingredient, screen.stackArea(x, y), false)
                        .map(value -> (mezz.jei.api.runtime.IClickableIngredient<?>) value);
            }
        });
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
    private static int overlayGeneration;

    /** The client overlay calls this before JEI re-registers, so the next pass reads the live maps. */
    public static void overlayChanged() { overlayGeneration++; }
    public static int overlayGeneration() { return overlayGeneration; }

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
            if (!map.mViewerAllowed
                    || map.mRecipeList.stream().noneMatch(recipe -> recipe.mEnabled && !recipe.mHidden)
                            && !map.hasDynamicEnumeration()) continue;
            var category = new RecipeMapCategory(guiHelper, map, iconFor(map));
            registration.addRecipeCategories(category);
            categories.put(map, category);
        }
        registration.addRecipeCategories(new LootInfoCategories.Category(guiHelper, false), new LootInfoCategories.Category(guiHelper, true));
        registration.addRecipeCategories(new MultiblockInfoCategory(guiHelper));
        registration.addRecipeCategories(new ToolAssemblyCategory.Category(guiHelper));
        registration.addRecipeCategories(new MoldChiselCategory(guiHelper));
        processCategory = new MaterialProcessCategory(guiHelper);
        registration.addRecipeCategories(processCategory);
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
        for(var table:net.minecraft.core.registries.BuiltInRegistries.BLOCK)
            if(table instanceof gregtech.content.block.misc.AdvancedCraftingTableBlock)
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
        for(var item:gregtech.engine.recipe.UsbRecipeDisplayBinding.sticks())
            registration.registerSubtypeInterpreter(item,(stack,context)->gregtech.engine.recipe.UsbRecipeDisplayBinding.subtype(stack));

//? if >=1.21.1 {
/*        for(var entry:gregtech.platform.neoforge.fluid.FluidRegistries.allItems().toList()) if(entry.isBound())
            registration.registerSubtypeInterpreter(entry.get(),(stack,context) -> gregtech.engine.fluid.FluidDisplayBinding.subtype(net.minecraft.client.Minecraft.getInstance().level.registryAccess(),stack));
*///?} else {
        for(var entry:gregtech.content.registry.GTFluidItems.allEntries()) if(entry.isPresent())
            registration.registerSubtypeInterpreter(entry.get(),(stack,context) -> gregtech.engine.fluid.FluidDisplayBinding.subtype(stack));
//?}

    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
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
        processSnapshot = MaterialProcessSnapshot.Store.current();
        var processNavigation = new ProcessNavigation(processSnapshot);
        registration.addRecipes(MaterialProcessCategory.TYPE,
                gregtech.compat.common.MaterialProcessProjection.initialPages(processSnapshot, processNavigation));
        var unitTags = gregtech.compat.common.MaterialUnitTagDisplay.entries();
        for (var family : MaterialUnitTagCategory.FAMILIES)
            registration.addRecipes(MaterialUnitTagCategory.type(family), MaterialUnitTagCategory.pages(family, unitTags));
        for (Map.Entry<RecipeMap, RecipeMapCategory> entry : categories.entrySet()) {
            List<Recipe> recipes = new ArrayList<>();
            for (Recipe recipe : entry.getKey().allRecipes()) {
                if (recipe.mEnabled && !recipe.mHidden) recipes.add(recipe);
            }
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
        var preferred = gregtech.compat.common.RecipeViewerIcons.map(map);
        if (!preferred.isEmpty()) return preferred;
        var catalysts=machines.get(map);
        if (catalysts!=null && !catalysts.isEmpty()) return catalysts.get(0).copy();
        for (Recipe recipe : map.mRecipeList) {
            for (ItemStack out : recipe.mOutputs) {
                if (out != null && !out.isEmpty()) return out;
            }
        }
        return new ItemStack(Items.ANVIL);
    }

    private final class ProcessNavigation implements gregtech.compat.common.MaterialProcessDisplay.Navigation {
        private final MaterialProcessSnapshot snapshot;

        private ProcessNavigation(MaterialProcessSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public boolean valid() {
            return MaterialProcessSnapshot.Store.current(snapshot);
        }

        @Override
        public boolean openStack(ItemStack stack, boolean uses) {
            if (runtime == null || !MaterialProcessSnapshot.Store.current(snapshot) || stack.isEmpty()) return false;
            var role = uses ? mezz.jei.api.recipe.RecipeIngredientRole.INPUT : mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT;
            var factory = runtime.getJeiHelpers().getFocusFactory();
            var focuses = new ArrayList<mezz.jei.api.recipe.IFocus<?>>();
            if (gregtech.engine.fluid.FluidDisplayBinding.hasPayload(stack)) {
//? if >=1.21.1 {
                /*var fluid = gregtech.engine.fluid.FluidDisplayBinding.resolveCurrent(stack);
                if (!fluid.isEmpty()) focuses.add(factory.createFocus(role, mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, fluid));
*///?} else {
                var fluid = gregtech.engine.fluid.FluidDisplayBinding.resolve(stack);
                if (!fluid.isEmpty()) focuses.add(factory.createFocus(role, mezz.jei.api.forge.ForgeTypes.FLUID_STACK, fluid));
//?}
            } else focuses.add(factory.createFocus(role, mezz.jei.api.constants.VanillaTypes.ITEM_STACK, stack));
            if (focuses.isEmpty()) return false;
            runtime.getRecipesGui().show(focuses);
            return true;
        }

        @Override
        public boolean openForm(gregtech.api.addon.material.DataKey resource, boolean uses) {
            if (runtime == null || !MaterialProcessSnapshot.Store.current(snapshot)) return false;
            var pages = gregtech.compat.common.MaterialProcessProjection.pages(snapshot, resource, this);
            runtime.getRecipesGui().showRecipes(processCategory, pages, List.of());
            return true;
        }
    }
}
