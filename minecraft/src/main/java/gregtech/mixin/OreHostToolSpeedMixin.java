package gregtech.mixin;

import gregtech.content.data.OreHostHarvest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps vanilla shovel/pickaxe speed coherent with reviewed ore host state, not shared static tags. */
@Mixin(ItemStack.class)
public abstract class OreHostToolSpeedMixin {
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true, require = 1)
    private void gregtech$sourceOreHostToolSpeed(BlockState state, CallbackInfoReturnable<Float> result) {
        OreHostHarvest.vanillaSpeed((ItemStack) (Object) this, state).ifPresent(result::setReturnValue);
    }
}
