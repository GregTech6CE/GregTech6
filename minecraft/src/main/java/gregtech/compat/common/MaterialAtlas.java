package gregtech.compat.common;

import gregtech.api.addon.material.MaterialFormKind;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.material.GTMaterial;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialDataRules;
import gregtech.content.block.OreBlock;
import gregtech.content.block.OreHostStone;
import gregtech.content.item.ElectricToolItem;
import gregtech.content.recipe.ToolAssemblyCatalog;
import gregtech.content.registry.*;
import gregtech.content.item.FluidDisplayItemLike;
import gregtech.engine.fluid.FluidDisplayBinding;
import gregtech.engine.fluid.FluidCompat;
import gregtech.engine.stack.RegistryContext;
import gregtech.engine.stack.StackCompat;
import gregtech.platform.Mc;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.api.addon.material.GregTechMaterials;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.tool.GTToolHelper;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.material.Fluid;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Viewer-local index, populated during the same enumeration that registers machine recipes. */
public final class MaterialAtlas {
    public enum Kind {
        MATERIAL("material_atlas", "book_bronze"), ORE("ore_processing", "crushed_chalcopyrite");
        public final String id;
        private final String icon;
        Kind(String id, String icon) { this.id = id; this.icon = icon; }
        public ItemStack icon() { return RecipeViewerIcons.item(icon); }
        public Component title() { return label(id); }
    }

    public static Component label(String key, Object... args) {
        return Component.translatable("gt6ce.atlas." + key, args);
    }

    public static Component name(GTMaterial material) {
        return gregtech.engine.material.MaterialPresentation.name(material);
    }

    public sealed interface Preview permits MachineRow, Assembly {
        ResourceLocation id();
        ItemStack workstation();
    }

    public record MachineRow(RecipeMap map, Recipe recipe, int index, ItemStack workstation) implements Preview {
        boolean isHostDust(int slot) {
            if (slot == 0 || recipe.itemInputs.length == 0
                    || (map != MachineRecipeMaps.Crusher && map != MachineRecipeMaps.Hammer)) return false;
            var input = recipe.itemInputs[0];
            if (!(input.getItem() instanceof net.minecraft.world.item.BlockItem block)) return false;
            var host = block.getBlock() instanceof OreBlock ? OreBlock.stoneOfStack(input) : taggedHost(input);
            if (host == null) return false;
            var profile = gregtech.content.block.stone.OreHostFacts.hostProfile(host.id());
            String gangue = profile == null ? host.smallOreDustMaterial()
                    : profile.hasGangue() ? profile.gangueMaterial().name() : null;
            if (gangue == null) return false;
            var target = GTMaterialRegistry.get(gangue).getTargetCrushingMaterial();
            var output = recipe.itemOutputs[slot];
            return output.getItem() instanceof gregtech.engine.material.MaterialFormItem form
                    && form.getPrefix().getName().startsWith("dust")
                    && (form.getMaterial().getName().equals(gangue)
                        || target != null && form.getMaterial().resolve() == target.resolve());
        }

        private static OreHostStone taggedHost(ItemStack input) {
            for (var tag : input.getTags().toList()) {
                var id = tag.location();
                if (!id.getNamespace().equals("forge") && !id.getNamespace().equals("c")) continue;
                switch (id.getPath()) {
                    case "ores_in_ground/stone" -> { return OreHostStone.STONE; }
                    case "ores_in_ground/deepslate" -> { return OreHostStone.DEEPSLATE; }
                    case "ores_in_ground/netherrack" -> { return OreHostStone.NETHERRACK; }
                }
            }
            return null;
        }

        public Component title() { return Component.translatableWithFallback(map.internalName, map.displayName); }
        public ResourceLocation id() { return machineId(map, index); }
    }

    public static ResourceLocation machineId(RecipeMap map, int index) {
        return Mc.gregtechId("/machine/" + map.internalName.toLowerCase(Locale.ROOT) + "/" + index);
    }

