package gregtech.compat.kubejs;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.ListRecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.rhino.Wrapper;
import dev.latvian.mods.rhino.type.TypeInfo;
import gregtech.api.addon.recipe.FluidComponent;
import gregtech.api.addon.recipe.ItemComponent;
import gregtech.api.addon.recipe.ItemResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Recipe components whose JSON is the machine-recipe document, not a vanilla ingredient. */
final class MachineRecipeComponents {
    static final RecipeComponent<List<ItemComponent>> ITEMS = list(new ItemComponentComponent());
    static final RecipeComponent<List<ItemResult>> RESULTS = list(new ItemResultComponent());
    static final RecipeComponent<List<FluidComponent>> FLUIDS = list(new FluidComponentComponent());

    private MachineRecipeComponents() {}

    private static <T> RecipeComponent<List<T>> list(RecipeComponent<T> component) {
        return new AllowEmpty<>(component.asList());
    }

    /** Empty input lists are valid: a recipe may use only items or only fluids. */
    private record AllowEmpty<T>(ListRecipeComponent<T> list) implements RecipeComponent<List<T>> {
        @Override
        public RecipeComponentType<?> type() {
            return list.type();
        }

        @Override
        public Codec<List<T>> codec() {
            return list.codec();
        }

        @Override
        public TypeInfo typeInfo() {
            return list.typeInfo();
        }

        @Override
        public List<T> wrap(RecipeScriptContext cx, Object from) {
            return list.wrap(cx, from);
        }

        @Override
        public boolean allowEmpty() {
            return true;
        }
    }

