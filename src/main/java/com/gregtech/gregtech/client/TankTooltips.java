package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.machine.ItemPipeSpec;
import com.gregtech.gregtech.api.machine.PipeSpec;
import com.gregtech.gregtech.api.machine.TankSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialTextureSet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/** GT6-style tooltips for fluid containers and pipes. */
public final class TankTooltips {
    private TankTooltips() {}

    // === Tank Tooltips ===

    public static void appendTank(TankSpec spec, List<Component> tooltip) {
        GTMaterial mat = spec.material();

        // Capacity
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tank.capacity")
                        .withStyle(ChatFormatting.AQUA))
                .append(Component.literal(formatLargeNumber(spec.capacity()) + " L")
                        .withStyle(ChatFormatting.WHITE)));

        // No GUI hint
        if (!spec.simpleOnly()) {
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tank.no_gui")
                    .withStyle(ChatFormatting.GOLD));
        }

        // Proof flags
        if (spec.gasProof())
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tank.gas_proof").withStyle(ChatFormatting.GOLD));
        if (spec.acidProof())
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tank.acid_proof").withStyle(ChatFormatting.GOLD));
        if (spec.plasmaProof())
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tank.plasma_proof").withStyle(ChatFormatting.GOLD));
        if (spec.magicProof())
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tank.magic_proof").withStyle(ChatFormatting.GOLD));
        if (spec.simpleOnly())
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tank.simple_only").withStyle(ChatFormatting.GOLD));

        // Meltdown warning
        long maxTempK = spec.maxTemperature();
        if (maxTempK < Long.MAX_VALUE) {
            tooltip.add(Component.empty()
                    .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tank.meltdown")
                            .withStyle(ChatFormatting.RED))
                    .append(Component.literal(" (" + maxTempK + " K)")
                            .withStyle(ChatFormatting.WHITE)));
        }

        // Tool usage hints
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tank.hint.monkey_wrench")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tank.hint.soft_hammer")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tank.hint.magnifying_glass")
                .withStyle(ChatFormatting.GRAY));

        // Blast resistance
        appendBlastResistance(spec.blastResistance(), tooltip);

        // Harvest info
        appendHarvestInfo(mat, tooltip);
    }

    // === Pipe Tooltips ===

    public static void appendPipe(PipeSpec spec, List<Component> tooltip) {
        GTMaterial mat = spec.material();

        // Bandwidth (throughput per tick)
        long bandwidth = spec.capacity() / 20; // rough throughput
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".pipe.bandwidth")
                        .withStyle(ChatFormatting.AQUA))
                .append(Component.literal(formatLargeNumber(bandwidth) + " L")
                        .withStyle(ChatFormatting.WHITE)));

        // Capacity
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".pipe.capacity")
                        .withStyle(ChatFormatting.AQUA))
                .append(Component.literal(formatLargeNumber(spec.capacity()) + " L")
                        .withStyle(ChatFormatting.WHITE)));

        // Proof flags
        if (spec.gasProof())
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tank.gas_proof").withStyle(ChatFormatting.GOLD));
        if (spec.acidProof())
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tank.acid_proof").withStyle(ChatFormatting.GOLD));
        if (spec.plasmaProof())
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tank.plasma_proof").withStyle(ChatFormatting.GOLD));
        if (spec.magicProof())
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tank.magic_proof").withStyle(ChatFormatting.GOLD));

        // Meltdown warning
        long maxTempK = spec.maxTemperature();
        if (maxTempK < Long.MAX_VALUE) {
            tooltip.add(Component.empty()
                    .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tank.meltdown")
                            .withStyle(ChatFormatting.RED))
                    .append(Component.literal(" (" + maxTempK + " K)")
                            .withStyle(ChatFormatting.WHITE)));
        }

        // Tool usage hints (pipe-specific — no auto output / state toggle)
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".pipe.hint.wrench")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".pipe.hint.monkey_wrench")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".pipe.hint.magnifying_glass")
                .withStyle(ChatFormatting.GRAY));

        // Blast resistance
        appendBlastResistance(spec.blastResistance(), tooltip);

        // Harvest tool info
        appendHarvestInfo(mat, tooltip);
    }

    // === Item Pipe Tooltips ===

    public static void appendItemPipe(ItemPipeSpec spec, List<Component> tooltip) {
        GTMaterial mat = spec.material();

        // Stepsize
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".pipe.stepsize")
                        .withStyle(ChatFormatting.AQUA))
                .append(Component.literal(formatLargeNumber(spec.stepSize()))
                        .withStyle(ChatFormatting.WHITE)));

        // Bandwidth (items per second = invSize)
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".pipe.bandwidth")
                        .withStyle(ChatFormatting.AQUA))
                .append(Component.literal(spec.invSize() + "/s")
                        .withStyle(ChatFormatting.WHITE)));

        // Tool usage hints
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".pipe.hint.wrench")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".pipe.hint.monkey_wrench_input")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".pipe.hint.monkey_wrench_output")
                .withStyle(ChatFormatting.GRAY));

        // Blast resistance
        appendBlastResistance(spec.blastResistance(), tooltip);

        // Tool harvest info
        appendHarvestInfo(mat, tooltip);
    }

    // === Shared helpers ===

    private static void appendBlastResistance(float resistance, List<Component> tooltip) {
        ChatFormatting levelColor;
        String levelKey;
        if (resistance >= 12.0F) {
            levelColor = ChatFormatting.GREEN;
            levelKey = "tooltip." + GregTech.NAMESPACE + ".machine.blast.creeper";
        } else if (resistance >= 7.0F) {
            levelColor = ChatFormatting.RED;
            levelKey = "tooltip." + GregTech.NAMESPACE + ".machine.blast.ghast";
        } else {
            levelColor = ChatFormatting.RED;
            levelKey = "tooltip." + GregTech.NAMESPACE + ".machine.blast.terrible";
        }
        tooltip.add(Component.empty()
                .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".machine.blast_resistance")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal(String.format(Locale.ROOT, "%.1f", resistance))
                        .withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" "))
                .append(Component.translatable(levelKey).withStyle(levelColor)));
    }

    private static void appendHarvestInfo(GTMaterial mat, List<Component> tooltip) {
        MaterialTextureSet texSet = mat.getTextureSet();
        String toolKey = resolveHarvestToolKey(texSet);
        if (mat.hasToolStats()) {
            tooltip.add(Component.empty()
                    .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".machine.harvest.tool_label")
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.translatable(toolKey)
                            .withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" (" + mat.getToolQuality() + ", " + mat.getLocalName() + ")")
                            .withStyle(ChatFormatting.GRAY)));
        } else {
            tooltip.add(Component.empty()
                    .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".machine.hand_harvest")
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(", "))
                    .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".machine.harvest.but_label")
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.translatable(toolKey)
                            .withStyle(ChatFormatting.WHITE))
                    .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".machine.harvest.faster")
                            .withStyle(ChatFormatting.GRAY)));
        }
    }

    private static String resolveHarvestToolKey(MaterialTextureSet texSet) {
        if (texSet == MaterialTextureSet.RUBBER) {
            return "tooltip." + GregTech.NAMESPACE + ".machine.harvest_shears";
        }
        if (texSet == MaterialTextureSet.WOOD) {
            return "tooltip." + GregTech.NAMESPACE + ".machine.harvest_axe";
        }
        return "tooltip." + GregTech.NAMESPACE + ".machine.harvest_wrench";
    }

    // === Magnifying Glass Info (sent to player chat) ===

    public static void sendTankInfo(com.gregtech.gregtech.blockentity.machine.TankBlockEntity tank,
                                    net.minecraft.world.entity.player.Player player) {
        TankSpec spec = tank.spec();
        var fluid = tank.getFluidTank().getFluid();
        boolean autoOutput = tank.isAutoOutput();
        boolean someState = tank.getSoftHammerState();

        // Auto output status
        player.sendSystemMessage(Component.empty()
                .append(autoOutput
                        ? Component.translatable("tooltip." + GregTech.NAMESPACE + ".tank.info.auto_output")
                        : Component.translatable("tooltip." + GregTech.NAMESPACE + ".tank.info.no_auto_output"))
                .withStyle(autoOutput ? ChatFormatting.GREEN : ChatFormatting.WHITE));

        // Soft hammer state — GT6 calls this "Sealed" and uses it for barrel fermentation.
        if (!someState) {
            player.sendSystemMessage(Component.literal("Normal")
                    .withStyle(ChatFormatting.GRAY));
        } else if (tank.getMaxSealedTime() > 0) {
            player.sendSystemMessage(Component.literal("Sealed (" + tank.getSealedTime() + " / "
                            + tank.getMaxSealedTime() + ")")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.literal("Sealed")
                    .withStyle(ChatFormatting.GREEN));
        }
        if (someState && !fluid.isEmpty()) {
            var output = tank.getFermentationOutput();
            player.sendSystemMessage(Component.literal(output.isEmpty()
                            ? "Not fermenting"
                            : "Fermenting into " + output.getDisplayName().getString())
                    .withStyle(output.isEmpty() ? ChatFormatting.GRAY : ChatFormatting.AQUA));
        }

        if (fluid.isEmpty()) {
            player.sendSystemMessage(Component.empty()
                    .append(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tank.info.capacity")
                            .withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(": " + formatLargeNumber(spec.capacity()) + " L")
                            .withStyle(ChatFormatting.AQUA)));
        } else {
            player.sendSystemMessage(Component.empty()
                    .append(Component.literal(formatLargeNumber(fluid.getAmount()) + " L of ")
                            .withStyle(ChatFormatting.WHITE))
                    .append(fluid.getDisplayName())
                    .append(Component.literal("; Max: " + formatLargeNumber(spec.capacity()) + " L")
                            .withStyle(ChatFormatting.AQUA)));
        }
    }

    // === Number formatting ===

    static String formatLargeNumber(long value) {
        if (value >= 1_000_000) {
            return String.format(Locale.ROOT, "%,d", value).replace(',', '_');
        }
        if (value >= 1000) {
            return String.format(Locale.ROOT, "%,d", value);
        }
        return Long.toString(value);
    }

}
