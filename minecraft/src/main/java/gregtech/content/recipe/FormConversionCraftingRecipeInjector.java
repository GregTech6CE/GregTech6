package gregtech.content.recipe;

//? if >=1.21.1 {
/*import java.util.Map;
import gregtech.content.data.OriginalCraftingJson;
*///?} else {
import gregtech.GregTech;
//?}
import gregtech.api.material.GTMaterial;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.content.recipe.FormConversionSelector;
import gregtech.content.recipe.OriginalFormConversions;
import gregtech.content.recipe.OriginalFormConversions.Conversion;
import gregtech.content.recipe.CraftingMaterialForms;
import gregtech.engine.material.MaterialUnification;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.prefix.BlockPrefixRegistry;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Loader_Recipes_Handlers:552-625 material-form conversions. Single-input variants select their
 * output in fixed grid slots in this port; dust uses small then tiny as requested. Other outputs
 * retain source prefix registration order, and missing outputs still reserve their source offset. XToY requires exactly the same material in every occupied cell.
 */
public final class FormConversionCraftingRecipeInjector {
    private static final List<Conversion> ONE_TO_MANY = OriginalFormConversions.FIXED.stream().filter(Conversion::single).toList();
    private static final List<Conversion> MANY_TO_MANY = OriginalFormConversions.FIXED.stream().filter(c -> !c.single()).toList();

    private static final List<String> REGISTERED = new ArrayList<>();
    /** Source constructor replacement rules also guard plain recipes added after the reload lifecycle. */
    public static boolean disallowsPlainRecipe(net.minecraft.world.item.crafting.CraftingRecipe recipe,
                                               net.minecraft.core.RegistryAccess access) {
        return CraftingMaterialForms.replaces(recipe, access);
    }

    private FormConversionCraftingRecipeInjector() {}

    /** Recipe ids registered by the last server start, for diagnostics and tests. */
    public static List<String> registeredIds() { return List.copyOf(REGISTERED); }

    public static List<String> skipped() {
        return CraftingMaterialForms.contains("rawOreChunk") ? List.of() : List.of(
                "rawOreChunk (:553/:576): no external tagged item; GT6 registers no item for this Harder Ores prefix");
    }

//? if >=1.21.1 {
    /*public static void add(Map<ResourceLocation, byte[]> recipes) {
        int before = recipes.size();
*///?} else {
    public static synchronized void apply(RecipeManager manager, net.minecraft.core.RegistryAccess access) {
        List<Recipe<?>> recipes = new ArrayList<>(manager.getRecipes());
//?}
        REGISTERED.clear();
//? if >=1.21.1 {
        /*CraftingMaterialForms.rebuild();
*///?} else {
        CraftingMaterialForms.rebuild(true);
        int before = recipes.size();
        recipes.removeIf(recipe -> recipe instanceof net.minecraft.world.item.crafting.CraftingRecipe crafting
                && !ConfiguredRecipeRules.keepVanillaStorageBlockRecipe(recipe.getId())
                && disallowsPlainRecipe(crafting, access));
        GregTech.LOGGER.info("Replaced {} plain source-equivalent crafting rows", before - recipes.size());
//?}

        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (!material.isValid()) continue; // Both source crafting constructors default to MT.NULL.NOT.
            for (Conversion conversion : ONE_TO_MANY) {
                ItemStack in = item(conversion.input(), material, 1);
                ItemStack out = item(conversion.output(), material, conversion.outputCount());
                if (in.isEmpty() || out.isEmpty()) continue;
                register(recipes, conversion, material, in, out);
            }
            for (Conversion conversion : MANY_TO_MANY) {
                ItemStack in = item(conversion.input(), material, 1);
                ItemStack out = item(conversion.output(), material, conversion.outputCount());
                if (in.isEmpty() || out.isEmpty()) continue;
                register(recipes, conversion, material, in, out);
            }
        }

        registerOreConversions(recipes);

