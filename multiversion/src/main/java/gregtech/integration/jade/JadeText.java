package gregtech.integration.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import snownee.jade.api.ITooltip;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

/** Resolve names on the viewing client, never using the dedicated server's language. */
final class JadeText {
    private JadeText() {}

    /** Extra translation-key fields are optional so old server data retains its literal fallback. */
    static Component translated(CompoundTag data, String keyField, String fallbackField) {
        String key = data.getString(keyField);
        String fallback = data.getString(fallbackField);
        return key.isEmpty() ? Component.literal(fallback) : Component.translatableWithFallback(key, fallback);
    }

    static Component direction(String direction) {
        return Component.translatableWithFallback("jade.gt6ce.direction." + direction, direction);
    }

    /** Minecraft translations only reliably substitute %s; precision belongs in the argument. */
    static String decimal(double value, int places) {
        return String.format(java.util.Locale.ROOT, "%." + places + "f", value);
    }

    /** Omni's DISPLAYNAME + TAB + ALIGNRIGHT, expressed through Jade's layout API. */
    static void detail(ITooltip tooltip, String key, Object... args) {
        String pattern = Language.getInstance().getOrDefault(key);
        int colon = pattern.indexOf('：');
        if (colon < 0) colon = pattern.indexOf(':');
        if (colon <= 0 || pattern.substring(0, colon).contains("%")) {
            tooltip.add(Component.translatable(key, args));
            return;
        }
        var helper = IElementHelper.get();
        tooltip.add(Component.literal(pattern.substring(0, colon)).withStyle(ChatFormatting.GRAY));
        tooltip.append(helper.spacer(8, 0));
        tooltip.append(helper.text(Component.translatableWithFallback(key + ".value", pattern.substring(colon + 1).strip(), args)
                .withStyle(ChatFormatting.WHITE)).align(IElement.Align.RIGHT));
    }

    static MutableComponent status(String key) {
        var text = Component.translatable(key);
        String state = key.substring(key.lastIndexOf('.') + 1);
        return switch (state) {
            case "working", "running", "yes", "enabled" -> text.withStyle(ChatFormatting.GREEN);
            case "waiting", "preheating", "manual_work" -> text.withStyle(ChatFormatting.BLUE);
            case "disabled", "stopped", "dry", "incomplete" -> text.withStyle(ChatFormatting.RED);
            case "collect_outputs", "pending", "prepared" -> text.withStyle(ChatFormatting.GOLD);
            default -> text;
        };
    }

    /** Unit colours follow the supplied GT6 Omni script, without legacy section-sign strings. */
    static Component unit(String unit) {
        ChatFormatting color = switch (unit) {
            case "EU" -> ChatFormatting.BLUE;
            case "RU" -> ChatFormatting.GREEN;
            case "KU" -> ChatFormatting.DARK_GREEN;
            case "HU" -> ChatFormatting.RED;
            case "CU", "L", "K", "A" -> ChatFormatting.AQUA;
            case "LU" -> ChatFormatting.YELLOW;
            case "MU" -> ChatFormatting.DARK_GRAY;
            case "NU" -> ChatFormatting.BLACK;
            case "QU" -> ChatFormatting.DARK_PURPLE;
            case "TU" -> ChatFormatting.DARK_BLUE;
            case "RF", "MJ" -> ChatFormatting.DARK_RED;
            case "SU" -> ChatFormatting.GRAY;
            default -> ChatFormatting.WHITE;
        };
        return Component.literal(unit).withStyle(color);
    }
}
