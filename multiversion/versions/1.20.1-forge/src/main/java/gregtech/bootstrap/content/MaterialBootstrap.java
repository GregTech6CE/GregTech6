package gregtech.bootstrap.content;

import gregtech.internal.addon.AddonMaterials;
import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.material.MaterialRoleFlags;
import gregtech.bootstrap.InitializationStep;

public record MaterialBootstrap() implements InitializationStep {
    @Override
    public void run() {
        AddonMaterials.collectContributors();
        GTMaterialRegistry.init();
        MaterialRoleFlags.apply();
    }
}
