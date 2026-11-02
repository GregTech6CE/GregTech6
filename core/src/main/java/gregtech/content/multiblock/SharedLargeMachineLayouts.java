package gregtech.content.multiblock;

import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.LayoutRepeat;
import gregtech.api.multiblock.StructureConstraint;
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
            int right,
            int up,
            int back,
            int part,
            StructureRole role,
            String plain,
            int design,
            List<Choice> alternatives,
            gregtech.api.multiblock.StructurePorts ports) {
        public Cell(
                int right,
                int up,
                int back,
                int part,
                StructureRole role,
                String plain,
                int design,
                List<Choice> alternatives) {
            this(
                    right,
                    up,
                    back,
                    part,
                    role,
                    plain,
                    design,
                    alternatives,
                    gregtech.api.multiblock.StructurePorts.DEFAULT);
        }

        public Cell(int right, int up, int back, int part, StructureRole role) {
            this(right, up, back, part, role, null, -1, List.of());
        }

        public Cell(int right, int up, int back, int part, StructureRole role, String plain) {
            this(right, up, back, part, role, plain, -1, List.of());
        }

        public Cell(int right, int up, int back, int part, StructureRole role, String plain, int design) {
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

    public record Layout(List<Cell> cells, List<Repeat> repeats, List<StructureConstraint> constraints) {
        public Layout(List<Cell> cells, List<Repeat> repeats) {
            this(cells, repeats, List.of());
        }

        public Layout {
            cells = cells == null ? List.of() : List.copyOf(cells);
            repeats = repeats == null ? List.of() : List.copyOf(repeats);
            constraints = List.copyOf(constraints);
        }
    }

    private static final Map<String, Layout> DEFINITIONS = new LinkedHashMap<>(create());
    /** Names present before any addon layout is added. Those layouts are not replaceable. */
    private static final Set<String> BUILTIN = Set.copyOf(DEFINITIONS.keySet());

    private static Map<String, Layout> overlay = Map.of();
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
                            cell.design(),
                            cell.alternatives(),
                            cell.ports()));
        return List.copyOf(out);
    }
    /** Fixed cells without expanding repeats. */
    public static List<Cell> baseCells(String machine) {
        var layout = layout(machine);
        return layout == null ? null : layout.cells();
    }

    public static List<Repeat> repeats(String machine) {
        var layout = layout(machine);
        return layout == null ? List.of() : layout.repeats();
    }
    /** The layout a structure check reads. Null when the machine has no layout. */
    public static Layout layout(String machine) {
        return overlay.getOrDefault(machine, DEFINITIONS.get(machine));
    }
    /** The layout stored at startup, ignoring a datapack replacement. */
    public static List<Cell> baseline(String machine) {
        var layout = DEFINITIONS.get(machine);
        return layout == null ? null : layout.cells();
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
        var layouts = new LinkedHashMap<String, Layout>();
        if (replacements != null)
            for (var entry : replacements.entrySet()) {
                var groups = repeated == null ? List.<Repeat>of() : repeated.getOrDefault(entry.getKey(), List.of());
                layouts.put(entry.getKey(), new Layout(entry.getValue(), groups));
            }
        overlayLayouts(layouts);
    }

    /** Complete declarations keep rules attached through server reload and client synchronization. */
    public static void overlayLayouts(Map<String, Layout> replacements) {
        if (!frozen) throw new IllegalStateException("Large machine layouts are not ready");
        var next = new LinkedHashMap<String, Layout>();
        for (var entry : replacements.entrySet()) {
            if (!replaceable(entry.getKey()))
                throw new IllegalArgumentException("Layout cannot be replaced: " + entry.getKey());
            var layout = entry.getValue();
            if (layout.cells().isEmpty() && layout.repeats().isEmpty())
                throw new IllegalArgumentException("Large machine layout '" + entry.getKey() + "' is empty");
            next.put(entry.getKey(), layout);
        }
        overlay = Map.copyOf(next);
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
        if (frozen) throw new IllegalStateException("Large machine layouts are frozen");
        if (machine == null || !machine.matches("[a-z0-9_]+"))
            throw new IllegalArgumentException("Invalid large machine layout: " + machine);
        if (layout.cells().isEmpty() && layout.repeats().isEmpty())
            throw new IllegalArgumentException("Large machine layout '" + machine + "' is empty");
        if (DEFINITIONS.containsKey(machine))
            throw new IllegalStateException("Duplicate large machine layout: " + machine);
        DEFINITIONS.put(machine, layout);
    }

    public static void add(String machine, List<Cell> cells, List<Repeat> repeats) {
        add(machine, new Layout(cells, repeats));
    }

    public static void freeze() {
        frozen = true;
    }

    private static CellMatch ovenCoils() {
        return CellMatch.oneOf(CellMatch.part("large_nichrome_coil"), CellMatch.part("large_carborundum_coil"));
    }

    private static Map<String, Layout> create() {
        var definitions = new LinkedHashMap<String, Layout>();
        definitions.put(
                "largemixer",
                MultiblockPatterns.layout(StructureLayout.pattern("largemixer")
                        .layer(0, "ASA", "ABA", "AAA")
                        .layer(1, "CCC", "CBC", "CCC")
                        .where('A', CellMatch.part("tank_wall"), StructureRole.ITEM_FLUID_OUTPUT)
                        .where('B', CellMatch.part("tank_wall"), StructureRole.ENERGY_INPUT, 3)
                        .where('C', CellMatch.part("tank_wall"), StructureRole.ITEM_FLUID_INPUT)));
        definitions.put(
                "largebath",
                MultiblockPatterns.layout(StructureLayout.pattern("largebath")
                        .layer(0, "AASAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA")
                        .layer(1, "AAAAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA")
                        .where('A', CellMatch.part("tank_wall"), StructureRole.ITEM_FLUID_IO)));
        definitions.put(
                "largecentrifuge",
                MultiblockPatterns.layout(StructureLayout.pattern("largecentrifuge")
                        .layer(0, "BSD", "EJF", "GHI")
                        .layer(1, "BCD", "EJF", "GHI")
                        .where('B', CellMatch.part("centrifuge_part"), StructureRole.ITEM_FLUID_IO, 1)
                        .where('C', CellMatch.part("centrifuge_part"), StructureRole.ITEM_FLUID_IO, 2)
                        .where('D', CellMatch.part("centrifuge_part"), StructureRole.ITEM_FLUID_IO, 3)
                        .where('E', CellMatch.part("centrifuge_part"), StructureRole.ITEM_FLUID_IO, 4)
                        .where('F', CellMatch.part("centrifuge_part"), StructureRole.ITEM_FLUID_IO, 5)
                        .where('G', CellMatch.part("centrifuge_part"), StructureRole.ITEM_FLUID_IO, 6)
                        .where('H', CellMatch.part("centrifuge_part"), StructureRole.ITEM_FLUID_IO, 7)
                        .where('I', CellMatch.part("centrifuge_part"), StructureRole.ITEM_FLUID_IO, 8)
                        .where('J', CellMatch.part("centrifuge_part"), StructureRole.ENERGY_INPUT, 0)));
        definitions.put(
                "largecoagulator",
                MultiblockPatterns.layout(StructureLayout.pattern("largecoagulator")
                        .layer(0, "AASAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA")
                        .layer(1, "AAAAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA")
                        .where('A', CellMatch.part("tank_wall"), StructureRole.ITEM_FLUID_IO)));
        definitions.put(
                "largeelectrolyzer",
                MultiblockPatterns.layout(StructureLayout.pattern("largeelectrolyzer")
                        .layer(0, "ASA", "AAA", "AAA")
                        .layer(1, "BBB", "BBB", "BBB")
                        .where('A', CellMatch.part("electrolyzer_part"), StructureRole.ITEM_FLUID_ENERGY_INPUT, 1)
                        .where('B', CellMatch.part("electrolyzer_part"), StructureRole.ITEM_FLUID_OUTPUT, 0)));
        definitions.put(
                "largefermenter",
                MultiblockPatterns.layout(StructureLayout.pattern("largefermenter")
                        .layer(-1, "AAAAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA")
                        .layer(0, "BBSBB", "BBBBB", "BBBBB", "BBBBB", "BBCBB")
                        .layer(1, "BBBBB", "BBBBB", "BBBBB", "BBBBB", "BBCBB")
                        .where('A', CellMatch.part("heat_transmitter"), StructureRole.ENERGY_INPUT)
                        .where('B', CellMatch.part("tank_wall"), StructureRole.ITEM_FLUID_IO)
                        .where('C', CellMatch.part("tank_wall"), StructureRole.ITEM_FLUID_IO, 7)));
        definitions.put(
                "largeautoclave",
                MultiblockPatterns.layout(StructureLayout.pattern("largeautoclave")
                        .layer(0, "ASA", "AAA", "AAA")
                        .layer(1, "AAA", "A A", "AAA")
                        .layer(2, "AAA", "AAA", "AAA")
                        .where('A', CellMatch.part("tank_wall_dense"), StructureRole.ITEM_FLUID_ENERGY)
                        .where(' ', CellMatch.air(), StructureRole.AIR)));
        definitions.put(
                "largecrusher",
                MultiblockPatterns.layout(StructureLayout.pattern("largecrusher")
                        .layer(0, "AASAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA")
                        .layer(1, "BBBBB", "BCCCB", "DCCCD", "BCCCB", "BBBBB")
                        .layer(2, "BBBBB", "BEEEB", "BEEEB", "BEEEB", "BBBBB")
                        .where('A', CellMatch.part("tungstensteel_wall"), StructureRole.ITEM_FLUID_OUTPUT)
                        .where('B', CellMatch.part("tungstensteel_wall"), StructureRole.CASING)
                        .where('C', CellMatch.part("crusher_wheels"), StructureRole.CASING, 0)
                        .where('D', CellMatch.part("tungstensteel_wall"), StructureRole.ENERGY_INPUT, 3)
                        .where('E', CellMatch.part("crusher_wheels"), StructureRole.ITEM_FLUID_INPUT, 0)));
        definitions.put(
                "largeshredder",
                MultiblockPatterns.layout(StructureLayout.pattern("largeshredder")
                        .layer(0, "AASAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA")
                        .layer(1, "BBBBB", "BCCCB", "DCCCD", "BCCCB", "BBBBB")
                        .layer(2, "BBBBB", "BEEEB", "BEEEB", "BEEEB", "BBBBB")
                        .where('A', CellMatch.part("tungstensteel_wall"), StructureRole.ITEM_FLUID_OUTPUT)
                        .where('B', CellMatch.part("tungstensteel_wall"), StructureRole.CASING)
                        .where('C', CellMatch.part("shredder_blades"), StructureRole.CASING, 0)
                        .where('D', CellMatch.part("tungstensteel_wall"), StructureRole.ENERGY_INPUT, 3)
                        .where('E', CellMatch.part("shredder_blades"), StructureRole.ITEM_FLUID_INPUT, 0)));
        definitions.put(
                "largesqueezer",
                MultiblockPatterns.layout(StructureLayout.pattern("largesqueezer")
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
                MultiblockPatterns.layout(StructureLayout.pattern("largeoven")
                        .layer(0, "ASA", "AAA", "AAA")
                        .layer(1, "BBB", "B B", "BBB")
                        .layer(2, "AAA", "AAA", "AAA")
                        .where('A', CellMatch.part("invar_wall"), StructureRole.ITEM_FLUID_ENERGY)
                        .where('B', ovenCoils(), StructureRole.CASING)
                        .require(StructureConstraint.sameBlock(ovenCoils()))
                        .where(' ', CellMatch.air(), StructureRole.AIR)));
        definitions.put(
                "largesluice",
                MultiblockPatterns.layout(StructureLayout.pattern("largesluice")
                        .layer(0, "ASA", "BBB", "BBB", "BBB", "BBB", "BBB", "BBB")
                        .layer(1, "BBB", "BBB", "BBB", "BBB", "BBB", "CBC", "BBB")
                        .layer(2, "DDD", "DDD", "DDD", "DDD", "DDD", "DDD", "EEE")
                        .where('A', CellMatch.part("titanium_wall"), StructureRole.ITEM_FLUID_OUTPUT, 1)
                        .where('B', CellMatch.part("titanium_wall"), StructureRole.CASING)
                        .where('C', CellMatch.part("titanium_wall"), StructureRole.ENERGY_INPUT, 3)
                        .where('D', CellMatch.part("sluice_part"), StructureRole.CASING, 0)
                        .where('E', CellMatch.part("sluice_part"), StructureRole.ITEM_FLUID_INPUT, 0)));
        definitions.put("largemassfab", SharedMatterFabricatorStructure.LAYOUT);
        return definitions;
    }
}