    public static ResourceLocation toolId(ToolAssemblyCatalog.ToolAssemblyInfo tool) {
        return Mc.gregtechId("/tool_assembly/" + tool.type().id() + "/" + GTToolHelper.getHead(tool.output()).getId());
    }

    public record Assembly(ItemStack output, ResourceLocation id, ToolAssemblyCatalog.ToolAssemblyInfo tool,
                           CraftingRecipe crafting, List<ItemStack> manualGrid) implements Preview {
        public ItemStack workstation() { return new ItemStack(net.minecraft.world.item.Items.CRAFTING_TABLE); }
        public List<ItemStack> grid() {
            if (tool != null) return manualGrid;
            int width = crafting instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
            var grid = new ArrayList<ItemStack>(Collections.nCopies(9, ItemStack.EMPTY));
            List<net.minecraft.world.item.crafting.Ingredient> ingredients = crafting.getIngredients();
            if (crafting instanceof gregtech.content.recipe.ToolShapelessRecipe conversion)
                ingredients = gregtech.content.recipe.FormConversionDisplay.layout(conversion).orElse(ingredients);
            for (int i = 0; i < ingredients.size(); i++) {
                var choices = ingredients.get(i).getItems();
                if (choices.length > 0) grid.set(i / width * 3 + i % width, choices[0]);
            }
            return grid;
        }
    }
    public record Form(String prefix, ItemStack stack) {}
    private record FluidKey(Fluid fluid, String metadata) {
        static FluidKey of(FluidStack stack) {
            return new FluidKey(stack.getFluid(), FluidCompat.metadataKey(stack));
        }
    }
    public record Route(ItemStack feed, RecipeMap map, List<MachineRow> variants) {}
    public record Page(Kind kind, Entry entry) {
        public List<ItemStack> byproducts() {
            return kind == Kind.ORE ? List.copyOf(entry.byproducts) : List.of();
        }

        public List<ItemStack> lookup() {
            if (kind == Kind.ORE) {
                var feeds = new LinkedHashMap<Item, ItemStack>();
                for (var form : entry.forms)
                    if (STAGES.contains(form.prefix()) || LOOKUP_ONLY.contains(form.prefix()))
                        feeds.putIfAbsent(form.stack().getItem(), form.stack());
                if (!entry.byproducts.isEmpty()) {
                    for (var form : entry.forms) {
                        if (form.prefix().equals("rockGt")) feeds.putIfAbsent(form.stack().getItem(), form.stack());
                    }
                }
                return List.copyOf(feeds.values());
            }
            var stacks = new ArrayList<ItemStack>();
            entry.forms.forEach(f -> stacks.add(f.stack()));
            stacks.addAll(entry.products);
            entry.tools.forEach(t -> stacks.add(t.output()));
            return stacks;
        }
    }

    public static final class Entry {
        public final GTMaterial material;
        public final List<Form> forms = new ArrayList<>();
        public final List<Assembly> tools = new ArrayList<>();
        public final List<ItemStack> products = new ArrayList<>();
        public final List<ItemStack> byproducts = new ArrayList<>();
        public final Map<RouteKey, Route> routes = new LinkedHashMap<>();
        private final Map<Item, Preview> previews;
        private final Map<FluidKey, MachineRow> fluidPreviews;
        private final Map<GTMaterial, Entry> catalog;
        private ItemStack icon = ItemStack.EMPTY;
        int recipeRevision;

        private Entry(GTMaterial material, Map<Item, Preview> previews, Map<FluidKey, MachineRow> fluidPreviews,
                      Map<GTMaterial, Entry> catalog) {
            this.material = material;
            this.previews = previews;
            this.fluidPreviews = fluidPreviews;
            this.catalog = catalog;
        }

        public ItemStack icon() { return icon; }
        public Entry component(GTMaterial part) { return catalog.get(part.resolve()); }
        public Preview preview(ItemStack stack) {
            var fluid = displayedFluid(stack);
            return fluid.isEmpty() ? previews.get(stack.getItem()) : fluidPreviews.get(FluidKey.of(fluid));
        }
    }