        int wireSizes = registerWireSizes(recipes);
        int pipeSizes = registerPipeSizes(recipes);
//? if >=1.21.1 {
        /*com.mojang.logging.LogUtils.getLogger().info("[gregtech] Native material grid recipes generated={} wire-size={} pipe-size={}", recipes.size()-before, wireSizes, pipeSizes);
        com.mojang.logging.LogUtils.getLogger().info("Registered {} crafting-grid material form conversions from GT6"
*///?} else {
        if (!REGISTERED.isEmpty() || wireSizes > 0 || pipeSizes > 0) gregtech.content.recipe.RuntimeRecipeLifecycle.replaceGenerated(manager, recipes);
        GregTech.LOGGER.info("Registered {} crafting-grid material form conversions from GT6"
//?}
                + " AdvancedCraftingXToY/1ToY ({} GT6 pairs skipped: {})",
                REGISTERED.size(), skipped().size(), skipped());
    }

//? if >=1.21.1 {
    /*public static void replacePlain(RecipeManager manager, net.minecraft.core.RegistryAccess access) {
        CraftingMaterialForms.rebuild(true);
        var recipes = new ArrayList<>(manager.getRecipes());
        int before = recipes.size();
        recipes.removeIf(holder -> holder.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe crafting
                && !ConfiguredRecipeRules.keepVanillaStorageBlockRecipe(holder.id())
                && disallowsPlainRecipe(crafting, access));
        int removed = before - recipes.size();
        // Rebind existing generated rows too: their JSON ingredient aliases were indexed before
        // this reload's tags existed. Only this loader's own form-conversion rows are rebuilt.
        int beforeRebind = recipes.size();
        recipes.removeIf(holder -> holder.id().getNamespace().equals("gt6ce")
                && holder.id().getPath().startsWith("form_conversion/")
                && holder.value() instanceof gregtech.content.recipe.ToolShapelessRecipe);
        int rebound = beforeRebind - recipes.size();
        REGISTERED.removeIf(id -> id.startsWith("gt6ce:form_conversion/"));
        var ids = new java.util.HashSet<ResourceLocation>();
        for (var holder : recipes) ids.add(holder.id());
        int added = 0;
        // Generated JSON is built before this reload's tags exist. Bind all source forms now,
        // including Harder Ores raw chunks, without creating a GT-owned item for the prefix.
        for (var material : GTMaterialRegistry.allMaterials()) if (material.isValid()) {
            for (var conversion : OriginalFormConversions.FIXED) {
                added += addBoundConversion(recipes, ids, conversion, material);
            }
        }
        for (var entry : CraftingMaterialForms.nativeAliases().keySet()) {
            int count = OriginalFormConversions.STANDARD_ORES.contains(entry.prefix()) ? 1
                    : OriginalFormConversions.DENSE_ORES.contains(entry.prefix()) ? 2 : 0;
            if (count > 0) added += addBoundConversion(recipes, ids,
                    new Conversion(entry.prefix(), 1, "oreRaw", count, true), GTMaterialRegistry.get(entry.material()).resolve());
        }
        if (removed > 0 || rebound > 0 || added > 0) manager.replaceRecipes(recipes);
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Replaced {} plain source-equivalent crafting rows; rebuilt {} tag-bound conversions ({} prior rows)", removed, added, rebound);
    }

    private static int addBoundConversion(List<net.minecraft.world.item.crafting.RecipeHolder<?>> recipes,
                                          java.util.Set<ResourceLocation> ids, Conversion conversion, GTMaterial material) {
        var id = conversionId(conversion, material);
        if (ids.contains(id)) return 0;
        var input = item(conversion.input(), material, 1);
        var output = item(conversion.output(), material, conversion.outputCount());
        if (input.isEmpty() || output.isEmpty()) return 0;
        var ingredients = NonNullList.withSize(conversion.inputCount(), formIngredient(input));
        var base = new ShapelessRecipe("gt.form_conversion", CraftingBookCategory.MISC,
                CraftingMaterialForms.canonical(output.copy()), ingredients);
        var recipe = new gregtech.content.recipe.ToolShapelessRecipe(base, false,
                FormConversionPlacement.orderedSelector(conversion), conversion.input(), material.resolve().getName(), false, FormConversionPlacement.FIXED);
        recipes.add(new net.minecraft.world.item.crafting.RecipeHolder<>(id, recipe));
        ids.add(id); REGISTERED.add(id.toString()); return 1;
    }

*///?} else {
//?}
    /** Source :615-616: all standard/dense prefix aliases, including actual vanilla ore blocks. */
