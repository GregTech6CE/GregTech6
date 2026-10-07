package com.gregtech.gregtech.mixin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class CoverCollisionMixin {
    @Inject(method="getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",at=@At("RETURN"),cancellable=true,require=1)
    private void gregtech$coverCollision(BlockGetter level,BlockPos pos,CollisionContext context,CallbackInfoReturnable<VoxelShape> cir){cir.setReturnValue(com.gregtech.gregtech.content.cover.CoverWorldInteraction.collision(level,pos,cir.getReturnValue()));}
    @Inject(method="getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;",at=@At("RETURN"),cancellable=true,require=1)
    private void gregtech$cachedCoverCollision(BlockGetter level,BlockPos pos,CallbackInfoReturnable<VoxelShape> cir){cir.setReturnValue(com.gregtech.gregtech.content.cover.CoverWorldInteraction.collision(level,pos,cir.getReturnValue()));}

}
