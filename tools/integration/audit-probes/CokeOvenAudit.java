import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.api.multiblock.StructureGrid;
import com.gregtech.gregtech.api.recipe.MachineWorkCost;
import com.gregtech.gregtech.content.multiblock.OriginalCokeOvenRules;
import com.gregtech.gregtech.content.multiblock.OriginalMultiblockMachineParameters;
import java.util.HashSet;

/** Observes maintained core parameters/geometry. Does not run native machine ticks or inventories. */
class CokeOvenAudit {
    public static void main(String[] args) {
        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        var spec = OriginalMultiblockMachineParameters.cokeOven();
        System.out.println("SPEC|" + spec.id() + "|" + spec.energyType() + "|" + spec.energyIn()
                + "|" + spec.energyInMin() + "|" + spec.energyInMax() + "|" + spec.parallelLimit()
                + "|" + spec.hardness() + "|" + spec.blastResistance());
        var faces = spec.faceConfig();
        System.out.println("FACES|" + faces.itemInputs() + "|" + faces.itemOutputs()
                + "|" + faces.fluidInputs() + "|" + faces.fluidOutputs() + "|" + faces.energyInputs()
                + "|" + faces.itemAutoInput() + "|" + faces.itemAutoOutput()
                + "|" + faces.fluidAutoInput() + "|" + faces.fluidAutoOutput());
        int brick = 0, air = 0;
        for (var cell : OriginalCokeOvenRules.cells()) {
            if (cell.role() == StructureGrid.Role.AIR) air++;
            else if (cell.part() == 18000) brick++;
            else throw new AssertionError("Unexpected coke oven part");
        }
        if (brick != 25 || air != 1) throw new AssertionError("Wrong hollow structure");
        System.out.println("CELLS|" + brick + "|" + air);
        for (int[] front : new int[][] {{0, -1}, {1, 0}, {0, 1}, {-1, 0}}) {
            var actual = new HashSet<StructureGrid.Position>();
            for (var local : OriginalCokeOvenRules.fluidTargets())
                actual.add(StructureGrid.offset(front[0], 0, front[1], local.x(), local.y(), local.z()));
            var expected = new HashSet<StructureGrid.Position>();
            for (int x = -1; x <= 1; x++)
                for (int z = -1; z <= 1; z++)
                    expected.add(new StructureGrid.Position(-front[0] + x, -2, -front[1] + z));
            if (!actual.equals(expected)) throw new AssertionError("Output position set differs");
            var first = OriginalCokeOvenRules.fluidTargets().get(0);
            var currentFirst = StructureGrid.offset(front[0], 0, front[1], first.x(), first.y(), first.z());
            System.out.println("TARGETS|" + front[0] + "|" + front[1] + "|" + actual.size()
                    + "|" + currentFirst.x() + "|" + currentFirst.z()
                    + "|" + (-front[0] - 1) + "|" + (-front[1] - 1));
        }
        for (int parallel : new int[] {1, 16}) {
            var cost = MachineWorkCost.calculate(0, 3600, parallel, false, 10000, 1, 16, false, true);
            System.out.println("TU_COST|" + parallel + "|" + cost.minimumPower() + "|" + cost.totalWork());
        }
        System.out.println("IGNITION|" + OriginalCokeOvenRules.IGNITION_TICKS + "|"
                + OriginalCokeOvenRules.tickIgnition(1));
    }
}
