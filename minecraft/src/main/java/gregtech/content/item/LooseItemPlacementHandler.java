package gregtech.content.item;

import gregtech.content.registry.GTBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
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
 * GT6 {@code GT_Proxy.java:293-304}: sneak-clicking a wooden stick or flint puts the loose
 * item on the ground instead of doing nothing. Sticks become a placed-stick block, flint
 * becomes the lying-item rock. GT6's {@code stickAnyNormalWood} is the
 * {@code gt6ce:ammunition/sticks_wood} tag plus the common wooden-rod tag.
 */
//? if >=1.21.1 {
/*@net.neoforged.fml.common.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
*///?} else {
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
//?}
public final class LooseItemPlacementHandler {
    private static final ResourceLocation WOODEN_STICKS =
            ResourceLocation.fromNamespaceAndPath("gt6ce", "ammunition/sticks_wood");

    private LooseItemPlacementHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        InteractionResult result = tryPlace(event.getLevel(), event.getEntity(), event.getHand(), event.getHitVec());
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    public static InteractionResult tryPlace(Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.FLINT)) return RockItemPlacement.placeFlint(new UseOnContext(player, hand, hit));
        if (!isWoodenStick(held)) return InteractionResult.PASS;

        BlockPlaceContext context = new BlockPlaceContext(level, player, hand, held, hit);
        BlockPos target = context.getClickedPos();
        if (!context.canPlace() || !level.isInWorldBounds(target)
                || !level.getWorldBorder().isWithinBounds(target)
                || !player.mayBuild() || !level.mayInteract(player, target)
                || !player.mayUseItemAt(target, context.getClickedFace(), held)) return InteractionResult.PASS;

        BlockState state = GTBlocks.PLACED_TWIGS.get().defaultBlockState();
        if (!state.canSurvive(level, target)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!level.setBlock(target, state, Block.UPDATE_ALL)) return InteractionResult.PASS;

        SoundType sound = state.getSoundType(level, target, player);
        level.playSound(player, target, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1) / 2, sound.getPitch() * .8f);
        level.gameEvent(player, GameEvent.BLOCK_PLACE, target);
        if (!player.getAbilities().instabuild) held.shrink(1);
        return InteractionResult.CONSUME;
    }

    /** Vanilla sticks, GT wooden sticks and other mods' wooden rods (GT6 {@code stickAnyNormalWood}). */
    private static boolean isWoodenStick(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.STICK)
                || stack.is(ItemTags.create(WOODEN_STICKS))
                || gregtech.engine.material.MaterialEquivalence.hasForgeTag(stack, "rods/wooden");
    }
}
