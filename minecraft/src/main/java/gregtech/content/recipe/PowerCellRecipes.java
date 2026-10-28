/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Multi-tile ids 14601/14701, 200 material units of gas per cell. */
package gregtech.content.recipe;

import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.energy.PowerCellSpec;

public final class PowerCellRecipes extends OriginalRecipeBatch {
    public static int register() {
        PowerCellRecipes batch = new PowerCellRecipes();
        for (PowerCellSpec spec : PowerCellSpec.ALL) {
            if (!spec.filled()) continue;
            batch.add(
                    spec.id() + "/fill",
                    MachineRecipeMaps.Canner,
                    256,
                    16,
                    items(item("gt6ce:" + spec.emptyId(), 1)),
                    items(item("gt6ce:" + spec.id(), 1)),
                    fluids(fluid(spec.fuel(), 200_000)),
                    fluids());
            batch.add(
                    spec.id() + "/empty",
                    MachineRecipeMaps.Canner,
                    256,
                    16,
                    items(item("gt6ce:" + spec.id(), 1)),
                    items(item("gt6ce:" + spec.emptyId(), 1)),
                    fluids(),
                    fluids());
        }
        return batch.entries().size();
    }
}
