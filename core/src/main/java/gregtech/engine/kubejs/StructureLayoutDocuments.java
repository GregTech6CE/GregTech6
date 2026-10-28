package gregtech.engine.kubejs;

import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;
import gregtech.engine.recipe.data.JsonMaps;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Reads and writes {@code data/<namespace>/gt6ce_layouts/<machineType>.json}. */
public final class StructureLayoutDocuments {
    private static final Set<String> ROOT = Set.of("cells", "repeats");
    private static final Set<String> CELL = Set.of("right", "up", "back", "match", "role", "design");
    private static final Set<String> REPEAT = Set.of("key", "up", "min", "max", "step", "cells");

    private StructureLayoutDocuments() {}

    /** One cell before it is checked against {@link StructureLayout}. {@code matches} has one entry or an alternative group. */
    public record CellSpec(int right, int up, int back, List<String> matches, String role, int design) {}

    /** One repeat group. Its cells use {@code up} relative to the anchor. */
    public record RepeatSpec(String key, int up, int min, int max, int step, List<CellSpec> cells) {}

    public static StructureLayout parse(String machine, String json) {
        var root = JsonMaps.object(json);
        for (var key : root.keySet())
            if (!key.startsWith("_") && !ROOT.contains(key)) throw new IllegalArgumentException("Unknown field " + key);
        return layout(machine, cells(root), repeats(root));
    }

    public static StructureLayout layout(String machine, List<CellSpec> cells) {
        return layout(machine, cells, List.of());
    }

    public static StructureLayout layout(String machine, List<CellSpec> cells, List<RepeatSpec> repeats) {
        var builder = StructureLayout.builder(machine);
        for (CellSpec cell : cells) {
            var match = match(cell.matches());
            var part = role(cell.role());
            if (cell.design() < 0) builder.cell(cell.right(), cell.up(), cell.back(), match, part);
            else builder.cell(cell.right(), cell.up(), cell.back(), match, part, cell.design());
        }
        for (RepeatSpec repeat : repeats) {
            var group =
                    gregtech.api.multiblock.LayoutRepeat.builder(repeat.key(), repeat.up(), repeat.min(), repeat.max());
            if (repeat.step() != 1) group.step(repeat.step());
            for (CellSpec cell : repeat.cells()) {
                var match = match(cell.matches());
                var part = role(cell.role());
                if (cell.design() < 0) group.cell(cell.right(), cell.up(), cell.back(), match, part);
                else group.cell(cell.right(), cell.up(), cell.back(), match, part, cell.design());
            }
            builder.repeat(group.build());
        }
        return builder.build();
    }

    public static String canonical(StructureLayout layout) {
        var json = new StringBuilder().append("{\"cells\":[");
        cells(json, layout.cells());
        json.append(']');
        var repeats = layout.repeats();
        if (!repeats.isEmpty()) {
            json.append(",\"repeats\":[");
            for (int i = 0; i < repeats.size(); i++) {
                if (i > 0) json.append(',');
                var repeat = repeats.get(i);
                json.append("{\"key\":");
                string(json, repeat.key());
                json.append(",\"up\":")
                        .append(repeat.up())
                        .append(",\"min\":")
                        .append(repeat.min())
                        .append(",\"max\":")
                        .append(repeat.max());
                if (repeat.step() != 1) json.append(",\"step\":").append(repeat.step());
                json.append(",\"cells\":[");
                cells(json, repeat.cells());
                json.append("]}");
            }
            json.append(']');
        }
        return json.append('}').toString();
    }

