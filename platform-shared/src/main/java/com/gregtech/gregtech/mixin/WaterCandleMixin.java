package com.gregtech.gregtech.mixin;

import com.gregtech.gregtech.api.fluid.WaterFamilyIdentity;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The common vanilla container API accepts source water from the tagged water family. */
@Mixin(CandleBlock.class)
public abstract class WaterCandleMixin {
    @Redirect(method = "placeLiquid", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;getType()Lnet/minecraft/world/level/material/Fluid;"), require = 1)
    private Fluid gregtech$waterFamilyForFilling(FluidState state) {
        return WaterFamilyIdentity.forVanillaCheck(state);
    }
}
