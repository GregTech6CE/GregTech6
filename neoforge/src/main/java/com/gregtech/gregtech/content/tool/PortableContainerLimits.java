package com.gregtech.gregtech.content.tool;
import com.gregtech.gregtech.api.fluid.PortableFluidContainerSpec;import net.minecraft.world.item.ItemStack;import net.minecraft.core.component.DataComponents;import net.minecraft.world.item.component.CustomData;
public final class PortableContainerLimits {
 private static final String KEY="gt.mode";private PortableContainerLimits(){}public static boolean adjustable(PortableFluidContainerSpec s){return PortableCapacityRules.adjustable(s);}
 public static int capacity(ItemStack stack,PortableFluidContainerSpec s){var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();return !adjustable(s)||!data.contains(KEY)?s.capacity():PortableCapacityRules.capacity(s,data.getLong(KEY));}
 public static int adjust(ItemStack stack,PortableFluidContainerSpec s,double y,boolean precise){int value=PortableCapacityRules.adjust(s,capacity(stack,s),y,precise);var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(value==s.capacity())data.remove(KEY);else data.putInt(KEY,value);if(data.isEmpty())stack.remove(DataComponents.CUSTOM_DATA);else stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return value;}
}
