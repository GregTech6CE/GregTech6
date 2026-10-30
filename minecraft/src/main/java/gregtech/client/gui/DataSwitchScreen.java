package gregtech.client.gui;

//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.content.block.inventory.UsbSwitchBlock;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GT6's USBSwitch.png and HDDSwitch.png, with the selected data position displayed. */
public final class DataSwitchScreen extends MachineContainerScreen<DataSwitchMenu> {
//? if >=1.21.1 {
    /*private static final ResourceLocation USB = ResourceLocation.fromNamespaceAndPath("gt6ce","textures/gui/machines/usb_switch.png");
    private static final ResourceLocation HDD = ResourceLocation.fromNamespaceAndPath("gt6ce","textures/gui/machines/hdd_switch.png");
*///?} else {
    private static final ResourceLocation USB = GregTech.id("textures/gui/machines/usb_switch.png");
    private static final ResourceLocation HDD = GregTech.id("textures/gui/machines/hdd_switch.png");
//?}

    public DataSwitchScreen(DataSwitchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("gui.gt6ce.data_switch.slot", menu.selectedSlot()),
                8, 20, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, 8, 72, 0x404040, false);
    }
    @Override protected ResourceLocation brandingTexture() {
        return menu.kind() == UsbSwitchBlock.Kind.USB ? USB : HDD;
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GuiTemplates.background(graphics, brandingTexture(), leftPos, topPos, imageWidth, imageHeight, menu.slots);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

//? if >=1.21.1 {
//?} else {
        renderBackground(graphics);
//?}
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
