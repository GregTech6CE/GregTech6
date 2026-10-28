package gregtech.content.food;

import gregtech.content.blockentity.misc.SandwichBlockEntity;
import gregtech.content.item.GroundItemPlacement;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/** GT_Proxy:288-293: sneak-click toast/toasted toast to place a batch on solid ground. */
public final class SandwichPlacementHandler {
    private SandwichPlacementHandler() {}

    public static InteractionResult tryPlace(Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) return InteractionResult.PASS;
        var held = player.getItemInHand(hand);
        int amount = SandwichRules.starterQuantity(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem()).toString(), held.getCount());
        if (amount == 0) return InteractionResult.PASS;
        var context = new BlockPlaceContext(level, player, hand, held, hit);
//? if >=1.21.1 {
        /*var state = gregtech.content.registry.GTSandwich.SANDWICH_BLOCK.get().defaultBlockState();
*///?} else {
        var state = gregtech.content.registry.GTDecorBlocks.SANDWICH_BLOCK.get().defaultBlockState();
//?}
        return GroundItemPlacement.place(context, state, (placedLevel, pos, placedState, stack) -> {
            if (!(placedLevel.getBlockEntity(pos) instanceof SandwichBlockEntity sandwich)) return 0;
            sandwich.seedBase(stack.copyWithCount(amount));
            return amount;
        });
    }
}
