/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import gregtech.api.addon.AddonNames;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reads and writes the shared machine-recipe JSON objects. The same field names are used by the
 * datapack readers, the KubeJS schema and {@link MachineRecipeDocument}.
 *
 * <p>Every entry names its identity exactly once: {@code item}, {@code tag}, or a
 * {@code material} plus {@code form} pair. Outputs additionally carry {@code count} and
 * {@code chance}. Item and fluid outputs may use a tag; a tag output resolves to the
 * alphabetically first member when the host loads the row.</p>
 */
public final class MachineRecipeDocuments {
    private static final Set<String> ROOT_ADDITION = Set.of(
            MachineRecipeFields.TYPE_ADDITION,
            MachineRecipeFields.TYPE,
            MachineRecipeFields.MAP,
            MachineRecipeFields.ITEM_INPUTS,
            MachineRecipeFields.FLUID_INPUTS,
            MachineRecipeFields.ITEM_OUTPUTS,
            MachineRecipeFields.FLUID_OUTPUTS,
            MachineRecipeFields.EU_PER_TICK,
            MachineRecipeFields.DURATION,
            MachineRecipeFields.SPECIAL,
            MachineRecipeFields.PRIORITY,
            MachineRecipeFields.MATERIAL_INPUT);
    private static final Set<String> ROOT_REMOVAL = Set.of(
            MachineRecipeFields.TYPE_REMOVAL,
            MachineRecipeFields.TYPE,
            MachineRecipeFields.MAP,
            MachineRecipeFields.CLEAR,
            MachineRecipeFields.ITEM_INPUTS,
            MachineRecipeFields.FLUID_INPUTS,
            MachineRecipeFields.ITEM_OUTPUTS,
            MachineRecipeFields.FLUID_OUTPUTS);
    private static final Set<String> ITEM = Set.of(
            MachineRecipeFields.ITEM,
            MachineRecipeFields.TAG,
            MachineRecipeFields.MATERIAL,
            MachineRecipeFields.FORM,
            MachineRecipeFields.COUNT);
    private static final Set<String> FLUID =
            Set.of(MachineRecipeFields.FLUID, MachineRecipeFields.TAG, MachineRecipeFields.AMOUNT);
    private static final Set<String> RESULT = Set.of(
            MachineRecipeFields.ITEM,
            MachineRecipeFields.TAG,
            MachineRecipeFields.MATERIAL,
            MachineRecipeFields.FORM,
            MachineRecipeFields.COUNT,
            MachineRecipeFields.CHANCE);

    private MachineRecipeDocuments() {}

    /** Parses and validates one addition object. Unknown fields and multiple identities fail. */
    public static MachineRecipeDocument addition(Map<String, Object> json) {
        rejectUnknownRoot(json, ROOT_ADDITION);
        var document = new MachineRecipeDocument(
                map(json),
                items(json, MachineRecipeFields.ITEM_INPUTS, true),
                fluids(json, MachineRecipeFields.FLUID_INPUTS, true),
                results(json, true),
                fluids(json, MachineRecipeFields.FLUID_OUTPUTS, true),
                requiredLong(json, MachineRecipeFields.EU_PER_TICK),
                requiredLong(json, MachineRecipeFields.DURATION),
                optionalLong(json, MachineRecipeFields.SPECIAL),
                json.containsKey(MachineRecipeFields.MATERIAL_INPUT)
                        ? material(object(json.get(MachineRecipeFields.MATERIAL_INPUT)))
                        : null,
                optionalLong(json, MachineRecipeFields.PRIORITY));
        for (var input : document.itemInputs()) if (input.count() < 1) throw new IllegalArgumentException("count");
        for (var input : document.fluidInputs()) if (input.amount() < 0) throw new IllegalArgumentException("amount");
        for (var output : document.itemOutputs()) {
            if (output.count() < 1) throw new IllegalArgumentException("count");
            if (output.chance() < 1) throw new IllegalArgumentException("chance");
        }
        for (var output : document.fluidOutputs())
            if (output.amount() < 1) throw new IllegalArgumentException("amount");
        return document;
    }

