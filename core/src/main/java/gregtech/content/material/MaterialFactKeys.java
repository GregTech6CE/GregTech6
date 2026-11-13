package gregtech.content.material;

import gregtech.api.addon.material.DataKey;
import gregtech.api.addon.material.MaterialInfoTool;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialProcessKey;
import gregtech.api.material.GTMaterial;
import gregtech.content.material.catalog.SourceExtrusionMaterialFacts;
import gregtech.content.material.catalog.SourceLensMaterialFacts;
import gregtech.content.material.catalog.SourceRecyclingMaterialFacts;
import gregtech.content.material.catalog.SourceWashingMaterialFacts;
import java.util.Optional;
import java.util.Set;

/** Typed material process keys for facts consumed by recipe families. */
public final class MaterialFactKeys {
    private MaterialFactKeys() {}

    public enum Decision {
        YES,
        NO,
        UNKNOWN
    }

    public record Identity(int id, String name) {
        public Identity {
            java.util.Objects.requireNonNull(name, "name");
        }

        public boolean matches(int materialId, String canonicalName) {
            return id == materialId && name.equals(canonicalName);
        }
    }

    public static final MaterialProcessKey<Decision> SOURCE_FURNACE =
            single("source_furnace", Decision.class);
    public static final MaterialProcessKey<Decision> SOURCE_GRADED_GEM =
            single("source_graded_gem", Decision.class);
    public static final MaterialProcessKey<Decision> SOURCE_ORE_INPUT =
            single("source_ore_input", Decision.class);
    public static final MaterialProcessKey<PrefixExclusions> SOURCE_SIFTING_EXCLUSIONS =
            single("source_sifting_exclusions", PrefixExclusions.class);
    public static final MaterialProcessKey<PrefixExclusions> SOURCE_CRUSHED_EXCLUSIONS =
            single("source_crushed_exclusions", PrefixExclusions.class);
    public static final MaterialProcessKey<OreShredderProfile> SOURCE_ORE_SHREDDER =
            single("source_ore_shredder", OreShredderProfile.class);
    public static final MaterialProcessKey<Decision> SOURCE_MACERATION =
            single("source_maceration", Decision.class);
    public static final MaterialProcessKey<SourceWashingMaterialFacts.Binding> SOURCE_WASHING =
            single("source_washing", SourceWashingMaterialFacts.Binding.class);
    public static final MaterialProcessKey<MaterialFluidUnits> MATERIAL_FLUID_UNIT =
            single("material_fluid_unit", MaterialFluidUnits.class);
    public static final MaterialProcessKey<GenerificationTarget> GENERIFICATION_TARGET =
            single("generification_target", GenerificationTarget.class);
    public static final MaterialProcessKey<SourceExtrusionMaterialFacts.Profile> SOURCE_EXTRUSION =
            single("source_extrusion", SourceExtrusionMaterialFacts.Profile.class);
    public static final MaterialProcessKey<SourceLensMaterialFacts.Color> SOURCE_LENS_ROLE =
            single("source_lens_role", SourceLensMaterialFacts.Color.class);
    public static final MaterialProcessKey<CinnabarFollowonRule> SOURCE_CINNABAR_FOLLOWON =
            single("source_cinnabar_followon", CinnabarFollowonRule.class);
    public static final MaterialProcessKey<GeneratedFluidRules> SOURCE_GENERATED_FLUID =
            single("source_generated_fluid", GeneratedFluidRules.class);
    public static final MaterialProcessKey<SourceRecyclingMaterialFacts.Facts> SOURCE_RECYCLING =
            single("source_recycling", SourceRecyclingMaterialFacts.Facts.class);
    public static final MaterialProcessKey<Boolean> SOURCE_MOLTEN_FLUID =
            single("source_molten_fluid", Boolean.class);
    public static final MaterialProcessKey<OreCrushingFact> SOURCE_ORE_CRUSHING =
            single("source_ore_crushing", OreCrushingFact.class);
    public static final MaterialProcessKey<PurifiedCentrifugePhase> SOURCE_PURIFIED_CENTRIFUGE_PHASE =
            single("source_purified_centrifuge_phase", PurifiedCentrifugePhase.class);
    public static final MaterialProcessKey<PurifiedCentrifugeNoDefault> SOURCE_PURIFIED_CENTRIFUGE_NO_DEFAULT =
            single("source_purified_centrifuge_no_default", PurifiedCentrifugeNoDefault.class);
    public static final MaterialProcessKey<Boolean> SOURCE_ADDITIONAL_WIRE_FAMILY =
            single("source_additional_wire_family", Boolean.class);
    public static final MaterialProcessKey<Boolean> COMPRESSOR_DUST_TO_GEM_PLATE =
            single("compressor_dust_to_gem_plate", Boolean.class);

