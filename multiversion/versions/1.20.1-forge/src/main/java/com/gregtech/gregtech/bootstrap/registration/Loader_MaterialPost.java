package com.gregtech.gregtech.bootstrap.registration;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.bootstrap.IGTLoader;

public record Loader_MaterialPost() implements IGTLoader {
    @Override
    public void run() {
        GTMaterialRegistry.postInit();
        com.gregtech.gregtech.content.material.VanillaUnificationLoader.register();
        com.gregtech.gregtech.content.material.VanillaCompositionLoader.register();
    }
}
