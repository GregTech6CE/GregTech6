package com.gregtech.gregtech.api.addon;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * Cheap overclocking for one machine. An addon basic machine always uses its declaration.
 * Any machine whose declaration was set uses that boolean. Everyone else keeps {@code legacy}.
 */
public final class AddonOverclock {
    private AddonOverclock() {}

    public static boolean enabled(boolean addonBasic, boolean declared, boolean declaredValue, boolean legacy) {
        if (addonBasic || declared) return declaredValue;
        return legacy;
    }
}