    private static final class ItemComponentComponent implements RecipeComponent<ItemComponent> {
        static final RecipeComponentType<ItemComponent> TYPE = RecipeComponentType.unit(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "machine_item"), new ItemComponentComponent());
        private static final Codec<ItemComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("item").forGetter(value -> optional(value.item())),
                        Codec.STRING.optionalFieldOf("tag").forGetter(value -> optional(value.tag())),
                        Codec.STRING.optionalFieldOf("material").forGetter(value -> optional(value.material())),
                        Codec.STRING.optionalFieldOf("form").forGetter(value -> optional(value.form())),
                        Codec.INT.optionalFieldOf("count", 1).forGetter(ItemComponent::count))
                .apply(
                        instance,
                        (item, tag, material, form, count) -> item(
                                item.orElse(null), tag.orElse(null), material.orElse(null), form.orElse(null), count)));

        @Override
        public RecipeComponentType<?> type() {
            return TYPE;
        }

        @Override
        public Codec<ItemComponent> codec() {
            return CODEC;
        }

        @Override
        public TypeInfo typeInfo() {
            return TypeInfo.of(ItemComponent.class);
        }

        @Override
        public ItemComponent wrap(RecipeScriptContext cx, Object from) {
            Object value = unwrap(from);
            if (value instanceof ItemComponent component)
                return item(
                        component.item(), component.tag(), component.material(), component.form(), component.count());
            if (value instanceof ItemStack stack) {
                if (stack.isEmpty()) throw new IllegalArgumentException("empty item");
                return item(
                        BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), null, null, null, stack.getCount());
            }
            if (value instanceof CharSequence text) return fromText(text.toString(), 1);
            return fromMap(map(value), 1);
        }
    }

    private static final class ItemResultComponent implements RecipeComponent<ItemResult> {
        static final RecipeComponentType<ItemResult> TYPE = RecipeComponentType.unit(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "machine_result"), new ItemResultComponent());
        private static final Codec<ItemResult> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("item").forGetter(value -> optional(value.item())),
                        Codec.STRING.optionalFieldOf("tag").forGetter(value -> optional(value.tag())),
                        Codec.STRING.optionalFieldOf("material").forGetter(value -> optional(value.material())),
                        Codec.STRING.optionalFieldOf("form").forGetter(value -> optional(value.form())),
                        Codec.INT.optionalFieldOf("count", 1).forGetter(ItemResult::count),
                        Codec.INT.optionalFieldOf("chance", 10000).forGetter(ItemResult::chance))
                .apply(
                        instance,
                        (item, tag, material, form, count, chance) -> result(
                                item.orElse(null),
                                tag.orElse(null),
                                material.orElse(null),
                                form.orElse(null),
                                count,
                                chance)));

        @Override
        public RecipeComponentType<?> type() {
            return TYPE;
        }

        @Override
        public Codec<ItemResult> codec() {
            return CODEC;
        }

        @Override
        public TypeInfo typeInfo() {
            return TypeInfo.of(ItemResult.class);
        }

        @Override
        public ItemResult wrap(RecipeScriptContext cx, Object from) {
            Object value = unwrap(from);
            if (value instanceof ItemResult result)
                return result(
                        result.item(), result.tag(), result.material(), result.form(), result.count(), result.chance());
            if (value instanceof ItemStack stack) {
                if (stack.isEmpty()) throw new IllegalArgumentException("empty item");
                return result(
                        BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                        null,
                        null,
                        null,
                        stack.getCount(),
                        10000);
            }
            if (value instanceof CharSequence text) return resultFromText(text.toString(), 1, 10000);
            var json = map(value);
            int count = json.containsKey("count") ? number(json.get("count")) : 1;
            int chance = json.containsKey("chance") ? number(json.get("chance")) : 10000;
            return result(
                    json.get("item") == null ? null : text(json.get("item")),
                    json.get("tag") == null ? null : text(json.get("tag")),
                    json.get("material") == null ? null : text(json.get("material")),
                    json.get("form") == null ? null : text(json.get("form")),
                    count,
                    chance);
        }
    }

    private static final class FluidComponentComponent implements RecipeComponent<FluidComponent> {
        static final RecipeComponentType<FluidComponent> TYPE = RecipeComponentType.unit(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "machine_fluid"), new FluidComponentComponent());
        private static final Codec<FluidComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("fluid").forGetter(value -> optional(value.fluid())),
                        Codec.STRING.optionalFieldOf("tag").forGetter(value -> optional(value.tag())),
                        Codec.LONG.fieldOf("amount").forGetter(FluidComponent::amount))
                .apply(instance, (fluid, tag, amount) -> fluid(fluid.orElse(null), tag.orElse(null), amount)));

        @Override
        public RecipeComponentType<?> type() {
            return TYPE;
        }

        @Override
        public Codec<FluidComponent> codec() {
            return CODEC;
        }

        @Override
        public TypeInfo typeInfo() {
            return TypeInfo.of(FluidComponent.class);
        }

        @Override
        public FluidComponent wrap(RecipeScriptContext cx, Object from) {
            Object value = unwrap(from);
            if (value instanceof FluidComponent component)
                return fluid(component.fluid(), component.tag(), component.amount());
            if (value instanceof FluidStack stack) {
                if (stack.isEmpty()) throw new IllegalArgumentException("empty fluid");
                return fluid(BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString(), null, stack.getAmount());
            }
            var json = map(value);
            return fluid(
                    json.get("fluid") == null ? null : text(json.get("fluid")),
                    json.get("tag") == null ? null : text(json.get("tag")),
                    ((Number) unwrap(json.get("amount"))).longValue());
        }
    }

    private static ItemComponent fromText(String text, int count) {
        if (text.startsWith("#")) return item(null, text.substring(1), null, null, count);
        return item(text, null, null, null, count);
    }

    private static ItemComponent fromMap(Map<String, Object> json, int fallback) {
        int count = json.containsKey("count") ? number(json.get("count")) : fallback;
        return item(
                json.get("item") == null ? null : text(json.get("item")),
                json.get("tag") == null ? null : text(json.get("tag")),
                json.get("material") == null ? null : text(json.get("material")),
                json.get("form") == null ? null : text(json.get("form")),
                count);
    }

    private static ItemComponent item(String item, String tag, String material, String form, int count) {
        if (count < 1) throw new IllegalArgumentException("count must be at least 1");
        return new ItemComponent(item, tag, material, form, count);
    }

    private static ItemResult resultFromText(String text, int count, int chance) {
        if (text.startsWith("#")) return result(null, text.substring(1), null, null, count, chance);
        return result(text, null, null, null, count, chance);
    }

    private static ItemResult result(String item, String tag, String material, String form, int count, int chance) {
        if (count < 1) throw new IllegalArgumentException("count must be at least 1");
        if (chance < 1 || chance > 10000) throw new IllegalArgumentException("chance must be from 1 to 10000");
        return new ItemResult(item, tag, material, form, count, chance);
    }

    private static FluidComponent fluid(String fluid, String tag, long amount) {
        if (amount < 0 || amount > Integer.MAX_VALUE) throw new IllegalArgumentException("amount");
        return new FluidComponent(fluid, tag, amount);
    }

    private static Optional<String> optional(String value) {
        return value == null ? Optional.empty() : Optional.of(value);
    }

    static Map<String, Object> map(Object value) {
        Object unwrapped = unwrap(value);
        if (unwrapped instanceof Map<?, ?> raw) {
            var out = new LinkedHashMap<String, Object>();
            for (var entry : raw.entrySet()) out.put(String.valueOf(entry.getKey()), unwrap(entry.getValue()));
            return out;
        }
        if (unwrapped instanceof JsonObject json) {
            var out = new LinkedHashMap<String, Object>();
            for (var entry : json.entrySet()) {
                var child = entry.getValue();
                if (child.isJsonPrimitive()) {
                    var primitive = child.getAsJsonPrimitive();
                    out.put(
                            entry.getKey(),
                            primitive.isBoolean()
                                    ? primitive.getAsBoolean()
                                    : primitive.isNumber() ? primitive.getAsNumber() : primitive.getAsString());
                } else {
                    out.put(entry.getKey(), child);
                }
            }
            return out;
        }
        throw new IllegalArgumentException("expected an object");
    }

    static Object unwrap(Object value) {
        return value instanceof Wrapper wrapper ? unwrap(wrapper.unwrap()) : value;
    }

    static String text(Object value) {
        Object unwrapped = unwrap(value);
        if (unwrapped instanceof CharSequence text && !text.isEmpty()) return text.toString();
        if (unwrapped instanceof com.google.gson.JsonPrimitive primitive && primitive.isString())
            return primitive.getAsString();
        throw new IllegalArgumentException("expected text");
    }

    static int number(Object value) {
        Object unwrapped = unwrap(value);
        if (unwrapped instanceof Number number) return number.intValue();
        if (unwrapped instanceof com.google.gson.JsonPrimitive primitive && primitive.isNumber())
            return primitive.getAsInt();
        throw new IllegalArgumentException("expected a number");
    }
}
