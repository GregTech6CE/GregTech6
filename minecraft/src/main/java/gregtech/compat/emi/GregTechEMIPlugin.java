package gregtech.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import gregtech.compat.jei.RecipeMachines;
import gregtech.content.item.GTToolItem;
import gregtech.content.recipe.MaterialProcessSnapshot;
import gregtech.engine.recipe.RecipeMap;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Native EMI integration: machine maps are independent of the vanilla RecipeManager and JEI. */
@EmiEntrypoint
public final class GregTechEMIPlugin implements EmiPlugin {
    private static int overlayGeneration;

    @Override
    public void initialize(dev.emi.emi.api.EmiInitRegistry registry) {
        registry.addIngredientSerializer(FluidDisplayEmiStack.class, new FluidDisplayEmiStack.Serializer());
    }

    /** The client overlay calls this before EMI re-registers, so this pass reads the live maps. */
    public static void overlayChanged() {
        overlayGeneration++;
    }

    public static int overlayGeneration() {
        return overlayGeneration;
    }

    @Override
    public void register(EmiRegistry registry) {
        var processSnapshot = MaterialProcessSnapshot.Store.current();
        var processNavigation = new ProcessNavigation(processSnapshot);
        MaterialProcessEmiRecipe.register(
                registry,
                gregtech.compat.common.MaterialProcessProjection.initialPages(processSnapshot, processNavigation));
        gregtech.client.gui.RecipeViewerNavigation.registerEmi(map -> {
            var id = ResourceLocation.fromNamespaceAndPath("gt6ce", map.mNameInternal.toLowerCase(Locale.ROOT));
            var manager = dev.emi.emi.api.EmiApi.getRecipeManager();
            for (var category : manager.getCategories()) {
                if (id.equals(category.getId()) && !manager.getRecipes(category).isEmpty()) {
                    dev.emi.emi.api.EmiApi.displayRecipeCategory(category);
                    return true;
                }
            }
            return false;
        });
        registry.addGenericExclusionArea((screen, areas) -> {
            if (screen instanceof gregtech.client.gui.MachineContainerScreen<?> machine) {
                var tab = machine.titleBounds();
                if (tab.getWidth() > 0)
                    areas.accept(
                            new dev.emi.emi.api.widget.Bounds(tab.getX(), tab.getY(), tab.getWidth(), tab.getHeight()));
            }
        });
        gregtech.engine.fluid.FluidContainerDisplayBinding.invalidate();
        FluidScreenStackProvider.register(registry);
        WoodenBucketLookupRecipe.register(registry);
        FormConversionEmiRecipe.register(registry);
        registry.addRecipeHandler(
                gregtech.content.registry.GTMenuTypes.ADVANCED_CRAFTING.get(), new AdvancedCraftingEmiHandler());
        for (var table : net.minecraft.core.registries.BuiltInRegistries.BLOCK)
            if (table instanceof gregtech.content.block.misc.AdvancedCraftingTableBlock)
                registry.addWorkstation(
                        dev.emi.emi.api.recipe.VanillaEmiRecipeCategories.CRAFTING, EmiStack.of(new ItemStack(table)));
        EmiCatalogRepair.apply();
        registry.removeEmiStacks(stack -> !gregtech.content.item.CatalogVisibility.visible(stack.getItemStack()));
        // EMI builds repair entries from canBeDepleted() alone, so GT tools would still show a dead recipe.
        registry.removeRecipes(GregTechEMIPlugin::repairedGtTool);
        for (var item : gregtech.engine.recipe.UsbRecipeDisplayBinding.sticks())
            registry.setDefaultComparison(
                    item,
                    dev.emi.emi.api.stack.Comparison.compareData(
                            stack -> gregtech.engine.recipe.UsbRecipeDisplayBinding.subtype(stack.getItemStack())));
        var candidates = new EmiItemCandidates();
        // Cans of the same size/food share recipes even when packing gives them a flavour name.
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM)
            if (item instanceof gregtech.content.item.CannedFoodItem)
                registry.setDefaultComparison(item, dev.emi.emi.api.stack.Comparison.compareData(stack -> ""));
        ToolAssemblyEmiRecipe.register(registry, candidates);
        MoldChiselEmiRecipe.register(registry);
        LootEmiRecipe.register(registry);
        StructureEmiRecipe.register(registry);
        WorldgenEmiRecipe.register(registry);
        // Rebuild aliases from this reload's compositions before creating ingredient templates.
        gregtech.engine.material.MaterialDisplayBinding.invalidate();
        var machines = RecipeMachines.collect();
        MaterialUnitTagEmiRecipe.register(registry);
        var ingredients = new MachineEmiIngredients(candidates);
        long started = System.nanoTime();
        int rows = 0, categories = 0;
        for (RecipeMap map : RecipeMap.RECIPE_MAP_LIST) {
            if (!map.mViewerAllowed
                    || map.mRecipeList.stream().noneMatch(r -> r.mEnabled && !r.mHidden)
                            && !map.hasDynamicEnumeration()) continue;
            var workstations = machines.getOrDefault(map, java.util.List.of());
            ItemStack icon = workstations.isEmpty() ? new ItemStack(Items.ANVIL) : workstations.get(0);
            var preferred = gregtech.compat.common.RecipeViewerIcons.map(map);
            if (!preferred.isEmpty()) icon = preferred;
            var id = ResourceLocation.fromNamespaceAndPath("gt6ce", map.mNameInternal.toLowerCase(Locale.ROOT));
            var category = new EmiRecipeCategory(id, EmiStack.of(icon)) {
                @Override
                public Component getName() {
                    return Component.translatableWithFallback(map.mNameInternal, map.mNameLocal);
                }
            };
            registry.addCategory(category);
            for (var station : workstations) registry.addWorkstation(category, EmiStack.of(station));
            int i = 0;
            int before = rows;
            long mapStarted = System.nanoTime();
            com.mojang.logging.LogUtils.getLogger().debug("[gregtech] EMI registering {}", map.mNameInternal);
            for (var recipe : map.allRecipes()) {
                int recipeIndex = i++;
                if (!recipe.mEnabled || recipe.mHidden) continue;
                registry.addRecipe(new MachineEmiRecipe(
                        category,
                        map,
                        recipe,
                        // EMI reserves leading '/' for recipes outside vanilla RecipeManager.
                        ResourceLocation.fromNamespaceAndPath("gt6ce", "/machine/" + id.getPath() + "/" + recipeIndex),
                        ingredients));
                rows++;
            }
            categories++;
            if (rows - before >= 1000)
                com.mojang.logging.LogUtils.getLogger()
                        .info(
                                "[gregtech] EMI registered {} recipes for {} in {} ms",
                                rows - before,
                                map.mNameInternal,
                                (System.nanoTime() - mapStarted) / 1_000_000);
        }
        candidates.finish();
        com.mojang.logging.LogUtils.getLogger()
                .info(
                        "[gregtech] EMI registered {} machine recipes in {} categories in {} ms (overlay {})",
                        rows,
                        categories,
                        (System.nanoTime() - started) / 1_000_000,
                        overlayGeneration);
        if (Integer.getInteger("gregtech.materialScale", 0) > 0) {
            Runtime runtime = Runtime.getRuntime();
            com.mojang.logging.LogUtils.getLogger()
                    .info(
                            "EMI_SCALE recipes {} ms {} items {} heapMb {}",
                            rows,
                            (System.nanoTime() - started) / 1_000_000L,
                            net.minecraft.core.registries.BuiltInRegistries.ITEM.size(),
                            (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576L);
        }
    }

