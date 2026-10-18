package com.gregtech.gregtech.loaders.b;

import com.gregtech.gregtech.loaders.IGTLoader;
import com.gregtech.gregtech.registry.GTMaterialRegistration;

public record Loader_MaterialRegistration() implements IGTLoader {
    @Override
    public void run() {
        GTMaterialRegistration.registerAll();
    }
}
