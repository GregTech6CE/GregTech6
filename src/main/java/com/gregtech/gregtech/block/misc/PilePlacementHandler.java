package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.blockentity.misc.PileBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * GT6 {@code GT_Proxy.java:307-326}: sneak-clicking a block with an ingot, plate or gem plate places
 * the whole held stack as multi-tile 32084, 32085 or 32086. The same ore-prefix lookup also covers
 * unified vanilla ingots; their {@code Item} classes cannot implement a GT-specific {@code useOn}.
 */
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PilePlacementHandler {
    private PilePlacementHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        InteractionResult result = tryPlace(event.getLevel(), event.getEntity(), event.getHand(), event.getHitVec());
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    /** Returns PASS when GT6 would let the ordinary block/item interaction continue. */
    public static InteractionResult tryPlace(Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        // GT6 uses the current equipped item, and only enters this branch while sneaking.
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() || held.getItem() instanceof BlockItem
                || PileBlockEntity.materialOf(held) == null) return InteractionResult.PASS;

        var rockResult = com.gregtech.gregtech.item.RockItemPlacement.place(
                new net.minecraft.world.item.context.UseOnContext(player, hand, hit));
        if (rockResult != InteractionResult.PASS) return rockResult;

        var prefix = PileBlockEntity.prefixOf(held);
        PileBlock.Kind kind;
        if (prefix == PileBlock.Kind.INGOT.prefix()) kind = PileBlock.Kind.INGOT;
        else if (prefix == PileBlock.Kind.PLATE.prefix()) kind = PileBlock.Kind.PLATE;
        else if (prefix == PileBlock.Kind.GEM_PLATE.prefix()) kind = PileBlock.Kind.GEM_PLATE;
        else return InteractionResult.PASS;

        BlockPlaceContext context = new BlockPlaceContext(level, player, hand, held, hit);
        BlockPos target = context.getClickedPos();
        if (!context.canPlace() || !level.isInWorldBounds(target)
                || !level.getWorldBorder().isWithinBounds(target)
                || !player.mayBuild() || !level.mayInteract(player, target)
                || !player.mayUseItemAt(target, context.getClickedFace(), held)) return InteractionResult.PASS;

        BlockState state = kind.block().defaultBlockState();
        if (!state.canSurvive(level, target)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!level.setBlock(target, state, Block.UPDATE_ALL)) return InteractionResult.PASS;

        if (!(level.getBlockEntity(target) instanceof PileBlockEntity pile)) {
            level.removeBlock(target, false);
            return InteractionResult.PASS;
        }
        int moved = pile.add(held);
        if (moved == 0) {
            level.removeBlock(target, false);
            return InteractionResult.PASS;
        }

        SoundType sound = state.getSoundType(level, target, player);
        level.playSound(player, target, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1) / 2, sound.getPitch() * .8F);
        level.gameEvent(player, GameEvent.BLOCK_PLACE, target);
        // ST.use in GT6 leaves creative inventory untouched after creating the stocked pile.
        if (!player.getAbilities().instabuild) held.shrink(moved);
        return InteractionResult.CONSUME;
    }
}
