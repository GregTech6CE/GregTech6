package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.item.RadiationSuitItem;
import net.minecraft.world.item.ArmorItem;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.DeferredHolder;
*///?} else {
import net.minecraftforge.registries.RegistryObject;
//?}
import java.util.LinkedHashMap;
import java.util.Map;

public final class GTRadiationProtection {
//? if >=1.21.1 {
    /*public static final Map<ArmorItem.Type, DeferredHolder<net.minecraft.world.item.Item,RadiationSuitItem>> SUIT = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<net.minecraft.world.item.Item,RadiationSuitItem>> ALL = new LinkedHashMap<>();
*///?} else {
    public static final Map<ArmorItem.Type, RegistryObject<RadiationSuitItem>> SUIT = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<RadiationSuitItem>> ALL = new LinkedHashMap<>();
//?}
    static {
        for (String family : com.gregtech.gregtech.content.hazard.HazmatSuitCatalog.FAMILIES)
            for (var type : new ArmorItem.Type[]{ArmorItem.Type.HELMET,ArmorItem.Type.CHESTPLATE,ArmorItem.Type.LEGGINGS,ArmorItem.Type.BOOTS}) {
                String id=family+"_suit_"+type.getName();
                var item=GTItems.ITEMS.register(id, () -> new RadiationSuitItem(type,family));
                ALL.put(id,item);
                if(family.equals("radiation"))SUIT.put(type,item);
            }
    }
    private GTRadiationProtection() {}
    public static void registerAll() {}
//? if >=1.21.1 {
    /*public static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.world.item.ArmorMaterial> MATERIALS=net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.ARMOR_MATERIAL,"gregtech");
    public static final DeferredHolder<net.minecraft.world.item.ArmorMaterial,net.minecraft.world.item.ArmorMaterial> MATERIAL=MATERIALS.register("radiation_suit",()->new net.minecraft.world.item.ArmorMaterial(java.util.Map.of(ArmorItem.Type.HELMET,1,ArmorItem.Type.CHESTPLATE,1,ArmorItem.Type.LEGGINGS,1,ArmorItem.Type.BOOTS,1),0,net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER,()->net.minecraft.world.item.crafting.Ingredient.EMPTY,java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","radiation_suit"))),0,0));
*///?} else {
//?}
}
