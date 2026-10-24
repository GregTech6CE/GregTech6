/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.internal.addon;

/**
 * Internal: not part of the addon contract; may change without notice.
 * Cheap overclocking for one machine. An addon basic machine always uses its declaration.
 * Any machine whose declaration was set uses that boolean. Everyone else keeps {@code legacy}.
 */
public final class AddonOverclock {
    private AddonOverclock() {}

    /**
     * {@code addonBasic} is an addon basic machine. {@code declared} means the declaration set the
     * flag; {@code declaredValue} is that flag. Everyone else keeps {@code legacy}.
     */
    public static boolean enabled(boolean addonBasic, boolean declared, boolean declaredValue, boolean legacy) {
        if (addonBasic || declared) return declaredValue;
        return legacy;
    }
}
