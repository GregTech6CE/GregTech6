package com.gregtech.gregtech.content.plant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
/** Platform item lookup over the one shared original8 berry/color table. */
public final class GTBerryBushes extends BerryBushCatalog {
 private GTBerryBushes(){}
 public static BerryType of(ItemStack stack){if(stack==null||stack.isEmpty())return null;ResourceLocation id=BuiltInRegistries.ITEM.getKey(stack.getItem());return id!=null&&id.getNamespace().equals("gregtech")?byId(id.getPath()):null;}
 public static BerryType ofOrDefault(ItemStack stack){BerryType type=of(stack);return type==null?DEFAULT:type;}
}
