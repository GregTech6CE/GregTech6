package com.gregtech.gregtech.api.addon.platform;

import com.gregtech.gregtech.api.addon.MachineFace;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * What one placed behavior machine does. GregTech creates a new instance for every block entity.
 * {@link #acceptFluid} is the pipe and player fill path; override it when a fluid should be
 * stored as a different fluid, such as a concentrate.
 */
public interface MachineBehavior {
    void tick(MachineBehaviorContext context);

    /** Items a player or a pipe may put in this slot. Output slots never call this. */
    boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack);

    /** Fluids this tank may hold. */
    boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack);

    /**
     * How much of {@code stack} the tank takes. The default stores that fluid when {@link #canFill}
     * allows it and the tank is empty or already holds it.
     */
    default int acceptFluid(MachineBehaviorContext context, int tank, FluidStack stack, boolean simulate) {
        if (stack == null || stack.isEmpty() || !canFill(context, tank, stack)) return 0;
        return context.fill(tank, stack, simulate);
    }

    /** A player flipped this switch. The new value is already stored. */
    default void onToggle(MachineBehaviorContext context, int toggle) {}

    /** The block is about to drop its contents. Inventory and state items are dropped afterwards. */
    default void beforeBreak(MachineBehaviorContext context) {}

    /**
     * A player used this block with a recognized GregTech tool. GregTech asks before a wrench
     * rotates the block, on the side that received the click. Return true to consume the click:
     * the server then spends the usual tool-click durability, and the block does not rotate or open.
     * The default returns false. An unrecognized tool never calls this.
     */
    default boolean onTool(MachineBehaviorContext context, AddonTool tool, Player player, InteractionHand hand,
                           MachineFace face) { return false; }

    /**
     * A player used this block. Return true when the behavior handled it.
     * The default returns false and the machine menu opens. A recognized tool is asked first.
     * A wrench that was not handled, then a fluid container, come before this.
     */
    default boolean onUse(MachineBehaviorContext context, Player player, InteractionHand hand) { return false; }
}
