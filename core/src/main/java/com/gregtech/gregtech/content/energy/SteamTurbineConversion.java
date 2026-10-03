package com.gregtech.gregtech.content.energy;

/** MultiTileEntityTurbineSteam: split each batch across two ticks, 160 L per condensate (CS.STEAM_PER_WATER). */
public final class SteamTurbineConversion {
    private SteamTurbineConversion() {}
    public record Step(long energy, long pending, long consumed, long condensate, long remainder) {}
    public static Step step(long energy, long pending, long steam, long remainder, long inputRate) {
        if (pending > 0) return new Step(energy + pending, 0, 0, 0, remainder);
        if (steam < (inputRate <= 16 ? 1 : inputRate / 2) * 2)
            return new Step(energy, 0, 0, 0, remainder);
        long total = remainder + steam;
        return new Step(energy + steam / 2, steam / 2, steam, total / com.gregtech.gregtech.api.machine.BoilerSpec.STEAM_PER_WATER, total % com.gregtech.gregtech.api.machine.BoilerSpec.STEAM_PER_WATER);
    }
    public static long output(long energy, long inputRate, long outputRate) {
        return energy * outputRate / inputRate;
    }
    public static long waste(long energy, long inputRate) { return Math.max(0, energy - inputRate * 2); }
}
