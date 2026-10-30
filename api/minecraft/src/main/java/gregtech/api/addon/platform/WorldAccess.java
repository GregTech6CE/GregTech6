/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/**
 * Interactions with blocks and item entities outside this machine. Valid only during the same
 * behavior callback as its context.
 */
public interface WorldAccess {
    /**
     * Breaks one block inside the declared range and puts its drops in the output slots.
     * Returns false when the range is closed, the position is outside it or is this machine,
     * the chunk is not loaded, the block is air or unbreakable, the drops do not fit,
     * a break event cancels it, or this tick already broke a block.
     */
    boolean breakBlock(BlockPos pos);

    /**
     * Takes item entities in the cube of {@code radius} around this machine.
     * {@code voidThem} discards them. Otherwise they move into the output slots, and an entity
     * that does not fit completely keeps what is left. Returns how many entities were taken.
     * A radius outside the declaration, or a declaration whose range is 0, takes nothing.
     */
    int collectItems(int radius, boolean voidThem);

    /**
     * Stores {@code stack} in the compressed-gas receiver on {@code face}.
     * A single-block drum accepts on every side. A behavior machine accepts on its valve face.
     * Returns how many millibuckets that receiver stored.
     * A missing receiver, a pipe, or an ordinary tank accepts nothing.
     * This writes immediately. The behavior removes the fluid from its own tank.
     */
    int pushCompressedGas(Direction face, FluidStack stack);
}
