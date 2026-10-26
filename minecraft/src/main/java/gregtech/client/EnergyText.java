package gregtech.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Shared presentation colours for energy names and units, independent of the tooltip provider. */
public final class EnergyText {
    private static final Pattern UNIT = Pattern.compile(
            "(?<![\\p{L}_])(SU|MJ|RF|AU|QU|MU|LU|HU|CU|KU|RU|EU|NU|TU|Ordo|Aer|Aqua|Terra|Ignis|Perditio)(?![\\p{L}\\p{N}_])");

    private EnergyText() {}

    public static ChatFormatting color(String energy) {
        return switch (energy.toUpperCase(Locale.ROOT)) {
            case "SU", "STEAM" -> ChatFormatting.GRAY;
            case "MJ", "MINECRAFT_JOULES", "RF", "REDSTONE_FLUX" -> ChatFormatting.DARK_RED;
            case "AU", "AIR", "ORDO", "VIS_ORDO" -> ChatFormatting.WHITE;
            case "QU", "QUANTUM" -> ChatFormatting.DARK_PURPLE;
            case "MU", "MAGNETIC" -> ChatFormatting.DARK_GRAY;
            case "LU", "LIGHT", "AER", "VIS_AER" -> ChatFormatting.YELLOW;
            case "HU", "HEAT", "IGNIS", "VIS_IGNIS" -> ChatFormatting.RED;
            case "CU", "CRYO", "AQUA", "VIS_AQUA" -> ChatFormatting.AQUA;
            case "KU", "KINETIC_PUSH" -> ChatFormatting.DARK_GREEN;
            case "RU", "KINETIC_ROTATION", "TERRA", "VIS_TERRA" -> ChatFormatting.GREEN;
            case "EU", "ELECTRICITY" -> ChatFormatting.BLUE;
            case "NU", "NEUTRON", "PERDITIO", "VIS_PERDITIO" -> ChatFormatting.BLACK;
            case "TU", "TIME" -> ChatFormatting.DARK_BLUE;
            default -> ChatFormatting.WHITE;
        };
    }

    public static Component unit(String unit) {
        return Component.literal(unit).withStyle(color(unit));
    }

    /** Called at display time: retain inherited styles/events, colour only energy spans. */
    public static Component format(Component source) {
        Component named = names(source);
        MutableComponent result = Component.empty();
        boolean[] changed = {false};
        named.visit((style, text) -> {
            var matcher = UNIT.matcher(text);
            int start = 0;
            while (matcher.find()) {
                changed[0] = true;
                if (start < matcher.start()) result.append(Component.literal(text.substring(start, matcher.start())).setStyle(style));
                result.append(Component.literal(matcher.group()).setStyle(style.withColor(color(matcher.group()))));
                start = matcher.end();
            }
            if (start < text.length()) result.append(Component.literal(text.substring(start)).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return changed[0] ? result : named;
    }

    private static Component names(Component source) {
        MutableComponent result;
        if (source.getContents() instanceof TranslatableContents translated) {
            Object[] args = translated.getArgs().clone();
            for (int i = 0; i < args.length; i++) if (args[i] instanceof Component text) args[i] = names(text);
            result = Component.translatableWithFallback(translated.getKey(), translated.getFallback(), args).setStyle(source.getStyle());
            String key = translated.getKey();
            if (key.startsWith("gt.td.short.energy.") || key.startsWith("gt.td.long.energy.")) {
                result.withStyle(color(key.substring(key.lastIndexOf('.') + 1)));
            }
        } else {
            result = MutableComponent.create(source.getContents()).setStyle(source.getStyle());
        }
        for (Component sibling : source.getSiblings()) result.append(names(sibling));
        return result;
    }
}
