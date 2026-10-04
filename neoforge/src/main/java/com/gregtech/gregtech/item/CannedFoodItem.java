/* Derived from GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original authors and license retained; see NOTICE and source provenance. */
package com.gregtech.gregtech.item;

import com.gregtech.gregtech.content.food.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

/** Native FoodStat consumption and container boundary for the 57 source cans. */
public final class CannedFoodItem extends TechItem {
    private final CannedFoodCatalog.Can spec;
    public CannedFoodItem(String id, String name, Properties props) { super(name,props); spec=java.util.Objects.requireNonNull(CannedFoodCatalog.forId(id),id); }
    public CannedFoodCatalog.Can spec() { return spec; }
    public static ItemStack emptyCan() { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech","empty_food_can"))); }
    @Override public boolean hasCraftingRemainingItem(ItemStack stack) { return true; }
    @Override public ItemStack getCraftingRemainingItem(ItemStack stack) { return emptyCan(); }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return spec.rebreathe()>0 ? UseAnim.DRINK : UseAnim.EAT; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return spec.useDuration(); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if (!player.canEat(spec.food().food().alwaysEdible())) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand); return InteractionResultHolder.consume(stack);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) return stack;
        if (!level.isClientSide) {
            var food=spec.food().food(); player.getFoodData().eat(food.level(),food.saturation());
            SandwichNutrition.apply(stack,player);
            player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
            level.playSound(null,player.getX(),player.getY(),player.getZ(),net.minecraft.sounds.SoundEvents.PLAYER_BURP,net.minecraft.sounds.SoundSource.PLAYERS,.5f,.9f+player.getRandom().nextFloat()*.1f);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            if (stack.isEmpty()) return emptyCan();
            if (!level.isClientSide) GTFoodItems.deliverContainer(player,emptyCan());
        }
        return stack;
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        boolean used=false;
        if (spec.dogFood() && entity instanceof net.minecraft.world.entity.animal.Wolf wolf && wolf.isTame()) {
            if(wolf.getHealth()<20) { if(!player.level().isClientSide) wolf.heal(spec.food().food().level()); used=true; }
            else if(wolf.getAge()==0 && !wolf.isInLove()) { if(!player.level().isClientSide) wolf.setInLove(player); used=true; }
        } else if(spec.catFood() && entity instanceof net.minecraft.world.entity.animal.Cat cat && cat.isTame() && cat.getAge()==0 && !cat.isInLove()) {
            if(!player.level().isClientSide) cat.setInLove(player); used=true;
        }
        if(!used) return InteractionResult.PASS;
        if(!player.getAbilities().instabuild) { stack.shrink(1); if(!player.level().isClientSide) GTFoodItems.deliverContainer(player,emptyCan()); }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
    /** Preserves stack quantity and custom data, as MultiItemCans.getRotten does. */
    public ItemStack getRotten(ItemStack stack) {
        String target=CannedFoodCatalog.rottenId(spec.id()); if(target.equals(spec.id())) return stack;
        Item item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",target));
        return stack.transmuteCopy(item, stack.getCount());
    }
    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        var food=spec.food().food();
        if (food.level()>0 || food.saturation()>0) tooltip.add(Component.translatable("tooltip.gregtech.sandwich.food",food.level(),food.saturation()).withStyle(net.minecraft.ChatFormatting.RED));
        if (food.rotten()) tooltip.add(Component.translatable("tooltip.gregtech.food.rotten").withStyle(net.minecraft.ChatFormatting.DARK_RED));
        if (spec.catFood()) tooltip.add(Component.translatable("tooltip.gregtech.food.cat"));
        if (spec.dogFood()) tooltip.add(Component.translatable("tooltip.gregtech.food.dog"));
    }
}
