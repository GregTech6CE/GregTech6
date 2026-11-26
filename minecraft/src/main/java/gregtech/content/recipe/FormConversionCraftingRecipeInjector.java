package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.recipe.OriginalFormConversions.Conversion;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialForm;
import gregtech.engine.material.ItemMaterialForms;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.prefix.BlockPrefixRegistry;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.platform.Mc;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

/**
 * Loader_Recipes_Handlers material-form conversions. Single-input variants follow the
 * source's AdvancedCrafting1ToY rule: the empty slots before the item select the output by modulo,
 * in source prefix registration order (dust selects tiny before small). Missing outputs still
 * reserve their source offset. XToY requires exactly the same material in every occupied cell.
 */
public final class FormConversionCraftingRecipeInjector {
    private static final List<String> REGISTERED = new ArrayList<>();
    /** Source constructor replacement rules also guard plain recipes added after the reload lifecycle. */
    public static boolean disallowsPlainRecipe(
            ResourceLocation id,
            net.minecraft.world.item.crafting.CraftingRecipe recipe,
            net.minecraft.core.RegistryAccess access) {
        return CraftingMaterialForms.replaces(
                recipe,
                access,
                id != null && id.getNamespace().equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE));
    }

    public static boolean disallowsPlainRecipe(
            net.minecraft.world.item.crafting.CraftingRecipe recipe, net.minecraft.core.RegistryAccess access) {
        return CraftingMaterialForms.replaces(recipe, access);
    }

    private FormConversionCraftingRecipeInjector() {}

    /** Recipe ids registered by the last server start, for diagnostics and tests. */
    public static synchronized List<String> registeredIds() {
        return List.copyOf(REGISTERED);
    }

    static synchronized Set<String> snapshotRegistered() {
        return new HashSet<>(REGISTERED);
    }

    static synchronized void removeRegisteredWithPrefix(String prefix) {
        REGISTERED.removeIf(id -> id.startsWith(prefix));
    }

    static synchronized void recordRegistered(String id) {
        REGISTERED.add(id);
    }

    public static List<String> skipped() {
        return ItemMaterialForms.contains("rawOreChunk")
                ? List.of()
                : List.of(
                        "rawOreChunk (:553/:576): no external tagged item; GT6 registers no item for this Harder Ores prefix");
    }

    /**
     * Removes source rows that duplicate a generated row with the same result and first ingredient
     * of every slot. Only the known source families are considered; unrelated recipes stay untouched.
     */
    static int dedupGenerated(
            List<?> recipes,
            net.minecraft.core.RegistryAccess access,
            java.util.function.Function<Object, ResourceLocation> idOf,
            java.util.function.Function<Object, Recipe<?>> valueOf) {
        var targets = new java.util.HashSet<net.minecraft.world.item.Item>();
        for (var entry : recipes) {
            if (!isDedupSource(idOf.apply(entry))) continue;
            if (!(valueOf.apply(entry) instanceof net.minecraft.world.item.crafting.CraftingRecipe crafting)) continue;
            var result = crafting.getResultItem(access);
            if (!result.isEmpty()) targets.add(result.getItem());
        }
        if (targets.isEmpty()) return 0;
        var groups = new java.util.HashMap<String, java.util.List<Object>>();
        for (var entry : recipes) {
            var id = idOf.apply(entry);
            if (!isDedupSource(id) && !isCanonicalSource(id)) continue;
            if (!(valueOf.apply(entry) instanceof net.minecraft.world.item.crafting.CraftingRecipe crafting)) continue;
            var result = crafting.getResultItem(access);
            if (result.isEmpty() || !targets.contains(result.getItem())) continue;
            var key = new StringBuilder();
            key.append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem()))
                    .append('x')
                    .append(result.getCount())
                    .append('|');
            if (crafting instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped)
                key.append("shape:")
                        .append(shaped.getWidth())
                        .append('x')
                        .append(shaped.getHeight())
                        .append('|');
            for (var ingredient : crafting.getIngredients()) {
                var items = ingredient.getItems();
                key.append(
                        items.length == 0
                                ? "[]"
                                : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(items[0].getItem()));
                key.append('/');
            }
            groups.computeIfAbsent(key.toString(), unused -> new ArrayList<>()).add(entry);
        }
        var remove = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object, Boolean>());
        for (var group : groups.values()) {
            if (group.size() < 2) continue;
            if (group.stream().noneMatch(entry -> isCanonicalSource(idOf.apply(entry)))) continue;
            for (var entry : group) if (isDedupSource(idOf.apply(entry))) remove.add(entry);
        }
        if (remove.isEmpty()) return 0;
        recipes.removeIf(remove::contains);
        return remove.size();
    }

    private static boolean isDedupSource(ResourceLocation id) {
        if (!id.getNamespace().equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE)) return false;
        String path = id.getPath();
        return path.startsWith("axial/")
                || path.startsWith("hand/pipe/")
                || path.startsWith("hand_material/rocks/")
                || path.startsWith("machines/basic/massfab_")
                || path.startsWith("machines/multiblock/");
    }

    private static boolean isCanonicalSource(ResourceLocation id) {
        if (!id.getNamespace().equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE)) return false;
        String path = id.getPath();
        return path.startsWith("form_conversion/")
                || path.startsWith("wire_sizes/")
                || path.startsWith("pipe_sizes/")
                || path.startsWith("hand_material/shapeless/cable/")
                || path.startsWith("stone/")
                || path.startsWith("matter/")
                || path.startsWith("fusion/");
    }

    public static synchronized void add(GeneratedCraftingSink sink, Map<ItemMaterialForm, List<Item>> aliases) {
        REGISTERED.clear();

        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (!material.isValid()) continue; // Both source crafting constructors default to MT.NULL.NOT.
            // FIXED already lists single-input rows before many-input rows (source order).
            for (Conversion conversion : OriginalFormConversions.FIXED) {
                ItemStack in = item(aliases, conversion.input(), material, 1);
                ItemStack out = item(aliases, conversion.output(), material, conversion.outputCount());
                if (in.isEmpty() || out.isEmpty()) continue;
                register(sink, aliases, conversion, material, in, out);
            }
        }

        registerOreConversions(sink, aliases);

        int wireSizes = registerWireSizes(sink, aliases);
        int pipeSizes = registerPipeSizes(sink);
        if (!REGISTERED.isEmpty() || wireSizes > 0 || pipeSizes > 0) sink.commit();
        if (sink.isDatapack()) {
            Mc.logger()
                    .info(
                            "[gregtech] Native material grid recipes generated={} wire-size={} pipe-size={}",
                            sink.writtenCount(),
                            wireSizes,
                            pipeSizes);
        }
        Mc.logger()
                .info(
                        "Registered {} crafting-grid material form conversions from GT6"
                                + " AdvancedCraftingXToY/1ToY ({} GT6 pairs skipped: {})",
                        REGISTERED.size(),
                        skipped().size(),
                        skipped());
    }

    /** All standard/dense prefix aliases, including actual vanilla ore blocks ({@code Loader_Recipes_Handlers}). */
    private static void registerOreConversions(GeneratedCraftingSink sink, Map<ItemMaterialForm, List<Item>> aliases) {
        for (var entry : aliases.entrySet()) {
            var form = entry.getKey();
            int count = ItemMaterialForm.STANDARD_ORES.contains(form.prefix())
                    ? 1
                    : ItemMaterialForm.DENSE_ORES.contains(form.prefix()) ? 2 : 0;
            if (count == 0 || entry.getValue().isEmpty()) continue;
            var material = GTMaterialRegistry.get(form.material()).resolve();
            var output = GTItems.getStack(MaterialPrefix.oreRaw, material, count);
            if (output.isEmpty()) continue;
            register(
                    sink,
                    aliases,
                    new Conversion(form.prefix(), 1, "oreRaw", count, true),
                    material,
                    new ItemStack(entry.getValue().get(0)),
                    output);
        }
    }

    /**
     * GT6 {@code Loader_Recipes_Handlers}: for every size pair {@code big % small == 0} with
     * divisible sizes, the crafting grid combines fewer than ten small wires into one big wire
     * and splits one big wire into all {@code tAmount} small ones (the machine
     * variants run on the Loom and the Unboxinator). The port's wires are blocks, not material-prefix
     * items, so these rows are built from the wire registry instead of the prefix tables.
     */
    private static int registerWireSizes(GeneratedCraftingSink sink, Map<ItemMaterialForm, List<Item>> aliases) {
        java.util.Map<String, java.util.Map<Integer, net.minecraft.world.item.Item>> families =
                new java.util.LinkedHashMap<>();
        for (var entry : gregtech.content.registry.GTWires.allWires()) {
            if (!entry.isPresent()) continue;
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
                    if (amount < 10)
                        added += recipe(
                                sink,
                                aliases,
                                new ItemStack(smallItem, amount),
                                new ItemStack(bigItem),
                                "wire_sizes/" + sanitize(family.getKey()) + "_" + small + "_to_" + big,
                                false,
                                FormConversionSelector.NONE);
                    added += recipe(
                            sink,
                            aliases,
                            new ItemStack(bigItem),
                            new ItemStack(smallItem, amount),
                            "wire_sizes/" + sanitize(family.getKey()) + "_" + big + "_to_" + small,
                            false,
                            selector);
                }
            }
        }
        return added;
    }

    /**
     * GT6 {@code MultiTileEntityPipeFluid}: the crafting grid takes a quadruple fluid pipe
     * apart into four medium ones and a nonuple one into nine small ones (the build-up direction is the
     * Boxinator's job, {@code Loader_Recipes_Handlers}).
     */
    private static int registerPipeSizes(GeneratedCraftingSink sink) {
        java.util.Map<String, java.util.Map<gregtech.engine.machine.PipeSpec.PipeSize, net.minecraft.world.item.Item>>
                families = new java.util.LinkedHashMap<>();
        for (var holder : gregtech.content.registry.GTFluidPipes.all()) {
            if (!holder.isBound()) continue;
            var spec = holder.get().spec();
            families.computeIfAbsent(
                            spec.material().getName(),
                            ignored -> new java.util.EnumMap<>(gregtech.engine.machine.PipeSpec.PipeSize.class))
                    .put(spec.size(), holder.get().asItem());
        }
        int added = 0;
        for (var family : families.entrySet()) {
            added += pipe(
                    sink,
                    family,
                    gregtech.engine.machine.PipeSpec.PipeSize.QUADRUPLE,
                    gregtech.engine.machine.PipeSpec.PipeSize.MEDIUM,
                    4);
            added += pipe(
                    sink,
                    family,
                    gregtech.engine.machine.PipeSpec.PipeSize.NONUPLE,
                    gregtech.engine.machine.PipeSpec.PipeSize.SMALL,
                    9);
        }
        return added;
    }

    private static int pipe(
            GeneratedCraftingSink sink,
            java.util.Map.Entry<
                            String,
                            java.util.Map<gregtech.engine.machine.PipeSpec.PipeSize, net.minecraft.world.item.Item>>
                    family,
            gregtech.engine.machine.PipeSpec.PipeSize from,
            gregtech.engine.machine.PipeSpec.PipeSize to,
            int count) {
        var big = family.getValue().get(from);
        var small = family.getValue().get(to);
        if (big == null || small == null) return 0;
        return recipe(
                sink,
                Map.of(),
                new ItemStack(big),
                new ItemStack(small, count),
                "pipe_sizes/" + sanitize(family.getKey()) + "_" + from.name().toLowerCase() + "_to_" + count + "x_"
                        + to.name().toLowerCase(),
                true,
                FormConversionSelector.NONE);
    }

    private static int recipe(
            GeneratedCraftingSink sink,
            Map<ItemMaterialForm, List<Item>> aliases,
            ItemStack input,
            ItemStack output,
            String path,
            boolean autocraftable,
            FormConversionSelector selector) {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(
                input.getCount(),
                autocraftable ? Ingredient.of(input) : CraftingMaterialForms.ingredient(input, aliases));
        var inputForm = autocraftable ? null : ItemMaterialForms.form(input);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, path);
        ItemStack result = ItemMaterialForms.canonical(output.copy());
        if (!autocraftable && inputForm != null) {
            sink.orderedFormConversion(
                    id,
                    "gt.wire_sizes",
                    CraftingBookCategory.MISC,
                    result,
                    ingredients,
                    selector,
                    inputForm.prefix(),
                    inputForm.material());
        } else {
            sink.toolShapeless(
                    id, "gt.wire_sizes", CraftingBookCategory.MISC, result, ingredients, autocraftable, selector);
        }
        REGISTERED.add(id.toString());
        return 1;
    }

    private static void register(
            GeneratedCraftingSink sink,
            Map<ItemMaterialForm, List<Item>> aliases,
            Conversion conversion,
            GTMaterial material,
            ItemStack input,
            ItemStack output) {
        NonNullList<Ingredient> ingredients =
                NonNullList.withSize(conversion.inputCount(), CraftingMaterialForms.ingredient(input, aliases));
        ItemStack result = ItemMaterialForms.canonical(output.copy());
        ResourceLocation id = conversionId(conversion, material);
        // Loader_Recipes_Handlers passes F to both conversion classes.
        sink.orderedFormConversion(
                id,
                "gt.form_conversion",
                CraftingBookCategory.MISC,
                result,
                ingredients,
                OriginalFormConversions.selector(conversion),
                conversion.input(),
                material.resolve().getName());
        REGISTERED.add(id.toString());
    }

    static ResourceLocation conversionId(Conversion conversion, GTMaterial material) {
        return ResourceLocation.fromNamespaceAndPath(
                GregTechIdentity.REGISTRY_NAMESPACE,
                "form_conversion/" + sanitize(conversion.input()) + "_to_" + sanitize(conversion.output()) + "/"
                        + sanitize(material.getName()) + "_" + conversion.inputCount());
    }

    static ItemStack item(
            Map<ItemMaterialForm, List<Item>> aliases, String prefixName, GTMaterial material, int count) {
        MaterialPrefix prefix = PrefixRegistry.byName(prefixName);
        if (prefix != null) {
            var stack = GTItems.getStack(prefix, material, count);
            if (!stack.isEmpty()) return stack;
        }
        BlockMaterialPrefix block = BlockPrefixRegistry.byName(prefixName);
        if (block != null) {
            var stack = GTBlocks.getStack(block, material);
            if (!stack.isEmpty()) {
                stack.setCount(count);
                return stack;
            }
        }
        return ItemMaterialForms.stack(aliases, prefixName, material, count);
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
