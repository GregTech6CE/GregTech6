package com.gregtech.gregtech.loaders;

import java.util.Map;
import com.gregtech.gregtech.data.OriginalCraftingJson;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
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
 * GT6's hand-crafting material form conversions: 9 ingots &harr; an ingot block, 9 dusts &harr; a dust
 * block, 9 tiny piles &rarr; dust, 4 small piles &rarr; dust, 9 nuggets &rarr; ingot, plates &harr; plate
 * blocks and the nugget/billet/chunk ratios.
 * <p>
 * GT6 registers these in {@code Loader_Recipes_Handlers:552-625} through
 * {@code GameRegistry.addRecipe(new AdvancedCraftingXToY(...))} / {@code AdvancedCrafting1ToY(...)}.
 * Those two classes are dynamic recipe objects: they match <em>any</em> material's item of the input
 * prefix and craft the same material's output prefix, which is why GT6 does not need one recipe per
 * material. The port has no ore-dictionary recipe layer, so the same table is materialised here as one
 * shapeless recipe per material and form — the behaviour on the crafting grid is the same, the count
 * is per-material.
 * <p>
 * GT6's matcher ({@code AdvancedCraftingXToY#matches:206-231}) wants the grid to contain exactly
 * {@code mInputCount} matching items and every other slot empty, with the items packed towards the
 * first slot; a vanilla shapeless recipe asks for exactly the same item count everywhere in the grid.
 * The one deliberate deviation: where GT6 lists several outputs for one input count (for example
 * {@code dust 1 -> dustTiny 9} at line 563 and {@code dust 1 -> dustSmall 4} at line 564), only the
 * first variant is registered, because a vanilla recipe list has no defined order and a player would
 * otherwise get a random one of the two. Those alternates are listed in {@link #skipped()}.
 * <p>
 * Native recipes are emitted into the existing reloadable pack through OriginalCraftingJson;
 * the original first-entry precedence is retained without a second RecipeManager mutation pipeline.
 */
public final class Loader_FormConversionCraftingRecipes {
    /** GT6 prefix pairs, in GT6 registration order, as (input, inputCount, output, outputCount). */
    private static final List<Conversion> ONE_TO_MANY = List.of(
            new Conversion("oreRaw", 1, "gem", 1),                                  // Loader_Recipes_Handlers:552
            new Conversion("crushed", 1, "crushedTiny", 9),                         // :554
            new Conversion("crushed", 1, "gemFlawed", 1),                           // :555
            new Conversion("crushedPurified", 1, "crushedPurifiedTiny", 9),         // :556
            new Conversion("crushedPurified", 1, "gemFlawed", 1),                   // :557
            new Conversion("crushedCentrifuged", 1, "crushedCentrifugedTiny", 9),   // :558
            new Conversion("crushedCentrifuged", 1, "gemFlawed", 1),                // :559
            new Conversion("ingot", 1, "nugget", 9),                                // :560
            new Conversion("billet", 1, "nugget", 6),                               // :562
            new Conversion("dust", 1, "dustTiny", 9),                               // :563
            new Conversion("dustTiny", 1, "dustDiv72", 8),                          // :565
            new Conversion("dustSmall", 1, "dustDiv72", 18),                        // :566
            new Conversion("blockRaw", 1, "oreRaw", 9),                             // :567
            new Conversion("blockDust", 1, "dust", 9),                              // :568
            new Conversion("blockIngot", 1, "ingot", 9),                            // :570
            new Conversion("blockGem", 1, "gem", 9),                                // :572
            new Conversion("blockPlate", 1, "plate", 9),                            // :573
            new Conversion("blockPlateGem", 1, "plateGem", 9));                     // :574

    private static final List<Conversion> MANY_TO_MANY = List.of(
            new Conversion("crushedTiny", 9, "crushed", 1),                         // :577
            new Conversion("crushedPurifiedTiny", 9, "crushedPurified", 1),         // :578
            new Conversion("crushedCentrifugedTiny", 9, "crushedCentrifuged", 1),   // :579
            new Conversion("ingot", 2, "billet", 3),                                // :580
            new Conversion("ingot", 3, "nugget", 27),                               // :581
            new Conversion("ingot", 4, "billet", 6),                                // :582
            new Conversion("ingot", 5, "nugget", 45),                               // :583
            new Conversion("ingot", 6, "billet", 9),                                // :584
            new Conversion("ingot", 7, "nugget", 63),                               // :585
            new Conversion("ingot", 8, "billet", 12),                               // :586
            new Conversion("ingot", 9, "blockIngot", 1),                            // :587
            new Conversion("billet", 2, "nugget", 12),                              // :588
            new Conversion("billet", 3, "ingot", 2),                                // :589
            new Conversion("billet", 4, "nugget", 24),                              // :590
            new Conversion("billet", 5, "nugget", 30),                              // :591
            new Conversion("billet", 6, "ingot", 4),                                // :592
            new Conversion("billet", 7, "nugget", 42),                              // :593
            new Conversion("billet", 8, "nugget", 48),                              // :594
            new Conversion("billet", 9, "ingot", 6),                                // :595
            new Conversion("chunkGt", 4, "ingot", 1),                               // :596
            new Conversion("chunkGt", 8, "billet", 3),                              // :597
            new Conversion("nugget", 6, "billet", 1),                               // :598
            new Conversion("nugget", 9, "ingot", 1),                                // :599
            new Conversion("dustDiv72", 8, "dustTiny", 1),                          // :600
            new Conversion("dustTiny", 9, "dust", 1),                               // :601
            new Conversion("dustSmall", 4, "dust", 1),                              // :602
            new Conversion("dustSmall", 8, "dust", 2),                              // :603
            new Conversion("oreRaw", 9, "blockRaw", 1),                             // :604
            new Conversion("dust", 9, "blockDust", 1),                              // :605
            new Conversion("gem", 9, "blockGem", 1),                                // :606
            new Conversion("plate", 9, "blockPlate", 1),                            // :607
            new Conversion("plateGem", 9, "blockPlateGem", 1));                     // :608

    /** GT6 pairs that would compete for the same input count, plus pairs needing missing prefixes. */
    private static final List<String> SKIPPED = List.of(
            "oreRaw 1 -> rawOreChunk 3 (:553): no rawOreChunk prefix",
            "ingot 1 -> chunkGt 4 (:561): competes with ingot 1 -> nugget 9 (:560)",
            "dust 1 -> dustSmall 4 (:564): competes with dust 1 -> dustTiny 9 (:563)",
            "blockDust 1 -> dustSmall 36 (:569): competes with blockDust 1 -> dust 9 (:568)",
            "blockIngot 1 -> chunkGt 36 (:571): competes with blockIngot 1 -> ingot 9 (:570)",
            "plateTiny 5/9 -> casingSmall 1/2 (:609-612): no casingSmall prefix",
            "wire size conversions (:624-625): registered per wire size instead, see registerWireSizes()");

    private static final List<String> REGISTERED = new ArrayList<>();
    private static volatile com.gregtech.gregtech.content.recipe.CraftingConversionPermissions<net.minecraft.world.item.Item> nativePermissions =
            new com.gregtech.gregtech.content.recipe.CraftingConversionPermissions.Builder<net.minecraft.world.item.Item>().build();
    private static com.gregtech.gregtech.content.recipe.CraftingConversionPermissions.Builder<net.minecraft.world.item.Item> pendingPermissions;

    /** Original constructors replace plain recipes for the same material/form and occupied-cell count. */
    public static boolean disallowsPlainPlan(ItemStack[] pattern) {
        var cells = new ArrayList<net.minecraft.world.item.Item>();
        for (var cell : pattern) if (!cell.isEmpty()) cells.add(cell.getItem());
        return nativePermissions.disallows(cells);
    }
    private static void captureInput(ItemStack input, int cells) {
        pendingPermissions.add(input.getItem(), MaterialUnification.canonical(input.copy()).getItem(), cells);
    }


    private Loader_FormConversionCraftingRecipes() {}

    /** Recipe ids registered by the last server start, for diagnostics and tests. */
    public static List<String> registeredIds() { return List.copyOf(REGISTERED); }

    public static List<String> skipped() { return SKIPPED; }

    public static void add(Map<ResourceLocation, byte[]> recipes) {
        int before = recipes.size();
        REGISTERED.clear();
        pendingPermissions = new com.gregtech.gregtech.content.recipe.CraftingConversionPermissions.Builder<>();

        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.has(MaterialProperty.ANTIMATTER)) continue; // GT6 gates these on ANTIMATTER.NOT
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

        registerDenseOreConversions(recipes);

        int wireSizes = registerWireSizes(recipes);
        int pipeSizes = registerPipeSizes(recipes);
        nativePermissions = pendingPermissions.build();
        pendingPermissions = null;
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Native material grid recipes generated={} wire-size={} pipe-size={}", recipes.size()-before, wireSizes, pipeSizes);
        com.mojang.logging.LogUtils.getLogger().info("Registered {} crafting-grid material form conversions from GT6"
                + " AdvancedCraftingXToY/1ToY ({} GT6 pairs skipped: {})",
                REGISTERED.size(), SKIPPED.size(), SKIPPED);
    }

    /** GT6 Loader_Recipes_Handlers:616, {@code oreDense 1 -> oreRaw 2} for both ore block families. */
    private static void registerDenseOreConversions(Map<ResourceLocation, byte[]> recipes) {
        Conversion conversion = new Conversion("oreDense", 1, "oreRaw", 2);
        for (var entry : com.gregtech.gregtech.registry.GTSpecialOreBlocks.all()) {
            if (!entry.isBound() || !(entry.get() instanceof com.gregtech.gregtech.block.DenseOreBlock denseOre))
                continue;
            var block = entry.get();
            GTMaterial material = denseOre.material();
            if (material == null || !material.isValid()) continue;
            ItemStack output = GTItems.getStack(MaterialPrefix.oreRaw, material.resolve(), 2);
            if (output.isEmpty()) continue;
            register(recipes, conversion, material.resolve(), new ItemStack(block), output);
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
                for (int small = 1; small < big; small++) {
                    var smallItem = sizes.get(small);
                    if (smallItem == null || big % small != 0) continue;
                    int amount = big / small;
                    // The source guards only XToY; the reverse 1ToY also allows 10..16 outputs.
                    if (amount < 10) added += recipe(recipes, new ItemStack(smallItem, amount), new ItemStack(bigItem),
                            "wire_sizes/" + sanitize(family.getKey()) + "_" + small + "_to_" + big);
                    added += recipe(recipes, new ItemStack(bigItem), new ItemStack(smallItem, amount),
                            "wire_sizes/" + sanitize(family.getKey()) + "_" + big + "_to_" + small);
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
        NonNullList<Ingredient> ingredients = NonNullList.withSize(input.getCount(), Ingredient.of(input));
        if (!autocraftable) captureInput(input, input.getCount());
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("gregtech", path);
        OriginalCraftingJson.shapeless(recipes, id, "gt.wire_sizes", CraftingBookCategory.MISC, MaterialUnification.canonical(output.copy()), ingredients, autocraftable);
        REGISTERED.add(id.toString());
        return 1;
    }

    private static void register(Map<ResourceLocation, byte[]> recipes, Conversion conversion, GTMaterial material,
                                 ItemStack input, ItemStack output) {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(conversion.inputCount(),
                formIngredient(input));
        ItemStack result = MaterialUnification.canonical(output.copy());
        captureInput(input, conversion.inputCount());
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("gregtech",
                "form_conversion/" + sanitize(conversion.input()) + "_to_" + sanitize(conversion.output()) + "/"
                        + sanitize(material.getName()) + "_" + conversion.inputCount());
        OriginalCraftingJson.shapeless(recipes, id, "gt.form_conversion", CraftingBookCategory.MISC, result, ingredients, false);
        REGISTERED.add(id.toString());
    }

    /**
     * The ingredient is the GT item only — deliberately, not the unified vanilla item as well.
     * <p>
     * GT6 identified forms through the ore dictionary, so its {@code AdvancedCraftingXToY} matched any
     * copper ingot. The port instead has two items for the unified forms (the GT material item and the
     * vanilla one its recipe outputs are rewritten onto by {@link MaterialUnification#canonical}), and
     * vanilla ships its own 9 ingots &harr; block recipes for iron, gold, copper and the unified gems.
     * Accepting both items here would make those two recipe sets compete for the same grid with no
     * defined winner. Matching only the GT item keeps vanilla's own recipes in charge of the vanilla
     * items and leaves every other material to this table.
     */
    private static Ingredient formIngredient(ItemStack form) {
        return Ingredient.of(form);
    }

    private static ItemStack item(String prefixName, GTMaterial material, int count) {
        MaterialPrefix prefix = PrefixRegistry.byName(prefixName);
        if (prefix != null) return GTItems.getStack(prefix, material, count);
        BlockMaterialPrefix block = BlockPrefixRegistry.byName(prefixName);
        if (block == null) return ItemStack.EMPTY;
        ItemStack stack = GTBlocks.getStack(block, material);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        stack.setCount(count);
        return stack;
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

    private record Conversion(String input, int inputCount, String output, int outputCount) {}
}
