package gregtech.content.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialForm;
import gregtech.engine.material.ItemMaterialForms;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.material.MaterialTagAliases;
import gregtech.engine.material.MaterialUnification;
import gregtech.engine.prefix.PrefixRegistry;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

/** Adapts shared material-form identities to the original hand conversions. */
public final class CraftingMaterialForms {
    private CraftingMaterialForms() {}

    /** Single-input conversions whose output depends on which slot the input occupies. */
    public static boolean positionSensitive(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var form = ItemMaterialForms.form(stack);
        return form != null && POSITIONED_INPUTS.contains(form.prefix());
    }

    private static final Set<String> POSITIONED_INPUTS = OriginalFormConversions.FIXED.stream()
            .filter(OriginalFormConversions.Conversion::single)
            .map(OriginalFormConversions.Conversion::input)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    public static Ingredient ingredient(ItemStack input) {
        return ingredient(input, ItemMaterialForms.aliases());
    }

    static Ingredient ingredient(ItemStack input, Map<ItemMaterialForm, List<Item>> aliases) {
        var expected = ItemMaterialForms.form(input);
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
    /** Form of the ingredient's first candidate without expanding its tags into item stacks. */
    private static ItemMaterialForm ingredientForm(Ingredient ingredient) {
//? if >=1.21.1 {
        /*if (ingredient.isCustom()) {
            var expanded = ingredient.getItems();
            return expanded.length == 0 ? null : ItemMaterialForms.plainForm(expanded[0]);
        }
        var values = ingredient.getValues();
        if (values.length == 0) {
            var expanded = ingredient.getItems();
            return expanded.length == 0 ? null : ItemMaterialForms.plainForm(expanded[0]);
        }
        // Vanilla flattens values in order, so only the first value decides values[0].
        // An empty tag contributes a barrier stack there, whose form is also null.
        var first = values[0];
        if (first instanceof Ingredient.ItemValue item) return ItemMaterialForms.plainForm(item.item());
        if (first instanceof Ingredient.TagValue tag) {
            var iterator = BuiltInRegistries.ITEM.getTagOrEmpty(tag.tag()).iterator();
            return iterator.hasNext() ? ItemMaterialForms.plainForm(new ItemStack(iterator.next().value())) : null;
        }
        var expanded = ingredient.getItems();
        return expanded.length == 0 ? null : ItemMaterialForms.plainForm(expanded[0]);
*///?} else {
        var values = ingredient.getItems();
        return values.length == 0 ? null : ItemMaterialForms.plainForm(values[0]);
//?}
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
        var output = ItemMaterialForms.plainForm(recipe.getResultItem(access));
        if (output == null) return false;
        var cells = new ArrayList<ItemMaterialForm>();
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
