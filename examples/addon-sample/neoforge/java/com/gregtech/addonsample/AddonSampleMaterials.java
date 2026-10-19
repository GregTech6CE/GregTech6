package com.gregtech.addonsample;

import com.gregtech.gregtech.api.addon.AddonMaterialContributor;
import com.gregtech.gregtech.api.addon.CrucibleReactionDeclaration;
import com.gregtech.gregtech.api.addon.CrucibleReactionRemoval;
import com.gregtech.gregtech.api.addon.MaterialComposition;
import com.gregtech.gregtech.api.addon.MaterialDeclaration;
import com.gregtech.gregtech.api.addon.MaterialFormRequest;
import com.gregtech.gregtech.api.addon.SmeltingTargetDeclaration;
import com.gregtech.gregtech.api.material.GTValues;

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
        materials.add(MaterialDeclaration.builder("SampleCrucibleAlloy")
                .displayName("Sample Crucible Alloy").color(0xC4A574).meltingPointKelvin(1400)
                .boilingPointKelvin(2500).density(8F).metal().build());
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

    @Override public List<MaterialComposition> compositions() {
        return List.of(new MaterialComposition("SampleCrucibleAlloy", 4, List.of(
                new MaterialComposition.Part("SampleAlloy", 3 * GTValues.U),
                new MaterialComposition.Part("Copper", GTValues.U)), true));
    }

    @Override public List<SmeltingTargetDeclaration> smeltingTargets() {
        return List.of(new SmeltingTargetDeclaration("SampleAlloy", "SampleAlloy", GTValues.U));
    }

    @Override public List<CrucibleReactionDeclaration> crucibleReactions() {
        return List.of(new CrucibleReactionDeclaration("SampleCrucibleAlloy", 2, List.of(
                new CrucibleReactionDeclaration.Part("SampleAlloy", 1),
                new CrucibleReactionDeclaration.Part("Tin", 1))));
    }

    @Override public List<CrucibleReactionRemoval> crucibleRemovals() {
        return List.of(new CrucibleReactionRemoval("StainlessSteel",
                List.of("WroughtIron", "Nichrome", "Chromium", "Manganese")));
    }
}
