package gregtech.bootstrap.registration;

import gregtech.internal.material.GTMaterialRegistry;
import gregtech.bootstrap.InitializationStep;

public record MaterialLinkBootstrap() implements InitializationStep {
    @Override
    public void run() {
        GTMaterialRegistry.postInit();
        gregtech.content.material.VanillaUnificationLoader.register();
        gregtech.content.material.VanillaCompositionLoader.register();
    }
}
