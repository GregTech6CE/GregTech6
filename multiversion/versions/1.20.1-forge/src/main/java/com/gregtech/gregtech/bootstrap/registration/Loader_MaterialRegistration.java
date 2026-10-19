package com.gregtech.gregtech.bootstrap.registration;

import com.gregtech.gregtech.bootstrap.IGTLoader;
import com.gregtech.gregtech.registry.GTMaterialRegistration;

public record Loader_MaterialRegistration() implements IGTLoader {
    @Override
    public void run() {
        GTMaterialRegistration.registerAll();
    }
}
