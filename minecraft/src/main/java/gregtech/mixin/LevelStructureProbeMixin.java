package gregtech.mixin;

import gregtech.engine.multiblock.tracking.StructureScanProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Records block, entity and chunk reads while a structure scan is on the stack. */
@Mixin(Level.class)
public abstract class LevelStructureProbeMixin {
    @Inject(
            method = "getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"))
    private void gt$probeBlock(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        StructureScanProbe.noteBlock(pos);
    }

    @Inject(
            method =
                    "getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
            at = @At("HEAD"))
    private void gt$probeEntity(BlockPos pos, CallbackInfoReturnable<BlockEntity> cir) {
        StructureScanProbe.noteEntity(pos);
        // Loading a part can read its neighbours. Those reads are not structure cells.
        StructureScanProbe.pushPause();
    }

    @Inject(
            method =
                    "getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
            at = @At("RETURN"))
    private void gt$probeEntityEnd(BlockPos pos, CallbackInfoReturnable<BlockEntity> cir) {
        StructureScanProbe.popPause();
    }
}
