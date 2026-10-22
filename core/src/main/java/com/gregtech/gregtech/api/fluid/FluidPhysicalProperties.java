package com.gregtech.gregtech.api.fluid;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.data.FluidCatalog;
import com.gregtech.gregtech.data.FluidCatalog.FluidEntry;
import com.gregtech.gregtech.data.FluidCatalog.FluidFlags;

/** Original FL.create phase defaults and material-density conversion, shared by both loaders. */
public final class FluidPhysicalProperties {
    private FluidPhysicalProperties() {}

    public static boolean nonLiquid(FluidEntry entry) {
        return entry.gas() || entry.hasFlag(FluidFlags.PLASMA);
    }

    public static int viscosity(FluidEntry entry) {
        if (entry.viscosity() != 0) return entry.viscosity();
        // FL.java:1103-1107: molten is STATE_LIQUID, not a separate 6000-viscosity state.
        return entry.hasFlag(FluidFlags.PLASMA) ? 10 : entry.gas() ? 200 : 1000;
    }

    public static int density(FluidEntry entry) {
        if (entry.density() != 0) return entry.density();
        // Loader_Fluids explicitly makes the three air fluids neutrally buoyant.
        if (entry.registryName().equals("air") || entry.registryName().equals("netherair")
                || entry.registryName().equals("enderair")) return 0;
        if (entry.hasFlag(FluidFlags.PLASMA)) return -100000;
        String key = entry.materialKey() != null ? entry.materialKey() : FluidCatalog.boundMaterial(entry.registryName());
        var material = key == null ? null : GTMaterialRegistry.get(key);
        if (material != null && material.resolve().isValid() && material.resolve().getDensity() > 0) {
            return materialDensity(material.resolve().getDensity());
        }
        return entry.gas() ? -100 : 1000;
    }

    /** FL.java:1128-1135; CS.WEIGHT_AIR_G_PER_CUBIC_CENTIMETER = 0.0012. */
    public static int materialDensity(double gramsPerCubicCentimeter) {
        double density = gramsPerCubicCentimeter > 0.0012 ? 1000 * gramsPerCubicCentimeter
                : gramsPerCubicCentimeter < 0.0012 ? -0.1 / gramsPerCubicCentimeter : 0;
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, (long) density));
    }
}
