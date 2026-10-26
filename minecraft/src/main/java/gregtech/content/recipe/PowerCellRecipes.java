/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Loader_MultiTileEntities:14701/14601, 200 material units of gas per cell. */
package gregtech.content.recipe;

import gregtech.content.energy.PowerCellSpec;
import gregtech.content.data.MachineRecipeMaps;

public final class PowerCellRecipes extends OriginalRecipeBatch {
    public static int register() {
        PowerCellRecipes batch = new PowerCellRecipes();
        for (PowerCellSpec spec : PowerCellSpec.ALL) {
            if (!spec.filled()) continue;
            batch.add(spec.id() + "/fill", MachineRecipeMaps.Canner, 256, 16,
                    items(item("gt6ce:" + spec.emptyId(), 1)), items(item("gt6ce:" + spec.id(), 1)),
                    fluids(fluid(spec.fuel(), 200_000)), fluids());
            batch.add(spec.id() + "/empty", MachineRecipeMaps.Canner, 256, 16,
                    items(item("gt6ce:" + spec.id(), 1)), items(item("gt6ce:" + spec.emptyId(), 1)), fluids(), fluids());
        }
        return batch.entries().size();
    }
}
