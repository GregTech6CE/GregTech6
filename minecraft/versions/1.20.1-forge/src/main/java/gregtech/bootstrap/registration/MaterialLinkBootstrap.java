package gregtech.bootstrap.registration;

import gregtech.engine.material.GTMaterialRegistry;
import gregtech.bootstrap.InitializationStep;

public record MaterialLinkBootstrap() implements InitializationStep {
    @Override
    public void run() {
        GTMaterialRegistry.postInit();
        gregtech.content.material.VanillaUnificationLoader.register();
        gregtech.content.material.VanillaCompositionLoader.register();
    }
}