    /** Parses and validates one removal object. Unknown fields and multiple identities fail. */
    public static MachineRecipeRemovalDocument removal(Map<String, Object> json) {
        rejectUnknownRoot(json, ROOT_REMOVAL);
        boolean clear = optionalBoolean(json);
        return new MachineRecipeRemovalDocument(
                map(json),
                clear,
                items(json, MachineRecipeFields.ITEM_INPUTS, false),
                fluids(json, MachineRecipeFields.FLUID_INPUTS, false),
                results(json, false),
                fluids(json, MachineRecipeFields.FLUID_OUTPUTS, false));
    }

    public static String canonical(MachineRecipeDocument document) {
        var json = new Out();
        json.string(MachineRecipeFields.MAP, document.map());
        json.raw(
                MachineRecipeFields.ITEM_INPUTS,
                array(document.itemInputs().stream()
                        .map(value -> itemJson(value, true))
                        .toList()));
        json.raw(
                MachineRecipeFields.FLUID_INPUTS,
                array(document.fluidInputs().stream()
                        .map(value -> fluidJson(value, true))
                        .toList()));
        json.raw(
                MachineRecipeFields.ITEM_OUTPUTS,
                array(document.itemOutputs().stream()
                        .map(value -> resultJson(value, true))
                        .toList()));
        json.raw(
                MachineRecipeFields.FLUID_OUTPUTS,
                array(document.fluidOutputs().stream()
                        .map(value -> fluidJson(value, true))
                        .toList()));
        json.number(MachineRecipeFields.EU_PER_TICK, document.euPerTick());
        json.number(MachineRecipeFields.DURATION, document.duration());
        json.number(MachineRecipeFields.SPECIAL, document.special());
        if (document.priority() != 0) json.number(MachineRecipeFields.PRIORITY, document.priority());
        if (document.materialInput() != null)
            json.raw(MachineRecipeFields.MATERIAL_INPUT, materialJson(document.materialInput()));
        return json.finish();
    }

    public static String canonical(MachineRecipeRemovalDocument document) {
        var json = new Out();
        json.string(MachineRecipeFields.MAP, document.map());
        json.bool(MachineRecipeFields.CLEAR, document.clear());
        json.raw(
                MachineRecipeFields.ITEM_INPUTS,
                array(document.itemInputs().stream()
                        .map(value -> itemJson(value, false))
                        .toList()));
        json.raw(
                MachineRecipeFields.FLUID_INPUTS,
                array(document.fluidInputs().stream()
                        .map(value -> fluidJson(value, false))
                        .toList()));
        json.raw(
                MachineRecipeFields.ITEM_OUTPUTS,
                array(document.itemOutputs().stream()
                        .map(value -> resultJson(value, false))
                        .toList()));
        json.raw(
                MachineRecipeFields.FLUID_OUTPUTS,
                array(document.fluidOutputs().stream()
                        .map(value -> fluidJson(value, false))
                        .toList()));
        return json.finish();
    }

    /** Parses and validates the {@code material_input} object. */
    public static MaterialUnitIngredient material(Map<String, Object> json) {
        rejectUnknown(json, Set.of(
                MachineRecipeFields.MATERIAL,
                "units",
                "forms",
                "rounding"));
        Object rawForms = json.get("forms");
        if (!(rawForms instanceof List<?> list)) throw new IllegalArgumentException("Material forms");
        var forms = new ArrayList<String>();
        for (Object form : list) {
            if (!(form instanceof String name) || !AddonNames.matches(AddonNames.MATERIAL, name))
                throw new IllegalArgumentException("Material form name");
            forms.add(name);
        }
        String rounding = optionalString(json, "rounding");
        return new MaterialUnitIngredient(
                requiredString(json, MachineRecipeFields.MATERIAL),
                MaterialUnitAmount.parse(requiredString(json, "units")),
                forms,
                MaterialUnitIngredient.Rounding.valueOf(
                        (rounding == null ? "whole_outputs" : rounding).toUpperCase(Locale.ROOT)));
    }

