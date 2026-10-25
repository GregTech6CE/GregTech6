package gregtech.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Shared title tab for GT machine, controller and storage screens. */
public abstract class MachineContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private final ItemStack titleIcon;

    protected MachineContainerScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        titleIcon = MachineGuiChrome.iconForTitle(title);
    }

    protected ItemStack titleIcon() { return titleIcon; }

    /** Only sheets with the standard GT layout opt into the center logo. */
    protected ResourceLocation brandingTexture() { return null; }

    public net.minecraft.client.renderer.Rect2i titleBounds() {
        return new net.minecraft.client.renderer.Rect2i(leftPos, topPos - 10,
                MachineGuiChrome.titleWidth(font, title, titleIcon()), 14);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        var tab = titleBounds();
        if (mouseX >= tab.getX() && mouseX < tab.getX() + tab.getWidth()
                && mouseY >= tab.getY() && mouseY < tab.getY() + tab.getHeight()) return false;
        return super.hasClickedOutside(mouseX, mouseY, left, top, button);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        MachineGuiChrome.brand(graphics, brandingTexture(), menu.slots);
        MachineGuiChrome.title(graphics, font, title, titleIcon());
    }
}
