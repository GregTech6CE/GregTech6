package com.gregtech.gregtech.item;
import net.minecraft.world.item.*;
/** The six GT6 hazard suits: four pieces each, 128 durability and one armor point per piece. */
public final class RadiationSuitItem extends ArmorItem{
 private final String family;
    public RadiationSuitItem(Type type) { this(type, "radiation"); }
    public RadiationSuitItem(Type type, String family) {
        super(com.gregtech.gregtech.registry.GTRadiationProtection.MATERIAL,type,new Properties().durability(128));
        com.gregtech.gregtech.content.hazard.HazmatSuitCatalog.texture(family);
        this.family=family;
    }
 @Override public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack stack,net.minecraft.world.entity.Entity entity,net.minecraft.world.entity.EquipmentSlot slot,ArmorMaterial.Layer layer,boolean innerModel){int index=switch(slot){case HEAD->0;case CHEST->1;case LEGS->2;default->3;};return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","textures/armor/" + com.gregtech.gregtech.content.hazard.HazmatSuitCatalog.texture(family) + "/"+index+".png");}
 @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flags){lines.add(net.minecraft.network.chat.Component.translatable(com.gregtech.gregtech.content.hazard.HazmatSuitCatalog.tooltip(family)));}
}
