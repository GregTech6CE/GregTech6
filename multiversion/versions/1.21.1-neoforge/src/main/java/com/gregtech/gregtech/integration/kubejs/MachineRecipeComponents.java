package com.gregtech.gregtech.integration.kubejs;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.ListRecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.rhino.Wrapper;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Recipe components whose JSON is the machine-recipe document, not a vanilla ingredient. */
final class MachineRecipeComponents {
    static final RecipeComponent<List<MachineItem>> ITEMS = list(new MachineItemComponent());
    static final RecipeComponent<List<MachineResult>> RESULTS = list(new MachineResultComponent());
    static final RecipeComponent<List<MachineFluid>> FLUIDS = list(new MachineFluidComponent());

    private MachineRecipeComponents() {}

    private static <T> RecipeComponent<List<T>> list(RecipeComponent<T> component) {
        return new AllowEmpty<>(component.asList());
    }

    /** Empty input lists are valid: a recipe may use only items or only fluids. */
    private record AllowEmpty<T>(ListRecipeComponent<T> list) implements RecipeComponent<List<T>> {
        @Override public RecipeComponentType<?> type() { return list.type(); }
        @Override public Codec<List<T>> codec() { return list.codec(); }
        @Override public TypeInfo typeInfo() { return list.typeInfo(); }
        @Override public List<T> wrap(RecipeScriptContext cx, Object from) { return list.wrap(cx, from); }
        @Override public boolean allowEmpty() { return true; }
    }

    record MachineItem(String item, String tag, int count) {}

    record MachineResult(String item, int count, int chance) {}

    record MachineFluid(String fluid, long amount) {}

