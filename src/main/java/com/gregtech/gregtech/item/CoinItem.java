package com.gregtech.gregtech.item;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.blockentity.misc.CoinPileBlockEntity;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * A GT6 coin is both an inventory item and a placeable multi-tile. The port keeps one material item
 * per metal, but using one on a replaceable position creates the coin pile and seeds the clicked
 * quarter-block cell with that very coin (GT6 MultiTileEntityCoin.onPlaced).
 */
public final class CoinItem extends MaterialItem {
    public CoinItem(Properties properties, GTMaterial material) {
        super(properties, MaterialPrefix.coin, material);
    }

    /** MultiTileEntityCoin keeps the family name; the material is a separate tooltip line. */
    @Override
    public net.minecraft.network.chat.Component getName(ItemStack stack) {
        return net.minecraft.network.chat.Component.translatable(getDescriptionId());
    }

    @Override
    public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable Level level,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(com.gregtech.gregtech.api.material.MaterialPresentation.name(getMaterial())
                .copy().withStyle(net.minecraft.ChatFormatting.AQUA));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.gregtech.gregtech.client.CoinItemRenderer.instance();
            }
        });
    }

    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        return 200; // GT6 MultiTileEntityCoin.getLifeSpan; expiry attempts to form a pile.
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack held = context.getItemInHand();
        if (held.isEmpty() || held.getItem() != this) return InteractionResult.PASS;

        BlockPlaceContext placement = new BlockPlaceContext(context);
        BlockPos target = placement.getClickedPos();
        if (!placement.canPlace() || !level.isInWorldBounds(target)
                || !level.getWorldBorder().isWithinBounds(target)) return InteractionResult.FAIL;

        Player player = context.getPlayer();
        if (player != null && (!player.mayBuild() || !level.mayInteract(player, target)
                || !player.mayUseItemAt(target, placement.getClickedFace(), held)))
            return InteractionResult.FAIL;

        BlockState state = GTDecorBlocks.COIN_PILE.get().defaultBlockState();
        if (!state.canSurvive(level, target)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!level.setBlock(target, state, Block.UPDATE_ALL)) return InteractionResult.FAIL;

        if (!(level.getBlockEntity(target) instanceof CoinPileBlockEntity pile)) {
            level.removeBlock(target, false);
            return InteractionResult.FAIL;
        }
        // GT6 clamps each coordinate into [0, .99] and selects x*4+z. This also handles a coin
        // placed against a block's side: the hit falls on the new block's nearest edge cell.
        int face = CoinPileBlockEntity.faceAt(
                context.getClickLocation().x - target.getX(),
                context.getClickLocation().z - target.getZ());
        if (pile.add(face, held) != 1) {
            level.removeBlock(target, false);
            return InteractionResult.FAIL;
        }

        SoundType sound = state.getSoundType(level, target, player);
        level.playSound(player, target, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1) / 2, sound.getPitch() * .8F);
        level.gameEvent(player, GameEvent.BLOCK_PLACE, target);
        if (player == null || !player.getAbilities().instabuild) held.shrink(1);
        return InteractionResult.CONSUME;
    }
}
