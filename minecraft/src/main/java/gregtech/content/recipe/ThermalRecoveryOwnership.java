package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;

/** One ownership decision shared by the audited input producer and the general thermal producer. */
final class ThermalRecoveryOwnership {
    private ThermalRecoveryOwnership() {}

    static boolean auditedInputRowsOwn(MaterialPrefix prefix, GTMaterial material) {
        if (material == null || !material.isValid()) return false;
        material = material.resolve();
        if (AddonMaterials.owns(material)) return false;
        return SourceThermalInputRules.form(prefix) != null || reviewedSpecialOwner(prefix, material);
    }

    static boolean reviewedSpecialOwner(MaterialPrefix prefix, GTMaterial material) {
        if (material == null || !material.isValid()) return false;
        material = material.resolve();
        if (!GTMaterialRegistry.isCoreMaterial(material)) return false;
        boolean metal = material.getId() == 220 && "Titanium".equals(material.getName())
                || material.getId() == 260 && "Iron".equals(material.getName());
        return metal
                && (prefix == MaterialPrefix.dustSmall
                        || prefix == MaterialPrefix.dustTiny
                        || prefix == MaterialPrefix.dustDiv72);
    }
}