    private static final class MachineItemComponent implements RecipeComponent<MachineItem> {
        static final RecipeComponentType<MachineItem> TYPE = RecipeComponentType.unit(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "machine_item"), new MachineItemComponent());
        private static final Codec<MachineItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("item").forGetter(value -> optional(value.item)),
                Codec.STRING.optionalFieldOf("tag").forGetter(value -> optional(value.tag)),
                Codec.INT.optionalFieldOf("count", 1).forGetter(MachineItem::count)
        ).apply(instance, (item, tag, count) -> checked(item.orElse(null), tag.orElse(null), count)));

        @Override public RecipeComponentType<?> type() { return TYPE; }
        @Override public Codec<MachineItem> codec() { return CODEC; }
        @Override public TypeInfo typeInfo() { return TypeInfo.of(MachineItem.class); }

        @Override
        public MachineItem wrap(RecipeScriptContext cx, Object from) {
            Object value = unwrap(from);
            if (value instanceof MachineItem item) return checked(item.item, item.tag, item.count);
            if (value instanceof ItemStack stack) {
                if (stack.isEmpty()) throw new IllegalArgumentException("empty item");
                return checked(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), null, stack.getCount());
            }
            if (value instanceof CharSequence text) return fromText(text.toString(), 1);
            return fromMap(map(value), 1);
        }
    }

    private static final class MachineResultComponent implements RecipeComponent<MachineResult> {
        static final RecipeComponentType<MachineResult> TYPE = RecipeComponentType.unit(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "machine_result"), new MachineResultComponent());
        private static final Codec<MachineResult> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("item").forGetter(MachineResult::item),
                Codec.INT.optionalFieldOf("count", 1).forGetter(MachineResult::count),
                Codec.INT.optionalFieldOf("chance", 10000).forGetter(MachineResult::chance)
        ).apply(instance, MachineRecipeComponents::result));

        @Override public RecipeComponentType<?> type() { return TYPE; }
        @Override public Codec<MachineResult> codec() { return CODEC; }
        @Override public TypeInfo typeInfo() { return TypeInfo.of(MachineResult.class); }

        @Override
        public MachineResult wrap(RecipeScriptContext cx, Object from) {
            Object value = unwrap(from);
            if (value instanceof MachineResult result) return result(result.item, result.count, result.chance);
            if (value instanceof ItemStack stack) {
                if (stack.isEmpty()) throw new IllegalArgumentException("empty item");
                return result(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), 10000);
            }
            if (value instanceof CharSequence text) return result(text.toString(), 1, 10000);
            var json = map(value);
            int count = json.containsKey("count") ? number(json.get("count")) : 1;
            int chance = json.containsKey("chance") ? number(json.get("chance")) : 10000;
            return result(text(json.get("item")), count, chance);
        }
    }

    private static final class MachineFluidComponent implements RecipeComponent<MachineFluid> {
        static final RecipeComponentType<MachineFluid> TYPE = RecipeComponentType.unit(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "machine_fluid"), new MachineFluidComponent());
        private static final Codec<MachineFluid> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(MachineFluid::fluid),
                Codec.LONG.fieldOf("amount").forGetter(MachineFluid::amount)
        ).apply(instance, MachineRecipeComponents::fluid));

        @Override public RecipeComponentType<?> type() { return TYPE; }
        @Override public Codec<MachineFluid> codec() { return CODEC; }
        @Override public TypeInfo typeInfo() { return TypeInfo.of(MachineFluid.class); }

        @Override
        public MachineFluid wrap(RecipeScriptContext cx, Object from) {
            Object value = unwrap(from);
            if (value instanceof MachineFluid fluid) return fluid(fluid.fluid, fluid.amount);
            if (value instanceof FluidStack stack) {
                if (stack.isEmpty()) throw new IllegalArgumentException("empty fluid");
                return fluid(BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString(), stack.getAmount());
            }
            var json = map(value);
            return fluid(text(json.get("fluid")), ((Number) unwrap(json.get("amount"))).longValue());
        }
    }

    private static MachineItem fromText(String text, int count) {
        if (text.startsWith("#")) return checked(null, text.substring(1), count);
        return checked(text, null, count);
    }

    private static MachineItem fromMap(Map<String, Object> json, int fallback) {
        int count = json.containsKey("count") ? number(json.get("count")) : fallback;
        if (json.get("item") != null && json.get("tag") == null) return checked(text(json.get("item")), null, count);
        if (json.get("tag") != null && json.get("item") == null) return checked(null, text(json.get("tag")), count);
        throw new IllegalArgumentException("item input needs exactly one of item or tag");
    }

    private static MachineItem checked(String item, String tag, int count) {
        if ((item == null) == (tag == null)) throw new IllegalArgumentException("item input needs exactly one of item or tag");
        if (count < 1) throw new IllegalArgumentException("count must be at least 1");
        return new MachineItem(item, tag, count);
    }

    private static MachineResult result(String item, int count, int chance) {
        if (item == null || item.isBlank() || item.indexOf(':') < 0) throw new IllegalArgumentException("item");
        if (count < 1) throw new IllegalArgumentException("count must be at least 1");
        if (chance < 1 || chance > 10000) throw new IllegalArgumentException("chance must be from 1 to 10000");
        return new MachineResult(item, count, chance);
    }

    private static MachineFluid fluid(String fluid, long amount) {
        if (fluid == null || fluid.isBlank() || fluid.indexOf(':') < 0) throw new IllegalArgumentException("fluid");
        if (amount < 1) throw new IllegalArgumentException("amount must be at least 1");
        return new MachineFluid(fluid, amount);
    }

    private static Optional<String> optional(String value) { return value == null ? Optional.empty() : Optional.of(value); }

    static Map<String, Object> map(Object value) {
        Object unwrapped = unwrap(value);
        if (unwrapped instanceof Map<?, ?> raw) {
            var out = new LinkedHashMap<String, Object>();
            for (var entry : raw.entrySet()) out.put(String.valueOf(entry.getKey()), unwrap(entry.getValue()));
            return out;
        }
        if (unwrapped instanceof JsonObject json) {
            var out = new LinkedHashMap<String, Object>();
            for (var entry : json.entrySet()) out.put(entry.getKey(), entry.getValue().isJsonPrimitive() ? entry.getValue().getAsString() : entry.getValue());
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
        if (unwrapped instanceof com.google.gson.JsonPrimitive primitive && primitive.isString()) return primitive.getAsString();
        throw new IllegalArgumentException("expected text");
    }

    static int number(Object value) {
        Object unwrapped = unwrap(value);
        if (unwrapped instanceof Number number) return number.intValue();
        if (unwrapped instanceof com.google.gson.JsonPrimitive primitive && primitive.isNumber()) return primitive.getAsInt();
        throw new IllegalArgumentException("expected a number");
    }
}
