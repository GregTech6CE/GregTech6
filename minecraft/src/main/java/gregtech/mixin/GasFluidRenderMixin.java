package gregtech.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import gregtech.client.GasFluidRenderer;
import gregtech.engine.fluid.FluidHazards;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lighter-than-air fluids are drawn top-anchored with vanilla-style corner smoothing by
 * {@link GasFluidRenderer}; vanilla's liquid renderer always fills from {@code y=0}, so its own
 * output is suppressed for them.
 */
@Mixin(LiquidBlockRenderer.class)
public abstract class GasFluidRenderMixin {
    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true, require = 1)
    private void gregtech$renderTopAnchoredGas(BlockAndTintGetter level, BlockPos pos, VertexConsumer consumer,
                                               BlockState blockState, FluidState fluidState, CallbackInfo info) {
        if (FluidHazards.isLighterThanAir(fluidState.getType())) {
            GasFluidRenderer.render(level, pos, consumer, fluidState);
            info.cancel();
        }
    }
}
