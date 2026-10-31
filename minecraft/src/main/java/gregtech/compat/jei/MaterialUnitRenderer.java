package gregtech.compat.jei;

import gregtech.client.MaterialUnitDisplay;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

/** Uses the usual item image but replaces its stack-count decoration with a material fraction. */
final class MaterialUnitRenderer implements IIngredientRenderer<ItemStack> {
    private final long units;
    MaterialUnitRenderer(long units) { this.units = units; }
    @Override public void render(GuiGraphics graphics, ItemStack stack) {
        graphics.renderItem(stack, 0, 0);
        MaterialUnitDisplay.draw(graphics, 0, 0, units);
    }
    @Override public List<Component> getTooltip(ItemStack stack, TooltipFlag flag) {
//? if >=1.21.1 {
        /*return stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(Minecraft.getInstance().level), Minecraft.getInstance().player, flag);
*///?} else {
        return stack.getTooltipLines(Minecraft.getInstance().player, flag);
//?}
    }
}
