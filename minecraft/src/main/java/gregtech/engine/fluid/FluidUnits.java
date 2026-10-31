package gregtech.engine.fluid;

import gregtech.GregTechConfig;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Presentation only: a legacy GT liter and a millibucket share the same stored amount. */
public final class FluidUnits {
    private FluidUnits() {}

    public static String unit() {
        return GregTechConfig.fluidInMillibuckets() ? "mB" : "L";
    }

    public static String compact(long amount) {
        return amount == 0 ? "0" + unit() : FluidSlotAmountOverlay.text(amount, GregTechConfig.fluidInMillibuckets());
    }

    public static String exact(long amount) {
        return amount + " " + unit();
    }

    /** Unit-aware variants keep translated sentences and their argument order intact. */
    public static MutableComponent text(String key, Object... arguments) {
        String variant = key + (GregTechConfig.fluidInMillibuckets() ? ".mb" : ".liters");
        return Component.translatable(Language.getInstance().has(variant) ? variant : key, arguments);
    }
}
