package gregtech.bootstrap.content;

import gregtech.bootstrap.InitializationStep;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialRoleFlags;

public record MaterialBootstrap() implements InitializationStep {
    @Override
    public void run() {
        AddonMaterials.collectContributors();
        GTMaterialRegistry.init();
        MaterialRoleFlags.apply();
    }
}
