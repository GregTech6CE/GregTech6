package com.gregtech.addonsample;

import com.gregtech.gregtech.api.addon.AddonMaterialContributor;
import com.gregtech.gregtech.api.addon.MaterialDeclaration;
import com.gregtech.gregtech.api.addon.MaterialFormRequest;

import java.util.ArrayList;
import java.util.List;

public final class AddonSampleMaterials implements AddonMaterialContributor {
    @Override public String addonId() { return "addon_sample"; }

    @Override public List<MaterialDeclaration> materials() {
        var materials = new ArrayList<MaterialDeclaration>();
        materials.add(MaterialDeclaration.builder("AmmoniumNitrate")
                .displayName("Ammonium Nitrate").color(0xC8D6C0).chemicalFormula("NH4NO3")
                .meltingPointKelvin(443).boilingPointKelvin(483).density(1.72F).dust().build());
        materials.add(MaterialDeclaration.builder("SampleAlloy")
                .displayName("Sample Alloy").color(0x8A9BB0).meltingPointKelvin(450)
                .boilingPointKelvin(2000).density(7F).metal().build());
        int scale = Integer.getInteger("gregtech.materialScale", 0);
        for (int i = 0; i < scale; i++) {
            String name = String.format(i % 2 == 0 ? "ScaleDust%04d" : "ScaleMetal%04d", i / 2);
            var builder = MaterialDeclaration.builder(name).color(0x808080);
            materials.add((i % 2 == 0 ? builder.dust() : builder.metal()).build());
        }
        return List.copyOf(materials);
    }

    @Override public List<MaterialFormRequest> forms() {
        return List.of();
    }
}
