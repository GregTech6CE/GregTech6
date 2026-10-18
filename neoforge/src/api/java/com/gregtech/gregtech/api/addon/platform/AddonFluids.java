package com.gregtech.gregtech.api.addon.platform;

import net.neoforged.neoforge.fluids.FluidStack;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * GregTech fluids. {@code name} is the fluid field, such as {@code DistW}, not a material name.
 * The result is null when that fluid is not registered.
 */
public final class AddonFluids {
    private AddonFluids() {}

    public enum Kind { STILL, FLOWING }

    public static FluidStack fluid(String name, Kind kind, int millibuckets) {
        return AddonLoader.services().fluid(name, kind, millibuckets);
    }

    public static FluidStack still(String name, int millibuckets) {
        return fluid(name, Kind.STILL, millibuckets);
    }
}
