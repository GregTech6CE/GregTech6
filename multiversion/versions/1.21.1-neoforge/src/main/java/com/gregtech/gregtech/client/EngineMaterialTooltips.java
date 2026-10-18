package com.gregtech.gregtech.client;
import com.gregtech.gregtech.api.material.*;import net.minecraft.ChatFormatting;import net.minecraft.network.chat.Component;
/** Original contained-material presentation for engine items. */
final class EngineMaterialTooltips {private EngineMaterialTooltips(){}
    public static Component containedMaterialLine(GTMaterial mat, long amount) {
        if (!mat.isValid()) {
            return Component.empty();
        }
        if (mat.getId() >= 7900 && mat.getId() <= 7912) return Component.literal(displayUnits(amount) + " ")
                .append(MaterialPresentation.name(mat)).append(Component.translatable("tooltip.gregtech.composition_category"));
        double weightKg = weightKg(mat, amount);
        long frac = ((long) (weightKg * 1000)) % 1000;

        return Component.empty()
                .append(Component.literal(displayUnits(amount) + " ").withStyle(ChatFormatting.WHITE))
                .append(MaterialPresentation.name(mat).copy().withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" (").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("M: ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(String.valueOf(mat.getMeltingPoint())).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("K ").withStyle(ChatFormatting.RED))
                .append(Component.literal(" B: ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(String.valueOf(mat.getBoilingPoint())).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("K ").withStyle(ChatFormatting.RED))
                .append(Component.literal(" W: ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(formatWeight(weightKg, frac)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("kg").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(")").withStyle(ChatFormatting.WHITE));
    }

    /** {@code (g/cm³ × amount × 0.111 - /U  -> kg}, same as GT6 {@code OreDictMaterial#getWeight}. */
    public static double weightKg(GTMaterial material, long amount) {
        return weightKg(material, amount, material.getDensity());
    }

    public static double weightKg(GTMaterial material, long amount, double densityGPerCm3) {
        return com.gregtech.gregtech.api.material.MaterialMass.kilograms(densityGPerCm3, amount);
    }

    public static String displayUnits(long amount) {
        if (amount < 0) {
            return "?.???";
        }
        long digits = ((amount % GTValues.U) * 1000) / GTValues.U;
        return (amount / GTValues.U) + "." + pad3(digits);
    }

    public static String pad3(long digits) {
        if (digits < 1) return "000";
        if (digits < 10) return "00" + digits;
        if (digits < 100) return "0" + digits;
        return Long.toString(digits);
    }

    private static String formatWeight(double whole, long frac) {
        long w = (long) whole;
        return w + "." + pad3(frac);
    }

}
