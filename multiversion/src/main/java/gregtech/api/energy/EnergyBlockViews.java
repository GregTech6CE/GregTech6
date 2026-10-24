package gregtech.api.energy;

import gregtech.api.addon.platform.EnergyBlock;
import gregtech.internal.addon.AddonEnergyAdapter;
import net.minecraft.world.level.block.entity.BlockEntity;

/** SPDX-License-Identifier: LGPL-3.0-or-later. Resolves either an internal energy block or an addon one. */
public final class EnergyBlockViews {
    private EnergyBlockViews() {}

    public static IEnergyBlock view(BlockEntity entity) {
        if (entity instanceof IEnergyBlock block) return block;
        if (entity instanceof EnergyBlock addon) return AddonEnergyAdapter.of(addon);
        return null;
    }
}
