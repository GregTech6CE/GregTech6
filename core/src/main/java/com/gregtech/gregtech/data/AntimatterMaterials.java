package com.gregtech.gregtech.data;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.material.MaterialTextureSet;

/**
 * Anti-materials (anti-matter elements and particles), analogous to GregTech {@code AM.java}.
 * <p>
 * Full GT6 set can be transpiled later; this registers the core anti-particle row and a starter element block.
 */
public class AntimatterMaterials {
    protected AntimatterMaterials() {}

    private static boolean initialized = false;

    private static GTMaterial create(int id, String name) {
        return GTMaterialRegistry.createMaterial(id, name, name, 0xFFFFFF)
                .put(MaterialProperty.HIDDEN);
    }

    private static GTMaterial element(int id, String name, int color, MaterialProperty... props) {
        return create(id, name)
                .put(MaterialProperty.ELEMENT, MaterialProperty.ANTIMATTER)
                .setTextureSet(MaterialTextureSet.DULL)
                .setStats(1000, 2000, 1.0F);
    }

    /** Subatomic anti-particles. */
    public static GTMaterial
            y, Photon = y = create(4001, "Anti-Photon").setTooltipChemical("y").hide(),
            v, Neutrino = v = create(4002, "Anti-Neutrino").setTooltipChemical("v").hide(),
            n, Neutron = n = create(4003, "Anti-Neutron").setTooltipChemical("n").hide(),
            p, Proton = p = create(4004, "Anti-Proton").setTooltipChemical("p").hide(),
            e, Positron = e = create(4005, "Positron").setTooltipChemical("e").hide();

    /** Representative anti-elements (GT6 uses 4010+). */
    public static GTMaterial
            H, Hydrogen = H = element(4010, "Anti-Hydrogen", 0x0000FF),
            He, Helium = He = element(4020, "Anti-Helium", 0xFFFF78),
            Fe, Iron = Fe = element(4260, "Anti-Iron", 0xC8C8C8),
            Cu, Copper = Cu = element(4290, "Anti-Copper", 0xFF6400);

    public static void register() {
        if (initialized) {
            return;
        }
        initialized = true;
        GTMaterialRegistry.registerAlias("AntiElectron", "Positron");
    }
}
