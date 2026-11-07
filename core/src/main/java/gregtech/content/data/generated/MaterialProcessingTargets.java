package gregtech.content.data.generated;

import gregtech.api.material.GTMaterial;
import gregtech.content.material.MaterialDefinitions;

/** Compatibility query facade over canonical processing targets, migrated from the GT6 MT import. */
public final class MaterialProcessingTargets {
    private MaterialProcessingTargets() {}

    public static final String[] KINDS = {
        "Solidifying", "Bending", "Compressing", "Cutting", "Forging", "Smashing", "Working"
    };
    public static final long DEFAULT_AMOUNT = 648_648_000L;

    public static String[] of(String materialName, String kind) {
        var definition = MaterialDefinitions.find(GTMaterial.sanitize(materialName));
        if (definition == null) return null;
        var target = processing(definition.name()).get(kind);
        return target == null
                ? null
                : new String[] {
                    target.material().name().equals(definition.name())
                            ? ""
                            : target.material().name(),
                    Long.toString(target.amount())
                };
    }

    public static int size() {
        return MaterialDefinitions.all().stream()
                .mapToInt(d -> processing(d.name()).size())
                .sum();
    }

    public static int rowCount() {
        return size();
    }

    private static java.util.Map<String, gregtech.api.addon.material.MaterialTarget> processing(String name) {
        var result = new java.util.LinkedHashMap<String, gregtech.api.addon.material.MaterialTarget>();
        for (var entry : MaterialDefinitions.info(name).processes().rules().entrySet()) {
            var kind = gregtech.api.addon.material.MaterialProcessKeys.legacyTargetName(entry.getKey());
            if (!kind.isEmpty()
                    && !entry.getValue().isEmpty()
                    && entry.getValue().get(0).value() instanceof gregtech.api.addon.material.MaterialTarget target)
                result.put(kind, target);
        }
        return result;
    }
}
