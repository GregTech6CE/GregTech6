package com.gregtech.gregtech.client;


import com.gregtech.gregtech.api.machine.CrucibleSpec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/** GT6 smeltery companion item tooltips ({@code MultiTileEntityMold}, {@code Basin}, {@code Faucet}). */
public final class SmelteryCompanionTooltips {
    private SmelteryCompanionTooltips() {}

    /** {@code MultiTileEntityMold#addToolTips} (unplaced item — shape not selected). */
    public static void appendMold(CrucibleSpec spec, List<Component> tooltip) {
        appendMoldSelect(tooltip);
        appendInteractTop(tooltip);
        appendAcidProofIfNeeded(spec, tooltip);
        appendMeltdown(spec, tooltip);
        appendContactDamage(tooltip);
        appendPincers(tooltip);
        appendMonkeyWrenchAutoInputs(tooltip);
        appendSoftHammerReset(tooltip);
    }

    /** {@code MultiTileEntityBasin#addToolTips}. */
    public static void appendMoldBasin(CrucibleSpec spec, List<Component> tooltip) {
        appendProducesBlockSolid(tooltip);
        appendInteractTop(tooltip);
        appendAcidProofIfNeeded(spec, tooltip);
        appendMeltdown(spec, tooltip);
        appendContactDamage(tooltip);
        appendPincers(tooltip);
    }

    /** {@code MultiTileEntityFaucet#addToolTips}. */
    public static void appendCrucibleFaucet(CrucibleSpec spec, List<Component> tooltip) {
        appendInteract(tooltip);
        appendAcidProofIfNeeded(spec, tooltip);
        appendMeltdown(spec, tooltip);
        appendMonkeyWrenchAutoInputs(tooltip);
    }

    /** {@code MultiTileEntityCrossing} — GT6 adds no tooltips. */
    public static void appendCrucibleCrossing(CrucibleSpec spec, List<Component> tooltip) {
        // Intentionally empty — matches GT6.
    }

    private static void appendMoldSelect(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".smeltery.mold.select")
                .withStyle(ChatFormatting.AQUA));
    }

    private static void appendProducesBlockSolid(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".smeltery.mold.produces",
                        Component.translatable("tooltip." + "gregtech" + ".smeltery.product.block_solid"))
                .withStyle(ChatFormatting.AQUA));
    }

    private static void appendInteractTop(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".smeltery.interact_top",
                        Component.translatable("tooltip." + "gregtech" + ".smeltery.face.top"))
                .withStyle(ChatFormatting.GOLD));
    }

    private static void appendInteract(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".smeltery.interact")
                .withStyle(ChatFormatting.GOLD));
    }

    private static void appendAcidProofIfNeeded(CrucibleSpec spec, List<Component> tooltip) {
        if (spec.acidProof()) {
            tooltip.add(Component.translatable("tooltip." + "gregtech" + ".crucible.acidproof")
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    private static void appendMeltdown(CrucibleSpec spec, List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".crucible.meltdown",
                        spec.meltDownTemperatureK())
                .withStyle(ChatFormatting.DARK_RED));
    }

    private static void appendContactDamage(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".crucible.contact_damage")
                .withStyle(ChatFormatting.DARK_RED));
    }

    private static void appendPincers(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".smeltery.tool.pincers")
                .withStyle(ChatFormatting.GRAY));
    }

    private static void appendMonkeyWrenchAutoInputs(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".smeltery.tool.monkey_wrench_auto_inputs")
                .withStyle(ChatFormatting.GRAY));
    }

    private static void appendSoftHammerReset(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".smeltery.tool.soft_hammer_reset")
                .withStyle(ChatFormatting.GRAY));
    }
}
