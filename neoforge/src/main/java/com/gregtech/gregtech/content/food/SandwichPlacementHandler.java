package com.gregtech.gregtech.content.food;

import com.gregtech.gregtech.blockentity.misc.SandwichBlockEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;

@net.neoforged.fml.common.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class SandwichPlacementHandler {
    private SandwichPlacementHandler() {}
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var result = tryPlace(event.getLevel(), event.getEntity(), event.getHand(), event.getHitVec());
        if (result.consumesAction()) { event.setCanceled(true); event.setCancellationResult(result); }
    }
    /** GT_Proxy:288-293: sneak-click toast/toasted toast to place a batch on solid ground. */
    public static InteractionResult tryPlace(Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) return InteractionResult.PASS;
        var held = player.getItemInHand(hand);
        int amount = SandwichRules.starterQuantity(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem()).toString(), held.getCount());
        if (amount == 0) return InteractionResult.PASS;
        var context = new BlockPlaceContext(level, player, hand, held, hit);
        var target = context.getClickedPos();
        if (!context.canPlace() || !level.isInWorldBounds(target) || !level.getWorldBorder().isWithinBounds(target)
                || !player.mayBuild() || !level.mayInteract(player, target)
                || !player.mayUseItemAt(target, context.getClickedFace(), held)) return InteractionResult.PASS;
        var state = com.gregtech.gregtech.registry.GTSandwich.SANDWICH_BLOCK.get().defaultBlockState();
        if (!state.canSurvive(level, target)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!level.setBlock(target, state, Block.UPDATE_ALL)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(target) instanceof SandwichBlockEntity sandwich)) return InteractionResult.FAIL;
        sandwich.seedBase(held.copyWithCount(amount));
        var sound = state.getSoundType(level, target, player);
        level.playSound(player, target, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * .8f);
        level.gameEvent(player, GameEvent.BLOCK_PLACE, target);
        if (!player.getAbilities().instabuild) held.shrink(amount);
        return InteractionResult.CONSUME;
    }
}
