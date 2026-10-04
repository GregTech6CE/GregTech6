package com.gregtech.gregtech.loaders;

import java.util.Map;
import com.gregtech.gregtech.data.OriginalCraftingJson;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.content.recipe.FormConversionSelector;
import com.gregtech.gregtech.content.recipe.OriginalFormConversions;
import com.gregtech.gregtech.content.recipe.OriginalFormConversions.Conversion;
import com.gregtech.gregtech.recipe.CraftingMaterialForms;
import com.gregtech.gregtech.api.material.MaterialUnification;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.api.prefix.BlockPrefixRegistry;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTItems;
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
 * output by leading empty grid cells, in source prefix registration order; missing material outputs
 * still reserve their source offset. XToY requires exactly the same material in every occupied cell.
 */
public final class Loader_FormConversionCraftingRecipes {
    private static final List<Conversion> ONE_TO_MANY = OriginalFormConversions.FIXED.stream().filter(Conversion::single).toList();
    private static final List<Conversion> MANY_TO_MANY = OriginalFormConversions.FIXED.stream().filter(c -> !c.single()).toList();

    private static final List<String> REGISTERED = new ArrayList<>();
    /** Source constructor replacement rules also guard plain recipes added after the reload lifecycle. */
    public static boolean disallowsPlainRecipe(net.minecraft.world.item.crafting.CraftingRecipe recipe,
                                               net.minecraft.core.RegistryAccess access) {
        return CraftingMaterialForms.replaces(recipe, access);
    }

    private Loader_FormConversionCraftingRecipes() {}

    /** Recipe ids registered by the last server start, for diagnostics and tests. */
    public static List<String> registeredIds() { return List.copyOf(REGISTERED); }

    public static List<String> skipped() {
        return CraftingMaterialForms.contains("rawOreChunk") ? List.of() : List.of(
                "rawOreChunk (:553/:576): no external tagged item; GT6 registers no item for this Harder Ores prefix");
    }

    public static void add(Map<ResourceLocation, byte[]> recipes) {
        int before = recipes.size();
        REGISTERED.clear();
        CraftingMaterialForms.rebuild();

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
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Native material grid recipes generated={} wire-size={} pipe-size={}", recipes.size()-before, wireSizes, pipeSizes);
        com.mojang.logging.LogUtils.getLogger().info("Registered {} crafting-grid material form conversions from GT6"
                + " AdvancedCraftingXToY/1ToY ({} GT6 pairs skipped: {})",
                REGISTERED.size(), skipped().size(), skipped());
    }

    public static void replacePlain(RecipeManager manager, net.minecraft.core.RegistryAccess access) {
        CraftingMaterialForms.rebuild(true);
        var recipes = new ArrayList<>(manager.getRecipes());
        int before = recipes.size();
        recipes.removeIf(holder -> holder.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe crafting
                && disallowsPlainRecipe(crafting, access));
        int removed = before - recipes.size();
        // Rebind existing generated rows too: their JSON ingredient aliases were indexed before
        // this reload's tags existed. Only this loader's own form-conversion rows are rebuilt.
        int beforeRebind = recipes.size();
        recipes.removeIf(holder -> holder.id().getNamespace().equals("gregtech")
                && holder.id().getPath().startsWith("form_conversion/")
                && holder.value() instanceof com.gregtech.gregtech.recipe.ToolShapelessRecipe);
        int rebound = beforeRebind - recipes.size();
        REGISTERED.removeIf(id -> id.startsWith("gregtech:form_conversion/"));
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
        var recipe = new com.gregtech.gregtech.recipe.ToolShapelessRecipe(base, false,
                OriginalFormConversions.selector(conversion), conversion.input(), material.resolve().getName());
        recipes.add(new net.minecraft.world.item.crafting.RecipeHolder<>(id, recipe));
        ids.add(id); REGISTERED.add(id.toString()); return 1;
    }

