package com.gregtech.gregtech.content.cover;

import com.gregtech.gregtech.api.machine.MachineControl;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Small host boundary: storage and machine capabilities are independent of cover behavior. */
public interface PanelCoverHost {
    ItemStack getCover(Direction side);
    boolean attachCover(Direction side,ItemStack stack);
    ItemStack removeCover(Direction side);
    PanelCoverRuntime panels();
    /** Both native cover stores share the original single all-covers stop lifetime. */
    default boolean hasAttachedCovers(){
        for(var face:Direction.values())if(!getCover(face).isEmpty())return true;
        if(coverOwner() instanceof com.gregtech.gregtech.content.logistics.LogisticsCoverHost logistics)
            for(var face:Direction.values())if(!logistics.logisticsCovers().get(face).isEmpty())return true;
        return false;
    }
    default boolean componentTicks() { return true; }
    default net.minecraftforge.items.IItemHandler componentItems(Direction side) {
        var owner=coverOwner();
        if (owner instanceof net.minecraftforge.items.IItemHandler handler) return handler;
        if (owner.getLevel()==null) return null;
        return owner.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,null).orElse(null);
    }
    default net.minecraftforge.fluids.capability.IFluidHandler componentFluids(Direction side) {
        var owner=coverOwner();
        if (owner instanceof net.minecraftforge.fluids.capability.IFluidHandler handler) return handler;
        if (owner.getLevel()==null) return null;
        return owner.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,null).orElse(null);
    }
    default BlockEntity coverOwner(){return (BlockEntity)this;}
    default MachineControl coverControl(Direction side){var c=MachineControl.find(coverOwner(),side);return c!=null?c:CoverProgress.control(this,side);}
    default boolean coverPossible(Direction side){var c=coverControl(side);return c!=null&&c.available()&&(c.active()||c.progressMax()>0);}
    default boolean coverSupportsPossible(){return false;}
}
