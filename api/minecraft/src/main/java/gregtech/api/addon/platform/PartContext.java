/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import gregtech.api.addon.recipe.CrucibleContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

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

    /** Uniform snapshot reads and explicit writes for this part's inventory. */
    InventoryAccess inventory();

    /** Uniform snapshot reads and mB operations for this part's tanks. */
    FluidAccess fluids();

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
     * Read-only snapshot of the molten material the bound controller exposes, with its amount
     * in GregTech units and its temperature in Kelvin. Empty when no controller is bound, the
     * structure is not formed, or that controller exposes no molten material.
     * This reads; it removes nothing, and no block entity is kept.
     */
    CrucibleContent crucible();

    /**
     * Values named by {@code LayoutPart.field}. A key that was not named throws.
     * A missing named key reads as zero, false, an empty string or an empty item.
     */
    BehaviorState state();
}
