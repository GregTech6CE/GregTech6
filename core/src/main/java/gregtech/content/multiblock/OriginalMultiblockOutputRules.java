package gregtech.content.multiblock;

import static gregtech.api.machine.MachineFace.*;
import static gregtech.api.multiblock.PortChannel.*;

import gregtech.api.machine.IoTarget;
import gregtech.api.multiblock.PortChannel;
import gregtech.engine.multiblock.StructureOutput;
import java.util.ArrayList;
import java.util.List;

/** Active GT6 outlets, separate from the passive IO permissions of the same wall. */
public final class OriginalMultiblockOutputRules {
    private OriginalMultiblockOutputRules() {}

    public static final StructureOutput SHAFT = StructureOutput.port(ENERGY_OUTPUT, IoTarget.at(0, 0, 4, FRONT));
    public static final StructureOutput DRILL_ITEMS = buffered(ITEM_OUTPUT, 0, 1, 0, BOTTOM);
    public static final StructureOutput LIGHTNING_ENERGY =
            StructureOutput.port(ENERGY_OUTPUT, IoTarget.at(0, -1, 0, TOP));
    public static final StructureOutput TOWER_ITEMS = buffered(ITEM_OUTPUT, 0, 0, 3, FRONT);
    public static final StructureOutput HEAT_EXCHANGER_FLUID = buffered(FLUID_OUTPUT, 0, -1, 0, TOP);
    public static final List<StructureOutput> COKE_FLUIDS = coke();
    public static final List<StructureOutput> BOILER_STEAM = List.of(
            StructureOutput.port(FLUID_OUTPUT, IoTarget.at(0, 3, 1, BOTTOM)),
            StructureOutput.port(FLUID_OUTPUT, IoTarget.at(0, 1, -1, BACK)),
            StructureOutput.port(FLUID_OUTPUT, IoTarget.at(0, 1, 3, FRONT)),
            StructureOutput.port(FLUID_OUTPUT, IoTarget.at(-2, 1, 1, LEFT)),
            StructureOutput.port(FLUID_OUTPUT, IoTarget.at(2, 1, 1, RIGHT)));
    public static final List<StructureOutput> HEAT_EXCHANGER_ENERGY = heat();

    public static StructureOutput towerFluid(boolean cryogenic, String fluid) {
        return buffered(FLUID_OUTPUT, 0, OriginalDistillationOutputRules.fluidHeight(cryogenic, fluid), 3, FRONT);
    }

    private static StructureOutput buffered(
            PortChannel channel, int right, int up, int back, gregtech.api.machine.MachineFace receiver) {
        return StructureOutput.buffered(channel, IoTarget.at(right, up, back, receiver));
    }

    private static List<StructureOutput> coke() {
        var result = new ArrayList<StructureOutput>();
        for (var p : OriginalCokeOvenRules.fluidTargets()) result.add(buffered(FLUID_OUTPUT, p.x(), p.y(), p.z(), TOP));
        return List.copyOf(result);
    }

    private static List<StructureOutput> heat() {
        var result = new ArrayList<StructureOutput>();
        // Preserve GT6's order; heat is spent equally at all eight transmitters, including blocked ones.
        for (int[] p : new int[][] {{0, -1}, {-1, 0}, {0, 1}, {1, 0}, {1, -1}, {-1, -1}, {-1, 1}, {1, 1}})
            result.add(buffered(ENERGY_OUTPUT, p[0], 2, p[1], BOTTOM));
        return List.copyOf(result);
    }
}
