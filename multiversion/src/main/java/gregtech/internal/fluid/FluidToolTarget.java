/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original funnel/tap access is separate from automatic sided fluid capabilities. */
package gregtech.internal.fluid;

import net.minecraft.core.Direction;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}
import java.util.function.Supplier;

public interface FluidToolTarget {

    /** Cap nozzles use funnel access; gas nozzles use tap access, as in TileEntityBase01Root. */
    IFluidHandler fluidToolHandler(Direction side, boolean filling);

    /** Resolve every operation again so a retained part handle cannot access a broken structure. */
    static IFluidHandler relay(Supplier<IFluidHandler> target) {
        return new IFluidHandler() {

            public int getTanks() {
                var handler = target.get();
                return handler == null ? 0 : handler.getTanks();
            }

            public FluidStack getFluidInTank(int tank) {
                var handler = target.get();
                return handler == null ? FluidStack.EMPTY : handler.getFluidInTank(tank).copy();
            }

            public int getTankCapacity(int tank) {
                var handler = target.get();
                return handler == null ? 0 : handler.getTankCapacity(tank);
            }

            public boolean isFluidValid(int tank, FluidStack fluid) {
                var handler = target.get();
                return handler != null && handler.isFluidValid(tank, fluid);
            }

            public int fill(FluidStack fluid, FluidAction action) {
                var handler = target.get();
                return handler == null ? 0 : handler.fill(fluid, action);
            }

            public FluidStack drain(FluidStack fluid, FluidAction action) {
                var handler = target.get();
                return handler == null ? FluidStack.EMPTY : handler.drain(fluid, action);
            }

            public FluidStack drain(int amount, FluidAction action) {
                var handler = target.get();
                return handler == null ? FluidStack.EMPTY : handler.drain(amount, action);
            }
        };
    }
}