//? if >=1.21.1 {
    /*private static void registerOreConversions(Map<ResourceLocation, byte[]> recipes) {
*///?} else {
    private static void registerOreConversions(List<Recipe<?>> recipes) {
//?}
        for (var entry : CraftingMaterialForms.nativeAliases().entrySet()) {
            var form = entry.getKey();
            int count = OriginalFormConversions.STANDARD_ORES.contains(form.prefix()) ? 1
                    : OriginalFormConversions.DENSE_ORES.contains(form.prefix()) ? 2 : 0;
            if (count == 0 || entry.getValue().isEmpty()) continue;
            var material = GTMaterialRegistry.get(form.material()).resolve();
            var output = GTItems.getStack(MaterialPrefix.oreRaw, material, count);
            if (output.isEmpty()) continue;
            register(recipes, new Conversion(form.prefix(), 1, "oreRaw", count, true), material,
                    new ItemStack(entry.getValue().get(0)), output);
        }
    }

    /**
     * GT6 {@code Loader_Recipes_Handlers:619-626}: for every size pair {@code big % small == 0} with
     * divisible sizes, the crafting grid combines fewer than ten small wires into one big wire
     * and splits one big wire into all {@code tAmount} small ones (the machine
     * variants run on the Loom and the Unboxinator). The port's wires are blocks, not material-prefix
     * items, so these rows are built from the wire registry instead of the prefix tables.
     */
//? if >=1.21.1 {
    /*private static int registerWireSizes(Map<ResourceLocation, byte[]> recipes) {
*///?} else {
    private static int registerWireSizes(List<Recipe<?>> recipes) {
//?}
        java.util.Map<String, java.util.Map<Integer, net.minecraft.world.item.Item>> families =
                new java.util.LinkedHashMap<>();
        for (var entry : gregtech.content.registry.GTWires.allWires()) {
//? if >=1.21.1 {
            /*if (!entry.isBound()) continue;
*///?} else {
            if (!entry.isPresent()) continue;
//?}
            var block = entry.get();
            families.computeIfAbsent(block.spec().id(), ignored -> new java.util.TreeMap<>())
                    .put(block.spec().size(), block.asItem());
        }
        int added = 0;
        for (var family : families.entrySet()) {
            var sizes = family.getValue();
            for (int big = 1; big <= 16; big++) {
                var bigItem = sizes.get(big);
                if (bigItem == null) continue;
                int variants = 0;
                for (int small = 1; small < big; small++) if (big % small == 0) variants++;
                int offset = 0;
                for (int small = 1; small < big; small++) {
                    if (big % small != 0) continue;
                    var selector = new FormConversionSelector(variants, offset++);
                    var smallItem = sizes.get(small);
                    if (smallItem == null) continue;
                    int amount = big / small;
                    // The source guards only XToY; the reverse 1ToY also allows 10..16 outputs.
                    if (amount < 10) added += recipe(recipes, new ItemStack(smallItem, amount), new ItemStack(bigItem),
                            "wire_sizes/" + sanitize(family.getKey()) + "_" + small + "_to_" + big);
                    added += recipe(recipes, new ItemStack(bigItem), new ItemStack(smallItem, amount),
                            "wire_sizes/" + sanitize(family.getKey()) + "_" + big + "_to_" + small, false, selector);
                }
            }
        }
        return added;
    }

    /**
     * GT6 {@code MultiTileEntityPipeFluid:100-101}: the crafting grid takes a quadruple fluid pipe
     * apart into four medium ones and a nonuple one into nine small ones (the build-up direction is the
     * Boxinator's job, {@code Loader_Recipes_Handlers:492}).
     */
