package gregtech.client.gui;

//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Uses the original GT6 texture, including its 16-slot store and five tool slots. */
public final class AdvancedCraftingScreen extends MachineContainerScreen<AdvancedCraftingMenu> {

    public AdvancedCraftingScreen(AdvancedCraftingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected net.minecraft.resources.ResourceLocation brandingTexture() {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", (menu.modes() & 32) != 0
                ? "textures/gui/machines/advanced_crafting_table_charging.png"
                : "textures/gui/machines/advanced_crafting_table.png");
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float tick, int x, int y) {
//? if >=1.21.1 {
        /*graphics.blit(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce",
                (menu.modes() & 32) != 0 ? "textures/gui/machines/advanced_crafting_table_charging.png"
                : "textures/gui/machines/advanced_crafting_table.png"), leftPos, topPos, 0, 0, imageWidth, imageHeight);
*///?} else {
        graphics.blit(GregTech.id((menu.modes() & 32) != 0 ? "textures/gui/machines/advanced_crafting_table_charging.png"
                : "textures/gui/machines/advanced_crafting_table.png"), leftPos, topPos, 0, 0, imageWidth, imageHeight);
//?}
        var blueprint = menu.getSlot(0).getItem();
        if (minecraft != null && minecraft.level != null) {
            var pattern = gregtech.content.data.CraftingBlueprintData.readItem(minecraft.level, blueprint);
            for (int i = 0; i < pattern.length; i++) if (!pattern[i].isEmpty() && !menu.getSlot(22 + i).hasItem()) {
                var ghost = pattern[i];
                int sx = leftPos + 80 + i % 3 * 18, sy = topPos + 28 + i / 3 * 18;
                graphics.renderItem(ghost, sx, sy);
                graphics.fill(sx, sy, sx + 16, sy + 16, 0x664080FF);
            }
        }
    }
//? if >=1.21.1 {
    /*@Override
    public void render(GuiGraphics graphics, int x, int y, float tick) {
        super.render(graphics, x, y, tick);
        renderTooltip(graphics, x, y);
    }
*///?} else {
    @Override
    public void render(GuiGraphics graphics, int x, int y, float tick) {
        renderBackground(graphics);
        super.render(graphics, x, y, tick);
        renderTooltip(graphics, x, y);
    }
//?}
    @Override
    protected void renderTooltip(GuiGraphics graphics, int x, int y) {
        if (menu.getCarried().isEmpty() && hoveredSlot != null) {
            int slot = hoveredSlot.index;
            String key = slot == 0 ? "gt.tooltip.advanced_crafting.blueprint" : slot == AdvancedCraftingMenu.FLUSH
                    ? "gt.tooltip.advanced_crafting.flush" : slot == AdvancedCraftingMenu.STORE ? "gt.tooltip.advanced_crafting.store"
                    : slot == AdvancedCraftingMenu.RESULT ? "gt.tooltip.advanced_crafting.craft" : null;
            if (key != null) {
                var stack = hoveredSlot.getItem();
                var lines = new java.util.ArrayList<Component>();
                if (!stack.isEmpty()) {
                    lines.addAll(getTooltipFromContainerItem(stack));
                    lines.add(Component.empty());
                }
                lines.add(Component.translatable(key));
                graphics.renderTooltip(font, lines, stack.getTooltipImage(), x, y);
                return;
            }
        }
        super.renderTooltip(graphics, x, y);
    }
}
