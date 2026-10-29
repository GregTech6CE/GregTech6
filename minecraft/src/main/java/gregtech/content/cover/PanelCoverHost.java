package gregtech.content.cover;

import gregtech.api.platform.machine.MachineControl;
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
        if(coverOwner() instanceof gregtech.content.logistics.LogisticsCoverHost logistics)
            for(var face:Direction.values())if(!logistics.logisticsCovers().get(face).isEmpty())return true;
        return false;
    }
    default boolean componentTicks() { return true; }
//? if >=1.21.1 {
    /*default net.neoforged.neoforge.items.IItemHandler componentItems(Direction side) {
*///?} else {
    default net.minecraftforge.items.IItemHandler componentItems(Direction side) {
//?}
        var owner=coverOwner();
//? if >=1.21.1 {
        /*if (owner instanceof net.neoforged.neoforge.items.IItemHandler handler) return handler;
*///?} else {
        if (owner instanceof net.minecraftforge.items.IItemHandler handler) return handler;
//?}
        if (owner.getLevel()==null) return null;
//? if >=1.21.1 {
        /*return owner.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,owner.getBlockPos(),null);
*///?} else {
        return owner.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,null).orElse(null);
//?}
    }
//? if >=1.21.1 {
    /*default net.neoforged.neoforge.fluids.capability.IFluidHandler componentFluids(Direction side) {
*///?} else {
    default net.minecraftforge.fluids.capability.IFluidHandler componentFluids(Direction side) {
//?}
        var owner=coverOwner();
//? if >=1.21.1 {
        /*if (owner instanceof net.neoforged.neoforge.fluids.capability.IFluidHandler handler) return handler;
*///?} else {
        if (owner instanceof net.minecraftforge.fluids.capability.IFluidHandler handler) return handler;
//?}
        if (owner.getLevel()==null) return null;
//? if >=1.21.1 {
        /*return owner.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,owner.getBlockPos(),null);
*///?} else {
        return owner.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,null).orElse(null);
//?}
    }
    default BlockEntity coverOwner(){return (BlockEntity)this;}
    default MachineControl coverControl(Direction side){var c=MachineControl.find(coverOwner(),side);return c!=null?c:CoverProgress.control(this,side);}
    default boolean coverPossible(Direction side){var c=coverControl(side);return c!=null&&c.available()&&(c.active()||c.progressMax()>0);}
    default boolean coverSupportsPossible(){return false;}
}
