package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.data.RegisteredFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
public final class GTFluidItems {
 private GTFluidItems(){}
 public static Item get(String field){var entry=RegisteredFluids.get(field);if(entry==null)return null;var item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech","fluid_item_"+RegisteredFluids.sanitizePath(entry.registryName())));return item==Items.AIR?null:item;}
}
