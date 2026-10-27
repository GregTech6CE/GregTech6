package gregtech.content.fluid;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.FluidCatalog;
import gregtech.engine.fluid.FluidPhysicalProperties;
import gregtech.engine.fluid.MoltenFluidPlans;
import gregtech.engine.material.GTMaterialRegistry;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/** Source molten declarations and shared physical metadata, not native world-fluid behavior. */
public final class SourceMoltenFluidContracts {
    private SourceMoltenFluidContracts() {}

    private record Expected(String name, int id, int temperature, int density) {}

    private static final List<Expected> EXPECTED = List.of(
            new Expected("Nikolite", 8340, 1500, 3195),
            new Expected("Glowstone", 8341, 500, 6351),
            new Expected("GlowstoneCeres", 8368, 500, 6351),
            new Expected("GlowstoneIo", 8369, 500, 6351),
            new Expected("GlowstoneEnceladus", 8370, 500, 6351),
            new Expected("GlowstoneProteus", 8371, 500, 6351),
            new Expected("GlowstonePluto", 8372, 500, 6351),
            new Expected("Gloomstone", 8456, 500, 6351),
            new Expected("VanadiumMagnetite", 9143, 1236, 2561));

    // Source MOLTEN declarations omitted from the imported flags. Densities apply the source
    // factory conversion to retained native float stats; this does not repair import precision.
    private static final List<Expected> MISSING_PHASES = List.of(
            new Expected("Carbon14", 62, 3800, 2266),
            new Expected("TritaniumElemental", 1250, 2000, 25000),
            new Expected("Trinium", 1260, 2645, 1068),
            new Expected("DuraniumElemental", 1450, 1200, 20000),
            new Expected("Naquadah", 1740, 1500, 21000),
            new Expected("NaquadahEnriched", 1741, 1500, 22000),
            new Expected("Naquadria", 1742, 1500, 20000),
            new Expected("Abyssalnite", 1850, 1500, 15000),
            new Expected("Coralium", 1860, 2000, 20000),
            new Expected("Dreadium", 1870, 2500, 25000),
            new Expected("Ethaxium", 1880, 3000, 30000),
            new Expected("Atlarus", 2210, 3276, 21246),
            new Expected("PotassiumCarbonate", 8020, 1164, 665),
            new Expected("TungstenTrioxide", 8026, 1746, 4813),
            new Expected("FerrousChloride", 8030, 950, 2626),
            new Expected("LithiumPerchlorate", 8034, 509, 90),
            new Expected("Tallow", 8244, 350, 1000),
            new Expected("Asphalt", 8266, 1000, 1000),
            new Expected("Stone", 8500, 1100, 1000),
            new Expected("SpaceStone", 8512, 1000, 1000),
            new Expected("MoonStone", 8513, 1000, 1000),
            new Expected("MarsStone", 8515, 1000, 1000),
            new Expected("Ilmenite", 9120, 782, 2483),
            new Expected("Rutile", 9192, 2041, 1514));

    private record MaterialState(Set<MaterialProperty> flags, GTMaterial target, long targetAmount,
                                 int melting, int boiling, float density) {}

    private static MaterialState state(GTMaterial material) {
        return new MaterialState(Set.copyOf(material.getProperties()), material.getTargetSmeltingMaterial(),
                material.getTargetSmeltingAmount(), material.getMeltingPoint(), material.getBoilingPoint(),
                material.getDensity());
    }

    public static void run() {
        GTMaterialRegistry.init();
        FluidCatalog.bootstrap();
        Map<String, Set<MaterialProperty>> originalFlags = new LinkedHashMap<>();
        Map<String, Float> originalDensity = new LinkedHashMap<>();
        for (Expected expected : EXPECTED) {
            GTMaterial material = GTMaterialRegistry.get(expected.name()).resolve();
            equal(expected.id(), material.getId(), "pinned material identity " + expected.name());
            originalFlags.put(expected.name(), Set.copyOf(material.getProperties()));
            originalDensity.put(expected.name(), material.getDensity());
        }
        var existing = FluidCatalog.all();
        var missingStates = restoreOnlyReviewedPhases();
        FluidDefinitions.prepare();
        FluidCatalogUsageContracts.run();
        for (Expected expected : EXPECTED) {
            GTMaterial material = GTMaterialRegistry.get(expected.name()).resolve();
            var flags = EnumSet.copyOf(originalFlags.get(expected.name()));
            flags.add(MaterialProperty.MOLTEN);
            equal(flags, material.getProperties(), "restore only MOLTEN for " + expected.name());
            check(!material.has(MaterialProperty.MELTING), "do not add crucible MELTING " + expected.name());
            equal(expected.temperature(), material.getMeltingPoint(), "keep source material temperature");
            equal(originalDensity.get(expected.name()), material.getDensity(), "keep source material density");

            moltenDefinition(expected);
        }
        for (Expected expected : MISSING_PHASES) {
            var material = GTMaterialRegistry.get(expected.name()).resolve();
            equal(missingStates.get(expected.name()), state(material),
                    "phase admission changes no material flags, targets or stats: " + expected.name());
            moltenDefinition(expected);
        }
        var cryolite = FluidCatalog.get("GenLiquid_Cryolite");
        equal("cryolite", cryolite.registryName(), "preserve established Cryolite ID");
        equal(1285, cryolite.temperature(), "Cryolite remains source molten temperature");
        equal(10, cryolite.luminosity(), "Cryolite remains source molten light");
        equal(null, FluidCatalog.get("GenMolten_Cryolite"), "no duplicate Cryolite molten phase");
        for (var entry : existing.entrySet()) {
            var prepared = FluidCatalog.get(entry.getKey());
            check(prepared != null, "no preexisting fluid field removed");
            equal(entry.getValue().registryName(), prepared.registryName(), "no preexisting fluid ID replaced");
        }
        var prepared = FluidCatalog.all();
        FluidDefinitions.prepare();
        equal(prepared, FluidCatalog.all(), "repeat preparation must neither duplicate nor overwrite definitions");
    }

