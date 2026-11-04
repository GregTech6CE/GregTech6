package gregtech.mixin;

import gregtech.engine.tool.GTToolHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GT6 auto-collect: a tool whose type collects drops ({@code MultiItemTool#canCollectDropsDirectly})
 * puts the broken block's drops straight into the player's inventory, spilling only when it is full.
 *
 * <p>Upstream does this in {@code HarvestDropsEvent}, which 1.20.1 no longer has, so the vanilla
 * player-break hook does it on both nodes. The block's own loot stays authoritative, which keeps
 * packed storage, tank contents and cover drops intact. Blocks that override {@code playerDestroy}
 * never reach this method, so the GT machines that already collect keep their own path.</p>
 */
@Mixin(Block.class)
public abstract class AutoCollectDropMixin {
    @Inject(method = "playerDestroy", at = @At("HEAD"), cancellable = true, require = 1)
    private void gregtech$collectDrops(Level level, Player player, BlockPos pos, BlockState state,
            BlockEntity blockEntity, ItemStack tool, CallbackInfo ci) {
        if (!(level instanceof ServerLevel server)
                || player == null
                || player.getAbilities().instabuild
                || !GTToolHelper.canCollectDrops(tool)) {
            return;
        }
        for (ItemStack drop : Block.getDrops(state, server, pos, blockEntity, player, tool)) {
            if (!drop.isEmpty() && !player.getInventory().add(drop)) {
                Block.popResource(level, pos, drop);
            }
        }
        state.spawnAfterBreak(server, pos, tool, true);
        ci.cancel();
    }
}
