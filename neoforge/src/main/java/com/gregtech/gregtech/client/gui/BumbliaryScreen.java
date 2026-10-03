package com.gregtech.gregtech.client.gui;


import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * GT6's bumbliary screen ({@code ContainerClient}, {@code MultiTileEntityBumbliary:497-509}): GT6's own
 * GUI sheet, drawn as it is.
 *
 * <p>GT6's {@code ContainerClient.drawGuiContainerForegroundLayer:57-60} is empty - the machine has no
 * title and no "Inventory" label inside the GUI, the texture already carries both - so this screen
 * draws no labels either. The standard machine uses
 * {@code textures/gui/machines/Bumbliary.png}, the advanced one
 * {@code textures/gui/machines/BumbliaryAdvanced.png} ({@code MultiTileEntityBumbliaryAdvanced:469,476});
 * the port ships both under lower-case names.</p>
 */
public class BumbliaryScreen extends AbstractContainerScreen<BumbliaryContainerMenu> {

    private static final ResourceLocation STANDARD =
            ResourceLocation.fromNamespaceAndPath("gregtech", "textures/gui/machines/bumbliary.png");
    private static final ResourceLocation ADVANCED =
            ResourceLocation.fromNamespaceAndPath("gregtech", "textures/gui/machines/bumbliary_advanced.png");

    private final ResourceLocation texture;

    public BumbliaryScreen(BumbliaryContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.texture = menu.advanced() ? ADVANCED : STANDARD;
        this.imageWidth = BumbliaryContainerMenu.IMAGE_WIDTH;
        this.imageHeight = BumbliaryContainerMenu.IMAGE_HEIGHT;
        this.inventoryLabelY = BumbliaryContainerMenu.PLAYER_INVENTORY_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // GT6 draws nothing here (ContainerClient:57-60): the sheet has the titles baked in.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