    public static String materialJson(MaterialUnitIngredient material) {
        var json = new Out();
        json.string(MachineRecipeFields.MATERIAL, material.material());
        json.string("units", MaterialUnitAmount.format(material.amount()));
        json.string("rounding", material.rounding().name().toLowerCase(Locale.ROOT));
        var forms = new ArrayList<String>();
        for (String form : material.forms()) {
            if (!AddonNames.matches(AddonNames.MATERIAL, form)) throw new IllegalArgumentException("Material form name");
            forms.add("\"" + form + "\"");
        }
        json.raw("forms", array(forms));
        return json.finish();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException("Expected material input object");
        return (Map<String, Object>) map;
    }

    private static String map(Map<String, Object> json) {
        var map = requiredString(json, MachineRecipeFields.MAP);
        checkMap(map);
        return map;
    }

    private static List<ItemComponent> items(Map<String, Object> json, String key, boolean addition) {
        var out = new ArrayList<ItemComponent>();
        for (var element : objects(json, key)) {
            rejectUnknown(element, ITEM);
            var identity = identity(element);
            int count = addition ? count(element, 1) : count(element, 0);
            out.add(new ItemComponent(identity.item, identity.tag, identity.material, identity.form, count));
        }
        return out;
    }

    private static List<FluidComponent> fluids(Map<String, Object> json, String key, boolean addition) {
        var out = new ArrayList<FluidComponent>();
        for (var element : objects(json, key)) {
            rejectUnknown(element, FLUID);
            var fluid = optionalString(element, MachineRecipeFields.FLUID);
            var tag = optionalString(element, MachineRecipeFields.TAG);
            if (fluid != null) checkId(fluid, MachineRecipeFields.FLUID);
            if (tag != null) checkId(tag, MachineRecipeFields.TAG);
            long amount = addition ? requiredLong(element, MachineRecipeFields.AMOUNT) : optionalAmount(element);
            if (amount < 0) throw new IllegalArgumentException("amount");
            if (amount > MachineRecipeFields.MAX_AMOUNT) throw new IllegalArgumentException("amount");
            out.add(new FluidComponent(fluid, tag, amount));
        }
        return out;
    }

    private static List<ItemResult> results(Map<String, Object> json, boolean addition) {
        var out = new ArrayList<ItemResult>();
        for (var element : objects(json, MachineRecipeFields.ITEM_OUTPUTS)) {
            rejectUnknown(element, RESULT);
            var identity = identity(element);
            int count = addition ? count(element, 1) : count(element, 0);
            int chance = addition
                    ? (element.containsKey(MachineRecipeFields.CHANCE)
                            ? requiredInt(element, MachineRecipeFields.CHANCE)
                            : MachineRecipeFields.CHANCE_CERTAIN)
                    : (element.containsKey(MachineRecipeFields.CHANCE)
                            ? requiredInt(element, MachineRecipeFields.CHANCE)
                            : 0);
            if (addition && chance < 1) throw new IllegalArgumentException("chance");
            out.add(new ItemResult(
                    identity.item, identity.tag, identity.material, identity.form, count, chance));
        }
        return out;
    }

    private record Identity(String item, String tag, String material, String form) {}

