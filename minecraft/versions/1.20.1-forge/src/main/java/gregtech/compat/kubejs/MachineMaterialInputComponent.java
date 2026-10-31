package gregtech.compat.kubejs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import gregtech.api.addon.recipe.MaterialUnitIngredient;
import gregtech.engine.recipe.data.JsonMaps;
import gregtech.engine.recipe.data.MachineRecipeDocuments;

final class MachineMaterialInputComponent implements RecipeComponent<MaterialUnitIngredient> {
    static final MachineMaterialInputComponent INSTANCE = new MachineMaterialInputComponent();

    @Override
    public Class<?> componentClass() {
        return MaterialUnitIngredient.class;
    }

    @Override
    public JsonElement write(RecipeJS recipe, MaterialUnitIngredient input) {
        return JsonParser.parseString(MachineRecipeDocuments.materialJson(input));
    }

    @Override
    public MaterialUnitIngredient read(RecipeJS recipe, Object value) {
        while (value instanceof dev.latvian.mods.rhino.Wrapper wrapper) value = wrapper.unwrap();
        if (value instanceof MaterialUnitIngredient ingredient) return ingredient;
        if (value instanceof JsonObject json) return MachineRecipeDocuments.material(JsonMaps.object(json.toString()));
        if (value instanceof java.util.Map<?, ?> map) {
            var converted = new java.util.LinkedHashMap<String, Object>();
            map.forEach((key, entry) -> converted.put(String.valueOf(key), entry));
            return MachineRecipeDocuments.material(converted);
        }
        throw new IllegalArgumentException("Expected material input object");
    }
}
