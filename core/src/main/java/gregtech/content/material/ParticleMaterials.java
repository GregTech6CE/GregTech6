package gregtech.content.material;

import gregtech.api.material.GTMaterial;
import gregtech.engine.material.GTMaterialRegistry;

/** Particle symbols; identities and facts belong to the canonical catalog. */
public final class ParticleMaterials {
    private ParticleMaterials() {}

    public static final GTMaterial Photon = GTMaterialRegistry.requireInstalled("Photon");
    public static final GTMaterial Neutrino = GTMaterialRegistry.requireInstalled("Neutrino");
    public static final GTMaterial Neutron = GTMaterialRegistry.requireInstalled("Neutron");
    public static final GTMaterial Proton = GTMaterialRegistry.requireInstalled("Proton");
    public static final GTMaterial Electron = GTMaterialRegistry.requireInstalled("Electron");
}
