package gregtech.client.gui;


//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * GT6's bumbliary screen ({@code ContainerClient}, {@code MultiTileEntityBumbliary:497-509}): GT6's own
 * GUI sheet, drawn as it is.
 *
 * <p>The title tab stays outside the bee slots. The standard machine uses
 * {@code textures/gui/machines/Bumbliary.png}, the advanced one
 * {@code textures/gui/machines/BumbliaryAdvanced.png} ({@code MultiTileEntityBumbliaryAdvanced:469,476});
 * the port ships both under lower-case names.</p>
 */
public class BumbliaryScreen extends MachineContainerScreen<BumbliaryContainerMenu> {

    private static final ResourceLocation STANDARD =
//? if >=1.21.1 {
            /*ResourceLocation.fromNamespaceAndPath("gt6ce", "textures/gui/machines/bumbliary.png");
*///?} else {
            GregTech.id("textures/gui/machines/bumbliary.png");
//?}
    private static final ResourceLocation ADVANCED =
//? if >=1.21.1 {
            /*ResourceLocation.fromNamespaceAndPath("gt6ce", "textures/gui/machines/bumbliary_advanced.png");
*///?} else {
            GregTech.id("textures/gui/machines/bumbliary_advanced.png");
//?}

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
    protected ResourceLocation brandingTexture() { return texture; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

//? if >=1.21.1 {
//?} else {
        renderBackground(graphics);
//?}
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
