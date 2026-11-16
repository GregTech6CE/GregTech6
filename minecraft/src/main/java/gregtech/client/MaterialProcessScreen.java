package gregtech.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

/** Full-window host for the same graph, camera and query target used by the recipe viewer. */
public final class MaterialProcessScreen extends Screen {
    private static final int MARGIN = 6;
    private final MaterialProcessPanel panel;
    private final Screen parent;
    private int dragButton = -1;

    private MaterialProcessScreen(MaterialProcessPanel panel, Screen parent) {
        super(panel.title());
        this.panel = panel;
        this.parent = parent;
    }

    public static void toggle(MaterialProcessPanel panel) {
        var client = Minecraft.getInstance();
        if (client.screen instanceof MaterialProcessScreen screen && screen.panel == panel) screen.onClose();
        else client.setScreen(new MaterialProcessScreen(panel, client.screen));
    }

    @Override
    protected void init() {
        dragButton = -1;
        panel.fullscreen(true);
        panel.resize(width - MARGIN * 2, height - MARGIN * 2);
    }

    @Override
    public void removed() {
        dragButton = -1;
        panel.fullscreen(false);
        panel.resize(MaterialProcessPanel.preferredWidth(), MaterialProcessPanel.preferredHeight());
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    public ItemStack stackAt(double x, double y) {
        return panel.stacksAt(x - MARGIN, y - MARGIN).stream().findFirst().orElse(ItemStack.EMPTY);
    }

    public Rect2i stackArea(double x, double y) {
        var bounds = panel.boundsAt(x - MARGIN, y - MARGIN);
        return new Rect2i(MARGIN + bounds.left(), MARGIN + bounds.top(),
                Math.max(0, bounds.right() - bounds.left()), Math.max(0, bounds.bottom() - bounds.top()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!panel.valid()) {
            onClose();
            return;
        }
        graphics.fill(0, 0, width, height, 0xff303030);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(MARGIN, MARGIN, 0);
            panel.draw(graphics, mouseX - MARGIN, mouseY - MARGIN);
        } finally {
            graphics.pose().popPose();
        }
        var tooltip = panel.tooltip(mouseX - MARGIN, mouseY - MARGIN);
//? if >=1.21.1 {
        /*if (!tooltip.isEmpty()) graphics.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
*///?} else {
        if (!tooltip.isEmpty()) graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
//?}
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (!panel.valid()) {
            onClose();
            return true;
        }
        if (panel.clickControls(x - MARGIN, y - MARGIN, button)) return true;
        if (panel.press(x - MARGIN, y - MARGIN, button)) {
            dragButton = button;
            return true;
        }
        return panel.click(x - MARGIN, y - MARGIN, button) || super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (dragButton == button) {
            dragButton = -1;
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return dragButton == button && panel.drag(button, dx, dy) || super.mouseDragged(x, y, button, dx, dy);
    }

//? if >=1.21.1 {
    /*@Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        return panel.scroll(x - MARGIN, y - MARGIN, dy) || super.mouseScrolled(x, y, dx, dy);
    }
*///?} else {
    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        return panel.scroll(x - MARGIN, y - MARGIN, delta) || super.mouseScrolled(x, y, delta);
    }
//?}

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
