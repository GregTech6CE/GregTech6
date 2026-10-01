package com.gregtech.gregtech.api.machine.crucible;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.data.MaterialPrefix;

/** Original material item/raw ore payloads, after platform stack recovery checks. */
public final class CrucibleInputRules {
    private CrucibleInputRules() {}
    public static CrucibleMaterialStack materialItem(GTMaterial material, MaterialPrefix prefix) {
        return prefix == MaterialPrefix.oreRaw ? ore(material.resolve(), 1)
                : CrucibleMaterialStack.of(material.resolve(), prefix == null ? GTValues.U : prefix.getMaterialWeight());
    }
    public static CrucibleMaterialStack ore(GTMaterial material, long count) {
        return CrucibleMaterialStack.of(material.getTargetCrushingMaterial(),
                Math.multiplyExact(material.getTargetCrushingAmount(), count * material.getOreMultiplier()));
    }
}
