package gregtech.content.material;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialToolHelper;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;

/** Exact finite cohort for per-stack compatibility; overrides opt out of the captured source stats. */
public final class SourceMaterialToolStats {
    private SourceMaterialToolStats() {}

    public static boolean matchesCorrectedSource(GTMaterial material) {
        if (material == null || !material.isValid() || material.resolve() != material || AddonMaterials.owns(material))
            return false;
        var definition = MaterialDefinitions.find(material.getName());
        var info = MaterialDefinitions.info(material.getName());
        return definition != null
                && definition.id() == material.getId()
                && GTMaterialRegistry.get(material.getId()) == material
                && GTMaterialRegistry.get(material.getName()) == material
                && info.tooling().correctedToolStats()
                && info.toolProfile().equals(MaterialToolHelper.profile(material));
    }
}
