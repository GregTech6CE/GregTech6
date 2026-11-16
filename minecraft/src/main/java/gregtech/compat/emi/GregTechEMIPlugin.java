package gregtech.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import gregtech.compat.common.RecipeMachines;
import gregtech.content.item.GTToolItem;
import gregtech.content.recipe.MaterialProcessSnapshot;
import gregtech.engine.recipe.RecipeMap;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Native EMI integration: machine maps are independent of the vanilla RecipeManager and JEI. */
@EmiEntrypoint
public final class GregTechEMIPlugin implements EmiPlugin {
    private static int overlayGeneration;
    private static MaterialProcessSnapshot processSnapshot;

    @Override
    public void initialize(dev.emi.emi.api.EmiInitRegistry registry) {
        registry.addIngredientSerializer(FluidDisplayEmiStack.class, new FluidDisplayEmiStack.Serializer());
    }

    /** The client overlay calls this before EMI re-registers, so this pass reads the live maps. */
    public static void overlayChanged() {
        overlayGeneration++;
        processSnapshot = null;
    }

    public static int overlayGeneration() {
        return overlayGeneration;
    }

    @Override
    public void register(EmiRegistry registry) {
        processSnapshot = null;
        registry.addCategory(MaterialProcessEmiRecipe.category());
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
            if (screen instanceof gregtech.client.MaterialProcessScreen)
                areas.accept(new dev.emi.emi.api.widget.Bounds(0, 0, screen.width, screen.height));
            if (screen instanceof gregtech.client.gui.MachineContainerScreen<?> machine) {
                var tab = machine.titleBounds();
                if (tab.getWidth() > 0)
                    areas.accept(
                            new dev.emi.emi.api.widget.Bounds(tab.getX(), tab.getY(), tab.getWidth(), tab.getHeight()));
            }
        });
        gregtech.engine.fluid.FluidContainerDisplayBinding.invalidate();
        FluidScreenStackProvider.register(registry);
        registry.addGenericStackProvider(EmiWidgetInput::stackAt);
        registry.addStackProvider(gregtech.client.MaterialProcessScreen.class, (screen, x, y) -> {
            var stack = MaterialProcessEmiStacks.of(screen.stackAt(x, y));
            return stack.isEmpty()
                    ? dev.emi.emi.api.stack.EmiStackInteraction.EMPTY
                    : new dev.emi.emi.api.stack.EmiStackInteraction(stack);
        });
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
            ItemStack icon = gregtech.compat.common.RecipeViewerIcons.category(map, machines);
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

    /** Called after EMI has registered deferred rows and applied every recipe filter. */
    public static java.util.List<EmiRecipe> addMaterialProcesses(
            java.util.List<EmiRecipe> recipes,
            java.util.Map<EmiRecipeCategory, java.util.List<dev.emi.emi.api.stack.EmiIngredient>> workstations) {
        long started = System.nanoTime();
        // A repeated bake replaces our previous pages instead of retaining their snapshots and indexes.
        recipes = recipes.stream()
                .filter(recipe -> !(recipe instanceof MaterialProcessEmiRecipe))
                .toList();
        var source = MaterialProcessSnapshot.Store.current();
        var captured = MaterialProcessEmiRecipes.capture(source, recipes, workstations);
        var snapshot = captured.snapshot();
        processSnapshot = snapshot;
        var navigation = new ProcessNavigation(snapshot, source, captured.information(), captured.links());
        var result = new java.util.ArrayList<>(recipes);
        var materials = new java.util.HashSet<String>();
        for (var root : snapshot.roots()) {
            var information = captured.information().getOrDefault(root.material(), java.util.List.of());
            var indexedPorts = new java.util.ArrayList<>(
                    gregtech.compat.common.MaterialProcessProjection.indexedPorts(snapshot, root.resource()));
            information.forEach(entry -> indexedPorts.addAll(entry.index()));
            result.add(new MaterialProcessEmiRecipe(
                    root.material(),
                    () -> navigation.valid()
                            ? gregtech.compat.common.MaterialProcessProjection.pages(
                                            snapshot, root.resource(), navigation)
                                    .get(0)
                                    .withInformation(information)
                            : gregtech.compat.common.MaterialProcessDisplay.unbound()
                                    .get(0),
                    indexedPorts));
            materials.add(root.material());
        }
        captured.information().forEach((material, entries) -> {
            if (!materials.contains(material))
                result.add(
                        new MaterialProcessEmiRecipe(gregtech.compat.common.MaterialProcessProjection.informationPage(
                                material, entries, navigation)));
        });
        com.mojang.logging.LogUtils.getLogger()
                .info(
                        "[gregtech] EMI material process index: {} materials from {} viewer recipes in {} ms; pages build on demand",
                        result.size() - recipes.size(),
                        recipes.size(),
                        (System.nanoTime() - started) / 1_000_000L);
        return java.util.List.copyOf(result);
    }

    private static final class ProcessNavigation implements gregtech.compat.common.MaterialProcessDisplay.Navigation {
        private final MaterialProcessSnapshot snapshot;
        private final MaterialProcessSnapshot source;
        private final java.util.Map<String, java.util.List<gregtech.compat.common.MaterialProcessDisplay.Information>>
                information;
        private final java.util.Map<gregtech.api.addon.material.DataKey, EmiRecipe> informationLinks;

        private ProcessNavigation(
                MaterialProcessSnapshot snapshot,
                MaterialProcessSnapshot source,
                java.util.Map<String, java.util.List<gregtech.compat.common.MaterialProcessDisplay.Information>>
                        information,
                java.util.Map<gregtech.api.addon.material.DataKey, EmiRecipe> informationLinks) {
            this.snapshot = snapshot;
            this.source = source;
            this.information = information;
            this.informationLinks = informationLinks;
        }

        @Override
        public boolean valid() {
            return processSnapshot == snapshot
                    && MaterialProcessSnapshot.Store.current(source)
                    && net.minecraft.client.Minecraft.getInstance().level != null;
        }

        @Override
        public boolean openStack(ItemStack stack, boolean uses) {
            if (!valid() || stack.isEmpty()) return false;
            var ingredient = MaterialProcessEmiStacks.of(stack);
            if (ingredient.isEmpty()) return false;
            if (uses) dev.emi.emi.api.EmiApi.displayUses(ingredient);
            else dev.emi.emi.api.EmiApi.displayRecipes(ingredient);
            return true;
        }

        @Override
        public boolean openForm(gregtech.api.addon.material.DataKey resource, boolean uses) {
            if (!valid()) return false;
            var pages = gregtech.compat.common.MaterialProcessProjection.pages(snapshot, resource, this);
            if (pages.isEmpty()) return false;
            var page = pages.get(0);
            dev.emi.emi.api.EmiApi.displayRecipe(new MaterialProcessEmiRecipe(
                    page.withInformation(information.getOrDefault(page.material(), java.util.List.of()))));
            return true;
        }

        @Override
        public boolean openInformation(gregtech.api.addon.material.DataKey reference) {
            if (!valid()) return false;
            var recipe = informationLinks.get(reference);
            if (recipe == null) return false;
            if (net.minecraft.client.Minecraft.getInstance().screen
                    instanceof gregtech.client.MaterialProcessScreen screen) screen.onClose();
            dev.emi.emi.api.EmiApi.displayRecipe(recipe);
            return true;
        }
    }
}
