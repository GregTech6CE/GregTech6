package gregtech.content.block.misc;

import gregtech.content.blockentity.misc.PileBlockEntity;
import gregtech.content.item.GroundItemPlacement;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * GT6 {@code GT_Proxy.java:307-326}: sneak-clicking a block with an ingot, plate or gem plate places
 * the whole held stack as multi-tile 32084, 32085 or 32086. The same ore-prefix lookup also covers
 * unified vanilla ingots; their {@code Item} classes cannot implement a GT-specific {@code useOn}.
 */
public final class PilePlacementHandler {
    private PilePlacementHandler() {}

    /** Returns PASS when GT6 would let the ordinary block/item interaction continue. */
    public static InteractionResult tryPlace(Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        // GT6 uses the current equipped item, and only enters this branch while sneaking.
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() || held.getItem() instanceof BlockItem || PileBlockEntity.materialOf(held) == null)
            return InteractionResult.PASS;

        // The cover event already allows a matching pile's native merge callback, even
        // while sneaking. Do not steal that click here and create an adjacent pile.
        // canAccept intentionally stays true when full: the native callback leaves the
        // held items untouched rather than silently creating a second pile.
        if (level.getBlockEntity(hit.getBlockPos()) instanceof PileBlockEntity pile && pile.canAccept(held))
            return InteractionResult.PASS;

        // Manual vessel contents need not have a recipe. Never turn a hand-storage
        // attempt (including a full vessel) into loose-pile placement instead.
        if (level.getBlockEntity(hit.getBlockPos())
                        instanceof gregtech.content.blockentity.tool.ProcessingToolBlockEntity tool
                && tool.handlesHandInput(held)) return InteractionResult.PASS;

        var rockResult = gregtech.content.item.RockItemPlacement.place(
                new net.minecraft.world.item.context.UseOnContext(player, hand, hit));
        if (rockResult != InteractionResult.PASS) return rockResult;

        var prefix = PileBlockEntity.prefixOf(held);
        PileBlock.Kind kind;
        if (prefix == PileBlock.Kind.INGOT.prefix()) kind = PileBlock.Kind.INGOT;
        else if (prefix == PileBlock.Kind.PLATE.prefix()) kind = PileBlock.Kind.PLATE;
        else if (prefix == PileBlock.Kind.GEM_PLATE.prefix()) kind = PileBlock.Kind.GEM_PLATE;
        else return InteractionResult.PASS;

        BlockPlaceContext context = new BlockPlaceContext(level, player, hand, held, hit);
        BlockState state = kind.block().defaultBlockState();
        return GroundItemPlacement.place(context, state, (placedLevel, pos, placedState, stack) -> {
            if (!(placedLevel.getBlockEntity(pos) instanceof PileBlockEntity pile)) return 0;
            return pile.add(stack);
        });
    }
}
