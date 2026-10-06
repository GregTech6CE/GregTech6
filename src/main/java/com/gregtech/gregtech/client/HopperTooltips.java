package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.machine.HopperSpec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/** Hopper / queuehopper functional tooltips — material composition is shown by {@link MaterialTooltips} via F3+H. */
public final class HopperTooltips {
    private HopperTooltips() {}

    private static final int SLOT_SIZE = 64;

    public static void appendHopper(HopperSpec spec, List<Component> tooltip, TooltipFlag flag) {
        // Slot Count
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".hopper.slot_count")
                        .withStyle(ChatFormatting.AQUA))
                .append(Component.literal(String.valueOf(spec.slotCount()))
                        .withStyle(ChatFormatting.WHITE)));

        // Tool hints
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".hopper.hint.screwdriver")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".hopper.hint.monkey_wrench")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".hopper.hint.magnifying_glass")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".hopper.hint.soft_hammer")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".hopper.hint.wrench")
                .withStyle(ChatFormatting.GRAY));

        // Blast Resistance
        appendBlastResistance(spec.blastResistance(), tooltip);

        // Harvest tool
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".machine.harvest.tool_label")
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" "))
                .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".machine.harvest_wrench_short")
                        .withStyle(ChatFormatting.WHITE)));
    }

    public static void appendQueueHopper(HopperSpec spec, List<Component> tooltip, TooltipFlag flag) {
        // Slot Count
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".hopper.slot_count")
                        .withStyle(ChatFormatting.AQUA))
                .append(Component.literal(String.valueOf(spec.slotCount()))
                        .withStyle(ChatFormatting.WHITE)));

        // Slot Size
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".hopper.slot_size")
                        .withStyle(ChatFormatting.AQUA))
                .append(Component.literal(String.valueOf(SLOT_SIZE))
                        .withStyle(ChatFormatting.WHITE)));

        // Tool hints
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".hopper.hint.screwdriver")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".hopper.hint.magnifying_glass")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".hopper.hint.soft_hammer")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".hopper.hint.wrench")
                .withStyle(ChatFormatting.GRAY));

        // Blast Resistance
        appendBlastResistance(spec.blastResistance(), tooltip);

        // Harvest tool
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".machine.harvest.tool_label")
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" "))
                .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".machine.harvest_wrench_short")
                        .withStyle(ChatFormatting.WHITE)));
    }

    private static void appendBlastResistance(float resistance, List<Component> tooltip) {
        TooltipHelper.appendBlastResistance(resistance, tooltip);
    }
}