    public static GTMaterial toolMaterial(ItemStack stack) {
        return gregtech.api.addon.platform.AddonTools.headMaterial(stack)
                .map(key -> GTMaterialRegistry.get(key.name())).orElse(null);
    }

    public record RouteKey(String stage, RecipeMap map, Item primaryOutput) {}
    private final Map<GTMaterial, Entry> entries = new LinkedHashMap<>();
    private final Map<Item, Entry> oreInputs = new IdentityHashMap<>();
    private final Map<Item, String> routeStages = new IdentityHashMap<>();
    private final Set<Item> displayItems = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Item, Preview> previews = new IdentityHashMap<>();
    private final Map<FluidKey, MachineRow> fluidPreviews = new HashMap<>();
    private final Map<Item, Preview> craftingPreviews = new IdentityHashMap<>();
    private final Map<RecipeMap, ItemStack> machines = new IdentityHashMap<>();
    public static final List<String> STAGES = List.of(
            "ore", "oreRaw", "oreDense", "oreSmall", "rawOreChunk",
            "crushed", "crushedPurified", "crushedCentrifuged");
    private static final Set<String> LOOKUP_ONLY = Set.of(
            "crushedTiny", "crushedPurifiedTiny", "crushedCentrifugedTiny", "dustSmall", "dustTiny");

    public static String stage(String prefix) {
        return prefix.equals("oreRaw") || prefix.equals("oreDense") ? "ore" : prefix;
    }
    private static final List<String> ICONS = List.of("ingot", "gem", "dust", "oreRaw", "liquid", "unit");
    private static final Set<RecipeMap> ORE_MACHINES = Set.of(MachineRecipeMaps.Crusher, MachineRecipeMaps.Hammer,
            MachineRecipeMaps.Bath, MachineRecipeMaps.Sluice, MachineRecipeMaps.MagneticSeparator,
            MachineRecipeMaps.Centrifuge, MachineRecipeMaps.Sifting, MachineRecipeMaps.SteamCracking,
            MachineRecipeMaps.Shredder, MachineRecipeMaps.Drying, MachineRecipeMaps.Mixer,
            MachineRecipeMaps.Electrolyzer, MachineRecipeMaps.Anvil, MachineRecipeMaps.Mortar);

