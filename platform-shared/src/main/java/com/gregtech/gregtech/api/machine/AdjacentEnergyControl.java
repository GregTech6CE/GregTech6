/* Copyright (c) 2019 Gregorius Techneticies; GregTech-6 Team.
 * LGPL-3.0-or-later. Adapted from ITileEntityAdjacentOnOff and BasicMachine source requests. */
package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/** A source-specific adjacent request, distinct from exposing a generic manual machine switch. */
public interface AdjacentEnergyControl {
    boolean setAdjacentEnabled(boolean enabled);
    interface Provider {
        /** Null means the source does not implement the original adjacent switch contract. */
        AdjacentEnergyControl adjacentEnergyControl();
    }
    static void update(BlockEntity source, GregTechTags.Tag type, Direction outputFace, boolean enabled) {
        if (source == null || source.isRemoved() || !(source instanceof Provider provider)
                || !(source instanceof IEnergyBlock energy)
                || !energy.isEnergyEmittingTo(type, outputFace, true)) return;
        var control = provider.adjacentEnergyControl();
        if (control != null) control.setAdjacentEnabled(enabled);
    }
}
