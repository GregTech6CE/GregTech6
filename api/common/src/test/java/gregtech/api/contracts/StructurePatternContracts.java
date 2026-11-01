package gregtech.api.contracts;

import gregtech.api.multiblock.*;

/** Exercises the public, packaged character-layer API without the Minecraft host. */
final class StructurePatternContracts {
    static void check() {
        var pattern = oven().count('B', 25, 25).count(' ', 1, 1).build();
        if (pattern.cells().size() != 26) throw new AssertionError("Coke oven cell count");
        var expected = StructureLayout.builder("oven");
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = 0; z <= 2; z++) {
            if (x == 0 && y == 0 && z == 0) continue;
            boolean air = x == 0 && y == 0 && z == 1;
            expected.cell(x, y, z, air ? CellMatch.air() : CellMatch.part("fire_bricks"),
                    air ? StructureRole.AIR : StructureRole.ITEM_FLUID_ENERGY);
        }
        if (!new java.util.HashSet<>(pattern.cells()).equals(new java.util.HashSet<>(expected.build().cells())))
            throw new AssertionError("Character layout changed the original coordinates or roles");
        var tower = StructureLayout.pattern("tower").layer(0, "S")
                .where('B', CellMatch.block("minecraft:stone"), StructureRole.CASING)
                .repeat("shaft", 1, 2, 4, "B").count('B', 2, 4).build();
        if (tower.repeats().get(0).absoluteUp(tower.repeats().get(0).cells().get(0), 3) != 4)
            throw new AssertionError("Repeat anchor");
        var open = StructureLayout.pattern("open").layer(0, "S.B")
                .ignore('.').where('B', CellMatch.part("wall"), StructureRole.CASING)
                .repeat("optional", 1, 0, 3, "..B").build();
        if (open.cells().size() != 1 || open.cells().get(0).right() != 2
                || open.repeats().get(0).min() != 0 || open.repeats().get(0).cells().size() != 1)
            throw new AssertionError("Ignored cells must not become air requirements");
        fails(() -> oven().ignore('S').build());
        fails(() -> oven().ignore('B').build());
        fails(() -> oven().ignore('.').count('.', 0, 0).build());
        fails(() -> StructureLayout.pattern("bad").layer(0, "S").ignore('.')
                .where('B', CellMatch.part("wall"), StructureRole.CASING)
                .repeat("shaft", 1, 1, 2, 1, java.util.List.of(java.util.List.of("B"), java.util.List.of("."))).build());
        fails(() -> LayoutRepeat.builder("bad", 1, 0, 0)
                .cell(0, 0, 0, CellMatch.part("wall"), StructureRole.CASING).build());
        fails(() -> oven().count('B', 26, 26).build());
        fails(() -> StructureLayout.pattern("bad").layer(0, "SX").build());
        fails(() -> StructureLayout.pattern("bad").layer(0, "SS").build());
        fails(() -> StructureLayout.pattern("bad").layer(0, "S", "BB"));
        fails(() -> StructureLayout.pattern("bad").layer(0, "S").layer(1, "B", "B"));
        fails(() -> StructureLayout.pattern("bad").layer(0, "S").repeat("shaft", 1, 1, 2, "S").build());
        fails(() -> StructureLayout.pattern("bad").layer(0, "SB").layer(0, "BB")
                .where('B', CellMatch.air(), StructureRole.AIR).build());
    }

    private static StructurePattern oven() {
        return StructureLayout.pattern("oven").layer(-1, "BBB", "BBB", "BBB")
                .layer(0, "BSB", "B B", "BBB").layer(1, "BBB", "BBB", "BBB")
                .where('B', CellMatch.part("fire_bricks"), StructureRole.ITEM_FLUID_ENERGY)
                .where(' ', CellMatch.air(), StructureRole.AIR);
    }

    private static void fails(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid structure accepted");
    }
}
