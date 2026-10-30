package gregtech.compat.jade;

import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import snownee.jade.api.ITooltip;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

/** Resolve names on the viewing client, never using the dedicated server's language. */
final class JadeText {
    private static final Pattern NUMBER = Pattern.compile("(?<![\\p{L}\\p{N}_])[+-]?\\d+(?:[.,]\\d+)*(?:[%％])?");

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

    /** Resolve the whole translation before splitting, including arguments in the label. */
    static void detail(ITooltip tooltip, String key, Object... args) {
        add(tooltip, Component.translatable(key, args));
    }

    static MutableComponent status(String key) {
        var text = Component.translatable(key);
        String state = key.substring(key.lastIndexOf('.') + 1);
        return switch (state) {
            case "working", "running", "yes", "enabled" -> text.withStyle(ChatFormatting.GREEN);
            case "waiting", "preheating", "manual_work" -> text.withStyle(ChatFormatting.BLUE);
            case "disabled", "stopped", "dry", "incomplete", "sensor_over_limit" -> text.withStyle(ChatFormatting.RED);
            case "collect_outputs", "pending", "prepared" -> text.withStyle(ChatFormatting.GOLD);
            default -> text;
        };
    }

    static MutableComponent phase(String phase) {
        return switch (phase) {
            case "gas" -> Component.translatable("gt6ce.fluid_display.gas").withStyle(ChatFormatting.AQUA);
            case "molten", "liquid" ->
                Component.translatable("gt6ce.fluid_display.liquid").withStyle(ChatFormatting.BLUE);
            case "solidified" ->
                Component.translatable("jade.gt6ce.mold.state.solidified").withStyle(ChatFormatting.GREEN);
            default -> Component.translatable("jade.gt6ce.crucible.state.solid").withStyle(ChatFormatting.GREEN);
        };
    }

    static void add(ITooltip tooltip, Component text) {
        add(tooltip, null, text);
    }

    static void item(ITooltip tooltip, net.minecraft.world.item.ItemStack stack) {
        item(tooltip, stack, stack.getCount());
    }

    static void item(ITooltip tooltip, net.minecraft.world.item.ItemStack stack, long count) {
        if (stack.isEmpty()) return;
        tooltip.add(IElementHelper.get().smallItem(stack));
        tooltip.append(IElementHelper.get().spacer(3, 0));
        tooltip.append(Component.literal(count + "× ").append(stack.getHoverName()));
    }

    /** The same left-label/right-value layout for ordinary lines and rows with an item/fluid icon. */
    static void add(ITooltip tooltip, IElement icon, Component text) {
        Component formatted = format(text);
        Detail detail = split(formatted);
        Component first = detail == null ? formatted : detail.label();
        if (icon == null) tooltip.add(first);
        else {
            tooltip.add(icon);
            tooltip.append(first);
        }
        if (detail != null) {
            var helper = IElementHelper.get();
            tooltip.append(helper.spacer(8, 0));
            tooltip.append(helper.text(detail.value()).align(IElement.Align.RIGHT));
        }
    }

    record Detail(Component label, Component value) {}

    static Detail split(Component text) {
        String plain = text.getString();
        int ascii = plain.indexOf(':'), wide = plain.indexOf('：');
        int colon = ascii < 0 ? wide : wide < 0 ? ascii : Math.min(ascii, wide);
        if (colon <= 0) return null;
        int valueStart = colon + 1;
        while (valueStart < plain.length() && Character.isWhitespace(plain.charAt(valueStart))) valueStart++;
        if (valueStart == plain.length()) return null; // A heading, not a key/value row.
        return new Detail(
                slice(text, 0, colon, ChatFormatting.GRAY),
                slice(text, valueStart, plain.length(), ChatFormatting.WHITE));
    }

    /** Preserve styles of translated arguments and coloured units while extracting a text range. */
    private static Component slice(Component text, int from, int to, ChatFormatting fallback) {
        MutableComponent result = Component.empty();
        int[] offset = {0};
        text.visit(
                (style, part) -> {
                    int start = Math.max(0, from - offset[0]);
                    int end = Math.min(part.length(), to - offset[0]);
                    if (start < end)
                        result.append(Component.literal(part.substring(start, end))
                                .setStyle(style.getColor() == null ? style.withColor(fallback) : style));
                    offset[0] += part.length();
                    return Optional.empty();
                },
                Style.EMPTY);
        return result;
    }

    /** Neutral numbers are white even in free-form lines; explicit status/warning colours survive. */
    static Component format(Component source) {
        MutableComponent result = Component.empty();
        gregtech.client.EnergyText.format(source)
                .visit(
                        (style, part) -> {
                            if (style.getColor() != null
                                    && style.getColor().getValue() != ChatFormatting.GRAY.getColor()) {
                                result.append(Component.literal(part).setStyle(style));
                                return Optional.empty();
                            }
                            var numbers = NUMBER.matcher(part);
                            int start = 0;
                            while (numbers.find()) {
                                if (start < numbers.start())
                                    result.append(Component.literal(part.substring(start, numbers.start()))
                                            .setStyle(style));
                                result.append(Component.literal(numbers.group())
                                        .setStyle(style.withColor(ChatFormatting.WHITE)));
                                start = numbers.end();
                            }
                            if (start < part.length())
                                result.append(
                                        Component.literal(part.substring(start)).setStyle(style));
                            return Optional.empty();
                        },
                        Style.EMPTY);
        return result;
    }

    static Component unit(String unit) {
        // Physical units retain their existing colour; energy units share the item tooltip palette.
        if (unit.equals("L") || unit.equals("K") || unit.equals("A"))
            return Component.literal(unit).withStyle(ChatFormatting.AQUA);
        return gregtech.client.EnergyText.unit(unit);
    }
}
