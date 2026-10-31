package gregtech.compat.kubejs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.fluid.FluidStackJS;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import gregtech.engine.recipe.data.FluidComponent;

/** Keeps fluid tags in the machine document instead of coercing them into a fluid stack. */
final class MachineFluidInputComponent implements RecipeComponent<FluidComponent> {
    static final MachineFluidInputComponent INSTANCE = new MachineFluidInputComponent();

    @Override
    public Class<?> componentClass() {
        return FluidComponent.class;
    }

    @Override
    public JsonElement write(RecipeJS recipe, FluidComponent value) {
        var json = new JsonObject();
        json.addProperty(value.tag() == null ? "fluid" : "tag", value.tag() == null ? value.fluid() : value.tag());
        json.addProperty("amount", value.amount());
        return json;
    }

    @Override
    public FluidComponent read(RecipeJS recipe, Object value) {
        while (value instanceof dev.latvian.mods.rhino.Wrapper wrapper) value = wrapper.unwrap();
        if (value instanceof FluidComponent component) return component;
        if (value instanceof FluidStackJS stack) return new FluidComponent(stack.getId(), stack.getAmount());
        if (value instanceof JsonObject json)
            return new FluidComponent(
                    json.has("fluid") ? json.get("fluid").getAsString() : null,
                    json.has("tag") ? json.get("tag").getAsString() : null,
                    json.get("amount").getAsLong());
        return ScriptValues.fluid(value);
    }
}
