package com.gregtech.gregtech.content.tool;
//? if >=1.21.1 {
/*import com.gregtech.gregtech.api.fluid.PortableFluidContainerSpec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
*///?} else {

import com.gregtech.gregtech.api.fluid.PortableFluidContainerSpec;
import net.minecraft.world.item.ItemStack;

/** Original measuring-pot / barometer-cylinder hit bands and persistent gt.mode capacity. */
//?}
public final class PortableContainerLimits {
    private static final String KEY = "gt.mode";

    private PortableContainerLimits() {
    }

    public static boolean adjustable(PortableFluidContainerSpec spec) {
        return PortableCapacityRules.adjustable(spec);
    }

//? if >=1.21.1 {
    /*public static int capacity(ItemStack stack, PortableFluidContainerSpec spec) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return !adjustable(spec) || !data.contains(KEY) ? spec.capacity() : PortableCapacityRules.capacity(spec, data.getLong(KEY));
    }

    public static int adjust(ItemStack stack, PortableFluidContainerSpec spec, double hitY, boolean precise) {
        int limit = PortableCapacityRules.adjust(spec, capacity(stack, spec), hitY, precise);
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (limit == spec.capacity()) data.remove(KEY);
        else data.putInt(KEY, limit);
        if (data.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return limit;
    }
*///?} else {

    public static int capacity(ItemStack stack, PortableFluidContainerSpec spec) {
        if (!adjustable(spec) || !stack.hasTag() || !stack.getTag().contains(KEY)) return spec.capacity();
        return PortableCapacityRules.capacity(spec, stack.getTag().getLong(KEY));
    }

    public static int adjust(ItemStack stack, PortableFluidContainerSpec spec, double hitY, boolean precise) {
        if (!adjustable(spec)) return spec.capacity();
        int limit = PortableCapacityRules.adjust(spec, capacity(stack, spec), hitY, precise);
        if (limit == spec.capacity()) {
            if (stack.hasTag()) stack.getTag().remove(KEY);
        }
        else stack.getOrCreateTag().putInt(KEY, limit);
        return limit;
    }
//?}
}
