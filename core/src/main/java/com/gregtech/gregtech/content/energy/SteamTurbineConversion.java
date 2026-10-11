/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * MultiTileEntityTurbineSteam.doConversion. */
package com.gregtech.gregtech.content.energy;

/** Source local200L condensate ratio, independent of the boiler's CS.STEAM_PER_WATER160. */
public final class SteamTurbineConversion {
    private SteamTurbineConversion() {}
    public static final int STEAM_PER_WATER = 200;
    public record Step(long energy, long pending, long consumed, long condensate, long remainder) {}
    public static Step step(long energy, long pending, long steam, long remainder, long inputRate) {
        if (pending > 0) return new Step(energy + pending, 0, 0, 0, remainder);
        if (steam < (inputRate <= 16 ? 1 : inputRate / 2) * 2)
            return new Step(energy, 0, 0, 0, remainder);
        long total = remainder + steam;
        return new Step(energy + steam / 2, steam / 2, steam, total / STEAM_PER_WATER, total % STEAM_PER_WATER);
    }
    public static long output(long energy, long inputRate, long outputRate) {
        return energy * outputRate / inputRate;
    }
    public static long waste(long energy, long inputRate) { return Math.max(0, energy - inputRate * 2); }
}
