/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from manual station and MultiTileEntityBoilerTank.addToolTips. */
package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.machine.BoilerSpec;
import com.gregtech.gregtech.content.machine.OriginalFunctionalTooltipData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/** Native formatting of the shared original block tooltip data. */
public final class FunctionalBlockTooltips {
    private FunctionalBlockTooltips() {}

    public static void appendManual(String id, float resistance, List<Component> lines) {
        var data = OriginalFunctionalTooltipData.manual(id);
        lines.add(Component.translatable("gt.lang.recipes").withStyle(ChatFormatting.AQUA)
                .append(": ").append(Component.translatable(data.recipeKey()).withStyle(ChatFormatting.WHITE)));
        if (data.preparationKey() != null) add(lines, data.preparationKey(), ChatFormatting.AQUA);
        add(lines, data.usageKey(), ChatFormatting.AQUA);
        lines.add(Component.translatable("gt.lang.nogui.rightclick.interact").withStyle(ChatFormatting.GOLD)
                .append(" (").append(Component.translatable(data.faceKey())).append(")"));
        if (data.magnifier()) add(lines, "gt.lang.use.magnifyingglass.to.detail", ChatFormatting.DARK_GRAY);
        if (data.facingWrench()) appendFacingWrench(lines);
        TooltipHelper.appendBlastResistance(resistance, lines);
    }

    public static void appendBoiler(BoilerSpec spec, int savedEfficiency, float resistance, List<Component> lines) {
        var data = OriginalFunctionalTooltipData.boiler(spec, savedEfficiency);
        lines.add(Component.translatable("gt.lang.energy.convert.from").withStyle(ChatFormatting.AQUA)
                .append(" 1 L ").append(Component.translatable("block.minecraft.water")).append(" ")
                .append(Component.translatable("gt.lang.energy.convert.to")).append(" 160 L ")
                .append(Component.translatable("gt.td.long.energy.steam")).append(" ")
                .append(Component.translatable("gt.lang.energy.convert.using")).append(" 80 ")
                .append(Component.translatable("gt.td.short.energy.heat")));
        lines.add(label("gt.lang.efficiency", ChatFormatting.YELLOW)
                .append(Component.literal(OriginalFunctionalTooltipData.efficiencyPercent(data.efficiency()) + "%")
                        .withStyle(ChatFormatting.WHITE)));
        lines.add(energy("gt.lang.energy.input", data.heatInput(), "gt.td.short.energy.heat", "gt.lang.face.any", ChatFormatting.GREEN));
        lines.add(energy("gt.lang.energy.capacity", data.heatCapacity(), "gt.td.short.energy.heat", null, ChatFormatting.GREEN));
        lines.add(energy("gt.lang.energy.output", data.steamOutput(), "gt.td.long.energy.steam", "gt.lang.face.top", ChatFormatting.RED));
        lines.add(energy("gt.lang.energy.capacity", data.steamCapacity(), "gt.td.long.energy.steam", null, ChatFormatting.RED));
        add(lines, "gt.lang.requirement.water.pure", ChatFormatting.GOLD);
        add(lines, "gt.lang.nogui.funnel.tank", ChatFormatting.GOLD);
        add(lines, "gt.lang.hazard.explosion.steam", ChatFormatting.DARK_RED);
        add(lines, "gt.lang.hazard.meltdown", ChatFormatting.DARK_RED);
        add(lines, "gt.lang.use.chisel.to.decalcify", ChatFormatting.DARK_GRAY);
        add(lines, "gt.lang.use.magnifyingglass.to.detail", ChatFormatting.DARK_GRAY);
        appendFacingWrench(lines);
        TooltipHelper.appendBlastResistance(resistance, lines);
    }

    private static MutableComponent label(String key, ChatFormatting color) {
        return Component.translatable(key).withStyle(color).append(": ");
    }

    private static Component energy(String key, long amount, String unitKey, String faceKey, ChatFormatting color) {
        var line = label(key, color).append(Component.literal(Long.toString(amount) + " ").withStyle(ChatFormatting.WHITE))
                .append(Component.translatable(unitKey).withStyle(unitKey.endsWith("heat") ? ChatFormatting.RED : ChatFormatting.GRAY));
        if (faceKey != null) line.append(Component.literal("/t (").withStyle(ChatFormatting.WHITE))
                .append(Component.translatable(faceKey).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(")").withStyle(ChatFormatting.WHITE));
        return line;
    }

    private static void appendFacingWrench(List<Component> lines) {
        lines.add(Component.translatable("gt.lang.use.x.to.toggle.facing.pre").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.translatable("gt.lang.tool.name.wrench"))
                .append(Component.translatable("gt.lang.use.x.to.toggle.facing.post")));
    }

    private static void add(List<Component> lines, String key, ChatFormatting color) {
        lines.add(Component.translatable(key).withStyle(color));
    }
}
