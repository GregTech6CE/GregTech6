package com.gregtech.gregtech.content.logistics;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/** A storage endpoint that is itself part of a logistics network, without a bus cover. */
public interface LogisticsStorageHost extends LogisticsHost {
    /** GT6 priority: 0 disabled, 1 generic, 2 semi-filtered, 3 filtered. */
    int itemStoragePriority();

    /** GT6 priority: 0 disabled, 1 generic, 2 semi-filtered, 3 filtered. */
    int fluidStoragePriority();

    /** A strict item filter, or {@code null} for no strict filter. */
    @Nullable ItemStack itemStorageFilter();

    /** A strict fluid filter, or {@code null} for no strict filter. */
    @Nullable Fluid fluidStorageFilter();
}