//? if >=1.21.1 {
    /*private static int registerPipeSizes(Map<ResourceLocation, byte[]> recipes) {
*///?} else {
    private static int registerPipeSizes(List<Recipe<?>> recipes) {
//?}
        java.util.Map<String, java.util.Map<gregtech.engine.machine.PipeSpec.PipeSize,
                net.minecraft.world.item.Item>> families = new java.util.LinkedHashMap<>();
//? if >=1.21.1 {
        /*for (var holder : gregtech.content.transport.fluid.FluidTransportRegistries.pipes()) {
            if (!holder.isBound()) continue;
*///?} else {
        for (var holder : gregtech.content.registry.GTFluidPipes.all()) {
            if (!holder.isPresent()) continue;
//?}
            var spec = holder.get().spec();
            families.computeIfAbsent(spec.material().getName(), ignored -> new java.util.EnumMap<>(
                    gregtech.engine.machine.PipeSpec.PipeSize.class))
                    .put(spec.size(), holder.get().asItem());
        }
        int added = 0;
        for (var family : families.entrySet()) {
            added += pipe(recipes, family, gregtech.engine.machine.PipeSpec.PipeSize.QUADRUPLE,
                    gregtech.engine.machine.PipeSpec.PipeSize.MEDIUM, 4);
            added += pipe(recipes, family, gregtech.engine.machine.PipeSpec.PipeSize.NONUPLE,
                    gregtech.engine.machine.PipeSpec.PipeSize.SMALL, 9);
        }
        return added;
    }

//? if >=1.21.1 {
    /*private static int pipe(Map<ResourceLocation, byte[]> recipes,
*///?} else {
    private static int pipe(List<Recipe<?>> recipes,
//?}
                            java.util.Map.Entry<String, java.util.Map<
                                    gregtech.engine.machine.PipeSpec.PipeSize,
                                    net.minecraft.world.item.Item>> family,
                            gregtech.engine.machine.PipeSpec.PipeSize from,
                            gregtech.engine.machine.PipeSpec.PipeSize to, int count) {
        var big = family.getValue().get(from);
        var small = family.getValue().get(to);
        if (big == null || small == null) return 0;
        return recipe(recipes, new ItemStack(big), new ItemStack(small, count),
                "pipe_sizes/" + sanitize(family.getKey()) + "_" + from.name().toLowerCase() + "_to_" + count
                        + "x_" + to.name().toLowerCase(), true);
    }

//? if >=1.21.1 {
    /*private static int recipe(Map<ResourceLocation, byte[]> recipes, ItemStack input, ItemStack output, String path) {
*///?} else {
    private static int recipe(List<Recipe<?>> recipes, ItemStack input, ItemStack output, String path) {
//?}
        return recipe(recipes, input, output, path, false);
    }

//? if >=1.21.1 {
    /*private static int recipe(Map<ResourceLocation, byte[]> recipes, ItemStack input, ItemStack output, String path, boolean autocraftable) {
*///?} else {
    private static int recipe(List<Recipe<?>> recipes, ItemStack input, ItemStack output, String path, boolean autocraftable) {
//?}
        return recipe(recipes, input, output, path, autocraftable, FormConversionSelector.NONE);
    }