    public record PrefixExclusions(Set<String> prefixes) {
        public PrefixExclusions {
            prefixes = Set.copyOf(prefixes);
        }

        public boolean excludes(String prefix) {
            return prefixes.contains(prefix);
        }
    }

    public record OreShredderProfile(int quality, boolean mortar) {
        public OreShredderProfile {
            if (quality < 0) throw new IllegalArgumentException("Negative source tool quality");
        }
    }

    public record MaterialFluidUnit(
            String phase,
            String fluidField,
            String registryPath,
            long referenceSourceUnits,
            long referenceMillibuckets) {
        public MaterialFluidUnit {
            if (phase == null || phase.isBlank()) throw new IllegalArgumentException("Missing material fluid phase");
            if (fluidField == null || fluidField.isBlank())
                throw new IllegalArgumentException("Missing material fluid field");
            if (registryPath == null || registryPath.isBlank())
                throw new IllegalArgumentException("Missing material fluid registry path");
            if (referenceSourceUnits <= 0 || referenceMillibuckets <= 0)
                throw new IllegalArgumentException("Invalid material fluid unit");
        }
    }

    public record MaterialFluidUnits(Set<MaterialFluidUnit> units) {
        public MaterialFluidUnits {
            units = Set.copyOf(units);
            if (units.isEmpty()) throw new IllegalArgumentException("Material fluid units must not be empty");
        }
    }

    public record GenerificationTarget(int materialId) {
        public GenerificationTarget {
            if (materialId <= 0 || materialId >= 10_000)
                throw new IllegalArgumentException("Invalid generification target id: " + materialId);
        }
    }

    public record CinnabarFollowonRule(Identity cinnabar, java.util.List<Identity> byproducts, Identity sluiceSand) {
        public CinnabarFollowonRule {
            java.util.Objects.requireNonNull(cinnabar, "cinnabar");
            byproducts = java.util.List.copyOf(byproducts);
            java.util.Objects.requireNonNull(sluiceSand, "sluiceSand");
        }
    }

    public record GeneratedFluidRule(
            String field, String registryPath, int temperature, float materialDensity, boolean wholeDustMetal) {
        public GeneratedFluidRule {
            if (field == null || field.isBlank() || registryPath == null || registryPath.isBlank())
                throw new IllegalArgumentException("Invalid generated fluid binding");
            if (temperature < 0 || !Float.isFinite(materialDensity) || materialDensity < 0)
                throw new IllegalArgumentException("Invalid generated fluid physical facts");
        }
    }

    public record GeneratedFluidRules(Set<GeneratedFluidRule> rules) {
        public GeneratedFluidRules {
            rules = Set.copyOf(rules);
            if (rules.isEmpty()) throw new IllegalArgumentException("Generated fluid rules must not be empty");
        }
    }

    public enum HexoriumKind {
        NONE,
        BLACK_WHITE,
        RED_GREEN_BLUE
    }

    public record OreCrushingFact(Decision pulverizing, HexoriumKind hexorium) {
        public OreCrushingFact {
            java.util.Objects.requireNonNull(pulverizing, "pulverizing");
            java.util.Objects.requireNonNull(hexorium, "hexorium");
        }
    }

    public record PurifiedCentrifugePhase(String phase, int meltingKelvin, int boilingKelvin) {
        public PurifiedCentrifugePhase {
            if (!"gas".equals(phase) && !"liquid".equals(phase))
                throw new IllegalArgumentException("Invalid purified centrifuge phase");
        }
    }

    public record PurifiedCentrifugeNoDefault(String phase, int meltingKelvin, int boilingKelvin) {
        public PurifiedCentrifugeNoDefault {
            if (!"gas".equals(phase) && !"liquid".equals(phase))
                throw new IllegalArgumentException("Invalid purified centrifuge default phase");
        }
    }

    public static <T> Optional<T> value(GTMaterial material, MaterialProcessKey<T> key) {
        if (material == null || !material.isValid()) return Optional.empty();
        return MaterialInfoTool.currentProcess(new MaterialKey(material.resolve().getName()), key)
                .map(rule -> rule.value());
    }

    public static Optional<SourceRecyclingMaterialFacts.Facts> sourceRecycling(GTMaterial material) {
        return value(material, SOURCE_RECYCLING);
    }

    private static <T> MaterialProcessKey<T> single(String path, Class<T> type) {
        return MaterialProcessKey.single(new DataKey("gt6ce", path), type);
    }
}
