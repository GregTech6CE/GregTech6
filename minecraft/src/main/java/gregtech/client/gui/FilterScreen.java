package gregtech.client.gui;

import gregtech.content.logistics.FilterRules;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class FilterScreen extends MachineContainerScreen<FilterMenu> {
    private Button mode;

    public FilterScreen(FilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = menu.prefixMode() ? 132 : 222;
        inventoryLabelY = menu.prefixMode() ? 38 : 128;
    }

    @Override
    protected void init() {
        super.init();
        mode = addRenderableWidget(Button.builder(modeText(), clickedButton -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                0)).bounds(leftPos + 92, topPos + 3, 76, 13).build());
    }

    private Component modeText() {
        return Component.translatable(menu.blacklist() ? "gt6ce.filter.blacklist" : "gt6ce.filter.whitelist");
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GuiTemplates.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        var slot = GuiTemplates.sprite("slot/item");
        for (var placed : menu.slots) slot.draw(graphics, leftPos + placed.x - 1, topPos + placed.y - 1);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (menu.prefixMode()) {
            Component label = prefixText(menu.getSlot(0).getItem());
            graphics.drawString(font, font.plainSubstrByWidth(label.getString(), imageWidth - 16), 8, 38, 0x404040, false);
        } else if (menu.supportsItemMatchModes()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            for (int i = 0; i < menu.templateSlots(); i++) {
                var slot = menu.getSlot(i);
                if (!slot.hasItem()) continue;
                boolean wildcard = menu.ignoresItemData(i);
                graphics.drawString(font, wildcard ? "*" : "=", slot.x + 11, slot.y - 1, wildcard ? 0xFFFF55 : 0xFFFFFF, true);
            }
            graphics.pose().popPose();
        }
    }

    private static Component prefixText(ItemStack stack) {
        if (stack.isEmpty()) return Component.translatable("gt6ce.filter.prefix_empty");
        String prefix = FilterRules.prefix(stack);
        if (prefix.isEmpty()) return Component.translatable("gt6ce.filter.prefix_unknown");
        return Component.translatable("gt6ce.filter.prefix_selected",
                Component.translatableWithFallback("oredict.prefix." + prefix, prefix));
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int x, int y) {
        if (hoveredSlot == null || hoveredSlot.index >= menu.templateSlots()) {
            super.renderTooltip(graphics, x, y);
            return;
        }
        ItemStack carried = menu.getCarried();
        ItemStack template = hoveredSlot.getItem();
        var lines = new ArrayList<Component>();
        if (!template.isEmpty() && carried.isEmpty()) lines.addAll(getTooltipFromContainerItem(template));
        if (menu.prefixMode()) {
            lines.add(prefixText(carried.isEmpty() ? template : carried));
        } else if (carried.isEmpty() && !template.isEmpty()) {
            if (menu.supportsItemMatchModes()) {
                lines.add(Component.translatable(menu.ignoresItemData(hoveredSlot.index)
                        ? "gt6ce.filter.item_wildcard" : "gt6ce.filter.item_exact"));
                lines.add(Component.translatable("gt6ce.filter.toggle_slot").withStyle(ChatFormatting.GRAY));
            }
            if (menu.supportsFluids()) lines.add(Component.translatable("gt6ce.filter.fluid_match"));
        }
        if (!template.isEmpty()) lines.add(Component.translatable("gt6ce.filter.clear_slot").withStyle(ChatFormatting.GRAY));
        if (!lines.isEmpty()) graphics.renderTooltip(font, lines,
                carried.isEmpty() ? template.getTooltipImage() : java.util.Optional.empty(), x, y);
    }
//? if >=1.21.1 {
    /*@Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        mode.setMessage(modeText());
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
*///?} else {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        mode.setMessage(modeText());
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
//?}
}
