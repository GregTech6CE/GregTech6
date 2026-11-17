package gregtech.mixin;

import gregtech.content.block.machine.VesselInnerFaces;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** A fire charge that hits the vessel must not light the hollow it opens into. */
@Mixin(SmallFireball.class)
public abstract class CrucibleCavityFireballMixin {
    @Redirect(
            method = "onHitBlock",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/Level;isEmptyBlock(Lnet/minecraft/core/BlockPos;)Z"),
            require = 1)
    private boolean gregtech$keepCrucibleCavityClear(Level level, BlockPos pos) {
        return !VesselInnerFaces.isFormedLargeCrucibleCavity(level, pos) && level.isEmptyBlock(pos);
    }
}
