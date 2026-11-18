package gregtech.content.data;

import gregtech.content.recipe.EnergyEquipmentRecipeRules;
import gregtech.content.recipe.EnergyEquipmentRecipeRules.Ingredient;
import gregtech.content.recipe.EnergyEquipmentRecipeRules.Row;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Emits the {@code chemical_batteries}, {@code engines}, {@code energy_nodes} and
 * {@code long_distance} crafting recipes from {@link EnergyEquipmentRecipeRules}.
 *
 * <p>The tables keep the rows loader-free; this class is the only place that knows how a row
 * becomes recipe JSON, so a missing ingredient kind fails at compile time instead of producing a
 * half-written recipe.</p>
 */
public final class EnergyEquipmentRecipePack {
    private EnergyEquipmentRecipePack() {}

    public static void contribute(Map<ResourceLocation, byte[]> generated) {
        for (Row row : EnergyEquipmentRecipeRules.ROWS) {
            GeneratedRecipeJson.put(generated, row.jsonPath(), recipe(row));
        }
    }

    private static Map<String, Object> recipe(Row row) {
        Map<String, Object> key = new LinkedHashMap<>();
        for (var entry : row.key().entrySet()) {
            key.put(String.valueOf(entry.getKey()), ingredient(entry.getValue()));
        }
        // Every row of these four categories crafts a single item.
        return GeneratedRecipeJson.shaped(row.type(), row.pattern(), key, row.resultId(), 1, row.allowMirror());
    }

    private static Map<String, Object> ingredient(Ingredient ingredient) {
        return switch (ingredient.kind()) {
            case ITEM -> GeneratedRecipeJson.item(ingredient.id());
            case TAG -> GeneratedRecipeJson.tag(ingredient.id());
            case LUBRICANT_CONTAINER -> lubricantContainer(ingredient.id());
        };
    }

    /** The diesel engines' lubricant slot, whose serializer takes a fluid name instead of an id. */
    private static Map<String, Object> lubricantContainer(String fluid) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("allow_lubricant_bottle", true);
        json.put("fluid", fluid);
        json.put("type", "gt6ce:finite_fluid_container_1000");
        return json;
    }
}
