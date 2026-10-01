package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.material.GTMaterial;

/** Exact saltnya GTToolHelper durability/click arithmetic, without stack APIs. */
public final class ManualToolRules {
    private ManualToolRules() {}
    public static int maximumDurability(GTMaterial material, float multiplier) {
        long base = material.getToolDurability();
        if (base <= 0) base = 64;
        long maximum = Math.round(base * 100L * multiplier);
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, maximum));
    }
    public static int clickDamage(long gt6Return) { return (int) Math.ceil(gt6Return * 100.0D / 10000.0D); }
}
