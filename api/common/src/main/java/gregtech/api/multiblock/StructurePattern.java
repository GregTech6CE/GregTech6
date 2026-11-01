/* SPDX-License-Identifier: LGPL-3.0-or-later */
package gregtech.api.multiblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Horizontal character layers compiled to the existing coordinate API, with no game dependencies.
 * Columns run right, rows run back, and layer numbers run up relative to the controller.
 * Every character, including a space, must be explicitly bound or ignored. The controller occurs once at up=0.
 * Counts constrain declared symbol occurrences over ALL allowed repeat sizes, not the number of
 * world blocks selected from an alternative matcher. World-dependent constraints belong to the host.
 *
 * <pre>{@code
 * StructureLayout.pattern("example_tower") // same name as the controller machineType
 *     .layer(0, "CSC", "CCC")             // rows go front to back, S is the origin
 *     .where('C', CellMatch.part("example_wall"), StructureRole.ITEM_FLUID_IO)
 *     .repeat("walls", 1, 2, 4, "CCC", "CCC")
 *     .count('C', 17, 29)
 *     .build(); // return from AddonMachineContributor.structureLayouts()
 * }</pre>
 */
public final class StructurePattern {
    private record Symbol(CellMatch match, StructureRole role, int design) {}
    private record Layer(int up, List<String> rows) {}
    private record Repeat(String key, int up, int min, int max, int step, List<List<String>> layers) {}
    private record Limit(long min, long max) {}

    private final String name;
    private char controller = 'S';
    private final List<Layer> layers = new ArrayList<>();
    private final List<Repeat> repeats = new ArrayList<>();
    private final Map<Character, Symbol> symbols = new LinkedHashMap<>();
    private final Map<Character, Limit> limits = new LinkedHashMap<>();
    private final java.util.Set<Character> ignored = new java.util.HashSet<>();
    private int width = -1, depth = -1;

    private StructurePattern(String name) { this.name = name; }
    public static StructurePattern builder(String name) { return new StructurePattern(name); }

    public StructurePattern controller(char symbol) { controller = symbol; return this; }

    /** An explicitly unused location, not an air requirement. Useful for rings and open structures. */
    public StructurePattern ignore(char symbol) { ignored.add(symbol); return this; }

    public StructurePattern where(char symbol, CellMatch match, StructureRole role) {
        return where(symbol, match, role, -1);
    }

    public StructurePattern where(char symbol, CellMatch match, StructureRole role, int design) {
        var validated = LayoutCell.relative(0, 0, 0, match, role);
        if (design != -1) validated.withDesign(design);
        if (symbols.putIfAbsent(symbol, new Symbol(match, role, design)) != null)
            throw new IllegalArgumentException("Duplicate symbol: " + symbol);
        return this;
    }

    public StructurePattern layer(int up, String... rows) {
        layers.add(new Layer(up, rows(rows)));
        return this;
    }

    /** Repeat a single horizontal layer upward. Coordinates use the same controller anchor. */
    public StructurePattern repeat(String key, int up, int min, int max, String... rows) {
        return repeat(key, up, min, max, 1, List.of(List.of(rows)));
    }

    /** Repeat a whole group of layers upward (1) or downward (-1), starting at up. */
    public StructurePattern repeat(String key, int up, int min, int max, int step, List<List<String>> layers) {
        if (layers == null || layers.isEmpty()) throw new IllegalArgumentException("Empty repeated layers");
        var copy = new ArrayList<List<String>>();
        for (var layer : layers) copy.add(rows(layer.toArray(String[]::new)));
        repeats.add(new Repeat(key, up, min, max, step, List.copyOf(copy)));
        return this;
    }

    /** Inclusive bounds on a symbol's expanded count; -1 means no upper bound. */
    public StructurePattern count(char symbol, long min, long max) {
        if (min < 0 || max != -1 && max < min) throw new IllegalArgumentException("Invalid symbol count");
        if (limits.putIfAbsent(symbol, new Limit(min, max)) != null)
            throw new IllegalArgumentException("Duplicate count constraint: " + symbol);
        return this;
    }

