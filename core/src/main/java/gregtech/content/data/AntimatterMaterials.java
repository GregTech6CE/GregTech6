package gregtech.content.data;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.engine.material.GTMaterialRegistry;

/**
 * Anti-materials (anti-matter elements and particles), analogous to GregTech {@code AM.java}.
 * <p>
 * Full GT6 set can be transpiled later; this registers the core anti-particle row and a starter element block.
 */
public class AntimatterMaterials {
    protected AntimatterMaterials() {}

    private static GTMaterial create(int id, String name) {
        return GTMaterialRegistry.requireInstalled(name);
    }

    private static GTMaterial element(int id, String name, int color, MaterialProperty... props) {
        return create(id, name);
    }

    /** Subatomic anti-particles. */
    public static GTMaterial y,
            Photon = y = create(4001, "Anti-Photon"),
            v,
            Neutrino = v = create(4002, "Anti-Neutrino"),
            n,
            Neutron = n = create(4003, "Anti-Neutron"),
            p,
            Proton = p = create(4004, "Anti-Proton"),
            e,
            Positron = e = create(4005, "Positron");

    /** Representative anti-elements (GT6 uses 4010+). */
    public static GTMaterial H,
            Hydrogen = H = element(4010, "Anti-Hydrogen", 0x0000FF),
            He,
            Helium = He = element(4020, "Anti-Helium", 0xFFFF78),
            Fe,
            Iron = Fe = element(4260, "Anti-Iron", 0xC8C8C8),
            Cu,
            Copper = Cu = element(4290, "Anti-Copper", 0xFF6400);
}
