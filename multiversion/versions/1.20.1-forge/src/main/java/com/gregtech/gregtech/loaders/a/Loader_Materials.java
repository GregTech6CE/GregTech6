package com.gregtech.gregtech.loaders.a;

import com.gregtech.gregtech.api.addon.AddonMaterials;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialRoleFlags;
import com.gregtech.gregtech.loaders.IGTLoader;

public record Loader_Materials() implements IGTLoader {
    @Override
    public void run() {
        AddonMaterials.collectContributors();
        GTMaterialRegistry.init();
        MaterialRoleFlags.apply();
    }
}
