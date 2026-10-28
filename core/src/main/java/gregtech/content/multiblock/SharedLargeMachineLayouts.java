package gregtech.content.multiblock;

import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.LayoutRepeat;
import gregtech.api.multiblock.StructureGrid.Role;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;
import java.util.*;

/** Named GT6 recipe-machine patterns, compiled once to coordinates without a Minecraft registry. */
public final class SharedLargeMachineLayouts {
    private SharedLargeMachineLayouts() {}
    /** One accepted block for a cell. {@code plain} is a block id, tag or predicate; null means a part id. */
    public record Choice(int part, String plain) {
        public boolean plainBlock() {
            return plain != null;
        }
    }
    /** {@code plain} is a block id or {@code #} tag. Null means {@code part} is a structure part. */
    /** {@code design} is {@code -1} when the cell does not write {@code gt.design}. */
    /** {@code alternatives} are the extra accepted matches after the primary {@code part}/{@code plain}. */
    public record Cell(
            int right, int up, int back, int part, Role role, String plain, int design, List<Choice> alternatives) {
        public Cell(int right, int up, int back, int part, Role role) {
            this(right, up, back, part, role, null, -1, List.of());
        }

        public Cell(int right, int up, int back, int part, Role role, String plain) {
            this(right, up, back, part, role, plain, -1, List.of());
        }

        public Cell(int right, int up, int back, int part, Role role, String plain, int design) {
            this(right, up, back, part, role, plain, design, List.of());
        }

        public Cell {
            alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
        }
    }
    /** Repeat cells use coordinates relative to the anchor and the repeat direction. */
    public record Repeat(String key, int up, int min, int max, int step, int height, List<Cell> cells) {
        public Repeat {
            if (key == null || !key.matches("[a-z][a-z0-9_]{0,63}"))
                throw new IllegalArgumentException("Invalid repeat key: " + key);
            if (min < 0 || max == 0)
                throw new IllegalArgumentException("Repeat '" + key + "' needs min >= 0 and max > 0 or -1");
            if (max != -1 && max < min)
                throw new IllegalArgumentException("Repeat '" + key + "' needs max >= min or -1");
            if (max > LayoutRepeat.MAX_COPIES)
                throw new IllegalArgumentException("Repeat '" + key + "' max is too large: " + max);
            if (step != 1 && step != -1)
                throw new IllegalArgumentException("Repeat '" + key + "' step must be 1 or -1");
            if (cells == null || cells.isEmpty()) throw new IllegalArgumentException("Repeat '" + key + "' is empty");
            cells = List.copyOf(cells);
        }

        public int absoluteUp(Cell cell, int copy) {
            return up + step * (cell.up() + copy * height);
        }
    }

    public record Layout(List<Cell> cells, List<Repeat> repeats) {
        public Layout {
            cells = cells == null ? List.of() : List.copyOf(cells);
            repeats = repeats == null ? List.of() : List.copyOf(repeats);
        }
    }

    private static final Map<String, List<Cell>> DEFINITIONS = new LinkedHashMap<>(create());
    private static final Map<String, List<Repeat>> REPEATS = new LinkedHashMap<>();
    /** Names present before any addon layout is added. Those layouts are not replaceable. */
    private static final Set<String> BUILTIN = Set.copyOf(DEFINITIONS.keySet());

    private static Map<String, List<Cell>> overlay = Map.of();
    private static Map<String, List<Repeat>> overlayRepeats = Map.of();
    private static boolean frozen;
    /** Fixed cells plus every repeat group expanded to its minimum count, for previews. */
    public static List<Cell> cells(String machine) {
        var base = baseCells(machine);
        if (base == null) return null;
        var repeats = repeats(machine);
        if (repeats.isEmpty()) return base;
        var out = new ArrayList<>(base);
        for (var repeat : repeats)
            for (int copy = 0; copy < repeat.min(); copy++)
                for (var cell : repeat.cells())
                    out.add(new Cell(
                            cell.right(),
                            repeat.absoluteUp(cell, copy),
                            cell.back(),
                            cell.part(),
                            cell.role(),
                            cell.plain(),
                            cell.design()));
        return List.copyOf(out);
    }
    /** Fixed cells without expanding repeats. */
    public static List<Cell> baseCells(String machine) {
        var replaced = overlay.get(machine);
        return replaced != null ? replaced : DEFINITIONS.get(machine);
    }