    /** EMI's repair entry lives outside its API artifact, so match it by class name and GT tool output. */
    private static boolean repairedGtTool(EmiRecipe recipe) {
        if (!recipe.getClass().getName().equals("dev.emi.emi.recipe.special.EmiRepairItemRecipe")) return false;
        for (EmiStack stack : recipe.getOutputs()) {
            if (stack.getItemStack().getItem() instanceof GTToolItem) return true;
        }
        return false;
    }

    private static final class ProcessNavigation implements gregtech.compat.common.MaterialProcessDisplay.Navigation {
        private final MaterialProcessSnapshot snapshot;

        private ProcessNavigation(MaterialProcessSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public boolean openRecipe(gregtech.api.addon.material.DataKey reference) {
            if (!MaterialProcessSnapshot.Store.current(snapshot)) return false;
            var nativeRecipe = snapshot.recipe(reference);
            if (nativeRecipe.isEmpty()) return false;
            var row = nativeRecipe.get();
            var id = ResourceLocation.fromNamespaceAndPath(
                    "gt6ce", "/machine/" + row.map().mNameInternal.toLowerCase(Locale.ROOT) + "/" + row.index());
            var recipe = dev.emi.emi.api.EmiApi.getRecipeManager().getRecipe(id);
            if (recipe == null) return false;
            dev.emi.emi.api.EmiApi.displayRecipe(recipe);
            return true;
        }

        @Override
        public boolean openForm(gregtech.api.addon.material.DataKey resource, boolean uses) {
            if (!MaterialProcessSnapshot.Store.current(snapshot)) return false;
            var pages = gregtech.compat.common.MaterialProcessProjection.pages(
                    snapshot,
                    resource,
                    uses
                            ? gregtech.engine.material.MaterialProcessGraph.Direction.USES
                            : gregtech.engine.material.MaterialProcessGraph.Direction.SOURCES,
                    this);
            if (pages.isEmpty()) return false;
            dev.emi.emi.api.EmiApi.displayRecipe(new MaterialProcessEmiRecipe(pages.get(0)));
            return true;
        }
    }
}
