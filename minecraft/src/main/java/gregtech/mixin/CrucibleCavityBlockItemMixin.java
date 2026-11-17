package gregtech.mixin;

import gregtech.content.block.machine.VesselInnerFaces;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A block item never fills a formed large crucible's hollow. {@link BlockPlaceContext#getClickedPos()}
 * is the resolved placement position, so this also covers a block clicked against the vessel wall.
 */
@Mixin(BlockItem.class)
public abstract class CrucibleCavityBlockItemMixin {
    @Inject(method = "placeBlock", at = @At("HEAD"), cancellable = true, require = 1)
    private void gregtech$keepCrucibleCavityClear(
            BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (VesselInnerFaces.isFormedLargeCrucibleCavity(context.getLevel(), context.getClickedPos()))
            cir.setReturnValue(false);
    }
}
