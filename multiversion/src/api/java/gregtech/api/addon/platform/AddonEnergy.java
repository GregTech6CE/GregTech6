/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import net.minecraft.core.Direction;

/** GregTech's packet gate for an {@link EnergyBlock}. Cables call the block; the block calls this for the default rule. */
public final class AddonEnergy {
    private AddonEnergy() {}

    public static long inject(EnergyBlock block, String energy, Direction side, long size, long amount, boolean execute) {
        return AddonPlatform.services().inject(block, energy, side, size, amount, execute);
    }

    public static long extract(EnergyBlock block, String energy, Direction side, long size, long amount, boolean execute) {
        return AddonPlatform.services().extract(block, energy, side, size, amount, execute);
    }
}
