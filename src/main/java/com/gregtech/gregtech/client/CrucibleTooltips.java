package com.gregtech.gregtech.client;
import com.gregtech.gregtech.api.material.MaterialPresentation;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.machine.CrucibleSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/** GT6 {@code MultiTileEntitySmeltery#addToolTips} plus shared machine tags. */
public final class CrucibleTooltips {
    private CrucibleTooltips() {}

    public static void appendSmeltingCrucible(CrucibleSpec spec, List<Component> tooltip, TooltipFlag flag) {
        appendEnergyConversion(tooltip);
        appendThermalMass(spec, tooltip);
        appendMeltdown(spec, tooltip);
        appendKuSteelmaking(tooltip);
        if (spec.acidProof()) {
            appendAcidProof(tooltip);
        }
        MachineTooltips.appendHazardFire(tooltip);
        appendContactDamage(tooltip);
        appendThermometer(tooltip);
        MachineTooltips.appendShovelEmpty(tooltip);
        appendBlastResistance(spec, tooltip);
        appendHarvestPickaxe(tooltip);

        GTMaterial material = spec.material().resolve();
        String chemical = material.getResolvedTooltipChemical();
        if (chemical != null && !chemical.isEmpty()) {
            tooltip.add(Component.literal(chemical).withStyle(ChatFormatting.YELLOW));
        }

        if (flag.isAdvanced()) {
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".contained_materials")
                    .withStyle(ChatFormatting.DARK_AQUA));
            tooltip.add(containedHullLine(spec));
        } else {
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".f3h_hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void appendEnergyConversion(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".crucible.energy_convert",
                        CrucibleSpec.MIN_HU_PER_TICK, CrucibleSpec.KG_PER_ENERGY)
                .withStyle(ChatFormatting.AQUA));
    }

    private static void appendThermalMass(CrucibleSpec spec, List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".crucible.thermal_mass",
                        formatMass(spec.thermalMassKg()))
                .withStyle(ChatFormatting.YELLOW));
    }

    private static void appendMeltdown(CrucibleSpec spec, List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".crucible.meltdown",
                        spec.meltDownTemperatureK())
                .withStyle(ChatFormatting.DARK_RED));
    }

    private static void appendKuSteelmaking(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".crucible.ku_steelmaking")
                .withStyle(ChatFormatting.WHITE));
    }

    private static void appendAcidProof(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".crucible.acidproof")
                .withStyle(ChatFormatting.GOLD));
    }

    private static void appendContactDamage(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".crucible.contact_damage")
                .withStyle(ChatFormatting.DARK_RED));
    }

    private static void appendThermometer(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".crucible.thermometer")
                .withStyle(ChatFormatting.GRAY));
    }

    private static void appendBlastResistance(CrucibleSpec spec, List<Component> tooltip) {
        TooltipHelper.appendBlastResistance(spec.blastResistance(), tooltip);
    }

    private static void appendHarvestPickaxe(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".crucible.harvest_pickaxe")
                .withStyle(ChatFormatting.GRAY));
    }

    private static Component containedHullLine(CrucibleSpec spec) {
        GTMaterial material = spec.material().resolve();
        long amount = spec.hullMaterialUnits();
        double weightKg = spec.thermalMassKg();
        long frac = ((long) (weightKg * 1000)) % 1000;

        return Component.empty()
                .append(Component.literal(MaterialTooltips.displayUnits(amount) + " ").withStyle(ChatFormatting.WHITE))
                .append(MaterialPresentation.name(material).copy().withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" (").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("M: ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(String.valueOf(spec.meltingPointK())).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("K ").withStyle(ChatFormatting.RED))
                .append(Component.literal(" B: ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(String.valueOf(spec.boilingPointK())).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("K ").withStyle(ChatFormatting.RED))
                .append(Component.literal(" W: ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(formatWeight(weightKg, frac)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("kg").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(")").withStyle(ChatFormatting.WHITE));
    }

    private static String formatMass(double kg) {
        return Double.toString(kg);
    }

    private static String formatWeight(double whole, long frac) {
        long w = (long) whole;
        return w + "." + MaterialTooltips.pad3(frac);
    }
}
