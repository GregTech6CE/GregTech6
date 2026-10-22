package com.gregtech.gregtech.api.addon.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * The placed part a {@link PartBehavior} reads and writes. Valid during that behavior's callbacks,
 * and while a bound controller is calling back into the same behavior.
 */
public interface PartContext {
    Level level();

    BlockPos pos();

    Direction facing();

    /** Registry path of the part block, without the addon namespace. */
    String name();

    int slots();

    ItemStack item(int slot);

    /**
     * Replaces one slot. This does not ask {@link PartBehavior#canInsert} or the face list.
     * A count above the declared stack limit is cut to that limit.
     */
    void setItem(int slot, ItemStack stack);

    /** True when a formed controller currently claims this part. */
    boolean bound();

    /** The claiming controller, or null when {@link #bound()} is false. */
    @Nullable BlockPos controllerPos();

    /**
     * The behavior of the claiming controller when that controller is a formed behavior machine.
     * Null for a large recipe controller, or when {@link #bound()} is false.
     * Do not keep this past the callback. A broken structure drops it.
     */
    @Nullable MachineBehavior controller();

    /**
     * Values named by {@code LayoutPart.field}. A key that was not named throws.
     * A missing named key reads as zero, false, an empty string or an empty item.
     */
    BehaviorState state();
}
