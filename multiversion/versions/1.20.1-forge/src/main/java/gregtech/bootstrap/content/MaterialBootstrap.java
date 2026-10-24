package gregtech.bootstrap.content;

import gregtech.api.addon.AddonMaterials;
import gregtech.api.material.GTMaterialRegistry;
import gregtech.api.material.MaterialRoleFlags;
import gregtech.bootstrap.InitializationStep;

public record MaterialBootstrap() implements InitializationStep {
    @Override
    public void run() {
        AddonMaterials.collectContributors();
        GTMaterialRegistry.init();
        MaterialRoleFlags.apply();
    }
}
