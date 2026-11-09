package gregtech.compat.kubejs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import gregtech.api.addon.recipe.ItemComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Keeps item tags and material forms in the machine document instead of coercing them into a stack. */
final class MachineItemInputComponent implements RecipeComponent<ItemComponent> {
    static final MachineItemInputComponent INSTANCE = new MachineItemInputComponent();

    @Override
    public Class<?> componentClass() {
        return ItemComponent.class;
    }

    @Override
    public JsonElement write(RecipeJS recipe, ItemComponent value) {
        var json = new JsonObject();
        MachineItemJson.writeIdentity(json, value.item(), value.tag(), value.material(), value.form());
        json.addProperty("count", value.count());
        return json;
    }

    @Override
    public ItemComponent read(RecipeJS recipe, Object value) {
        while (value instanceof dev.latvian.mods.rhino.Wrapper wrapper) value = wrapper.unwrap();
        if (value instanceof ItemComponent component) return component;
        if (value instanceof InputItem input) {
            var json = input.toJsonJS();
            if (!json.isJsonObject())
                throw new IllegalArgumentException("a machine recipe item input needs one item or tag");
            return fromJson(json.getAsJsonObject(), Math.max(1, input.count));
        }
        if (value instanceof ItemStack stack) {
            if (stack.isEmpty()) throw new IllegalArgumentException("empty item");
            return new ItemComponent(
                    BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), null, null, null, stack.getCount());
        }
        if (value instanceof CharSequence text) return fromText(text.toString(), 1);
        if (value instanceof JsonObject json) return fromJson(json, 1);
        if (value instanceof java.util.Map<?, ?> map) return fromJson(MachineItemJson.map(map), 1);
        throw new IllegalArgumentException("Expected an item input");
    }

    private static ItemComponent fromText(String text, int count) {
        if (text.startsWith("#")) return new ItemComponent(null, text.substring(1), null, null, count);
        return new ItemComponent(text, null, null, null, count);
    }

    private static ItemComponent fromJson(JsonObject json, int fallback) {
        int count = MachineItemJson.integer(json, "count", fallback);
        if (count < 1) throw new IllegalArgumentException("count must be at least 1");
        return new ItemComponent(
                MachineItemJson.text(json, "item"),
                MachineItemJson.text(json, "tag"),
                MachineItemJson.text(json, "material"),
                MachineItemJson.text(json, "form"),
                count);
    }
}
