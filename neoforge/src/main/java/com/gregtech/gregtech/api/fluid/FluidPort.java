package com.gregtech.gregtech.api.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Transfer permissions around an existing tank; storage always belongs to the controller. */
public record FluidPort(IFluidHandler delegate,boolean accepts,boolean extracts) implements IFluidHandler {
    public int getTanks(){return delegate.getTanks();}
    public FluidStack getFluidInTank(int tank){return delegate.getFluidInTank(tank).copy();}
    public int getTankCapacity(int tank){return delegate.getTankCapacity(tank);}
    public boolean isFluidValid(int tank,FluidStack stack){return accepts&&delegate.isFluidValid(tank,stack);}
    public int fill(FluidStack stack,FluidAction action){return accepts?delegate.fill(stack,action):0;}
    public FluidStack drain(FluidStack stack,FluidAction action){return extracts?delegate.drain(stack,action):FluidStack.EMPTY;}
    public FluidStack drain(int amount,FluidAction action){return extracts?delegate.drain(amount,action):FluidStack.EMPTY;}
}
