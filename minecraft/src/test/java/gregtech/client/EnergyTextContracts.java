package gregtech.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import java.util.Optional;

/** Checks the shared display boundary without starting a world or optional tooltip mods. */
public final class EnergyTextContracts {
    public static void main(String[] args) {
        String[] units = {"SU", "MJ", "RF", "AU", "QU", "MU", "LU", "HU", "CU", "KU", "RU", "EU", "NU",
                "Ordo", "Aer", "Aqua", "Terra", "Ignis", "Perditio"};
        ChatFormatting[] colors = {ChatFormatting.GRAY, ChatFormatting.DARK_RED, ChatFormatting.DARK_RED,
                ChatFormatting.WHITE, ChatFormatting.DARK_PURPLE, ChatFormatting.DARK_GRAY, ChatFormatting.YELLOW,
                ChatFormatting.RED, ChatFormatting.AQUA, ChatFormatting.DARK_GREEN, ChatFormatting.GREEN,
                ChatFormatting.BLUE, ChatFormatting.BLACK, ChatFormatting.WHITE, ChatFormatting.YELLOW,
                ChatFormatting.AQUA, ChatFormatting.GREEN, ChatFormatting.RED, ChatFormatting.BLACK};
        for (int i = 0; i < units.length; i++) {
            if (EnergyText.color(units[i]) != colors[i]) throw new AssertionError(units[i]);
            var text = Component.translatableWithFallback("test.gt6ce.energy", "Input: %s/t", units[i])
                    .withStyle(ChatFormatting.GOLD);
            var formatted = EnergyText.format(text);
            checkSpan(formatted, units[i], colors[i]);
            checkSpan(formatted, "Input: ", ChatFormatting.GOLD);
            if (!formatted.getString().equals(text.getString())) throw new AssertionError("Text changed");
        }
        checkSpan(EnergyText.format(Component.literal("32RU/t; RUST").withStyle(ChatFormatting.GRAY)), "RU", ChatFormatting.GREEN);
        checkSpan(EnergyText.format(Component.literal("RUST").withStyle(ChatFormatting.GRAY)), "RUST", ChatFormatting.GRAY);
        checkSpan(EnergyText.format(Component.translatableWithFallback("gt.td.short.energy.vis_ignis", "火")), "火", ChatFormatting.RED);
        checkSpan(EnergyText.format(Component.translatableWithFallback("gt.td.long.energy.quantum", "Quantum Energy")), "Quantum Energy", ChatFormatting.DARK_PURPLE);
        var battery = new java.util.ArrayList<Component>();
        TooltipHelper.appendPortableBattery(1234, 10000, 16, 64, "LU", battery);
        var stored = withFallback(battery.get(0), "%s / %s %s");
        var packet = withFallback(battery.get(1), "Energy packet: %s–%s %s");
        if (!stored.getString().equals("1,234 / 10,000 LU") || !packet.getString().endsWith("16–64 LU"))
            throw new AssertionError("Crystal tooltips must keep the LU unit and formatted values");
        checkSpan(EnergyText.format(stored), "1,234", ChatFormatting.WHITE);
        checkSpan(EnergyText.format(packet), "Energy packet: ", ChatFormatting.AQUA);
        checkSpan(EnergyText.format(packet), "LU", ChatFormatting.YELLOW);
        System.out.println("Energy text contracts passed: 19 colours, translated arguments/names and inherited styles.");
    }

    private static Component withFallback(Component line, String fallback) {
        var translated = (net.minecraft.network.chat.contents.TranslatableContents) line.getContents();
        return Component.translatableWithFallback("test.gt6ce." + translated.getKey(), fallback, translated.getArgs())
                .setStyle(line.getStyle());
    }

    private static void checkSpan(Component text, String expected, ChatFormatting color) {
        boolean[] found = {false};
        text.visit((style, part) -> {
            if (part.equals(expected)) {
                found[0] = true;
                if (style.getColor() == null || style.getColor().getValue() != color.getColor())
                    throw new AssertionError("Wrong colour: " + expected);
            }
            return Optional.empty();
        }, Style.EMPTY);
        if (!found[0]) throw new AssertionError("Missing span: " + expected + " in " + text);
    }
}
