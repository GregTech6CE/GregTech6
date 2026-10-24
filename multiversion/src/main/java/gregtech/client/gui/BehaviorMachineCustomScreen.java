package gregtech.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Hosts an addon screen over the behavior machine menu, so its slots keep working. */
public final class BehaviorMachineCustomScreen extends BehaviorMachineHostScreen {
    private final Screen custom;

    public BehaviorMachineCustomScreen(BehaviorMachineMenu menu, Inventory inventory, Component title, Screen custom) {
        super(menu, inventory, title);
        this.custom = custom;
        this.imageWidth = menu.machine().declaration().guiWidth();
        this.imageHeight = menu.machine().declaration().guiHeight();
        this.inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        custom.init(minecraft, width, height);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        custom.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return custom.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return custom.mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return custom.mouseDragged(mouseX, mouseY, button, dragX, dragY)
                || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

//? if >=1.21.1 {
/*    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return custom.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
*///?} else {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        return custom.mouseScrolled(mouseX, mouseY, scroll) || super.mouseScrolled(mouseX, mouseY, scroll);
    }
//?}

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return custom.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return custom.charTyped(character, modifiers) || super.charTyped(character, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return custom.isPauseScreen();
    }
}
