package gregtech.content.fluid;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.FluidCatalog;
import gregtech.engine.fluid.FluidPhysicalProperties;
import gregtech.engine.fluid.MoltenFluidPlans;
import gregtech.engine.material.GTMaterialRegistry;

import java.util.EnumSet;
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
            equal(expected.temperature(), entry.temperature(), "source molten temperature");
            equal(expected.density(), FluidPhysicalProperties.density(entry), "source molten density");
            equal(1000, FluidPhysicalProperties.viscosity(entry), "source liquid viscosity");
            equal(10, entry.luminosity(), "createMolten final light overrides initial glowstone light");
            equal(144, MoltenFluidPlans.millibuckets(GTValues.U), "molten material units are 144 mB/U");
            equal(GTValues.U, MoltenFluidPlans.toUnits(144), "molten volume round trip");
        }
        for (var entry : existing.entrySet()) {
            var prepared = FluidCatalog.get(entry.getKey());
            check(prepared != null, "no preexisting fluid field removed");
            equal(entry.getValue().registryName(), prepared.registryName(), "no preexisting fluid ID replaced");
        }
        var prepared = FluidCatalog.all();
        FluidDefinitions.prepare();
        equal(prepared, FluidCatalog.all(), "repeat preparation must neither duplicate nor overwrite definitions");
    }
}
