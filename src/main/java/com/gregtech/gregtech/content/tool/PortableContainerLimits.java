package com.gregtech.gregtech.content.tool;

import com.gregtech.gregtech.api.fluid.PortableFluidContainerSpec;
import net.minecraft.world.item.ItemStack;

/** Original measuring-pot / barometer-cylinder hit bands and persistent gt.mode capacity. */
public final class PortableContainerLimits {
    private static final String KEY="gt.mode";
    private PortableContainerLimits() {}
    public static boolean adjustable(PortableFluidContainerSpec spec){
        return spec.shapeId().equals("measuring_pot")||spec.shapeId().equals("barometer_gas_cylinder");
    }
    public static int capacity(ItemStack stack,PortableFluidContainerSpec spec){
        if(!adjustable(spec)||!stack.hasTag()||!stack.getTag().contains(KEY))return spec.capacity();
        return (int)Math.max(1,Math.min(spec.capacity(),stack.getTag().getLong(KEY)));
    }
    public static int adjust(ItemStack stack,PortableFluidContainerSpec spec,double y,boolean precise){
        if(!adjustable(spec))return spec.capacity();
        int step;
        if(spec.shapeId().equals("measuring_pot")) step=y>6/16.0?50:y>4/16.0?10:y>2/16.0?-10:-50;
        else step=y>14/16.0?500:y>12/16.0?100:y>10/16.0?50:y>8/16.0?10:y>6/16.0?-10:y>4/16.0?-50:y>2/16.0?-100:-500;
        if(precise)step/=10;
        int limit=Math.max(1,Math.min(spec.capacity(),capacity(stack,spec)+step));
        if(limit==spec.capacity()){if(stack.hasTag())stack.getTag().remove(KEY);}
        else stack.getOrCreateTag().putInt(KEY,limit);
        return limit;
    }
}
