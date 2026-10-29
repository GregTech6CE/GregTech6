/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/**
 * Tanks owned by a machine or structure part. All amounts and capacities are mB.
 * Reads and returned fluids are detached snapshots; fill arguments remain caller-owned.
 * Use during server callbacks only and do not retain the view. Like the legacy context
 * operations, this is internal processing access and bypasses behavior/face filters.
 * Simulation must not change storage. Individual calls are not a multi-tank transaction.
 */
public interface FluidAccess {
    int tanks();

    /** A detached fluid stack. Invalid tank indexes throw, including on empty operations. */
    FluidStack snapshot(int tank);

    int capacity(int tank);

    /** Returns accepted mB. Null/empty means no fluid. Negative amounts throw. */
    int fill(int tank, FluidStack stack, boolean simulate);

    /** Returns up to {@code amount} mB. Zero drains nothing; negative amounts throw. */
    FluidStack drain(int tank, int amount, boolean simulate);
}