    private static void cells(StringBuilder json, List<gregtech.api.multiblock.LayoutCell> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) json.append(',');
            var cell = cells.get(i);
            json.append("{\"right\":")
                    .append(cell.right())
                    .append(",\"up\":")
                    .append(cell.up())
                    .append(",\"back\":")
                    .append(cell.back())
                    .append(",\"match\":");
            matchText(json, cell.match());
            json.append(",\"role\":");
            string(json, cell.role().name().toLowerCase(Locale.ROOT));
            if (cell.design() >= 0) json.append(",\"design\":").append(cell.design());
            json.append('}');
        }
    }

    private static List<CellSpec> cells(Map<String, Object> json) {
        var value = json.get("cells");
        if (value == null) {
            if (json.containsKey("repeats")) return List.of();
            throw new IllegalArgumentException("cells");
        }
        if (value instanceof List<?> list && list.isEmpty() && json.containsKey("repeats")) return List.of();
        return cellList(value, "cells");
    }

    private static List<RepeatSpec> repeats(Map<String, Object> json) {
        var value = json.get("repeats");
        if (value == null) return List.of();
        if (!(value instanceof List<?> list) || list.isEmpty()) throw new IllegalArgumentException("repeats");
        var out = new ArrayList<RepeatSpec>();
        for (var element : list) {
            var repeat = object(element, "repeats");
            for (var key : repeat.keySet())
                if (!REPEAT.contains(key)) throw new IllegalArgumentException("Unknown field " + key);
            var cellsValue = repeat.get("cells");
            if (cellsValue == null) throw new IllegalArgumentException("cells is required");
            out.add(new RepeatSpec(
                    required(repeat, "key"),
                    integer(repeat, "up"),
                    integer(repeat, "min"),
                    integer(repeat, "max"),
                    step(repeat),
                    cellList(cellsValue, "repeats.cells")));
        }
        return out;
    }

    private static List<CellSpec> cellList(Object value, String field) {
        if (!(value instanceof List<?> list) || list.isEmpty()) throw new IllegalArgumentException(field);
        var out = new ArrayList<CellSpec>();
        for (var element : list) {
            var cell = object(element, field + " entries must be objects");
            for (var key : cell.keySet())
                if (!CELL.contains(key)) throw new IllegalArgumentException("Unknown field " + key);
            out.add(new CellSpec(
                    integer(cell, "right"),
                    integer(cell, "up"),
                    integer(cell, "back"),
                    matches(cell.get("match")),
                    required(cell, "role"),
                    design(cell)));
        }
        return out;
    }

    private static Map<String, Object> object(Object value, String message) {
        if (!(value instanceof Map<?, ?> raw)) throw new IllegalArgumentException(message);
        var out = new LinkedHashMap<String, Object>();
        for (var entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String name)) throw new IllegalArgumentException(message);
            out.put(name, entry.getValue());
        }
        return out;
    }

    private static int step(Map<String, Object> json) {
        if (!json.containsKey("step")) return 1;
        int value = integer(json, "step");
        if (value != 1 && value != -1) throw new IllegalArgumentException("step");
        return value;
    }

    private static CellMatch match(List<String> matches) {
        if (matches.size() == 1) return CellMatch.parse(matches.get(0));
        var members = new ArrayList<CellMatch>();
        for (String value : matches) members.add(CellMatch.parse(value));
        return CellMatch.oneOf(members);
    }

    /** One match string, or an array of alternatives. */
    private static List<String> matches(Object value) {
        if (value instanceof String text && !text.isBlank()) return List.of(text);
        if (!(value instanceof List<?> list) || list.isEmpty()) throw new IllegalArgumentException("match is required");
        var out = new ArrayList<String>();
        for (Object element : list) {
            if (!(element instanceof String text) || text.isBlank())
                throw new IllegalArgumentException("match entries must be text");
            out.add(text);
        }
        return List.copyOf(out);
    }

    private static void matchText(StringBuilder json, CellMatch match) {
        if (match instanceof CellMatch.OneOf one) {
            json.append('[');
            for (int i = 0; i < one.members().size(); i++) {
                if (i > 0) json.append(',');
                string(json, one.members().get(i).toString());
            }
            json.append(']');
            return;
        }
        string(json, match.toString());
    }

    private static StructureRole role(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("role");
        try {
            return StructureRole.valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Unknown structure role: " + text);
        }
    }

    private static String required(Map<String, Object> json, String key) {
        var value = json.get(key);
        if (value instanceof String text && !text.isBlank()) return text;
        throw new IllegalArgumentException(key + " is required");
    }

    private static int design(Map<String, Object> json) {
        if (!json.containsKey("design")) return -1;
        int value = integer(json, "design");
        if (value < 0 || value > 255) throw new IllegalArgumentException("design");
        return value;
    }

    private static int integer(Map<String, Object> json, String key) {
        var value = json.get(key);
        if (!(value instanceof Number number)) throw new IllegalArgumentException(key + " must be a number");
        double wide = number.doubleValue();
        if (!Double.isFinite(wide) || wide != Math.rint(wide) || wide > Integer.MAX_VALUE || wide < Integer.MIN_VALUE)
            throw new IllegalArgumentException(key + " must be an integer");
        return number.intValue();
    }

    private static void string(StringBuilder json, String value) {
        json.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                default -> json.append(c);
            }
        }
        json.append('"');
    }
}
