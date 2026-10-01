package com.gregtech.gregtech.content.cover;

import com.gregtech.gregtech.platform.neoforge.NeoToolBindings;
import com.gregtech.gregtech.platform.neoforge.transport.FluidPipeToolInteractions;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/** Shared attachment, panel buttons and tool configuration for every panel host. */
public final class PanelCoverInteraction {
    private PanelCoverInteraction(){}
    public static InteractionResult use(PanelCoverHost host,Player player,InteractionHand hand,BlockHitResult hit,boolean manageAllCovers){
        var owner=host.coverOwner();var level=owner.getLevel();var held=player.getItemInHand(hand);var side=hit.getDirection();
        if(level==null)return InteractionResult.PASS;
        if(!player.mayBuild()||!level.mayInteract(player,owner.getBlockPos()))return InteractionResult.FAIL;
        if(PanelCover.of(held)!=null){
            if(!level.isClientSide){
                if(host.attachCover(side,held)){if(!player.getAbilities().instabuild)held.shrink(1);}
                else player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.gregtech.panel.unsupported"),true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(manageAllCovers&&NeoToolBindings.matches(held,"crowbar")){
            if(!level.isClientSide){var removed=host.removeCover(side);if(!removed.isEmpty()){if(!player.addItem(removed))player.drop(removed,false);NeoToolBindings.damageForUse(held,1,player);}}
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // Source controller forwards edge tool actions to the selected neighboring cover.
        if(PanelCover.of(host.getCover(side))==PanelCover.CONTROLLER&&FluidPipeToolInteractions.isInteractionTool(held))side=FluidPipeToolInteractions.selectedFace(hit);
        boolean cutter=NeoToolBindings.isWireCutter(held),screw=NeoToolBindings.isScrewdriver(held),chisel=NeoToolBindings.matches(held,"chisel");
        if((cutter||screw||chisel)&&(host.panels().configure(side,cutter,chisel)
                )){
            if(!level.isClientSide){
                NeoToolBindings.damageForUse(held,1,player);
                var stack=host.getCover(side);var panel=PanelCover.of(stack);
                String message=chisel?"message.gregtech.panel.style":cutter?(MachineCoverSpec.strong(stack)?"message.gregtech.cover.strong":"message.gregtech.cover.weak"):
                    panel==PanelCover.BUTTONS?(CoverStackData.readOrEmpty(stack).getBoolean(PanelCoverRuntime.RESET)?"message.gregtech.panel.reset":"message.gregtech.panel.latch"):
                    MachineCoverSpec.inverted(stack)?"message.gregtech.cover.inverted":"message.gregtech.cover.normal";
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(message,PanelCoverRuntime.style(stack)+1),true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(!FluidPipeToolInteractions.isInteractionTool(held)){
            var point=hit.getLocation().subtract(owner.getBlockPos().getX(),owner.getBlockPos().getY(),owner.getBlockPos().getZ());
            var uv=CoverFaceCoordinates.from(side,point.x,point.y,point.z);
            if(host.panels().click(side,uv.u(),uv.v()))return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
