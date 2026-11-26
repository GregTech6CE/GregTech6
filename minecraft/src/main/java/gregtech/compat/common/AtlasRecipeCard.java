package gregtech.compat.common;

import gregtech.client.gui.GuiTemplates;
import gregtech.client.gui.RecipeGuiLayout;
import gregtech.compat.common.MaterialAtlas.*;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public final class AtlasRecipeCard implements TooltipComponent, ClientTooltipComponent {
    private final Preview preview;
    private final RecipeGuiLayout layout;
    private final List<ItemStack> grid;
    private final Component title;
    private final int width, height;
    private final Component hint = MaterialAtlas.label("open_recipe");

    AtlasRecipeCard(Preview preview, boolean oreRoute) {
        this.preview = preview;
        layout = preview instanceof MachineRow row
                ? new RecipeGuiLayout(row.map(), row.recipe(), slot -> oreRoute && row.isHostDust(slot))
                : null;
        grid = preview instanceof Assembly assembly ? assembly.grid() : List.of();
        title = preview instanceof MachineRow row ? row.title() : Component.translatable("container.crafting");
        var font = Minecraft.getInstance().font;
        int contentWidth = Math.max(layout == null ? 114 : layout.width(), font.width(hint));
        width = Math.max(contentWidth, Math.min(180, font.width(title) + 20)) + 10;
        height = 23 + (layout == null ? 54 : layout.height()) + 15;
    }

    public Preview preview() {
        return preview;
    }

    @Override
    public int getWidth(Font font) {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics g) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        GuiTemplates.sprite("base/background").panel(g, 0, 0, width, height);
        var station = preview.workstation();
        if (!station.isEmpty()) g.renderItem(station, 5, 4);
        int titleX = station.isEmpty() ? 5 : 25;
        g.drawString(font, font.substrByWidth(title, width - titleX - 5).getString(), titleX, 8, 0xff393939, false);
        g.drawString(font, hint, 5, height - 12, 0xff505050, false);
        g.pose().translate((width - (layout == null ? 110 : layout.width())) / 2, 22, 0);
        if (preview instanceof MachineRow row) drawRecipe(g, row, layout);
        else if (preview instanceof Assembly assembly) {
            for (int i = 0; i < grid.size(); i++) AtlasPanel.slot(g, grid.get(i), i % 3 * 18, i / 3 * 18);
            g.drawString(font, "→", 66, 23, 0xff393939, false);
            AtlasPanel.slot(g, assembly.output(), 90, 18);
        }
        g.pose().popPose();
    }

    private static void drawRecipe(GuiGraphics g, MachineRow row, RecipeGuiLayout layout) {
        layout.draw(g);
        var r = row.recipe();
        var font = Minecraft.getInstance().font;
        for (var pos : layout.slots()) {
            int i = pos.index();
            var s = pos.fluid()
                    ? MaterialAtlas.fluid((pos.input() ? r.fluidInputs : r.fluidOutputs)[i])
                    : (pos.input() ? r.itemInputs : r.itemOutputs)[i];
            AtlasPanel.item(g, s, pos.x(), pos.y());
            String amount = "";
            if (pos.fluid()) {
                var fluids = pos.input() ? r.fluidInputs : r.fluidOutputs;
                amount = gregtech.engine.fluid.FluidUnits.compact(fluids[i].getAmount());
            } else if (!pos.input() && r.getOutputChance(i) < 10000) amount = (r.getOutputChance(i) / 100.0) + "%";
            if (!amount.isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(pos.x(), pos.y(), 210);
                float size = Math.min(.72f, 17f / font.width(amount));
                g.pose().scale(size, size, 1);
                g.drawString(font, amount, 0, 0, 0xffffffff, true);
                g.pose().popPose();
            }
        }
    }
}
