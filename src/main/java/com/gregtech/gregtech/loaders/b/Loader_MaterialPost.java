package com.gregtech.gregtech.loaders.b;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.loaders.IGTLoader;

public record Loader_MaterialPost() implements IGTLoader {
    @Override
    public void run() {
        GTMaterialRegistry.postInit();
        com.gregtech.gregtech.loader.VanillaUnificationLoader.register();
        com.gregtech.gregtech.loader.VanillaCompositionLoader.register();
    }
}
