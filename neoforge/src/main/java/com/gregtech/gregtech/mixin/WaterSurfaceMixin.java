package com.gregtech.gregtech.mixin;

import com.gregtech.gregtech.api.fluid.WaterFamilyIdentity;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** All vanilla water-surface plants use the same water family, with source/empty checks preserved. */
@Mixin(WaterlilyBlock.class)
public abstract class WaterSurfaceMixin {
    @Redirect(method = "mayPlaceOn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;getType()Lnet/minecraft/world/level/material/Fluid;"), require = 2)
    private Fluid gregtech$waterFamilyForSurface(FluidState state) {
        return WaterFamilyIdentity.forVanillaCheck(state);
    }
}
