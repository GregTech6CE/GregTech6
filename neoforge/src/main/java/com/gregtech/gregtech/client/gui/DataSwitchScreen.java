package com.gregtech.gregtech.client.gui;

import com.gregtech.gregtech.block.inventory.UsbSwitchBlock;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GT6's USBSwitch.png and HDDSwitch.png, with the selected data position displayed. */
public final class DataSwitchScreen extends AbstractContainerScreen<DataSwitchMenu> {
    private static final ResourceLocation USB = ResourceLocation.fromNamespaceAndPath("gregtech","textures/gui/machines/usb_switch.png");
    private static final ResourceLocation HDD = ResourceLocation.fromNamespaceAndPath("gregtech","textures/gui/machines/hdd_switch.png");

    public DataSwitchScreen(DataSwitchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // GT6 omits the title for 16-slot screens: the first USB slot begins at x=53, y=8.
        if (menu.kind() != UsbSwitchBlock.Kind.USB)
            graphics.drawString(font, title, 8, 6, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.gregtech.data_switch.slot", menu.selectedSlot()),
                8, 20, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, 8, 72, 0x404040, false);
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(menu.kind() == UsbSwitchBlock.Kind.USB ? USB : HDD,
                leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics,mouseX,mouseY,partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
