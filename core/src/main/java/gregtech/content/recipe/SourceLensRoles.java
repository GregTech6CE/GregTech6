package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.material.MaterialFactKeys;
import gregtech.content.material.catalog.SourceLensMaterialFacts;
import gregtech.engine.addon.AddonMaterials;
import java.util.ArrayList;
import java.util.List;

/** Recipe-facing lookup over the material-owned lens role facts. */
public final class SourceLensRoles {
    private SourceLensRoles() {}

    public static SourceLensMaterialFacts.Color color(GTMaterial material) {
        if (material == null || !material.isValid() || AddonMaterials.owns(material)) return null;
        return MaterialFactKeys.value(material, MaterialFactKeys.SOURCE_LENS_ROLE)
                .orElse(null);
    }

    public static List<GTMaterial> materials(SourceLensMaterialFacts.Color color) {
        var result = new ArrayList<GTMaterial>();
        for (var material : gregtech.engine.material.GTMaterialRegistry.allMaterials()) {
            var resolved = material.resolve();
            if (color(resolved) == color) result.add(resolved);
        }
        return List.copyOf(result);
    }
}
