package gregtech.mixin;

import gregtech.content.block.machine.VesselInnerFaces;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fire never survives in a formed large crucible's hollow, and fire cannot spread into the hollow.
 */
@Mixin(FireBlock.class)
public abstract class CrucibleCavityFireBlockMixin {
    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true, require = 1)
    private void gregtech$keepCrucibleCavityClear(
            BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (VesselInnerFaces.isFormedLargeCrucibleCavity(level, pos)) cir.setReturnValue(false);
    }

    @Inject(
            method = "getIgniteOdds(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)I",
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private void gregtech$noIgniteOddsInCavity(LevelReader level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (VesselInnerFaces.isFormedLargeCrucibleCavity(level, pos)) cir.setReturnValue(0);
    }
}
