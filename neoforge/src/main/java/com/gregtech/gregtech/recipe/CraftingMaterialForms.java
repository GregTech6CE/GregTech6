package com.gregtech.gregtech.recipe;

import com.google.gson.*;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.prefix.*;
import com.gregtech.gregtech.block.*;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.content.recipe.OriginalFormConversions;
import com.gregtech.gregtech.content.recipe.OriginalFormConversions.Form;
import com.gregtech.gregtech.data.MaterialPrefix;
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
    private static final Map<String, String> COMMON = Map.ofEntries(
            Map.entry("ingot", "ingots"), Map.entry("nugget", "nuggets"), Map.entry("gem", "gems"),
            Map.entry("dust", "dusts"), Map.entry("dustSmall", "small_dusts"), Map.entry("dustTiny", "tiny_dusts"),
            Map.entry("plate", "plates"), Map.entry("oreRaw", "raw_materials"), Map.entry("ore", "ores"));
    private static Map<Form, List<Item>> aliases = Map.of();
    private static Map<String, String> materialNames = Map.of();
    private static Map<String, String> prefixNames = Map.of();
    private static Map<Item, Form> vanilla = Map.of();
    private static Map<Form, Item> preferred = Map.of();

    public static synchronized void rebuild() {
        var materials = new HashMap<String, String>();
        for (var material : GTMaterialRegistry.allMaterials()) if (material.isValid()) {
            String name = material.resolve().getName();
            materials.put(MaterialEquivalence.materialName(material), name);
            materials.put(material.getName().toLowerCase(Locale.ROOT), name);
        }
        materialNames = Map.copyOf(materials);
        var prefixes = new HashMap<String, String>();
        for (var conversion : OriginalFormConversions.FIXED) {
            prefixes.put(MaterialPrefix.camelToSnake(conversion.input()), conversion.input());
            prefixes.put(MaterialPrefix.camelToSnake(conversion.output()), conversion.output());
        }
        for (String prefix : OriginalFormConversions.STANDARD_ORES) prefixes.put(MaterialPrefix.camelToSnake(prefix), prefix);
        for (String prefix : OriginalFormConversions.DENSE_ORES) prefixes.put(MaterialPrefix.camelToSnake(prefix), prefix);
        for (int size = 1; size <= 16; size++) prefixes.put(String.format(Locale.ROOT, "wire_%02d", size),
                String.format(Locale.ROOT, "wireGt%02d", size));
        prefixNames = Map.copyOf(prefixes);
        var forms = new IdentityHashMap<Item, Form>();
        var targets = new HashMap<Form, Item>();
        for (var definition : VanillaUnificationDefinitions.entries()) {
            String prefix = definition.prefix() == null ? null : definition.prefix().getName();
            String path = definition.itemPath();
            if (Set.of("iron_block", "gold_block", "copper_block", "netherite_block").contains(path)) prefix = "blockIngot";
            else if (Set.of("diamond_block", "emerald_block", "lapis_block", "coal_block").contains(path)) prefix = "blockGem";
            else if (path.equals("redstone_block")) prefix = "blockDust";
            else if (Set.of("raw_iron_block", "raw_gold_block", "raw_copper_block").contains(path)) prefix = "blockRaw";
            else if (definition.prefix() == null && (path.endsWith("_ore") || path.equals("ancient_debris"))) prefix = "oreVanillastone";
            if (prefix == null) continue;
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(path));
            var material = GTMaterialRegistry.get(definition.materialName()).resolve();
            if (item == Items.AIR || !material.isValid()) continue;
            var form = new Form(prefix, material.getName());
            forms.put(item, form);
            if (!OriginalFormConversions.STANDARD_ORES.contains(prefix)) targets.putIfAbsent(form, item);
        }
        vanilla = Collections.unmodifiableMap(forms);
        preferred = Map.copyOf(targets);
        var indexed = new HashMap<Form, List<Item>>();
        for (var item : BuiltInRegistries.ITEM) {
            var form = direct(new ItemStack(item));
            if (form != null) indexed.computeIfAbsent(form, unused -> new ArrayList<>()).add(item);
        }
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
            if (block instanceof MaterialBlockLike material)
                return new Form(material.prefix().getName(), material.material().resolve().getName());
            if (block instanceof ElectricWireBlock wire && !wire.spec().insulated())
                return new Form(String.format(Locale.ROOT, "wireGt%02d", wire.spec().size()), wire.spec().material().resolve().getName());
        }
        var known = vanilla.get(stack.getItem());
        if (known != null) return known;
        var form = MaterialEquivalence.form(stack);
        return form == null ? null : new Form(form.prefix().getName(), form.material().resolve().getName());
    }

    public static Form form(ItemStack stack) {
        ensureInitialized();
        var known = direct(stack);
        if (known != null) return known;
        // Prefer explicit GT prefix tags over common tags (dense ores can also be in ordinary ores tags).
        var tags = stack.getTags().map(tag -> tag.location()).sorted(Comparator.comparing(id -> id.getNamespace().equals("gregtech") ? 0 : 1)).toList();
        for (var tag : tags) {
            String namespace = tag.getNamespace(), path = tag.getPath();
            if (!Set.of("gregtech", "forge", "c").contains(namespace)) continue;
            int slash = path.lastIndexOf('/');
            if (slash < 0) continue;
            String material = materialNames.get(path.substring(slash + 1));
            if (material == null) continue;
            String group = path.substring(0, slash), prefix = namespace.equals("gregtech") ? prefixNames.get(group) : null;
            if (prefix == null) for (var entry : COMMON.entrySet()) if (entry.getValue().equals(group)) { prefix = entry.getKey(); break; }
            if (prefix != null) return new Form(prefix, material);
        }
        return null;
    }

    public static Ingredient ingredient(ItemStack input) {
        ensureInitialized();
        var expected = direct(input);
        if (expected == null) return Ingredient.of(input);
        var choices = new JsonArray();
        for (var item : aliases.getOrDefault(expected, List.of(input.getItem()))) {
            var value = new JsonObject(); value.addProperty("item", BuiltInRegistries.ITEM.getKey(item).toString()); choices.add(value);
        }
        String material = MaterialEquivalence.materialName(GTMaterialRegistry.get(expected.material()));
        addTag(choices, "gregtech:" + MaterialPrefix.camelToSnake(expected.prefix()) + "/" + material);
        String common = COMMON.get(expected.prefix());
        if (common != null) {
            addTag(choices, "forge:" + common + "/" + material);
            addTag(choices, "c:" + common + "/" + material);
        }
        return Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, choices).getOrThrow();
    }
    private static void addTag(JsonArray choices, String tag) {
        var value = new JsonObject(); value.addProperty("tag", tag); choices.add(value);
    }
    public static boolean matches(String prefix, String material, ItemStack stack) {
        return new Form(prefix, material).equals(form(stack));
    }
    public static ItemStack canonical(ItemStack output) {
        ensureInitialized();
        if (output.isEmpty() || !output.getComponentsPatch().isEmpty() || output.isDamaged()) return output;
        var target = preferred.get(direct(output));
        return target == null ? MaterialUnification.canonical(output) : new ItemStack(target, output.getCount());
    }
    private static void ensureInitialized() { if (aliases.isEmpty()) rebuild(); }
    public static Map<Form, List<Item>> nativeAliases() { ensureInitialized(); return aliases; }

    public static boolean replaces(CraftingRecipe recipe, RegistryAccess access) {
        if (recipe instanceof com.gregtech.gregtech.api.recipe.AutocraftableCraftingRecipe) return false;
        boolean shapeless = recipe instanceof ShapelessRecipe;
        if (!shapeless && !(recipe instanceof ShapedRecipe)) return false;
        var cells = new ArrayList<Form>();
        for (var ingredient : recipe.getIngredients()) {
            if (ingredient == Ingredient.EMPTY) { cells.add(null); continue; }
            var values = ingredient.getItems();
            var form = values.length == 0 ? null : form(values[0]);
            cells.add(form == null ? OriginalFormConversions.UNKNOWN : form);
        }
        int width = shapeless ? cells.size() : ((ShapedRecipe) recipe).getWidth();
        int height = shapeless ? 1 : ((ShapedRecipe) recipe).getHeight();
        return OriginalFormConversions.replaces(cells, width, height, shapeless, false, form(recipe.getResultItem(access)));
    }
}
