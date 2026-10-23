package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.fluid.FluidPhysicalProperties;
import com.gregtech.gregtech.content.fluid.FluidDefinitions;
import com.gregtech.gregtech.data.FluidCatalog;

/** Executable source contracts; not a replacement for native entity GameTests. */
public final class FluidPhysicalPropertiesChecks {
    private static int assertions;
    public static void main(String[] args) {
        com.gregtech.gregtech.api.mod.ModData.bindPresence(id -> false);
        com.gregtech.gregtech.api.material.GTMaterialRegistry.init();
        verify();
        System.out.println("FluidPhysicalPropertiesChecks passed");
    }

    static int verify() {
        assertions = 0;
        FluidDefinitions.prepare();
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
