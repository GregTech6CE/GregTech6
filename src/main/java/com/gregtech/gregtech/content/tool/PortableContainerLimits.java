package com.gregtech.gregtech.content.tool;

import com.gregtech.gregtech.api.fluid.PortableFluidContainerSpec;
import net.minecraft.world.item.ItemStack;

/** Original measuring-pot / barometer-cylinder hit bands and persistent gt.mode capacity. */
public final class PortableContainerLimits {
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
}