    private List<String> rows(String[] rows) {
        if (rows == null || rows.length == 0 || rows[0] == null || rows[0].isEmpty())
            throw new IllegalArgumentException("Empty structure layer");
        if (width == -1) { width = rows[0].length(); depth = rows.length; }
        if (rows.length != depth) throw new IllegalArgumentException("Structure layers have different depths");
        for (String row : rows)
            if (row == null || row.length() != width)
                throw new IllegalArgumentException("Structure rows have different widths");
        return List.of(rows);
    }

    public StructureLayout build() {
        if (symbols.containsKey(controller) || ignored.contains(controller))
            throw new IllegalArgumentException("Controller symbol has a cell binding");
        for (char symbol : ignored) if (symbols.containsKey(symbol) || limits.containsKey(symbol))
            throw new IllegalArgumentException("Ignored symbol has a binding or count: " + symbol);
        int originX = 0, originZ = 0, controllers = 0;
        for (var layer : layers) for (int z = 0; z < depth; z++) for (int x = 0; x < width; x++)
            if (layer.rows.get(z).charAt(x) == controller) {
                if (layer.up != 0) throw new IllegalArgumentException("Controller must be in layer zero");
                originX = x; originZ = z; controllers++;
            }
        if (controllers != 1) throw new IllegalArgumentException("Structure needs exactly one controller");
        var result = StructureLayout.builder(name);
        var minimum = new HashMap<Character, Long>();
        var maximum = new HashMap<Character, Long>();
        for (var layer : layers) for (int z = 0; z < depth; z++) for (int x = 0; x < width; x++) {
            char symbol = layer.rows.get(z).charAt(x);
            addCount(minimum, symbol, 1); addCount(maximum, symbol, 1);
            if (symbol == controller || ignored.contains(symbol)) continue;
            result.cell(cell(symbol, x - originX, layer.up, z - originZ));
        }
        for (var repeat : repeats) {
            // LayoutRepeat derives its stride from the last occupied layer. Reject a drawing
            // whose trailing ignored layer would otherwise silently shorten that stride.
            if (repeat.layers.get(repeat.layers.size() - 1).stream()
                    .allMatch(row -> row.chars().allMatch(ch -> ignored.contains((char) ch))))
                throw new IllegalArgumentException("Repeated group ends with an ignored layer: " + repeat.key);
            var group = LayoutRepeat.builder(repeat.key, repeat.up, repeat.min, repeat.max).step(repeat.step);
            for (int y = 0; y < repeat.layers.size(); y++) for (int z = 0; z < depth; z++) for (int x = 0; x < width; x++) {
                char symbol = repeat.layers.get(y).get(z).charAt(x);
                if (symbol == controller) throw new IllegalArgumentException("Controller cannot repeat");
                if (ignored.contains(symbol)) continue;
                group.cell(cell(symbol, x - originX, y, z - originZ));
                addCount(minimum, symbol, repeat.min);
                addCount(maximum, symbol, repeat.max == -1 ? LayoutRepeat.MAX_COPIES : repeat.max);
            }
            result.repeat(group.build());
        }
        limits.forEach((symbol, limit) -> {
            if (symbol != controller && !symbols.containsKey(symbol))
                throw new IllegalArgumentException("Count uses undefined symbol: " + symbol);
            long min = minimum.getOrDefault(symbol, 0L), max = maximum.getOrDefault(symbol, 0L);
            if (min < limit.min || limit.max != -1 && max > limit.max)
                throw new IllegalArgumentException("Symbol '" + symbol + "' expands to " + min + ".." + max
                        + ", outside " + limit.min + ".." + limit.max);
        });
        return result.build();
    }

    private LayoutCell cell(char character, int right, int up, int back) {
        var symbol = symbols.get(character);
        if (symbol == null) throw new IllegalArgumentException("Undefined structure symbol: '" + character + "'");
        var cell = LayoutCell.relative(right, up, back, symbol.match, symbol.role);
        return symbol.design == -1 ? cell : cell.withDesign(symbol.design);
    }

    private static void addCount(Map<Character, Long> counts, char symbol, long amount) {
        counts.merge(symbol, amount, Math::addExact);
    }
}
