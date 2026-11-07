package gregtech.content.material;

import gregtech.api.material.GTMaterial;
import gregtech.engine.material.GTMaterialRegistry;

/** Port-specific material references in the canonical catalog. */
public final class SupplementalMaterials {
    private SupplementalMaterials() {}

    public static GTMaterial porcelain() {
        return GTMaterialRegistry.requireInstalled("Porcelain");
    }

    public static GTMaterial sculk() {
        return GTMaterialRegistry.requireInstalled("Sculk");
    }
}
