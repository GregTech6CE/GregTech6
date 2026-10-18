package com.gregtech.gregtech.mixin;

import com.gregtech.gregtech.content.cover.CoverDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Snapshot covers before the native player harvest calls onRemove and empties face slots. */
@Mixin(ServerPlayerGameMode.class)
public abstract class CoverHarvestMixin {
    @Shadow protected ServerPlayer player;
    @Unique private BlockEntity gregtech$coverHarvest;
    @Inject(method="destroyBlock",at=@At("HEAD"),require=1)
    private void gregtech$beforeHarvest(BlockPos pos,CallbackInfoReturnable<Boolean> cir){
        gregtech$coverHarvest=CoverDrops.beginHarvest(player.serverLevel(),pos,player);
    }
    @Inject(method="destroyBlock",at=@At("RETURN"),require=1)
    private void gregtech$afterHarvest(BlockPos pos,CallbackInfoReturnable<Boolean> cir){
        CoverDrops.finishHarvest(gregtech$coverHarvest,cir.getReturnValue());gregtech$coverHarvest=null;
    }
}
