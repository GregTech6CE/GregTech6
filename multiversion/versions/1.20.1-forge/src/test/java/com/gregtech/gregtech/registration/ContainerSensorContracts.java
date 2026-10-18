package com.gregtech.gregtech.registration;

import com.gregtech.gregtech.api.fluid.PortableFluidContainerSpec;
import com.gregtech.gregtech.api.sensor.SensorMeasurements;
import com.gregtech.gregtech.api.material.MaterialMass;
import com.gregtech.gregtech.api.material.GTValues;

public final class ContainerSensorContracts {
    public static void main(String[] args) {
        int[] capacities = {2000, 250, 1000, 4000, 8000};
        int i = 0;
        for (var spec : PortableFluidContainerSpec.values()) {
            check(spec.capacity() == capacities[i++], "GT6 default capacity: " + spec);
            int limit = spec.maxTemperature();
            check(spec.accepts(limit - 1, false, false, false, false), "liquid below limit");
            check(!spec.accepts(limit, false, false, false, false), "strict temperature boundary");
            check(!spec.accepts(300, false, true, false, false), "acid rejection");
            check(!spec.accepts(300, false, false, false, true), "plasma rejection");
            check(spec.accepts(300, true, false, false, false) ==
                    (spec == PortableFluidContainerSpec.BAROMETER_GAS_CYLINDER), "gas containment");
            check(spec.accepts(300, false, false, true, false) ==
                    (spec == PortableFluidContainerSpec.CUP || spec == PortableFluidContainerSpec.THERMOS), "magic containment");
        }
        var porcelain = PortableFluidContainerSpec.CUP.material();
        check(porcelain.getId() == 8273 && porcelain.getMeltingPoint() == 1800, "original porcelain identity");
        check(porcelain.getCompositionDivider() == 4 && porcelain.getCompositionComponents().size() == 3,
                "porcelain composition");
        check(SensorMeasurements.cubicMetres(999) == 0 && SensorMeasurements.cubicMetres(1000) == 1,
                "bucketometer units");
        check(SensorMeasurements.cubicDecametres(999999) == 0 && SensorMeasurements.cubicDecametres(1000000) == 1,
                "kilobucketometer units");
        check(SensorMeasurements.tonnes(999) == 0 && SensorMeasurements.tonnes(1000) == 1
                && SensorMeasurements.tonnes(1e12) == 65535, "heavyweightometer range");
        check(Math.abs(MaterialMass.kilograms(9.0, GTValues.U) - 1000) < 0.00001, "GT6 ingot mass convention");
        System.out.println("Container and sensor contracts passed");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