    public static List<Repeat> repeats(String machine) {
        var replaced = overlayRepeats.get(machine);
        return replaced != null ? replaced : REPEATS.getOrDefault(machine, List.of());
    }
    /** The layout a structure check reads. Null when the machine has no layout. */
    public static Layout layout(String machine) {
        var base = baseCells(machine);
        return base == null ? null : new Layout(base, repeats(machine));
    }
    /** The layout stored at startup, ignoring a datapack replacement. */
    public static List<Cell> baseline(String machine) {
        return DEFINITIONS.get(machine);
    }

    public static boolean builtIn(String machine) {
        return BUILTIN.contains(machine);
    }
    /** True after layouts freeze, for an addon or script machine that already has a layout. */
    public static boolean replaceable(String machine) {
        return frozen && machine != null && !BUILTIN.contains(machine) && DEFINITIONS.containsKey(machine);
    }

    public static Set<String> overlaid() {
        return overlay.keySet();
    }
    /**
     * Replaces addon and script layouts. An empty map restores every baseline.
     * Built-in controllers are rejected. Callers drop unknown files before this.
     */
    public static void overlay(Map<String, List<Cell>> replacements) {
        overlay(replacements, Map.of());
    }
    /** Replaces addon and script layouts, including their repeat groups. An empty pair restores every baseline. */
    public static void overlay(Map<String, List<Cell>> replacements, Map<String, List<Repeat>> repeated) {
        if (!frozen) throw new IllegalStateException("Large machine layouts are not ready");
        var next = new LinkedHashMap<String, List<Cell>>();
        var nextRepeats = new LinkedHashMap<String, List<Repeat>>();
        if (replacements != null)
            for (var entry : replacements.entrySet()) {
                if (!replaceable(entry.getKey()))
                    throw new IllegalArgumentException("Layout cannot be replaced: " + entry.getKey());
                var groups = repeated == null ? List.<Repeat>of() : repeated.getOrDefault(entry.getKey(), List.of());
                if ((entry.getValue() == null || entry.getValue().isEmpty()) && groups.isEmpty())
                    throw new IllegalArgumentException("Large machine layout '" + entry.getKey() + "' is empty");
                next.put(entry.getKey(), entry.getValue() == null ? List.of() : List.copyOf(entry.getValue()));
                if (!groups.isEmpty()) nextRepeats.put(entry.getKey(), List.copyOf(groups));
            }
        overlay = Map.copyOf(next);
        overlayRepeats = Map.copyOf(nextRepeats);
    }

    public static Set<String> machines() {
        return Collections.unmodifiableSet(DEFINITIONS.keySet());
    }
    /** Open only while addon layouts are collected. Built-in names stay reserved. */
    public static void add(String machine, List<Cell> cells) {
        add(machine, cells, List.of());
    }

    public static void add(String machine, Layout layout) {
        if (layout == null) throw new IllegalArgumentException("Missing large machine layout");
        add(machine, layout.cells(), layout.repeats());
    }

    public static void add(String machine, List<Cell> cells, List<Repeat> repeats) {
        if (frozen) throw new IllegalStateException("Large machine layouts are frozen");
        if (machine == null || !machine.matches("[a-z0-9_]+"))
            throw new IllegalArgumentException("Invalid large machine layout: " + machine);
        if ((cells == null || cells.isEmpty()) && (repeats == null || repeats.isEmpty()))
            throw new IllegalArgumentException("Large machine layout '" + machine + "' is empty");
        if (DEFINITIONS.containsKey(machine))
            throw new IllegalStateException("Duplicate large machine layout: " + machine);
        DEFINITIONS.put(machine, cells == null ? List.of() : List.copyOf(cells));
        if (repeats != null && !repeats.isEmpty()) REPEATS.put(machine, List.copyOf(repeats));
    }

