package gregtech.content.item;

import gregtech.content.block.RockBlock;
import gregtech.content.blockentity.RockBlockEntity;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Replaces one collected pebble, preserving its material and never recreating an ore bonus. */
public final class RockItemPlacement {
    private RockItemPlacement() {}

    public static InteractionResult place(UseOnContext use) {
        var stack = use.getItemInHand();
        var form = gregtech.engine.material.MaterialEquivalence.form(stack);
        if (use.getPlayer() == null
                || !use.getPlayer().isShiftKeyDown()
                || form == null
                || form.prefix() != MaterialPrefix.rockGt && form.prefix() != MaterialPrefix.oreRaw)
            return InteractionResult.PASS;
        return placeLooseRock(
                use,
                form.material().resolve().getName(),
                net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(stack.getItem())
                        .toString());
    }

    /** GT6 {@code GT_Proxy.java:297}: flint put down by hand becomes a lying-item rock. */
    public static InteractionResult placeFlint(UseOnContext use) {
        if (use.getPlayer() == null
                || !use.getPlayer().isShiftKeyDown()
                || !use.getItemInHand().is(net.minecraft.world.item.Items.FLINT)) return InteractionResult.PASS;
        return placeLooseRock(use, null, "minecraft:flint");
    }

    private static InteractionResult placeLooseRock(UseOnContext use, String material, String itemId) {
        var stack = use.getItemInHand();
        var context = new BlockPlaceContext(use);
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        var below = pos.below();
        if (!player.mayBuild()
                || !level.mayInteract(player, pos)
                || !level.isInWorldBounds(pos)
                || !context.canPlace()
                || !level.getWorldBorder().isWithinBounds(pos)
                || !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                || player != null && !player.mayUseItemAt(pos, context.getClickedFace(), stack))
            return InteractionResult.FAIL;
        var state = GTBlocks.ROCK
                .get()
                .defaultBlockState()
                .setValue(RockBlock.GROUND, RockBlock.Ground.of(level.getBlockState(below)));
        if (!state.canSurvive(level, pos)
                || !level.isUnobstructed(
                        state, pos, player == null ? CollisionContext.empty() : CollisionContext.of(player)))
            return InteractionResult.FAIL;
        if (!level.isClientSide) {
            if (!level.setBlock(pos, state, 3)) return InteractionResult.FAIL;
            if (level.getBlockEntity(pos) instanceof RockBlockEntity rock) {
                if (material != null && !material.isEmpty()) rock.setMaterial(material);
                rock.setItemId(itemId);
                rock.setCount(1);
                rock.setRawOre(false);
            } else {
                level.removeBlock(pos, false);
                return InteractionResult.FAIL;
            }
            if (player != null) player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
            level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
        }
        var sound = state.getSoundType();
        level.playSound(
                player,
                pos,
                sound.getPlaceSound(),
                net.minecraft.sounds.SoundSource.BLOCKS,
                (sound.getVolume() + 1) / 2,
                sound.getPitch() * 0.8F);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
