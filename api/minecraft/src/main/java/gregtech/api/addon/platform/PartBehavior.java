/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

import java.util.List;

/**
 * What one placed functional part does. GregTech creates a new instance for every block entity.
 * The addon does not subclass the part block entity. A controller calls this same instance
 * through {@link BoundPart#behavior()}.
 */
public interface PartBehavior {
    default void tick(PartContext context) {}

    /**
     * A player used this block. {@code hit} is the clicked face and the position on the block.
     * Return true when the behavior handled it. A wrench and a magnifying glass are offered first.
     */
    default boolean onUse(PartContext context, Player player, InteractionHand hand, PartHit hit) { return false; }

    /** Items a pipe may put in this slot. The face list is checked before this. */
    default boolean canInsert(PartContext context, int slot, ItemStack stack) { return true; }

    /** A pipe may take from this slot. The face list is checked before this. */
    default boolean canExtract(PartContext context, int slot) { return true; }

    /** Fluid a pipe may put in this tank. The face list is checked before this. */
    default boolean canFill(PartContext context, int tank, FluidStack stack) { return true; }

    /** A pipe may take from this tank. The face list is checked before this. */
    default boolean canDrain(PartContext context, int tank) { return true; }

    /** A slot changed through a pipe or {@link PartContext#setItem}. */
    default void onSlotsChanged(PartContext context, int slot) {}

    /** Lines a magnifying glass shows. Empty leaves the click unhandled. */
    default List<Component> describe(PartContext context) { return List.of(); }

    /**
     * A wrench click. Return true when the behavior handled it.
     * False lets GregTech rotate the horizontal facing.
     */
    default boolean onWrench(PartContext context, Player player, PartHit hit) { return false; }
}
