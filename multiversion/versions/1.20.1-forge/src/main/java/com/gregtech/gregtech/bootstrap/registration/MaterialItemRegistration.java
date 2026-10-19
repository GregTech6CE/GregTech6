package com.gregtech.gregtech.bootstrap.registration;

import com.gregtech.gregtech.bootstrap.InitializationStep;
import com.gregtech.gregtech.registry.GTMaterialRegistration;

public record MaterialItemRegistration() implements InitializationStep {
    @Override
    public void run() {
        GTMaterialRegistration.registerAll();
    }
}
