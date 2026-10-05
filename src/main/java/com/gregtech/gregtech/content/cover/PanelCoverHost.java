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
    default MachineControl coverControl(Direction side){return MachineControl.find(coverOwner(),side);}
    default boolean coverPossible(Direction side){var c=coverControl(side);return c!=null&&c.available()&&(c.active()||c.progressMax()>0);}
    default boolean coverSupportsPossible(){return false;}
}
