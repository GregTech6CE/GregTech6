package gregtech.bootstrap.registration;

import gregtech.bootstrap.InitializationStep;
import gregtech.content.material.MaterialItemCompositions;
import gregtech.engine.material.GTMaterialRegistry;

public record MaterialLinkBootstrap() implements InitializationStep {
    @Override
    public void run() {
        GTMaterialRegistry.postInit();
        MaterialItemCompositions.registerVanillaBindings();
    }
}