    public MaterialAtlas(List<ToolAssemblyCatalog.ToolAssemblyInfo> assemblies,
                         Map<RecipeMap, List<ItemStack>> workstations) {
        workstations.forEach((map, stacks) -> stacks.stream()
                .filter(s -> s.getItem() instanceof net.minecraft.world.item.BlockItem)
                .findFirst().or(() -> stacks.stream().findFirst()).ifPresent(stack -> machines.put(map, stack)));
        var bindings = GregTechMaterials.formBindings();
        for (var m : GTMaterialRegistry.sortedMaterials()) {
            if (!m.isValid() || m.resolve() != m) continue;
            var entry = entry(m);
            boolean ambientGas = MaterialDataRules.ambientPhase(m) == MaterialDataRules.AmbientPhase.GAS;
            int best = Integer.MAX_VALUE;
            var forms = new ArrayList<>(bindings.find(new MaterialKey(m.getName())));
            forms.sort(Comparator.comparing((gregtech.api.addon.material.MaterialFormBinding f) -> f.form().name())
                    .thenComparing(gregtech.api.addon.material.MaterialFormBinding::id));
            for (var form : forms) {
                String prefix = form.form().name();
                if (ambientGas && prefix.equals("scrapGt")) continue;
                var id = Mc.parseId(form.id());
                var stack = form.kind() == MaterialFormKind.FLUID
                        ? fluid(new FluidStack(BuiltInRegistries.FLUID.get(id), 1000))
                        : new ItemStack(BuiltInRegistries.ITEM.get(id));
                if (stack.isEmpty()) continue;
                entry.forms.add(new Form(prefix, stack));
                displayItems.add(stack.getItem());
                int rank = ICONS.indexOf(prefix);
                if (rank < 0) rank = ICONS.size();
                if (ambientGas && prefix.equals("gas") && form.kind() == MaterialFormKind.FLUID) rank = -1;
                if (rank < best) { entry.icon = stack; best = rank; }
                if (STAGES.contains(prefix)) {
                    oreInputs.put(stack.getItem(), entry);
                    routeStages.put(stack.getItem(), stage(prefix));
                }
            }
        }
        var processes = GregTechMaterials.processes();
        for (var entry : entries.values()) {
            if (entry.forms.stream().noneMatch(form -> form.prefix().equals("crushed"))) continue;
            processes.findSingle(new MaterialKey(entry.material.getName()),
                    gregtech.api.addon.material.MaterialProcessKeys.ORE_PROCESSING).ifPresent(rule -> {
                for (var key : rule.value().byproducts()) {
                    var material = GTMaterialRegistry.get(key.name());
                    var stack = gregtech.util.OM.dustOrIngot(material, gregtech.api.material.GTValues.U);
                    if (!stack.isEmpty() && entry.byproducts.stream().noneMatch(s -> StackCompat.sameItem(s, stack)))
                        entry.byproducts.add(stack);
                }
            });
        }
        gregtech.api.addon.platform.AddonItems.visitMaterialProducts(
                (key, stack) -> product(GTMaterialRegistry.get(key.name()), stack));
        for (var row : assemblies) {
            var grid = new ArrayList<ItemStack>(Collections.nCopies(9, ItemStack.EMPTY));
            int i = 0;
            if (row.pattern() == null) {
                for (var cell : row.inputs()) grid.set(i++, cell.get(0));
            } else {
                var p = row.pattern();
                for (int y = 0; y < p.height(); y++) {
                    for (int x = 0; x < p.width(); x++) {
                        if (p.at(x, y) != ' ') grid.set(y * 3 + x, row.inputs().get(i++).get(0));
                    }
                }
            }
            var tool = new Assembly(row.output(), toolId(row), row, null, grid);
            entry(GTToolHelper.getHead(row.output())).tools.add(tool);
        }
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        record ToolKey(GTMaterial material, Item model) {}
        var electricTools = new LinkedHashMap<ToolKey, Assembly>();
        var recipes = level.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING);
        for (var registered : recipes) {
//? if >=1.21.1 {
            /*var recipe = registered.value();
            var id = registered.id();
*///?} else {
            var recipe = registered;
            var id = recipe.getId();
//?}
            if (!recipe.canCraftInDimensions(3, 3) || recipe.getIngredients().isEmpty()
                    || recipe.getIngredients().size() > 9) continue;
            var result = recipe.getResultItem(level.registryAccess());
            if (result.isEmpty()) continue;
            var assembly = new Assembly(result, id, null, recipe, List.of());
            if (result.getItem() instanceof ElectricToolItem electric) {
                var key = new ToolKey(electric.headMaterial(result).resolve(), electric);
                electricTools.merge(key, assembly, (a, b) -> a.id().compareTo(b.id()) <= 0 ? a : b);
            } else if (displayItems.contains(result.getItem())) {
                previews.merge(result.getItem(), assembly, (a, b) -> a.id().compareTo(b.id()) <= 0 ? a : b);
            }
        }
        electricTools.values().stream().sorted(Comparator.comparing(Assembly::id)).forEach(tool ->
                entry(((ElectricToolItem) tool.output().getItem()).headMaterial(tool.output())).tools.add(tool));
        craftingPreviews.putAll(previews);
    }

    public void clearMachineRecipes() {
        fluidPreviews.clear();
        previews.clear();
        previews.putAll(craftingPreviews);
        for (var entry : entries.values()) {
            entry.routes.clear();
            entry.recipeRevision++;
        }
    }

    private Entry entry(GTMaterial m) {
        return entries.computeIfAbsent(m.resolve(), key -> new Entry(key, previews, fluidPreviews, entries));
    }

    private void product(GTMaterial m, ItemStack stack) {
        var entry = entry(m);
        entry.products.add(stack);
        if (entry.icon.isEmpty()) entry.icon = stack;
        displayItems.add(stack.getItem());
    }

    public static ItemStack fluid(FluidStack fluid) {
        return FluidDisplayBinding.display(fluid, RegistryContext.current());
    }

    public static FluidStack displayedFluid(ItemStack stack) {
        return stack.getItem() instanceof FluidDisplayItemLike
                ? FluidDisplayBinding.resolveCurrent(stack) : FluidStack.EMPTY;
    }

    public void accept(RecipeMap map, Recipe recipe, int index) {
        if (!recipe.enabled || recipe.hidden
                || (recipe.fakeRecipe && (recipe.materialInputDisplay(0) == null
                        || (map != MachineRecipeMaps.Anvil && map != MachineRecipeMaps.Mortar)))
                || map == MachineRecipeMaps.ByProductList
                || map == MachineRecipeMaps.DidYouKnow) return;
        var row = new MachineRow(map, recipe, index, machines.getOrDefault(map, ItemStack.EMPTY));
        if (recipe.itemOutputs.length != 0) {
            var primary = recipe.itemOutputs[0];
            if (!primary.isEmpty() && displayItems.contains(primary.getItem())) {
                var old = previews.get(primary.getItem());
                if (!(old instanceof MachineRow previous) || priority(map) < priority(previous.map()))
                    previews.put(primary.getItem(), row);
            }
        }
        for (var output : recipe.fluidOutputs) {
            if (output == null || output.isEmpty()) continue;
            fluidPreviews.merge(FluidKey.of(output), row, (old, candidate) ->
                    fluidPriority(candidate) < fluidPriority(old) ? candidate : old);
        }
        if (recipe.itemInputs.length == 0 || map == MachineRecipeMaps.Boxinator
                || map == MachineRecipeMaps.Unboxinator) return;
        var feed = recipe.itemInputs[0];
        var entry = oreInputs.get(feed.getItem());
        if (entry == null) return;
        if (map == MachineRecipeMaps.Sifting && feed.getItem() instanceof net.minecraft.world.item.BlockItem block
                && block.getBlock() instanceof OreBlock && OreBlock.stoneOfStack(feed).isSediment()) return;
        // Additional maps belong here when they actually refine an ore into a later intermediate.
        if (!ORE_MACHINES.contains(map) && Arrays.stream(recipe.itemOutputs)
                .noneMatch(s -> !s.isEmpty() && oreInputs.containsKey(s.getItem()) && s.getItem() != feed.getItem()))
            return;
        var primary = recipe.itemOutputs.length == 0 ? ItemStack.EMPTY : recipe.itemOutputs[0];
        var key = new RouteKey(routeStages.get(feed.getItem()), map, primary.getItem());
        var route = entry.routes.computeIfAbsent(key, k -> new Route(feed.copyWithCount(1), map, new ArrayList<>()));
        route.variants.add(row);
    }

    private static int priority(RecipeMap map) {
        if (map == MachineRecipeMaps.Lathe || map == MachineRecipeMaps.Wiremill
                || map == MachineRecipeMaps.RollingMill) return 0;
        if (map == MachineRecipeMaps.Extruder) return 1;
        return 2;
    }

    private static int fluidPriority(MachineRow row) {
        if (row.map() == MachineRecipeMaps.Electrolyzer) {
            for (var input : row.recipe().fluidInputs) {
                if (input != null && input.getFluid() == net.minecraft.world.level.material.Fluids.WATER) return 0;
            }
            return 1;
        }
        return 2;
    }

    public List<Page> pages(Kind kind) {
        return entries.values().stream()
                .filter(e -> !e.icon.isEmpty())
                .filter(e -> kind == Kind.MATERIAL || !e.routes.isEmpty() || !e.byproducts.isEmpty())
                .map(e -> new Page(kind, e)).toList();
    }
}
