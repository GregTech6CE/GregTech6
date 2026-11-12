package gregtech.content.material;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.generated.MaterialWorkability;
import gregtech.engine.material.GTMaterialRegistry;
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
    // GT6 material factory/property identities retained by numeric ID across renamed port symbols.
    private static final Set<Integer> BOUNCY = Set.of(8196, 8197, 8198, 8199, 8217, 8218);
    private static final Set<Integer> STRETCHY = Set.of(8217);
    private static final Set<Integer> MEAT = Set.of(9700, 9701, 9710, 9711, 9712, 9713);
    private static final Set<Integer> ROTTEN = Set.of(9710, 9713);
    private static final Set<Integer> QUARTZ = Set.of(
            8000, 8003, 8317, 8342, 8346, 8347, 8348, 8374, 8389, 8393, 8394, 8395, 8396, 8397, 8398, 8399, 8445, 8457,
            9006, 9180);

    private static boolean coal(GTMaterial material) {
        if (material == null) return false;
        material = material.resolve();
        return !gregtech.engine.addon.AddonMaterials.owns(material)
                && COAL.contains(material.getId() + ":" + MaterialWorkability.normalize(material.getName()));
    }

    private static boolean sourceFact(GTMaterial material, Set<Integer> ids) {
        if (material == null) return false;
        material = material.resolve();
        return GTMaterialRegistry.isCoreMaterial(material) && ids.contains(material.getId());
    }

    public static boolean isMortarGrindable(GTMaterial material) {
        return coal(material) || MaterialWorkability.isMortarGrindable(material);
    }

    public static boolean isBrittle(GTMaterial material) {
        return coal(material) || MaterialWorkability.isBrittle(material);
    }

    public static boolean isQuartz(GTMaterial material) {
        return sourceFact(material, QUARTZ);
    }

    public static boolean isBouncy(GTMaterial material) {
        return sourceFact(material, BOUNCY);
    }

    public static boolean isStretchy(GTMaterial material) {
        return sourceFact(material, STRETCHY);
    }

    public static boolean isMeat(GTMaterial material) {
        return sourceFact(material, MEAT);
    }

    public static boolean isRotten(GTMaterial material) {
        return sourceFact(material, ROTTEN);
    }
}
