package com.gregtech.gregtech.client;


import com.gregtech.gregtech.api.energy.WireSpec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Wire-specific tooltips — material composition is shown by {@link MaterialTooltips} via F3+H. */
public final class WireTooltips {
    private WireTooltips() {}

    public static void append(WireSpec spec, List<Component> tooltip) {

        // Voltage line
        ChatFormatting voltColor = spec.voltage() >= 8192 ? ChatFormatting.GOLD :
                spec.voltage() >= 2048 ? ChatFormatting.DARK_GREEN :
                spec.voltage() >= 512 ? ChatFormatting.DARK_AQUA :
                ChatFormatting.AQUA;
        tooltip.add(Component.translatable("gt.lang.wire.stats.voltage")
                .append(Component.literal(String.valueOf(spec.voltage())).withStyle(ChatFormatting.WHITE))
                .append(" EU (").append(Component.literal(voltageTierName(spec.voltage())).withStyle(voltColor))
                .append(")").withStyle(ChatFormatting.AQUA));

        tooltip.add(Component.translatable("gt.lang.wire.stats.amperage")
                .append(Component.literal(String.valueOf(spec.amperage())).withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.AQUA));

        tooltip.add(Component.translatable("gt.lang.wire.stats.loss")
                .append(Component.literal(String.valueOf(spec.lossPerBlock())).withStyle(ChatFormatting.WHITE))
                .append(" EU/m").withStyle(ChatFormatting.AQUA));

        // Insulated / damage warning
        if (spec.insulated()) {
            tooltip.add(Component.translatable("tooltip." + "gregtech" + ".wire.insulated")
                    .withStyle(ChatFormatting.GREEN));
        } else if (spec.contactDamage()) {
            tooltip.add(Component.translatable("tooltip." + "gregtech" + ".wire.damage_warning")
                    .withStyle(ChatFormatting.RED));
        }

        // Use hint
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".wire.use_cutter")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gt.lang.tool.to.harvest").append(":")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" ")
                        .append(Component.translatable("gt.lang.tool.name.cutter")
                                .withStyle(ChatFormatting.WHITE))));

        // Shapeless recipes
        tooltip.add(Component.translatable("gt.lang.has.shapeless")
                .withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal("[2, 3, 4, 5, 6, 7, 8, 9]")
                        .withStyle(ChatFormatting.WHITE)));
    }

    private static String voltageTierName(long voltage) {
        return com.gregtech.gregtech.api.energy.GTVoltageTiers.NAMES[
                com.gregtech.gregtech.api.energy.GTVoltageTiers.tierMin(voltage)];
    }
}
