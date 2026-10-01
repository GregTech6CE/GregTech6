package com.gregtech.gregtech.api.fluid;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.machine.crucible.CrucibleMaterialStack;
import java.util.List;

/** Shared canonical material/GT-unit storage contract for platform molten-fluid capabilities. */
public interface MoltenMaterialStorage {
    List<CrucibleMaterialStack> getContentView();
    long getTemperature();
    long getMoltenCapacityUnits();
    int fillMoltenMaterial(GTMaterial material, int millibuckets, long temperatureK, boolean execute);
    int drainMoltenMaterial(GTMaterial material, int millibuckets, boolean execute);
}
