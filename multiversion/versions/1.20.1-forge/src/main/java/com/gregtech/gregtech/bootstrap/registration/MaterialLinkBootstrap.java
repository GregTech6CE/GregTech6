package com.gregtech.gregtech.bootstrap.registration;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.bootstrap.InitializationStep;

public record MaterialLinkBootstrap() implements InitializationStep {
    @Override
    public void run() {
        GTMaterialRegistry.postInit();
        com.gregtech.gregtech.content.material.VanillaUnificationLoader.register();
        com.gregtech.gregtech.content.material.VanillaCompositionLoader.register();
    }
}
