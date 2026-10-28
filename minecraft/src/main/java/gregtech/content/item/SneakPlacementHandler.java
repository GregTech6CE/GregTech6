package gregtech.content.item;

import gregtech.content.block.misc.PilePlacementHandler;
import gregtech.content.food.SandwichPlacementHandler;
import gregtech.content.registry.GTBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
*///?} else {
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
//?}

/**
 * GT6 {@code GT_Proxy.java:288-326}: one sneak-click entry that puts the held loose item on the
 * ground — toast batches, wooden sticks, flint, surface rocks and metal piles, in source order.
 * GT6's {@code stickAnyNormalWood} is the {@code gt6ce:ammunition/sticks_wood} tag plus the common
 * wooden-rod tag.
 */
//? if >=1.21.1 {
/*@net.neoforged.fml.common.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
*///?} else {
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
//?}
public final class SneakPlacementHandler {
    private static final ResourceLocation WOODEN_STICKS =
            ResourceLocation.fromNamespaceAndPath("gt6ce", "ammunition/sticks_wood");

    private SneakPlacementHandler() {}

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
        // GT6 uses the equipped item and only enters this branch while sneaking.
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) return InteractionResult.PASS;
        InteractionResult result = SandwichPlacementHandler.tryPlace(level, player, hand, hit);
        if (result != InteractionResult.PASS) return result;
        result = placeStick(level, player, hand, hit);
        if (result != InteractionResult.PASS) return result;
        if (player.getItemInHand(hand).is(Items.FLINT)) {
            result = RockItemPlacement.placeFlint(new UseOnContext(player, hand, hit));
            if (result != InteractionResult.PASS) return result;
        }
        return PilePlacementHandler.tryPlace(level, player, hand, hit);
    }

    private static InteractionResult placeStick(
            Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!isWoodenStick(held)) return InteractionResult.PASS;
        BlockPlaceContext context = new BlockPlaceContext(level, player, hand, held, hit);
        return GroundItemPlacement.place(context, GTBlocks.PLACED_TWIGS.get().defaultBlockState(),
                (placedLevel, pos, state, stack) -> 1);
    }

    /** Vanilla sticks, GT wooden sticks and other mods' wooden rods (GT6 {@code stickAnyNormalWood}). */
    private static boolean isWoodenStick(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.STICK)
                || stack.is(ItemTags.create(WOODEN_STICKS))
                || gregtech.engine.material.MaterialEquivalence.hasForgeTag(stack, "rods/wooden");
    }
}
