package gregtech.integration.kubejs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gregtech.internal.recipe.data.MachineRecipeScript;
import dev.latvian.mods.kubejs.fluid.FluidStackJS;
import dev.latvian.mods.kubejs.fluid.InputFluid;
import dev.latvian.mods.kubejs.fluid.OutputFluid;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.recipe.RecipeExceptionJS;
import dev.latvian.mods.kubejs.recipe.RecipeJS;

/** Writes ingredient JSON in the shape the machine-recipe parser accepts. */
public final class MachineRecipeJS extends RecipeJS {
    @Override
    public void afterLoaded() {
        try {
            MachineRecipeScript.requireKnownMap(getValue(MachineRecipeSchema.MAP));
        } catch (RuntimeException failure) {
            throw new RecipeExceptionJS(failure.getMessage());
        }
    }

    @Override
    public JsonElement writeInputItem(InputItem value) {
        JsonElement ingredient = value.ingredient.toJson();
        if (!(ingredient instanceof JsonObject object) || object.has("item") == object.has("tag"))
            throw new RecipeExceptionJS("Machine recipe item input needs exactly one of item or tag");
        if (value.count != 1) object.addProperty("count", value.count);
        return object;
    }

    @Override
    public JsonElement writeOutputItem(OutputItem value) {
        var object = new JsonObject();
        object.addProperty("item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(value.item.getItem()).toString());
        if (value.item.getCount() != 1) object.addProperty("count", value.item.getCount());
        if (value.hasChance()) object.addProperty("chance", chance(value.getChance()));
        return object;
    }

    @Override
    public JsonElement writeInputFluid(InputFluid value) {
        return fluid(value);
    }

    @Override
    public JsonElement writeOutputFluid(OutputFluid value) {
        return fluid(value);
    }

    private static JsonElement fluid(Object value) {
        if (!(value instanceof FluidStackJS stack))
            throw new RecipeExceptionJS("Machine recipe fluid must be a fluid stack");
        var json = stack.toJson().getAsJsonObject();
        json.remove("nbt");
        json.remove("chance");
        return json;
    }

    /** 5000 stays 5000. A fraction in {@code (0, 1]} is parts of one, scaled to parts of 10000. */
    private static int chance(double value) {
        if (Double.isNaN(value) || value < 0 || value > 10000)
            throw new RecipeExceptionJS("Machine recipe chance must be from 0 to 10000");
        int scaled = value > 0 && value <= 1 ? (int) Math.round(value * 10000) : (int) Math.round(value);
        if (scaled < 1) throw new RecipeExceptionJS("Machine recipe chance must be from 1 to 10000");
        return scaled;
    }
}
