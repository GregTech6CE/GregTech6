package com.gregtech.gregtech.item;
import net.minecraft.world.item.*;
/** Original four lead suit pieces: 128 durability, one armor point per piece. */
public final class RadiationSuitItem extends ArmorItem{
 public RadiationSuitItem(Type type){super(com.gregtech.gregtech.registry.GTRadiationProtection.MATERIAL,type,new Properties().durability(128));}
 @Override public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack stack,net.minecraft.world.entity.Entity entity,net.minecraft.world.entity.EquipmentSlot slot,ArmorMaterial.Layer layer,boolean innerModel){int index=switch(slot){case HEAD->0;case CHEST->1;case LEGS->2;default->3;};return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","textures/armor/hazard_radiation/"+index+".png");}
 @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flags){lines.add(net.minecraft.network.chat.Component.translatable("gt.tooltip.radiation_suit"));}
}