    /** Source :615-616: all standard/dense prefix aliases, including actual vanilla ore blocks. */
    private static void registerOreConversions(Map<ResourceLocation, byte[]> recipes) {
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
    private static int registerWireSizes(Map<ResourceLocation, byte[]> recipes) {
        java.util.Map<String, java.util.Map<Integer, net.minecraft.world.item.Item>> families =
                new java.util.LinkedHashMap<>();
        for (var entry : com.gregtech.gregtech.registry.GTWires.allWires()) {
            if (!entry.isBound()) continue;
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
    private static int registerPipeSizes(Map<ResourceLocation, byte[]> recipes) {
        java.util.Map<String, java.util.Map<com.gregtech.gregtech.api.machine.PipeSpec.PipeSize,
                net.minecraft.world.item.Item>> families = new java.util.LinkedHashMap<>();
        for (var holder : com.gregtech.gregtech.content.transport.fluid.FluidTransportRegistries.pipes()) {
            if (!holder.isBound()) continue;
            var spec = holder.get().spec();
            families.computeIfAbsent(spec.material().getName(), ignored -> new java.util.EnumMap<>(
                    com.gregtech.gregtech.api.machine.PipeSpec.PipeSize.class))
                    .put(spec.size(), holder.get().asItem());
        }
        int added = 0;
        for (var family : families.entrySet()) {
            added += pipe(recipes, family, com.gregtech.gregtech.api.machine.PipeSpec.PipeSize.QUADRUPLE,
                    com.gregtech.gregtech.api.machine.PipeSpec.PipeSize.MEDIUM, 4);
            added += pipe(recipes, family, com.gregtech.gregtech.api.machine.PipeSpec.PipeSize.NONUPLE,
                    com.gregtech.gregtech.api.machine.PipeSpec.PipeSize.SMALL, 9);
        }
        return added;
    }

    private static int pipe(Map<ResourceLocation, byte[]> recipes,
                            java.util.Map.Entry<String, java.util.Map<
                                    com.gregtech.gregtech.api.machine.PipeSpec.PipeSize,
                                    net.minecraft.world.item.Item>> family,
                            com.gregtech.gregtech.api.machine.PipeSpec.PipeSize from,
                            com.gregtech.gregtech.api.machine.PipeSpec.PipeSize to, int count) {
        var big = family.getValue().get(from);
        var small = family.getValue().get(to);
        if (big == null || small == null) return 0;
        return recipe(recipes, new ItemStack(big), new ItemStack(small, count),
                "pipe_sizes/" + sanitize(family.getKey()) + "_" + from.name().toLowerCase() + "_to_" + count
                        + "x_" + to.name().toLowerCase(), true);
    }

    private static int recipe(Map<ResourceLocation, byte[]> recipes, ItemStack input, ItemStack output, String path) {
        return recipe(recipes, input, output, path, false);
    }

    private static int recipe(Map<ResourceLocation, byte[]> recipes, ItemStack input, ItemStack output, String path, boolean autocraftable) {
        return recipe(recipes, input, output, path, autocraftable, FormConversionSelector.NONE);
    }

    private static int recipe(Map<ResourceLocation, byte[]> recipes, ItemStack input, ItemStack output, String path,
                              boolean autocraftable, FormConversionSelector selector) {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(input.getCount(),
                autocraftable ? Ingredient.of(input) : CraftingMaterialForms.ingredient(input));
        var inputForm = autocraftable ? null : CraftingMaterialForms.form(input);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("gregtech", path);
        if (!autocraftable && inputForm != null)
            OriginalCraftingJson.formConversion(recipes, id, "gt.wire_sizes", CraftingBookCategory.MISC,
                    CraftingMaterialForms.canonical(output.copy()), ingredients, selector, inputForm.prefix(), inputForm.material());
        else OriginalCraftingJson.shapeless(recipes, id, "gt.wire_sizes", CraftingBookCategory.MISC,
                    CraftingMaterialForms.canonical(output.copy()), ingredients, autocraftable);
        REGISTERED.add(id.toString());
        return 1;
    }

    private static void register(Map<ResourceLocation, byte[]> recipes, Conversion conversion, GTMaterial material,
                                 ItemStack input, ItemStack output) {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(conversion.inputCount(),
                formIngredient(input));
        ItemStack result = CraftingMaterialForms.canonical(output.copy());
        ResourceLocation id = conversionId(conversion, material);
        OriginalCraftingJson.formConversion(recipes, id, "gt.form_conversion", CraftingBookCategory.MISC, result, ingredients, OriginalFormConversions.selector(conversion),
                conversion.input(), material.resolve().getName());
        REGISTERED.add(id.toString());
    }

    private static ResourceLocation conversionId(Conversion conversion, GTMaterial material) {
        return ResourceLocation.fromNamespaceAndPath("gregtech",
                "form_conversion/" + sanitize(conversion.input()) + "_to_" + sanitize(conversion.output()) + "/"
                        + sanitize(material.getName()) + "_" + conversion.inputCount());
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
