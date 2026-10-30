package gregtech.client.gui;


//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Set;

/**
 * GT6 hopper / queuehopper screen ({@code ContainerClientDefault}).
 * Picks the GUI texture matching the slot count from {@code textures/gui/chests/}.
 */
public class HopperScreen extends MachineContainerScreen<HopperContainerMenu> {
    private static final Set<Integer> TEXTURE_SIZES = Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 12, 14, 15, 16, 18, 27, 36, 54);
    private static final int TEX_WIDTH = 176;

    private final ResourceLocation texture;

    public HopperScreen(HopperContainerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.texture = guiTexture(menu.slotCount());
        this.imageWidth = 176;
        this.imageHeight = menu.playerInvY() + 82;
        this.inventoryLabelY = menu.playerInvY() - 11;
    }

    private static ResourceLocation guiTexture(int slotCount) {
        String name = TEXTURE_SIZES.contains(slotCount) ? String.valueOf(slotCount) : "0";
//? if >=1.21.1 {
        /*return ResourceLocation.fromNamespaceAndPath("gt6ce","textures/gui/chests/" + name + ".png");
*///?} else {
        return GregTech.id("textures/gui/chests/" + name + ".png");
//?}
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        int n = menu.slotCount();
        // Compact layouts have no free row for the inventory caption.
        if (n < 27 && n != 16) {
            graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        }
    }

    @Override protected ResourceLocation brandingTexture() { return texture; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

//? if >=1.21.1 {
//?} else {
        renderBackground(graphics);
//?}
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GuiTemplates.background(graphics, texture, leftPos, topPos, imageWidth, imageHeight, menu.slots);
    }
}
