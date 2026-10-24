package gregtech.mixin;
import gregtech.data.OreHostPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Both mapped loaders expose the same cached-state getter; unknown owners/states pass through. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class OreHostHardnessMixin {
 @Inject(method="getDestroySpeed",at=@At("RETURN"),cancellable=true,require=1)
 private void gregtech$sourceOreHostHardness(BlockGetter level,BlockPos pos,CallbackInfoReturnable<Float> result) {
  OreHostPhysics.params((BlockState)(Object)this).ifPresent(value->result.setReturnValue(value.hardness()));
 }
 @Inject(method="requiresCorrectToolForDrops",at=@At("RETURN"),cancellable=true,require=1)
 private void gregtech$sourceOreHostToolRequirement(CallbackInfoReturnable<Boolean> result) {
  OreHostPhysics.params((BlockState)(Object)this).ifPresent(value->result.setReturnValue(value.requiresToolForDrops()));
 }
}
