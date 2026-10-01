package com.gregtech.gregtech.item;

import com.gregtech.gregtech.content.nuclear.PlayerRadiation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Original MultiItemFood 31001: edible even when full, zero nutrition, -50 radiation. */
public final class RadawayItem extends TechItem {
    public RadawayItem() {
        super("Radaway", new Properties().food(new FoodProperties.Builder()
                .nutrition(0).saturationModifier(0).alwaysEdible().build()));
    }

    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) PlayerRadiation.change(player, -50);
        return super.finishUsingItem(stack, level, entity);
    }
}
