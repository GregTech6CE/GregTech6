/**
 * Copyright (c) 2021 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package com.gregtech.gregtech.content.cover;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Cached native capability views always consult the current attached cover. */
public final class ComponentCoverAccess {
    private ComponentCoverAccess() {}
    public static IItemHandler items(PanelCoverHost host,Direction side,IItemHandler inner) {
        return new IItemHandler() {
            private boolean allowed(boolean insert){return !host.coverOwner().isRemoved()&&!host.panels().shuttered(side)&&ComponentCoverRuntime.allowsItem(host.getCover(side),insert);}
            public int getSlots(){return inner.getSlots();}
            public ItemStack getStackInSlot(int slot){return inner.getStackInSlot(slot);}
            public int getSlotLimit(int slot){return inner.getSlotLimit(slot);}
            public boolean isItemValid(int slot,ItemStack stack){return allowed(true)&&CoverUtilityBehaviors.itemFilterPermits(host.getCover(side),host.panels().stopped(),stack)&&inner.isItemValid(slot,stack);}
            public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return allowed(true)&&CoverUtilityBehaviors.itemFilterPermits(host.getCover(side),host.panels().stopped(),stack)?inner.insertItem(slot,stack,simulate):stack;}
            public ItemStack extractItem(int slot,int amount,boolean simulate){return allowed(false)&&CoverUtilityBehaviors.itemFilterPermits(host.getCover(side),host.panels().stopped(),inner.getStackInSlot(slot))?inner.extractItem(slot,amount,simulate):ItemStack.EMPTY;}
        };
    }
    public static IFluidHandler fluids(PanelCoverHost host,Direction side,IFluidHandler inner) {
        return new IFluidHandler() {
            private boolean allowed(boolean fill){return !host.coverOwner().isRemoved()&&!host.panels().shuttered(side)&&ComponentCoverRuntime.allowsFluid(host.getCover(side),fill);}
            public int getTanks(){return inner.getTanks();}
            public FluidStack getFluidInTank(int tank){return inner.getFluidInTank(tank);}
            public int getTankCapacity(int tank){return inner.getTankCapacity(tank);}
            public boolean isFluidValid(int tank,FluidStack stack){return allowed(true)&&CoverUtilityBehaviors.fluidFilterPermits(host.getCover(side),host.panels().stopped(),stack)&&inner.isFluidValid(tank,stack);}
            public int fill(FluidStack stack,FluidAction action){return allowed(true)&&CoverUtilityBehaviors.fluidFilterPermits(host.getCover(side),host.panels().stopped(),stack)?inner.fill(stack,action):0;}
            public FluidStack drain(FluidStack stack,FluidAction action){return allowed(false)&&CoverUtilityBehaviors.fluidFilterPermits(host.getCover(side),host.panels().stopped(),stack)?inner.drain(stack,action):FluidStack.EMPTY;}
            public FluidStack drain(int amount,FluidAction action){if(!allowed(false))return FluidStack.EMPTY;var sample=inner.drain(amount,FluidAction.SIMULATE);return CoverUtilityBehaviors.fluidFilterPermits(host.getCover(side),host.panels().stopped(),sample)?inner.drain(amount,action):FluidStack.EMPTY;}
        };
    }
}
