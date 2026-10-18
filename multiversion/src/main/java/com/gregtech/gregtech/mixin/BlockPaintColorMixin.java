package com.gregtech.gregtech.mixin;
import com.gregtech.gregtech.api.tool.Paintable;import net.minecraft.client.color.block.BlockColors;import net.minecraft.core.BlockPos;import net.minecraft.world.level.BlockAndTintGetter;import net.minecraft.world.level.block.state.BlockState;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Override only primary painted surface tint; existing material/core/overlay handlers keep ownership. */
@Mixin(BlockColors.class)public abstract class BlockPaintColorMixin {
@Inject(method="getColor(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;I)I",at=@At("RETURN"),cancellable=true,require=1)
private void gregtech$paint(BlockState state,BlockAndTintGetter level,BlockPos pos,int tint,CallbackInfoReturnable<Integer> cir){if(tint==0&&level!=null&&pos!=null&&level.getBlockEntity(pos) instanceof Paintable paint&&paint.isPainted())cir.setReturnValue(paint.getPaint());}
}
