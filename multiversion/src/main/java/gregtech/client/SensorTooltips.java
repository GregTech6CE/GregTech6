/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original MultiTileEntitySensor.addToolTips; descriptions supplied by the shared catalog. */
package gregtech.client;

import gregtech.api.addon.platform.AddonSensor;
import gregtech.api.sensor.SensorCatalog;
import gregtech.block.sensor.SensorBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class SensorTooltips {
    private SensorTooltips() {}
    public static void add(List<Component> lines, String kind) {
        addDescription(lines, SensorCatalog.byKind(kind).descriptionKey());
    }

    public static void add(List<Component> lines, SensorBlock block) {
        AddonSensor addon = block.addon();
        addDescription(lines, addon != null ? addon.titleKey() : SensorCatalog.byKind(block.kindName()).descriptionKey());
    }

    private static void addDescription(List<Component> lines, String key) {
        if (CommonBlockTooltips.containsKey(lines, key)) return;
        lines.add(Component.translatable(key).withStyle(ChatFormatting.AQUA));
        if (key.equals("gt.tooltip.sensor.kilogibblometer"))
            lines.add(Component.translatable("tooltip.gt6ce.sensor.kilogibblometer.boiler").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gt.lang.nogui.rightclick.interact").withStyle(ChatFormatting.GOLD));
        for (var tool : List.of("gt.tooltip.sensor.screwdrive.buttons", "gt.tooltip.sensor.screwdrive.display",
                "gt.tooltip.sensor.screwdrive.modes", "gt.lang.use.monkey.wrench.to.set.input.side"))
            lines.add(Component.translatable(tool).withStyle(ChatFormatting.DARK_GRAY));
        StorageBlockTooltips.facing(lines);
    }
}
