package gregtech.mixin;

import gregtech.content.registry.GTFluids;
import net.minecraft.world.entity.Entity;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidType;
*///?} else {
import net.minecraftforge.fluids.FluidType;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** GT6 MaterialGas is not liquid immersion. Leave fluid states/eye sampling and hazards intact. */
@Mixin(Entity.class)
public abstract class GasImmersionMixin {
    // Loader-added method, not a Mojang-mapped name. Both loaders populate immersion through it.
    @Inject(method = "setFluidTypeHeight", at = @At("HEAD"), cancellable = true, remap = false)
    private void gregtech$excludeGasImmersion(FluidType type, double height, CallbackInfo ci) {
        if (GTFluids.isNonLiquidType(type)) ci.cancel();
    }
}
