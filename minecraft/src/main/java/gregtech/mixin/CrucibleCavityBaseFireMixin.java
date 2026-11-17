package gregtech.mixin;

import gregtech.content.block.machine.VesselInnerFaces;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla asks this before placing a fire block, including the dispenser's flint and steel. A formed
 * large crucible's hollow never accepts one, whatever its own support shape reports.
 */
@Mixin(BaseFireBlock.class)
public abstract class CrucibleCavityBaseFireMixin {
    @Inject(method = "canBePlacedAt", at = @At("HEAD"), cancellable = true, require = 1)
    private static void gregtech$keepCrucibleCavityClear(
            Level level, BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (VesselInnerFaces.isFormedLargeCrucibleCavity(level, pos)) cir.setReturnValue(false);
    }
}
