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
    @Inject(method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void gregtech$toolSelection(BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof net.minecraft.world.entity.player.Player player
                && com.gregtech.gregtech.content.cover.CoverPlacement.fullSelection(level, pos,
                        (net.minecraft.world.level.block.state.BlockState) (Object) this, player))
            cir.setReturnValue(Shapes.block());
    }

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("RETURN"), cancellable = true, require = 1)
    private void gregtech$coverCollision(BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        cir.setReturnValue(com.gregtech.gregtech.content.cover.CoverWorldInteraction.collision(level, pos, cir.getReturnValue()));
    }

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("RETURN"), cancellable = true, require = 1)
    private void gregtech$cachedCoverCollision(BlockGetter level, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        cir.setReturnValue(com.gregtech.gregtech.content.cover.CoverWorldInteraction.collision(level, pos, cir.getReturnValue()));
    }

    @Inject(method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("RETURN"), cancellable = true, require = 1)
    private void gregtech$coverOutline(BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        cir.setReturnValue(com.gregtech.gregtech.content.cover.CoverWorldInteraction.outline(level, pos, cir.getReturnValue()));
    }

    @Inject(method = "getBlockSupportShape", at = @At("RETURN"), cancellable = true, require = 1)
    private void gregtech$coverSupport(BlockGetter level, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        cir.setReturnValue(com.gregtech.gregtech.content.cover.CoverWorldInteraction.support(level, pos, cir.getReturnValue()));
    }

    // BlockState caches vanilla sturdy faces independently of its dynamic collision getter.
    @Inject(method = "isFaceSturdy(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/SupportType;)Z",
            at = @At("RETURN"), cancellable = true, require = 1)
    private void gregtech$solidCover(BlockGetter level, BlockPos pos, net.minecraft.core.Direction side,
            net.minecraft.world.level.block.SupportType type, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && com.gregtech.gregtech.content.cover.CoverWorldInteraction.solidCover(level, pos, side))
            cir.setReturnValue(true);
    }
}
