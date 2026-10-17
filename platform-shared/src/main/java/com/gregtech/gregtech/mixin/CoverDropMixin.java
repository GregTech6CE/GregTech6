package com.gregtech.gregtech.mixin;
import com.gregtech.gregtech.content.cover.CoverDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
@Mixin(Block.class)
public abstract class CoverDropMixin {
    @Inject(method="getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",at=@At("RETURN"),cancellable=true,require=1)
    private static void gregtech$coverDrops(BlockState state,ServerLevel level,BlockPos pos,BlockEntity owner,Entity entity,ItemStack tool,CallbackInfoReturnable<List<ItemStack>> cir){
        cir.setReturnValue(CoverDrops.capture(state,level,owner,cir.getReturnValue()));
    }
    @Inject(method="getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Ljava/util/List;",at=@At("RETURN"),cancellable=true,require=1)
    private static void gregtech$coverDropsWithoutTool(BlockState state,ServerLevel level,BlockPos pos,BlockEntity owner,CallbackInfoReturnable<List<ItemStack>> cir){
        cir.setReturnValue(CoverDrops.capture(state,level,owner,cir.getReturnValue()));
    }

}
