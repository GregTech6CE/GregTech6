package com.gregtech.gregtech.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;

/** GT6 lead hazard suit: four pieces, 128 durability per piece, one armor point each. */
public final class RadiationSuitItem extends ArmorItem {
    public RadiationSuitItem(Type type) { super(SuitMaterial.INSTANCE, type, new Properties()); }
    @Override public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String layer) {
        int index = switch (slot) { case HEAD -> 0; case CHEST -> 1; case LEGS -> 2; default -> 3; };
        return "gregtech:textures/armor/hazard_radiation/" + index + ".png";
    }
    @Override public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level level,
            java.util.List<net.minecraft.network.chat.Component> lines, TooltipFlag flags) {
        lines.add(net.minecraft.network.chat.Component.translatable("gt.tooltip.radiation_suit"));
    }
    private enum SuitMaterial implements ArmorMaterial {
        INSTANCE;
        public int getDurabilityForType(Type type) { return 128; }
        public int getDefenseForType(Type type) { return 1; }
        public int getEnchantmentValue() { return 0; }
        public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_LEATHER; }
        public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
        public String getName() { return "gregtech:radiation_suit"; }
        public float getToughness() { return 0; }
        public float getKnockbackResistance() { return 0; }
    }
}