//? if >=1.21.1 {
    /*private static int recipe(Map<ResourceLocation, byte[]> recipes, ItemStack input, ItemStack output, String path,
*///?} else {
    private static int recipe(List<Recipe<?>> recipes, ItemStack input, ItemStack output, String path,
//?}
                              boolean autocraftable, FormConversionSelector selector) {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(input.getCount(),
                autocraftable ? Ingredient.of(input) : CraftingMaterialForms.ingredient(input));
        var inputForm = autocraftable ? null : CraftingMaterialForms.form(input);
//? if >=1.21.1 {
        /*ResourceLocation id = ResourceLocation.fromNamespaceAndPath("gt6ce", path);
        if (!autocraftable && inputForm != null)
            OriginalCraftingJson.orderedFormConversion(recipes, id, "gt.wire_sizes", CraftingBookCategory.MISC,
                    CraftingMaterialForms.canonical(output.copy()), ingredients, selector, inputForm.prefix(), inputForm.material());
        else OriginalCraftingJson.shapeless(recipes, id, "gt.wire_sizes", CraftingBookCategory.MISC,
                    CraftingMaterialForms.canonical(output.copy()), ingredients, autocraftable);
*///?} else {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE, path);
        recipes.add(new gregtech.content.recipe.ToolShapelessRecipe(new ShapelessRecipe(id, "gt.wire_sizes", CraftingBookCategory.MISC,
                CraftingMaterialForms.canonical(output.copy()), ingredients), autocraftable, selector,
                inputForm == null ? "" : inputForm.prefix(), inputForm == null ? "" : inputForm.material(), false, FormConversionPlacement.FIXED));
//?}
        REGISTERED.add(id.toString());
        return 1;
    }

//? if >=1.21.1 {
    /*private static void register(Map<ResourceLocation, byte[]> recipes, Conversion conversion, GTMaterial material,
*///?} else {
    private static void register(List<Recipe<?>> recipes, Conversion conversion, GTMaterial material,
//?}
                                 ItemStack input, ItemStack output) {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(conversion.inputCount(),
                formIngredient(input));
        ItemStack result = CraftingMaterialForms.canonical(output.copy());
//? if >=1.21.1 {
        /*ResourceLocation id = conversionId(conversion, material);
        OriginalCraftingJson.orderedFormConversion(recipes, id, "gt.form_conversion", CraftingBookCategory.MISC, result, ingredients, FormConversionPlacement.orderedSelector(conversion),
                conversion.input(), material.resolve().getName());
        REGISTERED.add(id.toString());
    }

    private static ResourceLocation conversionId(Conversion conversion, GTMaterial material) {
        return ResourceLocation.fromNamespaceAndPath("gt6ce",
*///?} else {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE,
//?}
                "form_conversion/" + sanitize(conversion.input()) + "_to_" + sanitize(conversion.output()) + "/"
                        + sanitize(material.getName()) + "_" + conversion.inputCount());
//? if >=1.21.1 {
//?} else {
        // Loader_Recipes_Handlers:552-625 passes F to both conversion classes.
        recipes.add(new gregtech.content.recipe.ToolShapelessRecipe(
                new ShapelessRecipe(id, "gt.form_conversion", CraftingBookCategory.MISC, result, ingredients), false, FormConversionPlacement.orderedSelector(conversion),
                        conversion.input(), material.resolve().getName(), false, FormConversionPlacement.FIXED));
        REGISTERED.add(id.toString());
//?}
    }

    private static Ingredient formIngredient(ItemStack form) { return CraftingMaterialForms.ingredient(form); }

    private static ItemStack item(String prefixName, GTMaterial material, int count) {
        MaterialPrefix prefix = PrefixRegistry.byName(prefixName);
        if (prefix != null) {
            var stack = GTItems.getStack(prefix, material, count);
            if (!stack.isEmpty()) return stack;
        }
        BlockMaterialPrefix block = BlockPrefixRegistry.byName(prefixName);
        if (block != null) {
            var stack = GTBlocks.getStack(block, material);
            if (!stack.isEmpty()) { stack.setCount(count); return stack; }
        }
        return CraftingMaterialForms.stack(prefixName, material, count);
    }

    private static String sanitize(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            builder.append(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' ? c : '_');
        }
        return builder.toString();
    }

}
