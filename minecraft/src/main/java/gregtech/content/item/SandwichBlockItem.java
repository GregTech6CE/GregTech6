package gregtech.content.item;

import gregtech.content.blockentity.misc.SandwichBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** GT6 sandwich items are edible in the hand, and place only while sneaking. */
public final class SandwichBlockItem extends GTBlockItem {
    public SandwichBlockItem(Block block, Properties properties) { super(block, properties.stacksTo(16)); }

//? if >=1.21.1 {
//?} else {
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return gregtech.client.SandwichItemRenderer.instance();
            }
        });
    }

//?}
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

//? if >=1.21.1 {
    /*@Override public int getUseDuration(ItemStack stack,LivingEntity entity) {
        return gregtech.content.food.SandwichRules.useDuration(SandwichBlockEntity.itemFood(stack,entity.level().registryAccess()));
*///?} else {
    @Override public int getUseDuration(ItemStack stack) {
        return gregtech.content.food.SandwichRules.useDuration(SandwichBlockEntity.itemFood(stack, null));
//?}
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.EAT; }

    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            player.getFoodData().eat(SandwichBlockEntity.itemFood(stack, level.registryAccess()),
                    SandwichBlockEntity.itemSaturation(stack, level.registryAccess()));
            for (var ingredient : SandwichBlockEntity.readItemIngredients(stack, level.registryAccess()))
                if (!ingredient.isEmpty()) gregtech.content.food.SandwichNutrition.apply(ingredient, player);
            level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_BURP,
                    net.minecraft.sounds.SoundSource.PLAYERS, .5f, .9f + level.random.nextFloat() * .1f);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return stack;
    }

    @Override
    protected void appendTooltip(ItemStack stack, @Nullable Level level,
            @Nullable net.minecraft.core.HolderLookup.Provider registries, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gt6ce.sandwich.food",
                SandwichBlockEntity.itemFood(stack, registries), SandwichBlockEntity.itemSaturation(stack, registries))
                .withStyle(ChatFormatting.RED));
        var layers = SandwichBlockEntity.readItemIngredients(stack, registries);
        for (int slot = layers.length - 1; slot >= 0; slot--) if (!layers[slot].isEmpty())
            tooltip.add(layers[slot].getHoverName().copy().withStyle(ChatFormatting.GRAY));
    }
}
