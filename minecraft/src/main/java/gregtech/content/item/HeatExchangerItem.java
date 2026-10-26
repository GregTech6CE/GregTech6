package gregtech.content.item;

import gregtech.content.block.machine.HeatExchangerBlock;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.List;

public final class HeatExchangerItem extends BlockItem {
    public HeatExchangerItem(HeatExchangerBlock block) { super(block, new Properties().stacksTo(16)); }
//? if >=1.21.1 {
    /*@Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
*///?} else {
    @Override public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level context, List<Component> lines, TooltipFlag flag) {
//?}
        super.appendHoverText(stack, context, lines, flag);
        var spec = ((HeatExchangerBlock)getBlock()).spec();
        lines.add(Component.translatable("gt.lang.energy.output").append(": " + spec.rate() + " HU/t (")
                .append(Component.translatable("gt.lang.face.top")).append(")").withStyle(ChatFormatting.RED));
        lines.add(Component.translatable("gt.lang.efficiency").append(": " + spec.efficiency() / 100.0 + "%").withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("gt.lang.recipes").append(": ").append(Component.translatable("gt.recipe.fuels.hot")).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("gt.lang.nogui.funnel.tap.tank").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gt.lang.hazard.fire").append(" (3m)").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gt.lang.hazard.contact").append(" (")
                .append(Component.translatable("gt.lang.face.top")).append(")").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gt.lang.use.magnifyingglass.to.detail").withStyle(ChatFormatting.GRAY));
    }
}
