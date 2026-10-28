package gregtech.bootstrap.registration;

import gregtech.bootstrap.InitializationStep;
import gregtech.engine.material.GTMaterialRegistry;

public record MaterialLinkBootstrap() implements InitializationStep {
    @Override
    public void run() {
        GTMaterialRegistry.postInit();
        gregtech.content.material.VanillaUnificationLoader.register();
        gregtech.content.material.ModMaterialUnificationLoader.register();
        gregtech.content.material.VanillaCompositionLoader.register();
    }
}
