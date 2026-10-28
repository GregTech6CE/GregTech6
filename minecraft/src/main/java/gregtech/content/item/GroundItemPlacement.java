package gregtech.content.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Port of GT6's {@code MultiTileEntityItemInternal#tryPlaceItemIntoWorld} for the ground blocks
 * {@code GT_Proxy} places by hand: validate the click once, store the held stack in the new block,
 * then consume what it took.
 */
public final class GroundItemPlacement {
    /** Stores the held stack in the placed block; returns the consumed amount, or 0 to roll back. */
    @FunctionalInterface
    public interface Fill {
        int fill(Level level, BlockPos pos, BlockState state, ItemStack held);
    }

    /** Extra per-block placement test, such as the rock's entity-obstruction check. */
    @FunctionalInterface
    public interface Check {
        boolean test(Level level, BlockPos pos, BlockState state, Player player);
    }

    private GroundItemPlacement() {}

    public static InteractionResult place(BlockPlaceContext context, BlockState state, Fill fill) {
        return place(context, state, (level, pos, placed, player) -> true, fill);
    }

    public static InteractionResult place(BlockPlaceContext context, BlockState state, Check check, Fill fill) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        ItemStack held = context.getItemInHand();
        if (player == null) return InteractionResult.PASS;
        BlockPos target = context.getClickedPos();
        if (!context.canPlace()
                || !level.isInWorldBounds(target)
                || !level.getWorldBorder().isWithinBounds(target)
                || !player.mayBuild()
                || !level.mayInteract(player, target)
                || !player.mayUseItemAt(target, context.getClickedFace(), held)) return InteractionResult.PASS;
        if (!state.canSurvive(level, target) || !check.test(level, target, state, player))
            return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!level.setBlock(target, state, Block.UPDATE_ALL)) return InteractionResult.PASS;

        int consumed = fill.fill(level, target, state, held);
        if (consumed <= 0) {
            level.removeBlock(target, false);
            return InteractionResult.PASS;
        }
        SoundType sound = state.getSoundType(level, target, player);
        level.playSound(
                player,
                target,
                sound.getPlaceSound(),
                SoundSource.BLOCKS,
                (sound.getVolume() + 1) / 2,
                sound.getPitch() * .8f);
        level.gameEvent(player, GameEvent.BLOCK_PLACE, target);
        if (!player.getAbilities().instabuild) held.shrink(consumed);
        return InteractionResult.CONSUME;
    }
}
