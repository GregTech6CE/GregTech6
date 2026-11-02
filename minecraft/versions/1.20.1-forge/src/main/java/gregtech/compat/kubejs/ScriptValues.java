package gregtech.compat.kubejs;

import dev.latvian.mods.rhino.Wrapper;
import gregtech.engine.kubejs.StructureLayoutDocuments;
import gregtech.engine.recipe.data.FluidComponent;
import gregtech.engine.recipe.data.ItemComponent;
import gregtech.engine.recipe.data.ItemResult;
import gregtech.engine.recipe.data.MachineRecipeScript;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads the plain objects a KubeJS script passes to {@code GT6CE}. */
final class ScriptValues {
    private ScriptValues() {}

    static String removal(Object spec) {
        var json = object(spec);
        String map = text(json.get("map"));
        if (Boolean.TRUE.equals(json.get("clear"))) return MachineRecipeScript.clear(map);
        return MachineRecipeScript.removal(
                map,
                items(json.get("itemInputs"), json.get("item_inputs"), 0),
                fluids(json.get("fluidInputs"), json.get("fluid_inputs")),
                results(json.get("itemOutputs"), json.get("item_outputs")),
                fluids(json.get("fluidOutputs"), json.get("fluid_outputs")));
    }

    static gregtech.api.multiblock.StructureLayout layout(String machine, Object spec) {
        Object value = layoutValue(spec);
        var root = value instanceof Map<?, ?> ? object(value) : Map.of("cells", value);
        return StructureLayoutDocuments.parse(machine, root);
    }

    /** Unwrap Rhino containers recursively before entering the common JSON validator. */
    private static Object layoutValue(Object value) {
        Object plain = unwrap(value);
        if (plain instanceof Map<?, ?>) {
            var out = new LinkedHashMap<String, Object>();
            object(plain).forEach((key, entry) -> out.put(key, layoutValue(entry)));
            return out;
        }
        if (plain instanceof List<?> || plain instanceof Object[])
            return list(plain).stream().map(ScriptValues::layoutValue).toList();
        if (plain instanceof CharSequence text) return text.toString();
        if (plain instanceof Number || plain instanceof Boolean || plain == null) return plain;
        throw new IllegalArgumentException(
                "Unsupported layout value: " + plain.getClass().getName());
    }

    private static List<ItemComponent> items(Object camel, Object snake, int count) {
        var values = first(camel, snake);
        if (values == null) return List.of();
        var out = new ArrayList<ItemComponent>();
        for (Object element : list(values)) out.add(item(element, count));
        return out;
    }

    private static List<ItemResult> results(Object camel, Object snake) {
        var values = first(camel, snake);
        if (values == null) return List.of();
        var out = new ArrayList<ItemResult>();
        for (Object element : list(values)) out.add(result(element));
        return out;
    }

    private static List<FluidComponent> fluids(Object camel, Object snake) {
        var values = first(camel, snake);
        if (values == null) return List.of();
        var out = new ArrayList<FluidComponent>();
        for (Object element : list(values)) out.add(fluid(element));
        return out;
    }

    private static ItemComponent item(Object value, int fallback) {
        Object unwrapped = unwrap(value);
        if (unwrapped instanceof CharSequence text) return MachineRecipeScript.item(text.toString(), fallback);
        var json = object(unwrapped);
        int count = json.containsKey("count") ? number(json.get("count")) : fallback;
        if (json.get("item") != null) return new ItemComponent(text(json.get("item")), null, count);
        if (json.get("tag") != null) return new ItemComponent(null, text(json.get("tag")), count);
        throw new IllegalArgumentException("item input needs item or tag");
    }

    private static ItemResult result(Object value) {
        Object unwrapped = unwrap(value);
        if (unwrapped instanceof CharSequence text) return MachineRecipeScript.result(text.toString(), 0, 0);
        var json = object(unwrapped);
        int count = json.containsKey("count") ? number(json.get("count")) : 0;
        int chance = json.containsKey("chance") ? number(json.get("chance")) : 0;
        return MachineRecipeScript.result(text(json.get("item")), count, chance);
    }

    static FluidComponent fluid(Object value) {
        Object unwrapped = unwrap(value);
        if (unwrapped instanceof CharSequence text) return MachineRecipeScript.fluid(text.toString(), 0);
        var json = object(unwrapped);
        long amount = json.containsKey("amount") ? ((Number) unwrap(json.get("amount"))).longValue() : 0;
        return new FluidComponent(
                json.get("fluid") == null ? null : text(json.get("fluid")),
                json.get("tag") == null ? null : text(json.get("tag")),
                amount);
    }

    private static Object first(Object camel, Object snake) {
        return camel != null ? camel : snake;
    }

    private static List<Object> list(Object value) {
        Object unwrapped = unwrap(value);
        if (unwrapped instanceof List<?> values) return new ArrayList<>(values);
        if (unwrapped instanceof Object[] values) return List.of(values);
        throw new IllegalArgumentException("expected a list");
    }

    private static Map<String, Object> object(Object value) {
        Object unwrapped = unwrap(value);
        if (unwrapped instanceof Map<?, ?> map) {
            var out = new LinkedHashMap<String, Object>();
            for (var entry : map.entrySet()) out.put(String.valueOf(entry.getKey()), unwrap(entry.getValue()));
            return out;
        }
        throw new IllegalArgumentException("expected an object");
    }

    private static String text(Object value) {
        Object unwrapped = unwrap(value);
        if (unwrapped instanceof CharSequence text && !text.isEmpty()) return text.toString();
        throw new IllegalArgumentException("expected text");
    }

    private static int number(Object value) {
        Object unwrapped = unwrap(value);
        if (!(unwrapped instanceof Number number)) throw new IllegalArgumentException("expected a number");
        return number.intValue();
    }

    private static Object unwrap(Object value) {
        return value instanceof Wrapper wrapper ? unwrap(wrapper.unwrap()) : value;
    }
}
