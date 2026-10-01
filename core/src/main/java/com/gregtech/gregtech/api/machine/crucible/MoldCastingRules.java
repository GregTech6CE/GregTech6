package com.gregtech.gregtech.api.machine.crucible;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialProperty;

/** Platform-free temperature/admission rules from saltnya's actual mold. */
public final class MoldCastingRules {
    private MoldCastingRules() {}

    public static long cool(long temperature, long environment) {
        if (temperature > environment) return Math.max(environment, temperature - Math.min(5, temperature - environment));
        if (temperature < environment) return Math.min(environment, temperature + Math.min(5, environment - temperature));
        return temperature;
    }

    /** Withdraw one complete casting charge, retaining every unused unit. */
    public static long castingAmount(long available, long required) {
        return required > 0 && available >= required ? required : 0;
    }

    public static long acceptedAmount(GTMaterial material, long offered, long required,
                                      boolean acidProof, boolean occupied) {
        if (material == null || occupied || required <= 0 || offered < required) return 0;
        if (!acidProof && material.has(MaterialProperty.ACID)) return 0;
        return required;
    }
}
