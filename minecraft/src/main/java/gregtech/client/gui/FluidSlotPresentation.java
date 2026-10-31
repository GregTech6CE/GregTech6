package gregtech.client.gui;

import gregtech.content.data.RegisteredFluids;
import gregtech.content.registry.GTFluidType;
import gregtech.content.registry.GTFluids;
import gregtech.engine.fluid.FluidSlotAmountOverlay;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Shared tank tooltips and compact, left-aligned quantity labels. */
final class FluidSlotPresentation {
    private FluidSlotPresentation() {}

    static List<Component> tooltip(FluidStack fluid, long amount, long capacity) {
        if (fluid.isEmpty()) return List.of(capacity == Long.MAX_VALUE
                ? Component.translatable("gui.gt6ce.fluid.capacity_unlimited")
                : gregtech.engine.fluid.FluidUnits.text("gui.gt6ce.fluid.capacity", capacity));
        var result = new ArrayList<Component>();
        var entry = GTFluids.entryForFluid(fluid.getFluid());
        result.add(entry == null ? fluid.getDisplayName()
                : GTFluidType.describe(entry, RegisteredFluids.sanitizePath(entry.registryName())));
        result.addAll(GTFluidType.describeTooltip(fluid, amount, capacity, false));
        return result;
    }

    static void amount(GuiGraphics graphics, Font font, long liters, int x, int y, int width, int height) {
        String text = gregtech.engine.fluid.FluidUnits.compact(liters);
        if (text.isEmpty()) return;
        float scale = Math.min(1.0F, (width - 1.0F) / Math.max(1, font.width(text)));
        graphics.pose().pushPose();
        graphics.pose().translate(FluidSlotAmountOverlay.x(x, font.width(text)), y + height - font.lineHeight * scale, 200);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, 0, 0, 0xFFFFFF, true);
        graphics.pose().popPose();
    }
}
