package com.gregtech.gregtech.integration.kubejs;

import com.gregtech.gregtech.api.kubejs.StructureLayoutDocuments;
import com.gregtech.gregtech.api.recipe.data.FluidComponent;
import com.gregtech.gregtech.api.recipe.data.ItemComponent;
import com.gregtech.gregtech.api.recipe.data.ItemResult;
import com.gregtech.gregtech.api.recipe.data.MachineRecipeScript;
import dev.latvian.mods.rhino.Wrapper;

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
        return MachineRecipeScript.removal(map,
                items(json.get("itemInputs"), json.get("item_inputs"), 0),
                fluids(json.get("fluidInputs"), json.get("fluid_inputs")),
                results(json.get("itemOutputs"), json.get("item_outputs")),
                fluids(json.get("fluidOutputs"), json.get("fluid_outputs")));
    }

    static List<StructureLayoutDocuments.CellSpec> cells(Object value) {
        var out = new ArrayList<StructureLayoutDocuments.CellSpec>();
        for (Object element : list(value)) {
            var cell = object(element);
            out.add(new StructureLayoutDocuments.CellSpec(
                    number(cell.get("right")), number(cell.get("up")), number(cell.get("back")),
                    text(cell.get("match")), text(cell.get("role")),
                    cell.containsKey("design") ? number(cell.get("design")) : -1));
        }
        return out;
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

    private static FluidComponent fluid(Object value) {
        Object unwrapped = unwrap(value);
        if (unwrapped instanceof CharSequence text) return MachineRecipeScript.fluid(text.toString(), 0);
        var json = object(unwrapped);
        long amount = json.containsKey("amount") ? ((Number) unwrap(json.get("amount"))).longValue() : 0;
        return MachineRecipeScript.fluid(text(json.get("fluid")), amount);
    }

    private static Object first(Object camel, Object snake) { return camel != null ? camel : snake; }

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