    private static Identity identity(Map<String, Object> json) {
        var item = optionalString(json, MachineRecipeFields.ITEM);
        var tag = optionalString(json, MachineRecipeFields.TAG);
        var material = optionalString(json, MachineRecipeFields.MATERIAL);
        var form = optionalString(json, MachineRecipeFields.FORM);
        boolean materialPair = material != null || form != null;
        int identities = (item == null ? 0 : 1) + (tag == null ? 0 : 1) + (materialPair ? 1 : 0);
        if (identities != 1)
            throw new IllegalArgumentException("item component needs exactly one of item, tag or material+form");
        if (materialPair) {
            if (material == null) throw new IllegalArgumentException(MachineRecipeFields.MATERIAL + " is required");
            if (form == null) throw new IllegalArgumentException(MachineRecipeFields.FORM + " is required");
            checkName(material, "material");
            checkName(form, "form");
        } else {
            if (item != null) checkId(item, MachineRecipeFields.ITEM);
            if (tag != null) checkId(tag, MachineRecipeFields.TAG);
        }
        return new Identity(item, tag, material, form);
    }

    private static int count(Map<String, Object> json, int fallback) {
        if (!json.containsKey(MachineRecipeFields.COUNT)) return fallback;
        int count = requiredInt(json, MachineRecipeFields.COUNT);
        if (count < 1) throw new IllegalArgumentException("count");
        return count;
    }

    private static long optionalAmount(Map<String, Object> json) {
        if (!json.containsKey(MachineRecipeFields.AMOUNT)) return 0;
        long amount = requiredLong(json, MachineRecipeFields.AMOUNT);
        if (amount < 1) throw new IllegalArgumentException("amount");
        return amount;
    }

    private static boolean optionalBoolean(Map<String, Object> json) {
        var value = json.get(MachineRecipeFields.CLEAR);
        if (value == null) return false;
        if (value instanceof Boolean flag) return flag;
        throw new IllegalArgumentException("clear must be a boolean");
    }

    private static List<Map<String, Object>> objects(Map<String, Object> json, String key) {
        var value = json.get(key);
        if (value == null) return List.of();
        if (!(value instanceof List<?> list)) throw new IllegalArgumentException(key + " must be a list");
        var out = new ArrayList<Map<String, Object>>();
        for (var element : list) {
            if (!(element instanceof Map<?, ?> raw)) throw new IllegalArgumentException(key + " entries must be objects");
            var copy = new LinkedHashMap<String, Object>();
            for (var entry : raw.entrySet()) {
                if (!(entry.getKey() instanceof String name)) throw new IllegalArgumentException(key + " key");
                copy.put(name, entry.getValue());
            }
            out.add(copy);
        }
        return out;
    }

    /** Root keys starting with {@code _} are annotations, such as {@code _comment} or KubeJS 7's {@code _kubejs_changed_marker}. */
    private static void rejectUnknownRoot(Map<String, Object> json, Set<String> allowed) {
        for (var key : json.keySet())
            if (!key.startsWith("_") && !allowed.contains(key))
                throw new IllegalArgumentException("Unknown field " + key);
    }

    private static void rejectUnknown(Map<String, Object> json, Set<String> allowed) {
        for (var key : json.keySet()) if (!allowed.contains(key)) throw new IllegalArgumentException("Unknown field " + key);
    }

    private static String requiredString(Map<String, Object> json, String key) {
        var value = optionalString(json, key);
        if (value == null) throw new IllegalArgumentException(key + " is required");
        return value;
    }

    private static String optionalString(Map<String, Object> json, String key) {
        var value = json.get(key);
        if (value == null) return null;
        if (value instanceof String text && !text.isBlank()) return text;
        throw new IllegalArgumentException(key + " must be a string");
    }

