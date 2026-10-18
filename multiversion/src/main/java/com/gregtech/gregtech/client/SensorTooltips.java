/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original MultiTileEntitySensor.addToolTips; descriptions supplied by the shared catalog. */
package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.sensor.SensorCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class SensorTooltips {
    private SensorTooltips() {}
    public static void add(List<Component> lines, String kind) {
        var key = SensorCatalog.byKind(kind).descriptionKey();
        if (CommonBlockTooltips.containsKey(lines, key)) return;
        lines.add(Component.translatable(key).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("gt.lang.nogui.rightclick.interact").withStyle(ChatFormatting.GOLD));
        for (var tool : List.of("gt.tooltip.sensor.screwdrive.buttons", "gt.tooltip.sensor.screwdrive.display",
                "gt.tooltip.sensor.screwdrive.modes", "gt.lang.use.monkey.wrench.to.set.input.side"))
            lines.add(Component.translatable(tool).withStyle(ChatFormatting.DARK_GRAY));
        StorageBlockTooltips.facing(lines);
    }
}
