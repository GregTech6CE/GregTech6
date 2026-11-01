package gregtech.content.material;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.generated.MaterialWorkability;
import java.util.Set;

/** Processing flags inherited from GT6 material factories in addition to declaration flags. */
public final class MaterialProcessingProperties {
    private MaterialProcessingProperties() {}

    private static final Set<String> COAL = Set.of(
            "8336:charcoal",
            "8334:coal",
            "8349:coalcoke",
            "8362:anthracite",
            "8363:prismane",
            "8364:lonsdaleite",
            "8337:lignite",
            "8365:lignitecoke",
            "8390:petroleumcoke");

    private static boolean coal(GTMaterial material) {
        if (material == null) return false;
        material = material.resolve();
        return !gregtech.engine.addon.AddonMaterials.owns(material)
                && COAL.contains(material.getId() + ":" + MaterialWorkability.normalize(material.getName()));
    }

    public static boolean isMortarGrindable(GTMaterial material) {
        return coal(material) || MaterialWorkability.isMortarGrindable(material);
    }

    public static boolean isBrittle(GTMaterial material) {
        return coal(material) || MaterialWorkability.isBrittle(material);
    }
}
