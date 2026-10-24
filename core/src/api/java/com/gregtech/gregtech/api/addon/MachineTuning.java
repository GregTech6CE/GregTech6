/* SPDX-License-Identifier: LGPL-3.0-or-later */

package com.gregtech.gregtech.api.addon;

/**
 * One override for a built-in machine variant, keyed by {@code machineName + tier}.
 * Only non-null fields change; every other field keeps its built-in value.
 * The result is rebuilt through the canonical {@code BasicMachineSpec} constructor.
 * The patch is applied at startup and is never written into a save.
 */
public record MachineTuning(
        String machineName, int tier,
        Integer parallelLimit,
        Long energyIn, Long energyInMin, Long energyInMax,
        Integer processingEfficiency,
        Boolean constantEnergy, Boolean cheapOverclocking) {
    public MachineTuning {
        if (machineName == null || !machineName.matches("[a-z0-9_]+"))
            throw new IllegalArgumentException("Invalid tuned machine name: " + machineName);
        if (tier < 1)
            throw new IllegalArgumentException("Invalid tuned tier for " + machineName + ": " + tier);
        if (parallelLimit == null && energyIn == null && energyInMin == null && energyInMax == null
                && processingEfficiency == null && constantEnergy == null && cheapOverclocking == null)
            throw new IllegalArgumentException("Empty machine tuning for " + machineName + "/T" + tier);
        if (parallelLimit != null && parallelLimit < 1)
            throw new IllegalArgumentException("Invalid tuned parallel for " + machineName + "/T" + tier);
        if (energyIn != null && energyIn < 0)
            throw new IllegalArgumentException("Invalid tuned energy for " + machineName + "/T" + tier);
        if (energyInMin != null && energyInMin < 1)
            throw new IllegalArgumentException("Invalid tuned minimum energy for " + machineName + "/T" + tier);
        if (energyInMax != null && energyInMax < 1)
            throw new IllegalArgumentException("Invalid tuned maximum energy for " + machineName + "/T" + tier);
        if (energyInMin != null && energyInMax != null && energyInMax < energyInMin)
            throw new IllegalArgumentException("Invalid tuned energy range for " + machineName + "/T" + tier);
        if (processingEfficiency != null && (processingEfficiency < 1 || processingEfficiency > 100_000))
            throw new IllegalArgumentException("Invalid tuned efficiency for " + machineName + "/T" + tier);
    }
}