    public static void freeze() {
        frozen = true;
    }

    private static Map<String, List<Cell>> create() {
        var definitions = new LinkedHashMap<String, List<Cell>>();
        definitions.put(
                "largemixer",
                MultiblockPatterns.cells(StructureLayout.pattern("largemixer")
                        .layer(0, "ASA", "ABA", "AAA")
                        .layer(1, "CCC", "CBC", "CCC")
                        .where('A', CellMatch.part("tank_wall"), StructureRole.ITEM_FLUID_OUTPUT)
                        .where('B', CellMatch.part("tank_wall"), StructureRole.ENERGY_INPUT)
                        .where('C', CellMatch.part("tank_wall"), StructureRole.ITEM_FLUID_INPUT)));
        definitions.put(
                "largebath",
                MultiblockPatterns.cells(StructureLayout.pattern("largebath")
                        .layer(0, "AASAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA")
                        .layer(1, "AAAAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA")
                        .where('A', CellMatch.part("tank_wall"), StructureRole.ITEM_FLUID_IO)));
        definitions.put(
                "largecentrifuge",
                MultiblockPatterns.cells(StructureLayout.pattern("largecentrifuge")
                        .layer(0, "ASA", "ABA", "AAA")
                        .layer(1, "AAA", "ABA", "AAA")
                        .where('A', CellMatch.part("centrifuge_part"), StructureRole.ITEM_FLUID_IO)
                        .where('B', CellMatch.part("centrifuge_part"), StructureRole.ENERGY_INPUT)));
        definitions.put(
                "largecoagulator",
                MultiblockPatterns.cells(StructureLayout.pattern("largecoagulator")
                        .layer(0, "AASAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA")
                        .layer(1, "AAAAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA")
                        .where('A', CellMatch.part("tank_wall"), StructureRole.ITEM_FLUID_IO)));
        definitions.put(
                "largeelectrolyzer",
                MultiblockPatterns.cells(StructureLayout.pattern("largeelectrolyzer")
                        .layer(0, "ASA", "AAA", "AAA")
                        .layer(1, "BBB", "BBB", "BBB")
                        .where('A', CellMatch.part("electrolyzer_part"), StructureRole.ITEM_FLUID_ENERGY_INPUT)
                        .where('B', CellMatch.part("electrolyzer_part"), StructureRole.ITEM_FLUID_OUTPUT)));
        definitions.put(
                "largefermenter",
                MultiblockPatterns.cells(StructureLayout.pattern("largefermenter")
                        .layer(-1, "AAAAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA")
                        .layer(0, "BBSBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB")
                        .layer(1, "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBBBB")
                        .where('A', CellMatch.part("heat_transmitter"), StructureRole.ENERGY_INPUT)
                        .where('B', CellMatch.part("tank_wall"), StructureRole.ITEM_FLUID_IO)));
        definitions.put(
                "largeautoclave",
                MultiblockPatterns.cells(StructureLayout.pattern("largeautoclave")
                        .layer(0, "ASA", "AAA", "AAA")
                        .layer(1, "AAA", "A A", "AAA")
                        .layer(2, "AAA", "AAA", "AAA")
                        .where('A', CellMatch.part("tank_wall_dense"), StructureRole.ITEM_FLUID_ENERGY)
                        .where(' ', CellMatch.air(), StructureRole.AIR)));
        definitions.put(
                "largecrusher",
                MultiblockPatterns.cells(StructureLayout.pattern("largecrusher")
                        .layer(0, "AASAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA")
                        .layer(1, "BBBBB", "BCCCB", "DCCCD", "BCCCB", "BBBBB")
                        .layer(2, "BBBBB", "BEEEB", "BEEEB", "BEEEB", "BBBBB")
                        .where('A', CellMatch.part("tungstensteel_wall"), StructureRole.ITEM_FLUID_OUTPUT)
                        .where('B', CellMatch.part("tungstensteel_wall"), StructureRole.CASING)
                        .where('C', CellMatch.part("crusher_wheels"), StructureRole.CASING)
                        .where('D', CellMatch.part("tungstensteel_wall"), StructureRole.ENERGY_INPUT)
                        .where('E', CellMatch.part("crusher_wheels"), StructureRole.ITEM_FLUID_INPUT)));
        definitions.put(
                "largeshredder",
                MultiblockPatterns.cells(StructureLayout.pattern("largeshredder")
                        .layer(0, "AASAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA")
                        .layer(1, "BBBBB", "BCCCB", "DCCCD", "BCCCB", "BBBBB")
                        .layer(2, "BBBBB", "BEEEB", "BEEEB", "BEEEB", "BBBBB")
                        .where('A', CellMatch.part("tungstensteel_wall"), StructureRole.ITEM_FLUID_OUTPUT)
                        .where('B', CellMatch.part("tungstensteel_wall"), StructureRole.CASING)
                        .where('C', CellMatch.part("shredder_blades"), StructureRole.CASING)
                        .where('D', CellMatch.part("tungstensteel_wall"), StructureRole.ENERGY_INPUT)
                        .where('E', CellMatch.part("shredder_blades"), StructureRole.ITEM_FLUID_INPUT)));
        definitions.put(
                "largesqueezer",
                MultiblockPatterns.cells(StructureLayout.pattern("largesqueezer")
                        .layer(0, "AASAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA")
                        .layer(1, "BBBBB", "B   B", "C   C", "B   B", "BBBBB")
                        .layer(2, "DDDDD", "DDDDD", "DDDDD", "DDDDD", "DDDDD")
                        .where('A', CellMatch.part("steel_wall"), StructureRole.ITEM_FLUID_OUTPUT)
                        .where('B', CellMatch.part("steel_wall"), StructureRole.CASING)
                        .where('C', CellMatch.part("steel_wall"), StructureRole.ENERGY_INPUT)
                        .where('D', CellMatch.part("steel_wall"), StructureRole.ITEM_FLUID_INPUT)
                        .where(' ', CellMatch.air(), StructureRole.AIR)));
        definitions.put(
                "largeoven",
                MultiblockPatterns.cells(StructureLayout.pattern("largeoven")
                        .layer(0, "ASA", "AAA", "AAA")
                        .layer(1, "BBB", "B B", "BBB")
                        .layer(2, "AAA", "AAA", "AAA")
                        .where('A', CellMatch.part("invar_wall"), StructureRole.ITEM_FLUID_ENERGY)
                        .where('B', CellMatch.part("large_nichrome_coil"), StructureRole.CASING)
                        .where(' ', CellMatch.air(), StructureRole.AIR)));
        definitions.put(
                "largesluice",
                MultiblockPatterns.cells(StructureLayout.pattern("largesluice")
                        .layer(0, "ASA", "BBB", "BBB", "BBB", "BBB", "BBB", "BBB")
                        .layer(1, "BBB", "BBB", "BBB", "BBB", "BBB", "CBC", "BBB")
                        .layer(2, "DDD", "DDD", "DDD", "DDD", "DDD", "DDD", "EEE")
                        .where('A', CellMatch.part("titanium_wall"), StructureRole.ITEM_FLUID_OUTPUT)
                        .where('B', CellMatch.part("titanium_wall"), StructureRole.CASING)
                        .where('C', CellMatch.part("titanium_wall"), StructureRole.ENERGY_INPUT)
                        .where('D', CellMatch.part("sluice_part"), StructureRole.CASING)
                        .where('E', CellMatch.part("sluice_part"), StructureRole.ITEM_FLUID_INPUT)));
        definitions.put("largemassfab", SharedMatterFabricatorStructure.CELLS);
        return definitions;
    }
}
