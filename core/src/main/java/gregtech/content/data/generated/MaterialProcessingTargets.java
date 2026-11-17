package gregtech.content.data.generated;

import gregtech.api.addon.material.MaterialKey;
import gregtech.api.material.GTMaterial;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialInfoRegistry;

/** Compatibility query facade over canonical processing targets, migrated from the GT6 MT import. */
public final class MaterialProcessingTargets {
    private MaterialProcessingTargets() {}

    public static final String[] KINDS = {
        "Solidifying", "Bending", "Compressing", "Cutting", "Forging", "Smashing", "Working"
    };
    public static final long DEFAULT_AMOUNT = 648_648_000L;

    public static String[] of(String materialName, String kind) {
        var material = GTMaterialRegistry.get(GTMaterial.sanitize(materialName));
        if (material == null || !material.isValid()) return null;
        String canonical = material.resolve().getName();
        var target = processing(canonical).get(kind);
        return target == null
                ? null
                : new String[] {
                    target.material().name().equals(canonical)
                            ? ""
                            : target.material().name(),
                    Long.toString(target.amount())
                };
    }

    public static int size() {
        return GTMaterialRegistry.sortedMaterials().stream()
                .filter(material -> material.isValid() && material.resolve() == material)
                .mapToInt(material -> processing(material.getName()).size())
                .sum();
    }

    public static int rowCount() {
        return size();
    }

    private static java.util.Map<String, gregtech.api.addon.material.MaterialTarget> processing(String name) {
        var result = new java.util.LinkedHashMap<String, gregtech.api.addon.material.MaterialTarget>();
        for (var entry : MaterialInfoRegistry.processes(new MaterialKey(name)).rules().entrySet()) {
            var kind = gregtech.api.addon.material.MaterialProcessKeys.legacyTargetName(entry.getKey())
                    .orElse(null);
            if (kind != null
                    && !entry.getValue().isEmpty()
                    && entry.getValue().get(0).value() instanceof gregtech.api.addon.material.MaterialTarget target)
                result.put(kind, target);
        }
        return result;
    }
}
