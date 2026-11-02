package gregtech.content.multiblock;

import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.StructureConstraint;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;
import java.util.List;

/** GT6 MultiTileEntityFusionReactor character layers.
 * CPU coordinates in this list are an assembly example; validation accepts all permutations. */
public final class SharedFusionStructure {
    private SharedFusionStructure() {}

    public static final StructureLayout STRUCTURE = StructureLayout.pattern("fusionreactor")
            .layer(
                    -2,
                    "...................",
                    ".......FFFFF.......",
                    "......FFFFFFF......",
                    ".....FFFFFFFFF.....",
                    "....FFF.....FFF....",
                    "...FFF.......FFF...",
                    "..FFF.........FFF..",
                    ".FFF...AAAAA...FFF.",
                    ".FFF...ABBBA...FFF.",
                    ".FFF...ABBBA...FFF.",
                    ".FFF...ABBBA...FFF.",
                    ".FFF...AAAAA...FFF.",
                    "..FFF.........FFF..",
                    "...FFF.......FFF...",
                    "....FFF.....FFF....",
                    ".....FFFFFFFFF.....",
                    "......FFFFFFF......",
                    ".......FFFFF.......",
                    "...................")
            .layer(
                    -1,
                    ".......FFFFF.......",
                    "......FIIIIIF......",
                    ".....FIJJJJJIF.....",
                    "....FIJIIIIIJIF....",
                    "...FIJIFFFFFIJIF...",
                    "..FIJIF.....FIJIF..",
                    ".FIJIF.......FIJIF.",
                    "FIJIF..ABBBA..FIJIF",
                    "FIJIF..BCDEB..FIJIF",
                    "FIJIF..BCDEB..FIJIF",
                    "FIJIF..BCDEB..FIJIF",
                    "FIJIF..ABBBA..FIJIF",
                    ".FIJIF.......FIJIF.",
                    "..FIJIF.....FIJIF..",
                    "...FIJIFFFFFIJIF...",
                    "....FIJIIIIIJIF....",
                    ".....FIJJJJJIF.....",
                    "......FIIIIIF......",
                    ".......FFFFF.......")
            .layer(
                    0,
                    ".......GGHGG.......",
                    "......GJJJJJG......",
                    ".....GJKKKKKJG.....",
                    "....GJKJJJJJKJG....",
                    "...GJKJGGGGGJKJG...",
                    "..GJKJG.....GJKJG..",
                    ".GJKJG.......GJKJG.",
                    "GJKJG..ABSBA..GJKJG",
                    "GJKJG..BDDEB..GJKJG",
                    "HJKJGAAADDEAAAGJKJH",
                    "GJKJG..BDDEB..GJKJG",
                    "GJKJG..ABABA..GJKJG",
                    ".GJKJG...A...GJKJG.",
                    "..GJKJG..A..GJKJG..",
                    "...GJKJGGGGGJKJG...",
                    "....GJKJJJJJKJG....",
                    ".....GJKKKKKJG.....",
                    "......GJJJJJG......",
                    ".......GGHGG.......")
            .layer(
                    1,
                    ".......FFFFF.......",
                    "......FIIIIIF......",
                    ".....FIJJJJJIF.....",
                    "....FIJIIIIIJIF....",
                    "...FIJIFFFFFIJIF...",
                    "..FIJIF.....FIJIF..",
                    ".FIJIF.......FIJIF.",
                    "FIJIF..ABBBA..FIJIF",
                    "FIJIF..BDEEB..FIJIF",
                    "FIJIF..BDEEB..FIJIF",
                    "FIJIF..BDEEB..FIJIF",
                    "FIJIF..ABBBA..FIJIF",
                    ".FIJIF.......FIJIF.",
                    "..FIJIF.....FIJIF..",
                    "...FIJIFFFFFIJIF...",
                    "....FIJIIIIIJIF....",
                    ".....FIJJJJJIF.....",
                    "......FIIIIIF......",
                    ".......FFFFF.......")
            .layer(
                    2,
                    "...................",
                    ".......FFFFF.......",
                    "......FFFFFFF......",
                    ".....FFFFFFFFF.....",
                    "....FFF.....FFF....",
                    "...FFF.......FFF...",
                    "..FFF.........FFF..",
                    ".FFF...AAAAA...FFF.",
                    ".FFF...ABBBA...FFF.",
                    ".FFF...ABBBA...FFF.",
                    ".FFF...ABBBA...FFF.",
                    ".FFF...AAAAA...FFF.",
                    "..FFF.........FFF..",
                    "...FFF.......FFF...",
                    "....FFF.....FFF....",
                    ".....FFFFFFFFF.....",
                    "......FFFFFFF......",
                    ".......FFFFF.......",
                    "...................")
            .ignore('.')
            .where('A', CellMatch.part("galvanized_steel_wall"), StructureRole.CASING)
            .where('B', CellMatch.part("fusion_ventilation_unit"), StructureRole.CASING)
            .where('C', processors("versatile_processor_unit"), StructureRole.CASING)
            .where('D', processors("logic_processor_unit"), StructureRole.CASING)
            .where('E', processors("control_processor_unit"), StructureRole.CASING)
            .where('F', CellMatch.part("tungstensteel_wall"), StructureRole.ITEM_FLUID_IO)
            .where('G', CellMatch.part("tungstensteel_wall"), StructureRole.ENERGY_INPUT, 5)
            .where('H', CellMatch.part("tungstensteel_wall"), StructureRole.ENERGY_OUTPUT, 2)
            .where('I', CellMatch.part("tungstensteel_wall"), StructureRole.CASING)
            .where('J', CellMatch.part("large_iridium_coil"), StructureRole.CASING)
            .where('K', CellMatch.part("tank_wall"), StructureRole.CASING)
            .require(StructureConstraint.exactly(CellMatch.part("versatile_processor_unit"), 3))
            .require(StructureConstraint.exactly(CellMatch.part("logic_processor_unit"), 12))
            .require(StructureConstraint.exactly(CellMatch.part("control_processor_unit"), 12))
            .build();

    public static final SharedLargeMachineLayouts.Layout LAYOUT = MultiblockPatterns.layout(STRUCTURE);
    public static final List<SharedLargeMachineLayouts.Cell> CELLS = LAYOUT.cells();

    /** The first choice supplies the assembly example; all three processors fit any CPU position. */
    private static CellMatch processors(String preferred) {
        var names = new java.util.ArrayList<String>();
        names.add(preferred);
        for (String name : List.of("versatile_processor_unit", "logic_processor_unit", "control_processor_unit"))
            if (!name.equals(preferred)) names.add(name);
        return CellMatch.oneOf(names.stream().map(CellMatch::part).toList());
    }
}
