package gregtech.content.tool;

import gregtech.engine.fluid.PortableFluidContainerSpec;
import gregtech.engine.stack.StackData;
import net.minecraft.world.item.ItemStack;

/** Original measuring-pot / barometer-cylinder hit bands and persistent gt.mode capacity. */
public final class PortableContainerLimits {
    private static final String KEY = "gt.mode";

    private PortableContainerLimits() {}

    public static boolean adjustable(PortableFluidContainerSpec spec) {
        return PortableCapacityRules.adjustable(spec);
    }

    public static int capacity(ItemStack stack, PortableFluidContainerSpec spec) {
        var data = StackData.read(stack);
        if (!adjustable(spec) || data == null || !data.contains(KEY)) return spec.capacity();
        return PortableCapacityRules.capacity(spec, data.getLong(KEY));
    }

    public static int adjust(ItemStack stack, PortableFluidContainerSpec spec, double hitY, boolean precise) {
        if (!adjustable(spec)) return spec.capacity();
        int limit = PortableCapacityRules.adjust(spec, capacity(stack, spec), hitY, precise);
        if (limit == spec.capacity()) StackData.remove(stack, KEY);
        else StackData.putInt(stack, KEY, limit);
        return limit;
    }
}
