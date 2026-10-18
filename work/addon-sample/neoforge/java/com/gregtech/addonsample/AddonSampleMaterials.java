package com.gregtech.addonsample;

import com.gregtech.gregtech.api.addon.AddonMaterialContributor;
import com.gregtech.gregtech.api.addon.MaterialDeclaration;
import com.gregtech.gregtech.api.addon.MaterialFormRequest;

import java.util.List;

public final class AddonSampleMaterials implements AddonMaterialContributor {
    @Override public String addonId() { return "addon_sample"; }

    @Override public List<MaterialDeclaration> materials() {
        return List.of(MaterialDeclaration.builder(4291, "AmmoniumNitrate")
                .displayName("Ammonium Nitrate").color(0xC8D6C0).chemicalFormula("NH4NO3")
                .meltingPointKelvin(443).boilingPointKelvin(483).density(1.72F).dust().build());
    }

    @Override public List<MaterialFormRequest> forms() {
        return List.of();
    }
}
