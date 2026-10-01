package com.gregtech.gregtech.content.material;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialDefinition;
import com.gregtech.gregtech.api.material.MaterialProperty;

/** GT6 MT.java particle row, IDs 1..5. Hidden identities, without generated item forms. */
public final class ParticleMaterials {
    private ParticleMaterials() {}
    public static final GTMaterial Photon = particle(1, "Photon", "y", 0xFFFFFF, 255, 0, 0, 0);
    public static final GTMaterial Neutrino = particle(2, "Neutrino", "v", 0xB4B4B4, 0, 0, 0, 0);
    public static final GTMaterial Neutron = particle(3, "Neutron", "n", 0x808080, 0, 0, 0, 1);
    public static final GTMaterial Proton = particle(4, "Proton", "p", 0xFF0000, 0, 1, 0, 0);
    public static final GTMaterial Electron = particle(5, "Electron", "e", 0x0000FF, 0, 0, 1, 0);

    private static GTMaterial particle(int id, String name, String symbol, int color, int alpha,
                                       long protons, long electrons, long neutrons) {
        return MaterialDefinition.builder(id, name).chemicalFormula(symbol).color(color).alpha(alpha)
                .atomicProperties(protons, electrons, neutrons, 0)
                .meltingPointKelvin(0).boilingPointKelvin(0).density(0)
                .properties(MaterialProperty.PARTICLE, MaterialProperty.HIDDEN).register();
    }

    public static void declare() { Photon.getClass(); }
}
