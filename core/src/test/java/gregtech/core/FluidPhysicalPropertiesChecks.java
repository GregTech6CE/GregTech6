package gregtech.core;

import gregtech.internal.fluid.FluidPhysicalProperties;
import gregtech.content.fluid.FluidDefinitions;
import gregtech.content.data.FluidCatalog;

/** Executable source contracts; not a replacement for native entity GameTests. */
public final class FluidPhysicalPropertiesChecks {
    private static int assertions;
    public static void main(String[] args) {
        gregtech.internal.mod.ModData.bindPresence(id -> false);
        gregtech.internal.material.GTMaterialRegistry.init();
        verify();
        System.out.println("FluidPhysicalPropertiesChecks passed");
    }

    static int verify() {
        assertions = 0;
        check(FluidCatalog.isBuiltInDefinition(FluidCatalog.Beer), "built-in provenance exists before preparation");
        check(FluidCatalog.isBuiltInDefinition(FluidCatalog.Beer.withTemperature(301)),
                "immutable metadata copies keep built-in registry provenance");
        var staticWax = FluidCatalog.Molten_Wax;
        var ender = FluidCatalog.Ender;
        var declaredPaths = FluidCatalog.all();
        FluidDefinitions.prepare();
        var wax = FluidCatalog.get("GenMolten_Wax");
        check(wax.temperature() == 350 && wax.luminosity() == 10, "prepared static factory metadata");
        check(FluidCatalog.Molten_Wax == staticWax, "immutable declaration snapshot is not reflection-written");
        check(wax.registryName().equals(staticWax.registryName()) && FluidCatalog.isBuiltInDefinition(wax),
                "canonical static factory preserves registry identity and built-in provenance");
        check(FluidCatalog.get("Ender") == ender && ender.temperature() == 2723 && ender.luminosity() == 5,
                "generic molten sprite does not override authored dedicated properties");
        for (var declaration : declaredPaths.entrySet()) {
            var prepared = FluidCatalog.get(declaration.getKey());
            check(prepared != null && declaration.getValue().registryName().equals(prepared.registryName()),
                    "existing registry identity retained: " + declaration.getKey());
        }
        var cryolite = FluidCatalog.get("GenLiquid_Cryolite");
        check(cryolite == FluidCatalog.get("GenGas_Cryolite") && !cryolite.gas()
                        && cryolite.temperature() == 1285 && cryolite.luminosity() == 10,
                "legacy molten aliases share canonical metadata without changing state");
        check(FluidCatalog.get("GenMolten_Cryolite") == null, "metadata repair does not add a replacement ID");
        var preparedOnce = FluidCatalog.all();
        FluidDefinitions.prepare();
        check(preparedOnce.equals(FluidCatalog.all()), "material fluid preparation is value-idempotent");
        check(FluidCatalog.isBuiltInDefinition(FluidCatalog.get("Beer")),
                "import reconciliation retains built-in provenance");
        var generatedIron = FluidCatalog.get("GenMolten_Iron");
        check(generatedIron != null && !FluidCatalog.isBuiltInDefinition(generatedIron),
                "material-generated definitions are not part of the static directory");
        check(FluidPhysicalProperties.viscosity(FluidCatalog.Steam) == 200, "gas viscosity");
        check(FluidPhysicalProperties.density(FluidCatalog.Steam) == -1000, "explicit steam override");
        check(FluidPhysicalProperties.viscosity(FluidCatalog.HeliumPlasma) == 10, "plasma viscosity");
        check(FluidPhysicalProperties.density(FluidCatalog.HeliumPlasma) == -100000, "plasma density");
        check(FluidPhysicalProperties.nonLiquid(FluidCatalog.HeliumPlasma), "plasma phase");
        check(FluidPhysicalProperties.viscosity(FluidCatalog.get("GenMolten_Iron")) == 1000, "molten liquid viscosity");
        check(FluidPhysicalProperties.density(FluidCatalog.get("GenMolten_Iron")) > 7000, "material-backed iron density");
        check(FluidPhysicalProperties.viscosity(FluidCatalog.Lava_Pahoehoe) == 250000, "explicit pahoehoe override");
        check(!FluidPhysicalProperties.nonLiquid(FluidCatalog.Oil_Light), "oil remains liquid");
        check(FluidPhysicalProperties.density(FluidCatalog.Air) == 0, "explicit neutral air density");
        check(FluidPhysicalProperties.density(FluidCatalog.Oil_Soulsand) == 650, "soulsand oil override");
        check(FluidPhysicalProperties.density(FluidCatalog.MatterCharged) == -5000
                && !FluidPhysicalProperties.nonLiquid(FluidCatalog.MatterCharged), "negative density does not imply gas");
        check(FluidPhysicalProperties.viscosity(FluidCatalog.Aerotheum) == 100
                && FluidPhysicalProperties.nonLiquid(FluidCatalog.Aerotheum), "source aerotheum gas and viscosity");
        check(FluidPhysicalProperties.materialDensity(0.0012) == 0, "neutral density");
        check(FluidPhysicalProperties.materialDensity(0.0001) == -1000, "lighter-than-air conversion");
        check(FluidPhysicalProperties.materialDensity(2) == 2000, "heavy gas/liquid conversion");
        check(FluidPhysicalProperties.materialDensity(Double.MAX_VALUE) == Integer.MAX_VALUE, "density saturation");
        for (var entry : FluidCatalog.all().values())
            check(FluidPhysicalProperties.viscosity(entry) > 0, "resolved viscosity " + entry.registryName());
        return assertions;
    }
    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
}
