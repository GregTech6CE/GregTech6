package com.gregtech.gregtech.mixin;

import com.gregtech.gregtech.content.cover.*;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Expose panel/detector outputs on native GT hosts without their own cover signal dispatch. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class CoverSignalMixin {
    private static PanelCoverHost emittingHost(BlockGetter level,BlockPos pos,Direction query){
        var owner=level.getBlockEntity(pos);
        if(owner==null||!ComponentCoverFallback.ownClass(owner)
                ||owner instanceof com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity
                ||owner instanceof com.gregtech.gregtech.blockentity.energy.SignalWireBlockEntity
                ||!(owner instanceof PanelCoverHost host))return null;
        var cover=host.getCover(query.getOpposite());
        var panel=PanelCover.of(cover);var detector=MachineCoverSpec.of(cover);
        return panel!=null&&panel.output()||detector!=null&&detector.detector()?host:null;
    }
    @Inject(method="isSignalSource",at=@At("RETURN"),cancellable=true,require=1)
    private void gregtech$signalSource(CallbackInfoReturnable<Boolean> cir){
        var block=((net.minecraft.world.level.block.state.BlockState)(Object)this).getBlock();
        if(block instanceof net.minecraft.world.level.block.EntityBlock&&block.getClass().getName().startsWith("com.gregtech.gregtech."))cir.setReturnValue(true);
    }
    @Inject(method="getSignal",at=@At("RETURN"),cancellable=true,require=1)
    private void gregtech$weakSignal(BlockGetter level,BlockPos pos,Direction query,CallbackInfoReturnable<Integer> cir){
        var host=emittingHost(level,pos,query);if(host!=null)cir.setReturnValue(host.panels().signal(query.getOpposite()));
    }
    @Inject(method="getDirectSignal",at=@At("RETURN"),cancellable=true,require=1)
    private void gregtech$strongSignal(BlockGetter level,BlockPos pos,Direction query,CallbackInfoReturnable<Integer> cir){
        var host=emittingHost(level,pos,query);if(host!=null)cir.setReturnValue(host.panels().strongSignal(query.getOpposite()));
    }
}
