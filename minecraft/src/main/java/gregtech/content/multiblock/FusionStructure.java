package gregtech.content.multiblock;
/** Original public platform cell binding over the shared source structure. */
public final class FusionStructure {
    private FusionStructure() {}

    public static final java.util.List<LargeMachineLayouts.Cell> CELLS =
            LargeMachineLayouts.fromShared(SharedFusionStructure.CELLS);
    public static final LargeMachineLayouts.Layout LAYOUT =
            new LargeMachineLayouts.Layout(CELLS, java.util.List.of(), SharedFusionStructure.STRUCTURE.constraints());
}
