package gregtech.internal.fluid;

import gregtech.internal.material.GTMaterial;
import gregtech.internal.machine.crucible.CrucibleMaterialStack;
import java.util.List;

/** Shared canonical material/GT-unit storage contract for platform molten-fluid capabilities. */
public interface MoltenMaterialStorage {
    List<CrucibleMaterialStack> getContentView();
    long getTemperature();
    long getMoltenCapacityUnits();
    int fillMoltenMaterial(GTMaterial material, int millibuckets, long temperatureK, boolean execute);
    int drainMoltenMaterial(GTMaterial material, int millibuckets, boolean execute);
}