    private static int requiredInt(Map<String, Object> json, String key) {
        long value = requiredLong(json, key);
        // A narrowing cast would silently wrap negative values into valid counts.
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) throw new IllegalArgumentException(key);
        return (int) value;
    }

    private static long optionalLong(Map<String, Object> json, String key) {
        return json.containsKey(key) ? requiredLong(json, key) : 0;
    }

    private static long requiredLong(Map<String, Object> json, String key) {
        var value = json.get(key);
        if (!(value instanceof Number number)) throw new IllegalArgumentException(key + " must be a number");
        double wide = number.doubleValue();
        if (!Double.isFinite(wide) || wide != Math.rint(wide))
            throw new IllegalArgumentException(key + " must be an integer");
        return number.longValue();
    }

    static void checkName(String name, String field) {
        if (!AddonNames.matches(AddonNames.MATERIAL, name)) throw new IllegalArgumentException(field);
    }

    /** Recipe map ids are internal names such as {@code gt.recipe.crusher}, not registry ids. */
    static void checkMap(String id) {
        if (id.isEmpty()) throw new IllegalArgumentException("map");
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.';
            if (!ok) throw new IllegalArgumentException("map has an illegal character");
        }
    }

    static void checkId(String id, String field) {
        int colon = id.indexOf(':');
        if (colon < 1 || colon != id.lastIndexOf(':') || colon == id.length() - 1)
            throw new IllegalArgumentException(field + " must be namespace:path");
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c == ':') continue;
            boolean ok =
                    (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '-' || c == '/';
            if (!ok) throw new IllegalArgumentException(field + " has an illegal character");
        }
    }

    private static String itemJson(ItemComponent value, boolean addition) {
        var json = new Out();
        if (value.item() != null) json.string(MachineRecipeFields.ITEM, value.item());
        else if (value.tag() != null) json.string(MachineRecipeFields.TAG, value.tag());
        else {
            json.string(MachineRecipeFields.MATERIAL, value.material());
            json.string(MachineRecipeFields.FORM, value.form());
        }
        if (addition || value.count() != 0) json.number(MachineRecipeFields.COUNT, value.count());
        return json.finish();
    }

    private static String fluidJson(FluidComponent value, boolean addition) {
        var json = new Out();
        if (value.fluid() != null) json.string(MachineRecipeFields.FLUID, value.fluid());
        else json.string(MachineRecipeFields.TAG, value.tag());
        if (addition || value.amount() != 0) json.number(MachineRecipeFields.AMOUNT, value.amount());
        return json.finish();
    }

    private static String resultJson(ItemResult value, boolean addition) {
        var json = new Out();
        if (value.item() != null) json.string(MachineRecipeFields.ITEM, value.item());
        else if (value.tag() != null) json.string(MachineRecipeFields.TAG, value.tag());
        else {
            json.string(MachineRecipeFields.MATERIAL, value.material());
            json.string(MachineRecipeFields.FORM, value.form());
        }
        if (addition || value.count() != 0) json.number(MachineRecipeFields.COUNT, value.count());
        if (addition || value.chance() != 0) json.number(MachineRecipeFields.CHANCE, value.chance());
        return json.finish();
    }

    private static String array(List<String> values) {
        var json = new StringBuilder().append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) json.append(',');
            json.append(values.get(i));
        }
        return json.append(']').toString();
    }

    private static final class Out {
        private final StringBuilder json = new StringBuilder().append('{');
        private boolean pending = true;

        private void comma() {
            if (!pending) json.append(',');
            pending = false;
        }

        private void string(String key, String value) {
            comma();
            json.append('"').append(key).append("\":");
            json.append('"');
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '"' -> json.append("\\\"");
                    case '\\' -> json.append("\\\\");
                    case '\n' -> json.append("\\n");
                    case '\r' -> json.append("\\r");
                    case '\t' -> json.append("\\t");
                    default -> json.append(c);
                }
            }
            json.append('"');
        }

        private void number(String key, long value) {
            comma();
            json.append('"').append(key).append("\":").append(value);
        }

        private void bool(String key, boolean value) {
            comma();
            json.append('"').append(key).append("\":").append(value);
        }

        private void raw(String key, String value) {
            comma();
            json.append('"').append(key).append("\":").append(value);
        }

        private String finish() {
            return json.append('}').toString();
        }
    }
}
