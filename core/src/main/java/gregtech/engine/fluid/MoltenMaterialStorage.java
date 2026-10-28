package gregtech.engine.fluid;

import gregtech.api.material.GTMaterial;
import gregtech.engine.machine.crucible.CrucibleMaterialStack;
import java.util.List;

/** Shared canonical material/GT-unit storage contract for platform molten-fluid capabilities. */
public interface MoltenMaterialStorage {
    List<CrucibleMaterialStack> getContentView();

    long getTemperature();

    long getMoltenCapacityUnits();

    int fillMoltenMaterial(GTMaterial material, int millibuckets, long temperatureK, boolean execute);

    int drainMoltenMaterial(GTMaterial material, int millibuckets, boolean execute);
    /** Explicit phase ratio; legacy implementations must opt in before accepting special-unit fluids. */
    default int fillMoltenMaterial(
            GTMaterial material,
            int millibuckets,
            long temperatureK,
            boolean execute,
            MoltenMaterialUnits.Ratio ratio) {
        return ratio.equals(MoltenMaterialUnits.STANDARD)
                ? fillMoltenMaterial(material, millibuckets, temperatureK, execute)
                : 0;
    }

    default int drainMoltenMaterial(
            GTMaterial material, int millibuckets, boolean execute, MoltenMaterialUnits.Ratio ratio) {
        return ratio.equals(MoltenMaterialUnits.STANDARD) ? drainMoltenMaterial(material, millibuckets, execute) : 0;
    }
}
