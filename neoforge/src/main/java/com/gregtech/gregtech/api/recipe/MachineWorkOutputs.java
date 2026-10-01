package com.gregtech.gregtech.api.recipe;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import com.gregtech.gregtech.api.fluid.FluidTankGT;

/** Rolled exactly once at job start; unfinished output survives saves and backpressure. */
public final class MachineWorkOutputs {
    private final List<ItemStack> items=new ArrayList<>();
    private final List<FluidStack> fluids=new ArrayList<>();
    public static MachineWorkOutputs roll(Recipe recipe,int parallel,java.util.function.IntUnaryOperator random) {
        var result=new MachineWorkOutputs();
        for(int i=0;i<recipe.mOutputs.length;i++) {
            var output=recipe.mOutputs[i]; long count=recipe.rollOutputCount(i,parallel,random);
            while(count>0) { int take=(int)Math.min(count,output.getMaxStackSize()); result.items.add(output.copyWithCount(take)); count-=take; }
        }
        for(var output:recipe.mFluidOutputs) if(output!=null && !output.isEmpty()) {
            var copy=output.copy(); copy.setAmount(Math.toIntExact((long)output.getAmount()*parallel)); result.fluids.add(copy);
        }
        return result;
    }
    /**
     * Deliver the rolled outputs, keeping whatever does not fit pending.
     * <p>
     * Pass 0 merges into slots/tanks that already hold the same thing, pass 1 uses empty
     * slots/tanks. A slot holding a different item is never touched: the earlier implementation
     * merged into any occupied slot, which inflated that stack and destroyed the pending output.
     * </p>
     */
    public boolean flush(IItemHandlerModifiable inventory,int firstOutput,FluidTankGT[] tanks) {
        for(var pending:items) {
            for(int pass=0;pass<2 && !pending.isEmpty();pass++) for(int i=firstOutput;i<inventory.getSlots() && !pending.isEmpty();i++) {
                var present=inventory.getStackInSlot(i);
                if(present.isEmpty() ? pass==0 : !ItemStack.isSameItemSameComponents(present,pending)) continue;
                int space=Math.min(inventory.getSlotLimit(i),pending.getMaxStackSize())-present.getCount();
                int take=Math.min(Math.max(0,space),pending.getCount());
                if(take==0) continue;
                var combined=present.isEmpty()?pending.copyWithCount(take):present.copyWithCount(present.getCount()+take);
                inventory.setStackInSlot(i,combined); pending.shrink(take);
            }
        }
        for(var pending:fluids) for(int pass=0;pass<2 && !pending.isEmpty();pass++) for(var tank:tanks) {
            if(pending.isEmpty()) break;
            if(tank.isEmpty() ? pass==0 : !FluidStack.isSameFluidSameComponents(tank.getFluidInTank(0),pending)) continue;
            int moved=tank.fill(pending,IFluidHandler.FluidAction.EXECUTE); pending.shrink(moved);
        }
        return items.stream().allMatch(ItemStack::isEmpty) && fluids.stream().allMatch(FluidStack::isEmpty);
    }

    /** How many item stacks of rolled output are still waiting for space. */
    public int pendingItemStacks() { return (int) items.stream().filter(stack -> !stack.isEmpty()).count(); }

    /** How many fluid outputs are still waiting for space. */
    public int pendingFluids() { return (int) fluids.stream().filter(fluid -> !fluid.isEmpty()).count(); }
    public CompoundTag save(net.minecraft.core.HolderLookup.Provider lookup) {
        var tag=new CompoundTag(); var itemList=new ListTag(); var fluidList=new ListTag();
        for(var stack:items) if(!stack.isEmpty()) itemList.add(stack.save(lookup));
        for(var stack:fluids) if(!stack.isEmpty()) fluidList.add(stack.save(lookup));
        tag.put("items",itemList); tag.put("fluids",fluidList); return tag;
    }
    public static MachineWorkOutputs load(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        var result=new MachineWorkOutputs();
        for(var data:tag.getList("items",10)) result.items.add(ItemStack.parseOptional(lookup,(CompoundTag)data));
        for(var data:tag.getList("fluids",10)) result.fluids.add(FluidStack.parseOptional(lookup,(CompoundTag)data));
        return result;
    }
}
