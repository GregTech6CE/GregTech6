package gregtech.content.tool;

import gregtech.engine.fluid.PortableFluidContainerSpec;

/** Source measuring-pot/barometer bands and clamp, shared by both storage adapters. */
public final class PortableCapacityRules {

    private PortableCapacityRules() {}

    public static boolean adjustable(PortableFluidContainerSpec spec) {
        return spec.shapeId().equals("measuring_pot") || spec.shapeId().equals("barometer_gas_cylinder");
    }

    public static int capacity(PortableFluidContainerSpec spec, long value) {
        return (int) Math.max(1, Math.min(spec.capacity(), value));
    }

    public static int adjust(PortableFluidContainerSpec spec, int currentCapacity, double hitY, boolean precise) {
        if (!adjustable(spec)) return spec.capacity();
        int step = adjustmentStep(spec, hitY);
        if (precise) step /= 10;
        return capacity(spec, currentCapacity + step);
    }

    private static int adjustmentStep(PortableFluidContainerSpec spec, double hitY) {
        return spec.shapeId().equals("measuring_pot") ? measuringPotStep(hitY) : gasCylinderStep(hitY);
    }

    private static int measuringPotStep(double hitY) {
        return hitY > 6 / 16.0 ? 50 : hitY > 4 / 16.0 ? 10 : hitY > 2 / 16.0 ? -10 : -50;
    }

    private static int gasCylinderStep(double hitY) {
        return hitY > 14 / 16.0
                ? 500
                : hitY > 12 / 16.0
                        ? 100
                        : hitY > 10 / 16.0
                                ? 50
                                : hitY > 8 / 16.0
                                        ? 10
                                        : hitY > 6 / 16.0 ? -10 : hitY > 4 / 16.0 ? -50 : hitY > 2 / 16.0 ? -100 : -500;
    }
}
