package gregtech.client;

import gregtech.content.block.misc.AutoIgniterBlock;
import gregtech.content.block.misc.AutoToolBlock;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Original automatic-tool function, quality and energy rows, using the runtime input limits. */
public final class AutomaticToolTooltips {
    private AutomaticToolTooltips() {}

    public static void append(AutoToolBlock block, List<Component> lines) {
        boolean igniter = block instanceof AutoIgniterBlock;
        lines.add(Component.translatable(igniter ? "gt.lang.recipes.igniter" : "gt.lang.recipes.autohammer")
                .withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("gt.lang.recipes.quality")
                .withStyle(ChatFormatting.AQUA)
                .append(": ")
                .append(Component.literal(Integer.toString(block.quality())).withStyle(ChatFormatting.WHITE)));
        lines.add(TooltipHelper.energyInLine(
                block.input(),
                Math.max(1, block.input() / (igniter ? 2 : 8)),
                block.input() * 2,
                igniter ? "EU" : "KU",
                Component.translatable(igniter ? "tooltip.gt6ce.ui.except_front" : "gt.lang.face.back")
                        .getString()));
        lines.add(Component.translatable("tooltip.gt6ce.auto_tool.toggle").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("tooltip.gt6ce.machine.tool.wrench_facing")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
