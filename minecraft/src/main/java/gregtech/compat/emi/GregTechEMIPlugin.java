package gregtech.compat.emi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import gregtech.client.RecipeViewerRefresh;
import gregtech.client.gui.MachineContainerScreen;
import gregtech.client.gui.RecipeViewerNavigation;
import gregtech.compat.common.RecipeMachines;
import gregtech.compat.common.RecipeViewerIcons;
import gregtech.content.block.misc.AdvancedCraftingTableBlock;
import gregtech.content.item.CannedFoodItem;
import gregtech.content.item.CatalogVisibility;
import gregtech.content.item.GTToolItem;
import gregtech.content.multiblock.MultiblockInfoData;
import gregtech.content.recipe.MachineRecipeOverlay;
import gregtech.content.registry.GTMenuTypes;
import gregtech.engine.fluid.FluidContainerDisplayBinding;
import gregtech.engine.material.MaterialDisplayBinding;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.recipe.UsbRecipeDisplayBinding;
import gregtech.platform.Mc;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Native EMI integration: machine maps are independent of the vanilla RecipeManager and JEI. */
@EmiEntrypoint
public final class GregTechEMIPlugin implements EmiPlugin {
    private static int overlayGeneration;

    @Override
    public void initialize(EmiInitRegistry registry) {
        registry.addIngredientSerializer(FluidDisplayEmiStack.class, new FluidDisplayEmiStack.Serializer());
        registry.addIngredientSerializer(NativeFluidEmiStack.class, new NativeFluidEmiStack.Serializer());
    }

    /** The client overlay calls this before EMI re-registers, so this pass reads the live maps. */
    public static void overlayChanged() {
        overlayGeneration++;
    }

    public static int overlayGeneration() {
        return overlayGeneration;
    }

    /** EMI's locked 1.1.24 release exposes reload through its runtime, not its public API. */
    public static void overlayFailed() {
        long generation = MachineRecipeOverlay.generation();
        Minecraft.getInstance().execute(() -> {
            if (generation != MachineRecipeOverlay.generation()) return;
            reload();
        });
    }

    private static boolean reloadFailed;

