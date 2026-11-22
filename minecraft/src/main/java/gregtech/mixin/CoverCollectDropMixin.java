package gregtech.mixin;

import gregtech.content.cover.CoverDrops;
import gregtech.engine.tool.GTToolHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Collects separated covers into the breaker's inventory when dismantling a covered host with a
 * collecting tool (such as a wrench or monkey wrench), matching GT6 direct-drop collection.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class CoverCollectDropMixin {
    @Shadow
    protected ServerLevel level;

    @Shadow
    @Final
    protected ServerPlayer player;

    @Shadow
    public abstract boolean isCreative();

    @Inject(method = "destroyBlock", at = @At("HEAD"), require = 1)
    private void gregtech$pushCoverCollector(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (isCreative() || level == null || player == null) {
            CoverDrops.pushCollector(null, null, null);
            return;
        }
        ItemStack tool = player.getMainHandItem();
        BlockState state = level.getBlockState(pos);
        if (!GTToolHelper.canCollectDrops(tool) || !state.canHarvestBlock(level, pos, player)) {
            CoverDrops.pushCollector(null, null, null);
            return;
        }
        CoverDrops.pushCollector(player, level, pos);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"), require = 1)
    private void gregtech$popCoverCollector(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        CoverDrops.popCollector();
    }
}
