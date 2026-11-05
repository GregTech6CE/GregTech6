package gregtech.mixin;

import gregtech.content.worldgen.VanillaOreGeneration;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.OreVeinifier;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla's large ore veins are written by {@link OreVeinifier} inside {@link NoiseChunk}, not by an ore feature. */
@Mixin(OreVeinifier.class)
public abstract class VanillaOreVeinMixin {
    @Inject(method = "create", at = @At("HEAD"), cancellable = true, require = 1)
    private static void gt6ce$vanillaOreVeins(
            DensityFunction veinToggle,
            DensityFunction veinRidged,
            DensityFunction veinGap,
            PositionalRandomFactory random,
            CallbackInfoReturnable<NoiseChunk.BlockStateFiller> result) {
        if (VanillaOreGeneration.veinsDisabled()) {
            result.setReturnValue(context -> null);
        }
    }
}
