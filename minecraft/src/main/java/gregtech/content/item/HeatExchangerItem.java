package gregtech.content.item;

import gregtech.content.block.machine.HeatExchangerBlock;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class HeatExchangerItem extends GTBlockItem {
    public HeatExchangerItem(HeatExchangerBlock block) {
        super(block, new Properties().stacksTo(16));
    }

    @Override
    protected void appendTooltip(
            ItemStack stack, net.minecraft.world.level.Level context, List<Component> lines, TooltipFlag flag) {
        super.appendTooltip(stack, context, lines, flag);
        var spec = ((HeatExchangerBlock) getBlock()).spec();
        lines.add(Component.translatable("gt.lang.energy.output")
                .append(": " + spec.rate() + " HU/t (")
                .append(Component.translatable("gt.lang.face.top"))
                .append(")")
                .withStyle(ChatFormatting.RED));
        lines.add(Component.translatable("gt.lang.efficiency")
                .append(": " + spec.efficiency() / 100.0 + "%")
                .withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("gt.lang.recipes")
                .append(": ")
                .append(Component.translatable("gt.recipe.fuels.hot"))
                .withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("gt.lang.nogui.funnel.tap.tank").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gt.lang.hazard.fire").append(" (3m)").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gt.lang.hazard.contact")
                .append(" (")
                .append(Component.translatable("gt.lang.face.top"))
                .append(")")
                .withStyle(ChatFormatting.GRAY));
        lines.add(
                Component.translatable("gt.lang.use.magnifyingglass.to.detail").withStyle(ChatFormatting.GRAY));
    }
}
