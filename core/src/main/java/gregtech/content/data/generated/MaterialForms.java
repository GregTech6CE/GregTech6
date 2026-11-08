package gregtech.content.data.generated;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialSentinels;
import gregtech.content.material.MaterialDefinitions;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.MaterialInfoRegistry;
import java.util.List;
import java.util.Set;

/** Compatibility query facade over canonical form facts, migrated from the GT6 MT/TD import. */
public final class MaterialForms {
    private MaterialForms() {}

    /** Flags in {@code TD.java} order. */
    public static final List<String> FLAGS = List.of(
            "ARMORS",
            "CONTAINERS",
            "CONTAINERS_FLUID",
            "CONTAINERS_GAS",
            "CONTAINERS_PLASMA",
            "CONTAINERS_SOLID",
            "DENSEPLATES",
            "DIRTY_DUSTS",
            "DUSTS",
            "EMPTY",
            "FOILS",
            "GASES",
            "GEMS",
            "INGOTS",
            "INGOTS_HOT",
            "LENSES",
            "LIQUID",
            "MOLTEN",
            "MULTIINGOTS",
            "MULTIPLATES",
            "ORES",
            "PARTS",
            "PIPES",
            "PLANTS",
            "PLASMA",
            "PLATES",
            "PROJECTILES",
            "RAILS",
            "STICKS",
            "TOOLS",
            "VAPORS",
            "WEAPONS",
            "WIRES");

    /** One line per material: {@code <GT6 id or normalised name>|<FLAG>,<FLAG>,…}. */
    /** Canonical per-material form facts from the published auxiliary snapshot. */
    public static Set<String> of(GTMaterial material) {
        if (material == null) return Set.of();
        if (material == MaterialSentinels.EMPTY) return Set.of("EMPTY");
        if (!material.isValid()) return Set.of();
        return MaterialInfoRegistry.relations(material.resolve().getName()).forms();
    }

    /**
     * Whether GT6 registers the given item-generator form for a material.
     * An addon material that declared ores also has {@code ORES}. That overlay is not part of the generated table.
     */
    public static boolean has(GTMaterial material, String flag) {
        if (material != null && "ORES".equals(flag) && AddonMaterials.hasOreForm(material)) return true;
        return of(material).contains(flag);
    }

    /** How many materials the original gives this form to. */
    public static int count(String flag) {
        int total = 0;
        for (var definition : MaterialDefinitions.all())
            if (MaterialDefinitions.info(definition.name()).relations().forms().contains(flag)) total++;
        return total;
    }

    public static String normalize(String name) {
        StringBuilder builder = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = Character.toLowerCase(name.charAt(i));
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') builder.append(c);
        }
        return builder.toString();
    }
}
