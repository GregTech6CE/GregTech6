package gregtech.mixin;

import gregtech.content.block.machine.VesselInnerFaces;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A falling block that reaches a formed large crucible's hollow is reported as a failed placement,
 * so it drops as an item instead of filling a cavity layer.
 */
@Mixin(FallingBlockEntity.class)
public abstract class CrucibleCavityFallingBlockMixin {
    @Redirect(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            require = 1)
    private boolean gregtech$keepCrucibleCavityClear(Level level, BlockPos pos, BlockState state, int flags) {
        return !VesselInnerFaces.isFormedLargeCrucibleCavity(level, pos) && level.setBlock(pos, state, flags);
    }
}
