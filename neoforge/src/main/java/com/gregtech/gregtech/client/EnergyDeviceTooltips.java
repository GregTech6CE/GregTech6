/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from LH.addEnergyToolTips, TE_Behavior_Energy_Stats.addToolTips
 * and the battery box / solar / bidirectional converter tooltip chains. */
package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData;
import com.gregtech.gregtech.content.machine.OriginalFunctionalTooltipData;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class EnergyDeviceTooltips {
    private EnergyDeviceTooltips() {}

    public static void append(EnergyNodeSpec spec, boolean reversed, float resistance, List<Component> lines) {
        var source = OriginalEnergyDeviceTooltipData.profile(spec, reversed);
        if (source.input() != null) {
            lines.add(energyLine(source.input(), spec.inType(), source.inputFaceKey(), false, source.alwaysShowRange()));
            if (spec.inType() == GregTechTags.Energy.RF)
                lines.add(Component.translatable("gt.lang.accepts.redstoneflux.lossless").withStyle(ChatFormatting.GOLD));
        }
        lines.add(energyLine(source.output(), spec.outType(), source.outputFaceKey(), true, source.alwaysShowRange()));
        if (spec.outType() == GregTechTags.Energy.RF)
            lines.add(Component.translatable("gt.lang.emits.redstoneflux.lossless").withStyle(ChatFormatting.GOLD));
        boolean thermal = com.gregtech.gregtech.content.energy.OriginalThermalConverter.handles(spec);
        boolean cooler = thermal && com.gregtech.gregtech.content.energy.OriginalThermalConverter.cooler(spec);
        if (cooler) lines.add(energyLine(source.output(), GregTechTags.Energy.HU, "gt.lang.face.back", true, true));
        for (int index = 0; index < (cooler ? 2 : 1); index++) if (source.efficiency() >= 0)
            lines.add(Component.translatable("gt.lang.efficiency").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(": " + OriginalFunctionalTooltipData.efficiencyPercent(source.efficiency()) + "%")
                            .withStyle(ChatFormatting.WHITE)));
        if (source.batteryModes())
            for (String key : List.of("gt.tooltip.energybattery.1", "gt.tooltip.energybattery.2", "gt.tooltip.energybattery.3"))
                lines.add(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("gt.lang.use.x.to.toggle.facing.pre").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.translatable("gt.lang.tool.name.wrench"))
                .append(Component.translatable("gt.lang.use.x.to.toggle.facing.post")));
        if (source.monkeyWrench())
            lines.add(Component.translatable("gt.lang.use.monkey.wrench.to.toggle.direction").withStyle(ChatFormatting.DARK_GRAY));
        TooltipHelper.appendBlastResistance(resistance, lines);
        if (com.gregtech.gregtech.content.energy.MagnetMachineDefinitions.handles(spec))
            lines.add(Component.translatable("gt.lang.reminder.extenders").withStyle(ChatFormatting.GRAY));
        if (thermal) {
            if (cooler) lines.add(Component.translatable("gt.lang.reminder.extenders").withStyle(ChatFormatting.GRAY));
            else lines.add(Component.translatable("gt.lang.hazard.contact").withStyle(ChatFormatting.DARK_RED)
                    .append(Component.literal(" (")).append(Component.translatable("gt.lang.face.front")).append(Component.literal(")")));
        }
    }

    private static Component energyLine(OriginalEnergyDeviceTooltipData.Stats stats, GregTechTags.Tag type,
                                        String faceKey, boolean emitting, boolean alwaysShowRange) {
        var line = Component.translatable(emitting ? "gt.lang.energy.output" : "gt.lang.energy.input")
                .withStyle(emitting ? ChatFormatting.RED : ChatFormatting.GREEN)
                .append(Component.literal(": " + stats.recommended() + " ").withStyle(ChatFormatting.WHITE))
                .append(Component.translatable(type == GregTechTags.Energy.RU ? "gt.td.short.energy.kinetic_rotation"
                        : type == GregTechTags.Energy.RF ? "gt.td.short.energy.redstone_flux"
                        : type == GregTechTags.Energy.HU ? "gt.td.short.energy.heat"
                        : type == GregTechTags.Energy.MU ? "gt.td.short.energy.magnetic"
                        : type == GregTechTags.Energy.CU ? "gt.td.short.energy.cryo" : "gt.td.short.energy.electricity")
                        .withStyle(type == GregTechTags.Energy.RU ? ChatFormatting.GREEN
                                : type == GregTechTags.Energy.RF ? ChatFormatting.DARK_RED
                                : type == GregTechTags.Energy.HU ? ChatFormatting.RED
                                : type == GregTechTags.Energy.MU ? ChatFormatting.DARK_GRAY
                                : type == GregTechTags.Energy.CU ? ChatFormatting.AQUA : ChatFormatting.BLUE))
                .append(Component.literal("/t").withStyle(ChatFormatting.WHITE));
        // LH omits both the range and face on fixed packets; the converter stats always include them.
        if (alwaysShowRange || stats.minimum() != stats.recommended() || stats.maximum() != stats.recommended()) {
            line.append(Component.literal(stats.minimum() <= 1 ? " (up to " : " (" + stats.minimum() + " to ")
                    .withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(Long.toString(stats.maximum())).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(", ").withStyle(ChatFormatting.WHITE))
                    .append(Component.translatable(faceKey).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(")").withStyle(ChatFormatting.WHITE));
        }
        return line;
    }
}
