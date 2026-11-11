package gregtech.content.recipe;

import com.google.gson.*;
import gregtech.api.material.*;
import gregtech.engine.material.*;
import gregtech.engine.prefix.*;
import gregtech.content.block.*;
import gregtech.content.block.energy.ElectricWireBlock;
import gregtech.content.recipe.OriginalFormConversions;
import gregtech.content.recipe.OriginalFormConversions.Form;
import gregtech.content.data.MaterialPrefix;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.*;
import net.minecraft.core.RegistryAccess;
import java.util.*;

/** Native material/form identity for the original hand conversions; generic material tags are insufficient. */
public final class CraftingMaterialForms {
    private CraftingMaterialForms() {}
    private static Map<Form, List<Item>> aliases = Map.of();
    private static Map<String, String> materialNames = Map.of();
    private static Map<String, String> prefixNames = Map.of();
    private static Map<Item, Form> vanilla = Map.of();
    private static Map<Item, List<Form>> extraForms = Map.of();
    private static Map<Form, Item> preferred = Map.of();

    public static void rebuild() { rebuild(false); }
    /** Tag-backed aliases are indexed only after the server has bound this reload's tags. */
    public static synchronized void rebuild(boolean taggedItems) {
        PLAIN_FORMS.clear();
        var materials = new HashMap<String, String>();
        for (var material : GTMaterialRegistry.allMaterials()) if (material.isValid()) {
            String name = material.resolve().getName();
            String canonical = MaterialEquivalence.materialName(material);
            materials.put(canonical, name);
            materials.put(material.getName().toLowerCase(Locale.ROOT), name);
            String alternate = gregtech.engine.material.MaterialTagAliases.ALTERNATE_SPELLINGS.get(canonical);
            if (alternate != null) materials.put(alternate, name);
        }
        materialNames = Map.copyOf(materials);
        var prefixes = new HashMap<String, String>();
        for (var conversion : OriginalFormConversions.FIXED) {
            prefixes.put(MaterialPrefix.camelToSnake(conversion.input()), conversion.input());
            prefixes.put(MaterialPrefix.camelToSnake(conversion.output()), conversion.output());
        }
        for (String prefix : OriginalFormConversions.STANDARD_ORES) prefixes.put(MaterialPrefix.camelToSnake(prefix), prefix);
        for (String prefix : OriginalFormConversions.DENSE_ORES) prefixes.put(MaterialPrefix.camelToSnake(prefix), prefix);
        // Both the source spelling and the port's identical renamed form tags are accepted.
        for (var prefix : PrefixRegistry.all()) prefixes.put(MaterialPrefix.camelToSnake(prefix.getName()),
                PrefixRegistry.sourceName(prefix.getName()));
        for (int size = 1; size <= 16; size++) prefixes.put(String.format(Locale.ROOT, "wire_%02d", size),
                String.format(Locale.ROOT, "wireGt%02d", size));
        prefixNames = Map.copyOf(prefixes);
        var forms = new IdentityHashMap<Item, Form>();
        var extras = new IdentityHashMap<Item, List<Form>>();
        var targets = new HashMap<Form, Item>();
        for (var definition : VanillaUnificationDefinitions.entries()) {
            String prefix = definition.prefix() == null ? null : definition.prefix().getName();
            String path = definition.itemPath();
            if (prefix == null) {
                // Modern raw metals are not GT6 native ore chunks or their storage blocks.
                if (Set.of("raw_iron", "raw_gold", "raw_copper").contains(path)) prefix = "rawVanilla";
                else if (Set.of("raw_iron_block", "raw_gold_block", "raw_copper_block").contains(path)) prefix = "blockRawVanilla";
                else if (path.endsWith("_ore") || path.equals("ancient_debris"))
                    prefix = VanillaUnificationDefinitions.vanillaOreForm(path);
            }
            if (prefix == null) continue;
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(path));
            var material = GTMaterialRegistry.get(definition.materialName()).resolve();
            if (item == Items.AIR || !material.isValid()) continue;
            var form = new Form(prefix, material.getName());
            forms.put(item, form);
            if (!OriginalFormConversions.STANDARD_ORES.contains(prefix)) targets.putIfAbsent(form, item);
            for (var extraPrefix : definition.extraPrefixes()) {
                var extraForm = new Form(extraPrefix.getName(), material.getName());
                extras.computeIfAbsent(item, unused -> new ArrayList<>()).add(extraForm);
                targets.putIfAbsent(extraForm, item);
            }
        }
        for (var target : VanillaUnificationDefinitions.blockTargets()) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(target.itemPath()));
            var material = GTMaterialRegistry.get(target.materialName()).resolve();
            if (item == Items.AIR || !material.isValid()) continue;
            var form = new Form(target.blockPrefix(), material.getName());
            forms.put(item, form);
            targets.putIfAbsent(form, item);
        }
        // Real plank blocks are registered outside VanillaUnificationDefinitions. Prefer them to
        // generic plate forms, deterministically choosing vanilla before an equivalent mod block.
        var planks = new ArrayList<Item>(BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof BlockItem).toList());
        planks.sort(Comparator.comparing((Item item) -> !BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft"))
                .thenComparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        for (var item : planks) {
            var data = ItemMaterialRegistry.declared(item).orElse(null);
            if (data == null || data.prefix() != MaterialPrefix.plate || data.components().size() != 1
                    || data.amount() != GTValues.U || !data.material().has(MaterialProperty.WOOD)) continue;
            var form = new Form("plate", data.material().resolve().getName());
            forms.put(item, form);
            targets.putIfAbsent(form, item);
        }
        targets.putIfAbsent(new Form("plate", "Wood"), Items.OAK_PLANKS);
        vanilla = Collections.unmodifiableMap(forms);
        preferred = Map.copyOf(targets);
        var indexed = new HashMap<Form, List<Item>>();
        for (var item : BuiltInRegistries.ITEM) {
            var stack = new ItemStack(item);
            var form = direct(stack);
            if (form == null && taggedItems) form = tagged(stack);
            if (form != null) indexed.computeIfAbsent(form, unused -> new ArrayList<>()).add(item);
        }
        for (var entry : extras.entrySet())
            for (var form : entry.getValue())
                indexed.computeIfAbsent(form, unused -> new ArrayList<>()).add(entry.getKey());
        extras.replaceAll((item, values) -> List.copyOf(values));
        extraForms = Map.copyOf(extras);
        indexed.replaceAll((form, values) -> List.copyOf(values));
        aliases = Map.copyOf(indexed);
    }

    private static Form direct(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof BlockItem item) {
            var block = item.getBlock();
            if (block instanceof OreBlock ore) {
                var host = OreBlock.stoneOfStack(stack);
                String prefix = host.isBedrock() ? "oreBedrock" : host.isSediment() ? "oreDust" : ore.prefix().getName();
                return new Form(prefix, ore.material().resolve().getName());
            }
            if (block instanceof MaterialBlockLike material) {
                String prefix = material.prefix().getName();
                if (prefix.equals("blockSolid") && MaterialUnification.hiddenDuplicate(stack)) prefix = "blockIngot";
                return new Form(prefix, material.material().resolve().getName());
            }
            if (block instanceof ElectricWireBlock wire && !wire.spec().insulated())
                return new Form(String.format(Locale.ROOT, "wireGt%02d", wire.spec().size()), wire.spec().material().resolve().getName());
        }
        var known = vanilla.get(stack.getItem());
        if (known != null) return known;
        var declared = ItemMaterialRegistry.declared(stack.getItem()).orElse(null);
        if (declared == null
                || declared.prefix() == null
                || declared.components().size() != 1
                || declared.amount() != declared.prefix().getMaterialWeight()) return null;
        return new Form(declared.prefix().getName(), declared.material().resolve().getName());
    }

    public static Form form(ItemStack stack) {
        ensureInitialized();
        var known = direct(stack);
        if (known != null) return known;
        return tagged(stack);
    }

    /** Single-input conversions whose output depends on which slot the input occupies. */
    public static boolean positionSensitive(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var form = form(stack);
        return form != null && POSITIONED_INPUTS.contains(form.prefix());
    }

    private static final Set<String> POSITIONED_INPUTS = OriginalFormConversions.FIXED.stream()
            .filter(OriginalFormConversions.Conversion::single)
            .map(OriginalFormConversions.Conversion::input)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private static Form tagged(ItemStack stack) {
        var composition = ItemMaterialRegistry.base(stack.getItem()).orElse(null);
        if (composition == null || composition.prefix() == null) return null;
        return new Form(composition.prefix().getName(), composition.material().resolve().getName());
    }

    /** Every gregtech, forge or c form tag on this item. Callers use a single hit to reject ambiguous items. */
    public static List<Form> taggedForms(ItemStack stack) {
        ensureInitialized();
        if (stack.isEmpty()) return List.of();
        var found = new ArrayList<Form>();
        var seen = new HashSet<Form>();
        for (var tag : stack.getTags().map(itemTag -> itemTag.location()).toList()) {
            var form = formOf(tag);
            if (form != null && seen.add(form)) found.add(form);
        }
        return List.copyOf(found);
    }

    private static Form formOf(ResourceLocation tag) {
        String namespace = tag.getNamespace(), path = tag.getPath();
        if (!Set.of("gt6ce", "forge", "c").contains(namespace)) return null;
        int slash = path.lastIndexOf('/');
        if (slash < 0) return null;
        String material = materialNames.get(path.substring(slash + 1));
        if (material == null) return null;
        String group = path.substring(0, slash), prefix = prefixNames.get(group);
        if (prefix == null) prefix = MaterialEquivalence.prefixForCommonGroup(group);
        return prefix == null ? null : new Form(prefix, material);
    }

    public static Ingredient ingredient(ItemStack input) {
        ensureInitialized();
        var expected = form(input);
        if (expected == null) return Ingredient.of(input);
        var choices = new JsonArray();
        var seen = new HashSet<ResourceLocation>();
        for (var item : aliases.getOrDefault(expected, List.of(input.getItem()))) {
            // Runtime rebinding bypasses the JSON recipe rewrite. Do not reintroduce the
            // hidden GT copies through explicit alternatives after tags were unified.
            var canonical = MaterialUnification.canonical(new ItemStack(item));
            var id = BuiltInRegistries.ITEM.getKey(canonical.getItem());
            if (!seen.add(id)) continue;
            var value = new JsonObject(); value.addProperty("item", id.toString()); choices.add(value);
        }
        String material = MaterialEquivalence.materialName(GTMaterialRegistry.get(expected.material()));
        addTag(choices, "gt6ce:" + MaterialPrefix.camelToSnake(expected.prefix()) + "/" + material);
        var nativePrefix = PrefixRegistry.byName(expected.prefix());
        if (nativePrefix != null && !nativePrefix.getName().equals(expected.prefix()))
            addTag(choices, "gt6ce:" + MaterialPrefix.camelToSnake(nativePrefix.getName()) + "/" + material);
        String common = MaterialEquivalence.commonGroup(expected.prefix());
        if (common != null) {
            addTag(choices, "forge:" + common + "/" + material);
            addTag(choices, "c:" + common + "/" + material);
            String alternate = MaterialTagAliases.alternate(material);
            if (alternate != null) {
                addTag(choices, "forge:" + common + "/" + alternate);
                addTag(choices, "c:" + common + "/" + alternate);
            }
        }
//? if >=1.21.1 {
        /*return Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, choices).getOrThrow();
*///?} else {
        return Ingredient.fromJson(choices);
//?}
    }
    private static void addTag(JsonArray choices, String tag) {
        var value = new JsonObject(); value.addProperty("tag", tag); choices.add(value);
    }
    public static boolean matches(String prefix, String material, ItemStack stack) {
        var expected = new Form(prefix, material);
        if (expected.equals(form(stack))) return true;
        var extras = extraForms.get(stack.getItem());
        return extras != null && extras.contains(expected);
    }
    public static ItemStack canonical(ItemStack output) {
        ensureInitialized();
//? if >=1.21.1 {
        /*if (output.isEmpty() || !output.getComponentsPatch().isEmpty() || output.isDamaged()) return output;
*///?} else {
        if (output.isEmpty() || output.hasTag() || output.isDamaged()) return output;
//?}
        var form = direct(output);
        // Ordinary recipe outputs need not have a material form; Map.copyOf rejects null keys.
        var target = form == null ? null : preferred.get(form);
        return target == null ? MaterialUnification.canonical(output) : new ItemStack(target, output.getCount());
    }
    private static void ensureInitialized() { if (aliases.isEmpty()) rebuild(); }
    public static Map<Form, List<Item>> nativeAliases() { ensureInitialized(); return aliases; }
    /** Optional source forms supplied by another mod; never manufacture a new GT item for them. */
    public static ItemStack stack(String prefix, GTMaterial material, int count) {
        ensureInitialized();
        var items = aliases.get(new Form(prefix, material.resolve().getName()));
        return items == null || items.isEmpty() ? ItemStack.EMPTY : new ItemStack(items.get(0), count);
    }
    public static boolean contains(String prefix) {
        ensureInitialized(); return aliases.keySet().stream().anyMatch(form -> form.prefix().equals(prefix));
    }

    /** Item-only forms for plain stacks, rebuilt with the tag-derived tables after every reload. */
    private static final Map<Item, Optional<Form>> PLAIN_FORMS = new java.util.concurrent.ConcurrentHashMap<>();

    /** Form of the ingredient's first candidate without expanding its tags into item stacks. */
    private static Form ingredientForm(Ingredient ingredient) {
//? if >=1.21.1 {
        /*if (ingredient.isCustom()) {
            var expanded = ingredient.getItems();
            return expanded.length == 0 ? null : plainForm(expanded[0]);
        }
        var values = ingredient.getValues();
        if (values.length == 0) {
            var expanded = ingredient.getItems();
            return expanded.length == 0 ? null : plainForm(expanded[0]);
        }
        // Vanilla flattens values in order, so only the first value decides values[0].
        // An empty tag contributes a barrier stack there, whose form is also null.
        var first = values[0];
        if (first instanceof Ingredient.ItemValue item) return plainForm(item.item());
        if (first instanceof Ingredient.TagValue tag) {
            var iterator = BuiltInRegistries.ITEM.getTagOrEmpty(tag.tag()).iterator();
            return iterator.hasNext() ? plainForm(new ItemStack(iterator.next().value())) : null;
        }
        var expanded = ingredient.getItems();
        return expanded.length == 0 ? null : plainForm(expanded[0]);
*///?} else {
        var values = ingredient.getItems();
        return values.length == 0 ? null : plainForm(values[0]);
//?}
    }

    private static Form plainForm(ItemStack stack) {
        if (stack.isEmpty() || stack.isDamaged()) return form(stack);
//? if >=1.21.1 {
        /*if (!stack.getComponentsPatch().isEmpty()) return form(stack);
*///?} else {
        if (stack.hasTag()) return form(stack);
//?}
        var cached = PLAIN_FORMS.get(stack.getItem());
        if (cached == null) {
            cached = Optional.ofNullable(form(stack));
            PLAIN_FORMS.put(stack.getItem(), cached);
        }
        return cached.orElse(null);
    }

    public static boolean replaces(CraftingRecipe recipe, RegistryAccess access) {
        return replaces(recipe, access, false);
    }

    /** {@code gtOwned} keeps the loader's own generated rows out of the plain-recipe replacement pass. */
    public static boolean replaces(CraftingRecipe recipe, RegistryAccess access, boolean gtOwned) {
        if (gtOwned) return false;
        // Tool-aware serializers also wrap ordinary material rows. Only the selector
        // makes a single-input row position-sensitive; the marker alone must not keep
        // a legacy shapeless unpacking recipe ahead of the ordered replacement.
        if (recipe instanceof ToolShapelessRecipe tool && tool.formSelector().variants() > 0) return false;
        if (recipe instanceof gregtech.engine.recipe.AutocraftableCraftingRecipe
                && !(recipe instanceof ToolShapelessRecipe) && !(recipe instanceof ToolShapedRecipe)) return false;
        boolean shapeless = recipe instanceof ShapelessRecipe;
        if (!shapeless && !(recipe instanceof ShapedRecipe) && !(recipe instanceof ToolShapedRecipe)) return false;
        // A conversion always needs a material-form result; skip the ingredient forms when the
        // result cannot be one. Most generated tool and machine rows stop here.
        var output = plainForm(recipe.getResultItem(access));
        if (output == null) return false;
        var cells = new ArrayList<Form>();
        for (var ingredient : recipe.getIngredients()) {
            if (ingredient == Ingredient.EMPTY) { cells.add(null); continue; }
            var form = ingredientForm(ingredient);
            cells.add(form == null ? OriginalFormConversions.UNKNOWN : form);
        }
        int width = shapeless ? cells.size() : recipe instanceof ToolShapedRecipe tool
                ? tool.getWidth() : ((ShapedRecipe) recipe).getWidth();
        int height = shapeless ? 1 : recipe instanceof ToolShapedRecipe tool
                ? tool.getHeight() : ((ShapedRecipe) recipe).getHeight();
        return OriginalFormConversions.replaces(cells, width, height, shapeless, gtOwned, output);
    }
}
