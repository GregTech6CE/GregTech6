package gregtech.mixin;

import gregtech.content.block.machine.VesselInnerFaces;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A bucket must not pour its fluid block into a formed large crucible's hollow. */
@Mixin(BucketItem.class)
public abstract class CrucibleCavityBucketMixin {
    /**
     * The container overload is a loader addition, so it has no mapping entry and both the name and
     * the descriptor stay as written ({@code AbstractMinecartSpeedCapMixin} does the same).
     */
    @Inject(
            method =
                    "emptyContents(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1)
    private void gregtech$keepCrucibleCavityClear(
            Player player,
            Level level,
            BlockPos pos,
            BlockHitResult hit,
            ItemStack container,
            CallbackInfoReturnable<Boolean> cir) {
        if (VesselInnerFaces.isFormedLargeCrucibleCavity(level, pos)) cir.setReturnValue(false);
    }
}