    private static boolean reload() {
        if (reloadFailed) return true;
        try {
            Class.forName("dev.emi.emi.runtime.EmiReloadManager")
                    .getMethod("reload")
                    .invoke(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            disableReload(error);
        }
        return true;
    }

    private static void disableReload(Throwable error) {
        reloadFailed = true;
        Mc.logger().warn("Cannot refresh EMI recipes", error);
    }

    @Override
    public void register(EmiRegistry registry) {
        RecipeViewerNavigation.registerEmi(map -> {
            var id = ResourceLocation.fromNamespaceAndPath("gt6ce", map.internalName.toLowerCase(Locale.ROOT));
            var manager = EmiApi.getRecipeManager();
            for (var category : manager.getCategories()) {
                if (id.equals(category.getId()) && !manager.getRecipes(category).isEmpty()) {
                    EmiApi.displayRecipeCategory(category);
                    return true;
                }
            }
            return false;
        });
        registry.addGenericExclusionArea((screen, areas) -> {
            if (screen instanceof MachineContainerScreen<?> machine) {
                var tab = machine.titleBounds();
                if (tab.getWidth() > 0)
                    areas.accept(new Bounds(tab.getX(), tab.getY(), tab.getWidth(), tab.getHeight()));
            }
        });
        FluidContainerDisplayBinding.invalidate();
        FluidScreenStackProvider.register(registry);
        WoodenBucketLookupRecipe.register(registry);
        FormConversionEmiRecipe.register(registry);
        registry.addRecipeHandler(GTMenuTypes.ADVANCED_CRAFTING.get(), new AdvancedCraftingEmiHandler());
        for (var table : Mc.blocks())
            if (table instanceof AdvancedCraftingTableBlock)
                registry.addWorkstation(VanillaEmiRecipeCategories.CRAFTING, EmiStack.of(new ItemStack(table)));
        EmiCatalogRepair.apply();
        // Native fluid proxies carry a creative payload only in cheat mode; never filter them out.
        registry.removeEmiStacks(
                stack -> !(stack instanceof NativeFluidEmiStack) && !CatalogVisibility.visible(stack.getItemStack()));
        // Dedicated lava display and the generic fluid_display carrier duplicate native fluids in EMI's item list.
        var lavaDisplayId =
                ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, "fluid_item_lava");
        var genericDisplayId =
                ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, "fluid_display");
        registry.removeEmiStacks(stack -> !(stack instanceof NativeFluidEmiStack)
                && (lavaDisplayId.equals(stack.getId()) || genericDisplayId.equals(stack.getId())));
        // EMI builds repair entries from canBeDepleted() alone, so GT tools would still show a dead recipe.
        registry.removeRecipes(GregTechEMIPlugin::repairedGtTool);
        for (var item : UsbRecipeDisplayBinding.sticks())
            registry.setDefaultComparison(
                    item, Comparison.compareData(stack -> UsbRecipeDisplayBinding.subtype(stack.getItemStack())));
        var candidates = new EmiItemCandidates();
        // Cans of the same size/food share recipes even when packing gives them a flavour name.
        for (var item : Mc.items())
            if (item instanceof CannedFoodItem)
                registry.setDefaultComparison(item, Comparison.compareData(stack -> ""));
        ToolAssemblyEmiRecipe.register(registry, candidates);
        MoldChiselEmiRecipe.register(registry);
        LootEmiRecipe.register(registry);
        var atlas = RecipeViewerRefresh.beginBuild();
        StructureEmiRecipe.register(registry, MultiblockInfoData.recipes(atlas.layouts()));
        WorldgenEmiRecipe.register(registry, atlas.summary());
        RecipeViewerRefresh.finishBuild(atlas, GregTechEMIPlugin::reload);
        // Rebuild aliases from this reload's compositions before creating ingredient templates.
        MaterialDisplayBinding.invalidate();
        var machines = RecipeMachines.collect();
        MaterialUnitTagEmiRecipe.register(registry);
        var ingredients = new MachineEmiIngredients(candidates);
        long started = System.nanoTime();
        int rows = 0, categories = 0;
        for (RecipeMap map : RecipeMap.RECIPE_MAP_LIST) {
            if (!map.viewerAllowed
                    || map.recipeRows.stream().noneMatch(r -> r.enabled && !r.hidden) && !map.hasDynamicEnumeration())
                continue;
            var workstations = machines.getOrDefault(map, List.of());
            ItemStack icon = RecipeViewerIcons.category(map, machines);
            var id = ResourceLocation.fromNamespaceAndPath("gt6ce", map.internalName.toLowerCase(Locale.ROOT));
            var category = new EmiRecipeCategory(id, EmiStack.of(icon)) {
                @Override
                public Component getName() {
                    return Component.translatableWithFallback(map.internalName, map.displayName);
                }
            };
            registry.addCategory(category);
            for (var station : workstations) registry.addWorkstation(category, EmiStack.of(station));
            int i = 0;
            int before = rows;
            long mapStarted = System.nanoTime();
            Mc.logger().debug("[gregtech] EMI registering {}", map.internalName);
            for (var recipe : map.allRecipes()) {
                int recipeIndex = i++;
                if (!recipe.enabled || recipe.hidden) continue;
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
                Mc.logger()
                        .info(
                                "[gregtech] EMI registered {} recipes for {} in {} ms",
                                rows - before,
                                map.internalName,
                                (System.nanoTime() - mapStarted) / 1_000_000);
        }
        candidates.finish();
        Mc.logger()
                .info(
                        "[gregtech] EMI registered {} machine recipes in {} categories in {} ms (overlay {})",
                        rows,
                        categories,
                        (System.nanoTime() - started) / 1_000_000,
                        overlayGeneration);
        if (Integer.getInteger("gregtech.materialScale", 0) > 0) {
            Runtime runtime = Runtime.getRuntime();
            Mc.logger()
                    .info(
                            "EMI_SCALE recipes {} ms {} items {} heapMb {}",
                            rows,
                            (System.nanoTime() - started) / 1_000_000L,
                            Mc.itemStream().count(),
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
}
