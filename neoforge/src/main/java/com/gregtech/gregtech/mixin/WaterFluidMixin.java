package com.gregtech.gregtech.mixin;

import com.gregtech.gregtech.registry.GTWorldWaterFluid;
import com.gregtech.gregtech.api.fluid.WaterMixinStatus;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * §105: the other half of the water family §100 introduced.
 *
 * <p>§100 made GT6's six world waters recognise vanilla water in {@code isSame}
 * ({@link GTWorldWaterFluid#isWaterFamily}), which is what vanilla drives surface merging and the
 * slope/level maths with ({@code FlowingFluid.getHeight}, {@code getSlopeDistance}) and what
 * {@code LiquidBlockRenderer} uses to smooth a water surface
 * ({@code LiquidBlockRenderer.java:44} and {@code :329}). That fixed the GT side, but the family
 * stayed one-way: vanilla's own {@code WaterFluid.isSame} is an identity test
 * ({@code WaterFluid.java:74-76}: {@code fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER}),
 * so a <em>vanilla</em> water block next to GT6 water still saw "another fluid" — its surface did not
 * merge with the GT6 water surface, and vanilla water would not spread into a GT6 water body.
 *
 * <p>{@code WaterFluid} is vanilla code, so this half can only be fixed by a mixin. The family
 * test recognises the six GT6 world waters directly and falls back to {@code #minecraft:water} for
 * other mods' water. Both directions of the relation agree, including during client tag reloads.
 */
@Mixin(WaterFluid.class)
public abstract class WaterFluidMixin {
    /**
     * {@code isSame} is a one-line identity test in vanilla; answer {@code true} for the water family.
     * This particular injection is required even though the config permits optional injections:
     * silently missing it restores a visible seam between vanilla and GT world water.
     */
    @Inject(method = "isSame", at = @At("HEAD"), cancellable = true, require = 1)
    private void gregtech$waterFamilyIsSame(Fluid fluid, CallbackInfoReturnable<Boolean> info) {
        WaterMixinStatus.markInvoked();
        if (GTWorldWaterFluid.isWaterFamily(fluid)) {
            info.setReturnValue(Boolean.TRUE);
        }
    }
}
