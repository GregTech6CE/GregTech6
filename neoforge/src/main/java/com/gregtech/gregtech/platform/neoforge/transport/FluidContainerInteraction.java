package com.gregtech.gregtech.platform.neoforge.transport;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
/** Filled-first original interaction priority, with real replacement-container/stacked-item transaction. */
public final class FluidContainerInteraction {
    private FluidContainerInteraction() {}
    public static boolean use(Player player,InteractionHand hand,IFluidHandler tank){
        var held=player.getItemInHand(hand);
        if(held.isEmpty())return false;
        var item=FluidUtil.getFluidHandler(held.copyWithCount(1)).orElse(null);
        if(item==null)return false;
        boolean filled=false;
        for(int i=0;i<item.getTanks();i++)if(!item.getFluidInTank(i).isEmpty()){filled=true;break;}
        var inventory=player.getCapability(Capabilities.ItemHandler.ENTITY);
        if(inventory==null)return false;
        var result=filled?FluidUtil.tryEmptyContainerAndStow(held,tank,inventory,Integer.MAX_VALUE,player,true):
                FluidUtil.tryFillContainerAndStow(held,tank,inventory,Integer.MAX_VALUE,player,true);
        if(!result.isSuccess())return false;
        player.setItemInHand(hand,result.getResult());return true;
    }
}
