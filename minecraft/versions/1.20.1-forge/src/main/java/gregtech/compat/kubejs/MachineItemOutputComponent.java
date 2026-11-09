package gregtech.compat.kubejs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import gregtech.api.addon.recipe.ItemResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Keeps item tags and material forms in the machine document instead of coercing them into a stack. */
final class MachineItemOutputComponent implements RecipeComponent<ItemResult> {
    static final MachineItemOutputComponent INSTANCE = new MachineItemOutputComponent();

    @Override
    public Class<?> componentClass() {
        return ItemResult.class;
    }

    @Override
    public JsonElement write(RecipeJS recipe, ItemResult value) {
        var json = new JsonObject();
        MachineItemJson.writeIdentity(json, value.item(), value.tag(), value.material(), value.form());
        json.addProperty("count", value.count());
        json.addProperty("chance", value.chance());
        return json;
    }

    @Override
    public ItemResult read(RecipeJS recipe, Object value) {
        while (value instanceof dev.latvian.mods.rhino.Wrapper wrapper) value = wrapper.unwrap();
        if (value instanceof ItemResult result) return result;
        if (value instanceof OutputItem output) {
            if (output.isEmpty()) throw new IllegalArgumentException("empty item");
            int chance = output.hasChance() ? chance(output.getChance()) : 10_000;
            var stack = output.item;
            return new ItemResult(
                    BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                    null,
                    null,
                    null,
                    stack.getCount(),
                    chance);
        }
        if (value instanceof ItemStack stack) {
            if (stack.isEmpty()) throw new IllegalArgumentException("empty item");
            return new ItemResult(
                    BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                    null,
                    null,
                    null,
                    stack.getCount(),
                    10_000);
        }
        if (value instanceof CharSequence text) return fromText(text.toString(), 1, 10_000);
        if (value instanceof JsonObject json) return fromJson(json, 1, 10_000);
        if (value instanceof java.util.Map<?, ?> map) return fromJson(MachineItemJson.map(map), 1, 10_000);
        throw new IllegalArgumentException("Expected an item output");
    }

    /** A fraction in {@code (0, 1]} is parts of one, scaled to parts of 10000. */
    private static int chance(double value) {
        if (Double.isNaN(value) || value < 0 || value > 10_000)
            throw new IllegalArgumentException("chance must be from 0 to 10000");
        int scaled = value > 0 && value <= 1 ? (int) Math.round(value * 10_000) : (int) Math.round(value);
        if (scaled < 1) throw new IllegalArgumentException("chance must be from 1 to 10000");
        return scaled;
    }

    private static ItemResult fromText(String text, int count, int chance) {
        if (text.startsWith("#")) return new ItemResult(null, text.substring(1), null, null, count, chance);
        return new ItemResult(text, null, null, null, count, chance);
    }

    private static ItemResult fromJson(JsonObject json, int fallbackCount, int fallbackChance) {
        int count = MachineItemJson.integer(json, "count", fallbackCount);
        int chance = MachineItemJson.integer(json, "chance", fallbackChance);
        if (count < 1) throw new IllegalArgumentException("count must be at least 1");
        if (chance < 1 || chance > 10_000) throw new IllegalArgumentException("chance must be from 1 to 10000");
        return new ItemResult(
                MachineItemJson.text(json, "item"),
                MachineItemJson.text(json, "tag"),
                MachineItemJson.text(json, "material"),
                MachineItemJson.text(json, "form"),
                count,
                chance);
    }
}
