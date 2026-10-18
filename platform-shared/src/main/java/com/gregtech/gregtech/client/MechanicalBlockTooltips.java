/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from MultiTileEntityAxle/GearBox.addToolTips and connector base. */
package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.energy.AxleSpec;
import com.gregtech.gregtech.api.energy.GearboxSpec;
import com.gregtech.gregtech.content.energy.GearboxRotationRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class MechanicalBlockTooltips {
    private MechanicalBlockTooltips() {}
    public static void appendAxle(AxleSpec spec, float resistance, List<Component> lines) {
        appendSpeed(spec.maxSpeed(), lines);
        lines.add(Component.translatable("gt.lang.axle.stats.power").withStyle(ChatFormatting.AQUA)
                .append(Long.toString(spec.maxPower())));
        lines.add(Component.translatable("gt.lang.use.x.to.toggle.connection.pre").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.translatable("gt.lang.tool.name.wrench"))
                .append(Component.translatable("gt.lang.use.x.to.toggle.connection.post")));
        TooltipHelper.appendBlastResistance(resistance, lines);
    }
    public static void appendGearbox(GearboxSpec spec, int gears, int axis, float resistance, List<Component> lines) {
        if (!GearboxRotationRules.gearsWork(gears, axis))
            lines.add(Component.translatable("gt.tooltip.gearbox.custom.1").withStyle(ChatFormatting.RED));
        appendSpeed(spec.maxSpeed(), lines);
        for (String key : List.of("gt.tooltip.gearbox.custom.2", "gt.tooltip.gearbox.custom.3",
                "gt.lang.use.soft.hammer.to.toggle", "gt.lang.use.magnifyingglass.to.detail"))
            lines.add(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY));
        TooltipHelper.appendBlastResistance(resistance, lines);
    }
    private static void appendSpeed(long amount, List<Component> lines) {
        lines.add(Component.translatable("gt.lang.axle.stats.speed").withStyle(ChatFormatting.AQUA)
                .append(amount + " ").append(Component.translatable("gt.td.short.energy.kinetic_rotation")));
    }
}
