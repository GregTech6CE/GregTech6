package gregtech.compat.kubejs;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.rhino.type.TypeInfo;
import gregtech.api.addon.recipe.MaterialUnitIngredient;
import gregtech.engine.recipe.data.JsonMaps;
import gregtech.engine.recipe.data.MachineRecipeDocuments;
import net.minecraft.resources.ResourceLocation;

final class MachineMaterialInputComponent implements RecipeComponent<MaterialUnitIngredient> {
    static final MachineMaterialInputComponent INSTANCE = new MachineMaterialInputComponent();
    private static final RecipeComponentType<MaterialUnitIngredient> TYPE =
            RecipeComponentType.unit(ResourceLocation.fromNamespaceAndPath("gt6ce", "material_unit_input"), INSTANCE);
    private static final Codec<MaterialUnitIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("material").forGetter(MaterialUnitIngredient::material),
                    Codec.STRING
                            .fieldOf("units")
                            .forGetter(value -> gregtech.engine.recipe.MaterialUnitAmount.format(value.amount())),
                    Codec.STRING.listOf().fieldOf("forms").forGetter(MaterialUnitIngredient::forms),
                    Codec.STRING
                            .optionalFieldOf("rounding", "whole_outputs")
                            .forGetter(value -> value.rounding().name().toLowerCase(java.util.Locale.ROOT)))
            .apply(
                    instance,
                    (material, units, forms, rounding) -> new MaterialUnitIngredient(
                            material,
                            gregtech.engine.recipe.MaterialUnitAmount.parse(units),
                            forms,
                            MaterialUnitIngredient.Rounding.valueOf(rounding.toUpperCase(java.util.Locale.ROOT)))));

    @Override
    public RecipeComponentType<?> type() {
        return TYPE;
    }

    @Override
    public Codec<MaterialUnitIngredient> codec() {
        return CODEC;
    }

    @Override
    public TypeInfo typeInfo() {
        return TypeInfo.of(MaterialUnitIngredient.class);
    }

    @Override
    public MaterialUnitIngredient wrap(RecipeScriptContext cx, Object value) {
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
