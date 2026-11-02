package gregtech.engine.kubejs;

import gregtech.api.machine.MachineFace;
import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.LayoutCell;
import gregtech.api.multiblock.PortChannel;
import gregtech.api.multiblock.PortRule;
import gregtech.api.multiblock.StructureConstraint;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructurePorts;
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
    private static final Set<String> ROOT = Set.of("cells", "repeats", "constraints");
    private static final Set<String> CELL = Set.of("right", "up", "back", "match", "role", "design", "ports");
    private static final Set<String> REPEAT = Set.of("key", "up", "min", "max", "step", "cells");
    private static final Set<String> COUNT = Set.of("type", "match", "role", "min", "max");
    private static final Set<String> SAME_BLOCK = Set.of("type", "match", "role");

    private StructureLayoutDocuments() {}

    /** One cell before it is checked against {@link StructureLayout}. {@code matches} has one entry or an alternative group. */
    public record CellSpec(
            int right, int up, int back, List<String> matches, String role, int design, StructurePorts ports) {
        public CellSpec(int right, int up, int back, List<String> matches, String role, int design) {
            this(right, up, back, matches, role, design, StructurePorts.DEFAULT);
        }
    }

    /** One repeat group. Its cells use {@code up} relative to the anchor. */
    public record RepeatSpec(String key, int up, int min, int max, int step, List<CellSpec> cells) {}

    public static StructureLayout parse(String machine, String json) {
        return parse(machine, JsonMaps.object(json));
    }

    /** Scripts and data packs share validation; neither adapter may silently discard new fields. */
    public static StructureLayout parse(String machine, Map<String, Object> root) {
        for (var key : root.keySet())
            if (!key.startsWith("_") && !ROOT.contains(key)) throw new IllegalArgumentException("Unknown field " + key);
        return layout(machine, cells(root), repeats(root), constraints(root));
    }

    public static StructureLayout layout(String machine, List<CellSpec> cells) {
        return layout(machine, cells, List.of());
    }

    public static StructureLayout layout(String machine, List<CellSpec> cells, List<RepeatSpec> repeats) {
        return layout(machine, cells, repeats, List.of());
    }

    private static StructureLayout layout(
            String machine, List<CellSpec> cells, List<RepeatSpec> repeats, List<StructureConstraint> constraints) {
        var builder = StructureLayout.builder(machine);
        for (CellSpec cell : cells) {
            var match = match(cell.matches());
            var part = role(cell.role());
            builder.cell(
                    new LayoutCell(cell.right(), cell.up(), cell.back(), match, part, cell.design(), cell.ports()));
        }
        for (RepeatSpec repeat : repeats) {
            var group =
                    gregtech.api.multiblock.LayoutRepeat.builder(repeat.key(), repeat.up(), repeat.min(), repeat.max());
            if (repeat.step() != 1) group.step(repeat.step());
            for (CellSpec cell : repeat.cells()) {
                var match = match(cell.matches());
                var part = role(cell.role());
                group.cell(
                        new LayoutCell(cell.right(), cell.up(), cell.back(), match, part, cell.design(), cell.ports()));
            }
            builder.repeat(group.build());
        }
        constraints.forEach(builder::require);
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
        if (!layout.constraints().isEmpty()) {
            json.append(",\"constraints\":[");
            for (int i = 0; i < layout.constraints().size(); i++) {
                if (i > 0) json.append(',');
                var constraint = layout.constraints().get(i);
                json.append("{\"type\":");
                string(json, constraint instanceof StructureConstraint.Count ? "count" : "same_block");
                var selection = constraint.selection();
                if (selection.match() != null) {
                    json.append(",\"match\":");
                    matchText(json, selection.match());
                }
                if (selection.role() != null) {
                    json.append(",\"role\":");
                    string(json, selection.role().name().toLowerCase(Locale.ROOT));
                }
                if (constraint instanceof StructureConstraint.Count count)
                    json.append(",\"min\":")
                            .append(count.min())
                            .append(",\"max\":")
                            .append(count.max());
                json.append('}');
            }
            json.append(']');
        }
        return json.append('}').toString();
    }

    private static List<StructureConstraint> constraints(Map<String, Object> root) {
        if (!root.containsKey("constraints")) return List.of();
        if (!(root.get("constraints") instanceof List<?> list))
            throw new IllegalArgumentException("constraints must be an array");
        var rules = new ArrayList<StructureConstraint>();
        for (var element : list) {
            var rule = object(element, "constraints entries must be objects");
            String type = required(rule, "type");
            var keys = switch (type) {
                case "count" -> COUNT;
                case "same_block" -> SAME_BLOCK;
                default -> throw new IllegalArgumentException("Unknown constraint type: " + type);
            };
            for (String key : rule.keySet())
                if (!keys.contains(key)) throw new IllegalArgumentException("Unknown constraint field " + key);
            var selection = new StructureConstraint.Selection(
                    rule.containsKey("match") ? match(matches(rule.get("match"))) : null,
                    rule.containsKey("role") ? role(required(rule, "role")) : null);
            rules.add(
                    type.equals("count")
                            ? new StructureConstraint.Count(selection, integer(rule, "min"), integer(rule, "max"))
                            : new StructureConstraint.SameBlock(selection));
        }
        return List.copyOf(rules);
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
            ports(json, cell.ports());
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
                    design(cell),
                    ports(cell)));
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

    private static StructurePorts ports(Map<String, Object> cell) {
        if (!cell.containsKey("ports")) return StructurePorts.DEFAULT;
        var builder = StructurePorts.builder();
        var rules = object(cell.get("ports"), "ports must be an object");
        for (var entry : rules.entrySet()) {
            PortChannel channel = PortChannel.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
            var rule = object(entry.getValue(), "port rule must be an object");
            for (String key : rule.keySet())
                if (!Set.of("faces", "slots", "filters").contains(key))
                    throw new IllegalArgumentException("Unknown port rule field " + key);
            var faces = rule.containsKey("faces")
                    ? strings(rule.get("faces")).stream()
                            .map(s -> MachineFace.valueOf(s.toUpperCase(Locale.ROOT)))
                            .toList()
                    : MachineFace.all();
            var slots = new ArrayList<Integer>();
            if (rule.containsKey("slots")) {
                if (!(rule.get("slots") instanceof List<?> list))
                    throw new IllegalArgumentException("slots must be an array");
                for (var slot : list) slots.add(integer(Map.of("slot", slot), "slot"));
            }
            builder.restrict(
                    channel,
                    new PortRule(faces, slots, rule.containsKey("filters") ? strings(rule.get("filters")) : List.of()));
        }
        return builder.build();
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof List<?> list)) throw new IllegalArgumentException("Expected string array");
        var result = new ArrayList<String>();
        for (var entry : list) {
            if (!(entry instanceof String text)) throw new IllegalArgumentException("Expected string array");
            result.add(text);
        }
        return result;
    }

    private static void ports(StringBuilder json, StructurePorts ports) {
        if (ports.rules().isEmpty()) return;
        json.append(",\"ports\":{");
        boolean comma = false;
        for (var channel : PortChannel.values()) {
            var rule = ports.rules().get(channel);
            if (rule == null) continue;
            if (comma) json.append(',');
            comma = true;
            string(json, channel.name().toLowerCase(Locale.ROOT));
            json.append(":{\"faces\":[");
            for (int i = 0; i < rule.faces().size(); i++) {
                if (i > 0) json.append(',');
                string(json, rule.faces().get(i).name().toLowerCase(Locale.ROOT));
            }
            json.append("],\"slots\":").append(rule.slots()).append(",\"filters\":[");
            for (int i = 0; i < rule.filters().size(); i++) {
                if (i > 0) json.append(',');
                string(json, rule.filters().get(i));
            }
            json.append("]}");
        }
        json.append('}');
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
