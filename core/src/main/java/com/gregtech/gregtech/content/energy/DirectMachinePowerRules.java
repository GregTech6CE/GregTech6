/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * MultiTileEntityBedrockDrill.doInject and MultiTileEntityLogisticsCore.doInject. */
package com.gregtech.gregtech.content.energy;

/** Source controllers with a soft buffer limit and deliberate, unconditional overvoltage explosions. */
public final class DirectMachinePowerRules {
    private DirectMachinePowerRules() {}
    public record Plan(long accepted, long energy, boolean explode) {}

    public static Plan inject(long energy, long capacity, long maximumPacket, long size, long amount, boolean execute) {
        if (energy > capacity || size == 0 || size == Long.MIN_VALUE || amount <= 0)
            return new Plan(0, energy, false);
        if (!execute) return new Plan(amount, energy, false);
        long packet = Math.abs(size);
        if (packet > maximumPacket) return new Plan(amount, energy, true);
        long accepted = Math.min(amount, (Long.MAX_VALUE - energy) / packet);
        return new Plan(accepted, energy + accepted * packet, false);
    }
}
