package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.item.RadiationSuitItem;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GTRadiationProtection {
    public static final Map<ArmorItem.Type, DeferredHolder<net.minecraft.world.item.Item,RadiationSuitItem>> SUIT = new LinkedHashMap<>();
    static {
        for (var type : new ArmorItem.Type[]{ArmorItem.Type.HELMET,ArmorItem.Type.CHESTPLATE,ArmorItem.Type.LEGGINGS,ArmorItem.Type.BOOTS}) SUIT.put(type, GTItems.ITEMS.register(
                "radiation_suit_" + type.getName(), () -> new RadiationSuitItem(type)));
    }
    private GTRadiationProtection() {}
    public static void registerAll() {}
    public static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.world.item.ArmorMaterial> MATERIALS=net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.ARMOR_MATERIAL,"gregtech");
    public static final DeferredHolder<net.minecraft.world.item.ArmorMaterial,net.minecraft.world.item.ArmorMaterial> MATERIAL=MATERIALS.register("radiation_suit",()->new net.minecraft.world.item.ArmorMaterial(java.util.Map.of(ArmorItem.Type.HELMET,1,ArmorItem.Type.CHESTPLATE,1,ArmorItem.Type.LEGGINGS,1,ArmorItem.Type.BOOTS,1),0,net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER,()->net.minecraft.world.item.crafting.Ingredient.EMPTY,java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","radiation_suit"))),0,0));
}
