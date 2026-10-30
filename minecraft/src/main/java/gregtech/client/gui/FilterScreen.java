package gregtech.client.gui;

import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

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