    private static Map<String, MaterialState> restoreOnlyReviewedPhases() {
        var before = FluidCatalog.all();
        // Numeric/name resemblance cannot grant a foreign object the built-in source phase.
        FluidCatalog.registerGeneratedMoltenFluids(List.of(
                new GTMaterial(62, "Carbon14", "same identity, not core", 0),
                new GTMaterial(62, "ForeignCarbon", "wrong name", 0),
                new GTMaterial(10062, "Carbon14", "wrong ID", 0),
                GTMaterialRegistry.get("Magicwood"), GTMaterialRegistry.get("TungsticAcid")));
        equal(before, FluidCatalog.all(), "no source-negative, wrong identity or foreign admission");

        Map<String, MaterialState> states = new LinkedHashMap<>();
        Set<String> expectedFields = new HashSet<>();
        for (Expected expected : MISSING_PHASES) {
            var material = GTMaterialRegistry.get(expected.name()).resolve();
            equal(expected.id(), material.getId(), "exact restored source identity " + expected.name());
            check(!material.has(MaterialProperty.MOLTEN) && !material.has(MaterialProperty.METAL)
                    && !material.has(MaterialProperty.ALLOY), "no general flag workaround " + expected.name());
            states.put(expected.name(), state(material));
            expectedFields.add("GenMolten_" + expected.name());
        }
        FluidCatalog.registerGeneratedMoltenFluids(MISSING_PHASES.stream()
                .map(expected -> GTMaterialRegistry.get(expected.name())).toList());
        Set<String> added = new HashSet<>(FluidCatalog.all().keySet());
        added.removeAll(before.keySet());
        equal(24, expectedFields.size(), "bounded source phase roster");
        equal(expectedFields, added, "all and only the 24 reviewed missing phases added");
        for (var entry : before.entrySet())
            check(entry.getValue() == FluidCatalog.get(entry.getKey()), "no preexisting definition overwritten");
        var prepared = FluidCatalog.all();
        FluidCatalog.registerGeneratedMoltenFluids(MISSING_PHASES.stream()
                .map(expected -> GTMaterialRegistry.get(expected.name())).toList());
        for (var entry : prepared.entrySet())
            check(entry.getValue() == FluidCatalog.get(entry.getKey()), "existing field/path retains exact ownership");
        return states;
    }

    private static void moltenDefinition(Expected expected) {
        String field = "GenMolten_" + expected.name();
        String registryName = "molten." + expected.name().toLowerCase(Locale.ROOT);
        var entry = FluidCatalog.get(field);
        check(entry != null, "missing restored molten definition " + field);
        equal(registryName, entry.registryName(), "existing molten registry convention");
        equal(expected.name(), entry.materialKey(), "exact material binding");
        check(FluidCatalog.isGeneratedMoltenDefinition(field, entry), "existing factory owns " + field);
        check(!FluidCatalog.isBuiltInDefinition(entry), "no built-in fluid replacement " + field);
        equal(1L, FluidCatalog.all().values().stream()
                .filter(e -> e.registryName().equals(registryName)).count(), "one exact molten registry ID");
        equal(FluidCatalog.FluidTextureMode.GENERIC_MOLTEN, entry.textureMode(), "molten visual family");
        check(!entry.gas() && entry.hasFlag(FluidCatalog.FluidFlags.LIQUID), "molten liquid phase");
        equal(expected.temperature(), entry.temperature(), "source molten factory temperature");
        equal(expected.density(), FluidPhysicalProperties.density(entry), "source factory on retained material density");
        equal(1000, FluidPhysicalProperties.viscosity(entry), "source liquid viscosity");
        equal(10, entry.luminosity(), "createMolten final light overrides initial glowstone light");
        equal(144, MoltenFluidPlans.millibuckets(GTValues.U), "molten material units are 144 mB/U");
        equal(GTValues.U, MoltenFluidPlans.toUnits(144), "molten volume round trip");
    }
}
