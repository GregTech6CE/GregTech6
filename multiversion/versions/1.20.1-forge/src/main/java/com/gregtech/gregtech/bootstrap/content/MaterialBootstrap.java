package com.gregtech.gregtech.bootstrap.content;

import com.gregtech.gregtech.api.addon.AddonMaterials;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialRoleFlags;
import com.gregtech.gregtech.bootstrap.InitializationStep;

public record MaterialBootstrap() implements InitializationStep {
    @Override
    public void run() {
        AddonMaterials.collectContributors();
        GTMaterialRegistry.init();
        MaterialRoleFlags.apply();
    }
}
