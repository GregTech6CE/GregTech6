package com.gregtech.gregtech.content.nuclear;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.Item;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
/** GT6 UT.Entities.applyRadioactivity: player tracker, potion fallback for other living entities. */
public final class ReactorRadiation {
    public static final TagKey<Item> PROTECTION=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("gregtech","radiation_protection"));
    private ReactorRadiation(){}
    public static boolean protectedByArmor(LivingEntity entity){
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})if(entity.getItemBySlot(slot).isEmpty()||!entity.getItemBySlot(slot).is(PROTECTION))return false;
        return true;
    }
    public static boolean apply(LivingEntity entity,int level,int dose){
        if(level<=0||dose<=0||!entity.isAlive()||entity.getMobType()==MobType.UNDEAD||entity.getMobType()==MobType.ARTHROPOD||protectedByArmor(entity))return false;
        if(entity.level().isClientSide)return false;
        if(entity instanceof net.minecraft.world.entity.player.Player player){
            PlayerRadiation.change(player,(long)level*dose);
            return true;
        }
        int amplifier=(int)Math.min(5,5L*level/7);
        add(entity,MobEffects.MOVEMENT_SLOWDOWN,140,level,dose,amplifier);
        add(entity,MobEffects.DIG_SLOWDOWN,150,level,dose,amplifier);
        add(entity,MobEffects.CONFUSION,130,level,dose,amplifier);
        add(entity,MobEffects.WEAKNESS,150,level,dose,amplifier);
        add(entity,MobEffects.HUNGER,130,level,dose,amplifier);
        add(entity,MobEffects.WITHER,130,level,dose,amplifier);
        return true;
    }
    private static void add(LivingEntity entity,MobEffect effect,int factor,int level,int dose,int amplifier){
        var previous=entity.getEffect(effect);long duration=(long)Math.min(Integer.MAX_VALUE,(double)factor*level*dose);
        duration=Math.min(Integer.MAX_VALUE,duration+(previous==null?0:Math.max(0,previous.getDuration())));
        entity.addEffect(new MobEffectInstance(effect,(int)duration,amplifier));
    }
}
