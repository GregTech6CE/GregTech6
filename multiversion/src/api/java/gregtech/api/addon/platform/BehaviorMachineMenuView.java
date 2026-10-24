/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/**
 * The values a behavior machine menu has already synced to the client.
 * A custom screen reads this instead of the block entity or the menu class.
 * Tanks are indexed in declaration order. Toggles use the declaration order too.
 */
public interface BehaviorMachineMenuView {
    /** Stored energy, truncated to an int. */
    int energy();

    /** Progress fraction from 0 to 1. */
    float progress();

    boolean isOn(int toggle);

    int tankAmount(int tank);

    FluidStack tankFluid(int tank);

    /** Menu id for {@code Minecraft#gameMode.handleInventoryButtonClick}, the path the default screen uses. */
    int containerId();
}
