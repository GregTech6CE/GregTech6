package com.gregtech.gregtech.content.logistics;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/** GT6 ITileEntityLogistics: null is SIDE_ANY; a concrete side must be open for traversal. */
public interface LogisticsHost {
    boolean canLogistics(@Nullable Direction side);
}
