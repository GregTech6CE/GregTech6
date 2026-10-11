/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original funnel/tap access is separate from automatic sided fluid capabilities. */
package com.gregtech.gregtech.api.fluid;

import net.minecraft.core.Direction;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import java.util.function.Supplier;

public interface FluidToolTarget {
    /** Cap nozzles use funnel access; gas nozzles use tap access, as in TileEntityBase01Root. */
    IFluidHandler fluidToolHandler(Direction side, boolean filling);

    /** Resolve every operation again so a retained part handle cannot access a broken structure. */
    static IFluidHandler relay(Supplier<IFluidHandler> target) {
        return new IFluidHandler() {
            public int getTanks() { var t=target.get(); return t==null?0:t.getTanks(); }
            public FluidStack getFluidInTank(int tank) { var t=target.get(); return t==null?FluidStack.EMPTY:t.getFluidInTank(tank).copy(); }
            public int getTankCapacity(int tank) { var t=target.get(); return t==null?0:t.getTankCapacity(tank); }
            public boolean isFluidValid(int tank,FluidStack fluid) { var t=target.get(); return t!=null&&t.isFluidValid(tank,fluid); }
            public int fill(FluidStack fluid,FluidAction action) { var t=target.get(); return t==null?0:t.fill(fluid,action); }
            public FluidStack drain(FluidStack fluid,FluidAction action) { var t=target.get(); return t==null?FluidStack.EMPTY:t.drain(fluid,action); }
            public FluidStack drain(int amount,FluidAction action) { var t=target.get(); return t==null?FluidStack.EMPTY:t.drain(amount,action); }
        };
    }
}
