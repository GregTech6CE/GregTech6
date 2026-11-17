package gregtech.mixin;

import gregtech.content.block.machine.VesselInnerFaces;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A powder-snow bucket must not place its block into a formed large crucible's hollow. */
@Mixin(SolidBucketItem.class)
public abstract class CrucibleCavitySolidBucketMixin {
    @Inject(method = "emptyContents", at = @At("HEAD"), cancellable = true, require = 1)
    private void gregtech$keepCrucibleCavityClear(
            Player player, Level level, BlockPos pos, BlockHitResult hit, CallbackInfoReturnable<Boolean> cir) {
        if (VesselInnerFaces.isFormedLargeCrucibleCavity(level, pos)) cir.setReturnValue(false);
    }
}
