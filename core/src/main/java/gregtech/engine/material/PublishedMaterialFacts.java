package gregtech.engine.material;

import gregtech.api.addon.material.*;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import java.util.List;

/** Runtime reads from the frozen material catalog and process registry. */
public final class PublishedMaterialFacts {
    private PublishedMaterialFacts() {}

    public static MaterialTarget target(GTMaterial material, MaterialProcessKey<MaterialTarget> key) {
        var canonical = material.resolve();
        return MaterialInfoRegistry.processes(new MaterialKey(canonical.getName()))
                .findSingle(key)
                .map(rule -> rule.value())
                .orElseGet(() -> new MaterialTarget(
                        new MaterialKey(canonical.getName()),
                        key.equals(MaterialProcessKeys.SMELTING) ? 0 : GTValues.U));
    }

    public static GTMaterial targetMaterial(GTMaterial material, MaterialProcessKey<MaterialTarget> key) {
        var resolved = GTMaterialRegistry.get(target(material, key).material().name());
        return resolved.isValid() ? resolved.resolve() : material.resolve();
    }

    public static OreProcessingRule ore(GTMaterial material) {
        return MaterialInfoRegistry.processes(new MaterialKey(material.resolve().getName()))
                .findSingle(MaterialProcessKeys.ORE_PROCESSING)
                .map(rule -> rule.value())
                .orElseGet(() -> new OreProcessingRule(List.of(), 1, 1));
    }

    public static <T> T scalar(GTMaterial material, PropertyKey<T> key) {
        return MaterialViews.catalog()
                .find(new MaterialKey(material.resolve().getName()))
                .flatMap(view -> view.property(key))
                .orElseThrow(() -> new IllegalStateException(
                        "Missing material property " + key.id() + " for " + material.getName()));
    }
}
