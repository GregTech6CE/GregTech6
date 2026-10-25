package com.gregtech.addoncontracts;

import gregtech.api.addon.platform.AddonStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** SDK-only compile contract: a part need not depend on GregTech's internal tracker types. */
public final class StructureNotifications {
    private StructureNotifications() {}
    public static void matchingDataChanged(Level level, BlockPos partPos) {
        AddonStructures.notifyChanged(level, partPos);
    }
}
