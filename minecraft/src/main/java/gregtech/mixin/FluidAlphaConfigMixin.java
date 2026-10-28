package gregtech.mixin;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemBlockRenderTypes.class)
public abstract class FluidAlphaConfigMixin {
    @Inject(method = "getRenderLayer", at = @At("RETURN"), cancellable = true, require = 1)
    private static void gt6ce$fluidAlpha(FluidState state, CallbackInfoReturnable<RenderType> result) {
        if (result.getReturnValue() == RenderType.translucent()
                && !gregtech.client.ConfiguredAlphaModels.enabled(
                        BuiltInRegistries.FLUID.getKey(state.getType()).getNamespace()))
            result.setReturnValue(RenderType.cutout());
    }
}
