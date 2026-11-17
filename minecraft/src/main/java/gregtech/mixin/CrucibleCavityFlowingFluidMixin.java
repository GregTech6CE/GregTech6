package gregtech.mixin;

import gregtech.content.block.machine.VesselInnerFaces;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps flowing fluids out of a formed large crucible's hollow. The vessel stores material units
 * instead of fluid blocks, so any spread into its two cavity layers contradicts the structure.
 */
@Mixin(FlowingFluid.class)
public abstract class CrucibleCavityFlowingFluidMixin {
    @Inject(method = "canSpreadTo", at = @At("HEAD"), cancellable = true, require = 1)
    private void gregtech$keepCrucibleCavityClear(
            BlockGetter level,
            BlockPos fromPos,
            BlockState fromState,
            Direction direction,
            BlockPos toPos,
            BlockState toState,
            FluidState toFluidState,
            Fluid fluid,
            CallbackInfoReturnable<Boolean> cir) {
        if (VesselInnerFaces.isFormedLargeCrucibleCavity(level, toPos)) cir.setReturnValue(false);
    }
}
