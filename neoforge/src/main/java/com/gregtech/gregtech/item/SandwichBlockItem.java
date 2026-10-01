package com.gregtech.gregtech.item;

import com.gregtech.gregtech.blockentity.misc.SandwichBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** GT6 sandwich items are edible in the hand, and place only while sneaking. */
public final class SandwichBlockItem extends BlockItem {
    public SandwichBlockItem(Block block, Properties properties) { super(block, properties.stacksTo(16)); }

    @Override public InteractionResult useOn(UseOnContext context) {
        return context.getPlayer() != null && !context.getPlayer().isShiftKeyDown()
                ? InteractionResult.PASS : super.useOn(context);
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.getAbilities().instabuild && !player.getFoodData().needsFood())
            return InteractionResultHolder.pass(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override public int getUseDuration(ItemStack stack,LivingEntity entity) {
        return com.gregtech.gregtech.content.food.SandwichRules.useDuration(SandwichBlockEntity.itemFood(stack,entity.level().registryAccess()));
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.EAT; }

    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            player.getFoodData().eat(SandwichBlockEntity.itemFood(stack,entity.level().registryAccess()), SandwichBlockEntity.itemSaturation(stack,entity.level().registryAccess()));
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return stack;
    }

    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,
                                          List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack,context,tooltip,flag);
        tooltip.add(Component.translatable("tooltip.gregtech.sandwich.food",
                SandwichBlockEntity.itemFood(stack,context.registries()), SandwichBlockEntity.itemSaturation(stack,context.registries()))
                .withStyle(ChatFormatting.GRAY));
    }
}
