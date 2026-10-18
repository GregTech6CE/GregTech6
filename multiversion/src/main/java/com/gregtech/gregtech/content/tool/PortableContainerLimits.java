package com.gregtech.gregtech.content.tool;
//? if >=1.21.1 {
/*import com.gregtech.gregtech.api.fluid.PortableFluidContainerSpec;import net.minecraft.world.item.ItemStack;import net.minecraft.core.component.DataComponents;import net.minecraft.world.item.component.CustomData;
*///?} else {

import com.gregtech.gregtech.api.fluid.PortableFluidContainerSpec;
import net.minecraft.world.item.ItemStack;

/** Original measuring-pot / barometer-cylinder hit bands and persistent gt.mode capacity. */
//?}
public final class PortableContainerLimits {
//? if >=1.21.1 {
 /*private static final String KEY="gt.mode";private PortableContainerLimits(){}public static boolean adjustable(PortableFluidContainerSpec s){return PortableCapacityRules.adjustable(s);}
 public static int capacity(ItemStack stack,PortableFluidContainerSpec s){var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();return !adjustable(s)||!data.contains(KEY)?s.capacity():PortableCapacityRules.capacity(s,data.getLong(KEY));}
 public static int adjust(ItemStack stack,PortableFluidContainerSpec s,double y,boolean precise){int value=PortableCapacityRules.adjust(s,capacity(stack,s),y,precise);var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(value==s.capacity())data.remove(KEY);else data.putInt(KEY,value);if(data.isEmpty())stack.remove(DataComponents.CUSTOM_DATA);else stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return value;}
*///?} else {
    private static final String KEY="gt.mode";
    private PortableContainerLimits() {}
    public static boolean adjustable(PortableFluidContainerSpec spec){
        return PortableCapacityRules.adjustable(spec);
    }
    public static int capacity(ItemStack stack,PortableFluidContainerSpec spec){
        if(!adjustable(spec)||!stack.hasTag()||!stack.getTag().contains(KEY))return spec.capacity();
        return PortableCapacityRules.capacity(spec,stack.getTag().getLong(KEY));
    }
    public static int adjust(ItemStack stack,PortableFluidContainerSpec spec,double y,boolean precise){
        if(!adjustable(spec))return spec.capacity();
        int limit=PortableCapacityRules.adjust(spec,capacity(stack,spec),y,precise);
        if(limit==spec.capacity()){if(stack.hasTag())stack.getTag().remove(KEY);}
        else stack.getOrCreateTag().putInt(KEY,limit);
        return limit;
    }
//?}
}
