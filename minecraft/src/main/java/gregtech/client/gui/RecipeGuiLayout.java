package gregtech.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import gregtech.client.RecipeDisplayCaptions;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.engine.recipe.MachineGuiLayout;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.recipe.RecipeSpecialItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Compact presentation shared by JEI and EMI; recipe indices never become compacted indices. */
public final class RecipeGuiLayout {
    public record Slot(boolean input, boolean fluid, int index, int x, int y) {}

    public record Point(int x, int y) {}

    private final List<Slot> slots;
    private final List<Component> captions;
    private final GuiTemplate template;
    private final GuiTemplate.Progress progress;
    private final RecipeMap map;
    private final int width, height, captionY, arrowX, arrowY;
    private final Point special;

    public RecipeGuiLayout(RecipeMap map, Recipe recipe) {
        this.map = map;
        template = GuiTemplates.find(GuiTemplates.forTexture(MachineGuiLayout.texture(map)))
                .orElse(null);
        progress = template != null && template.progress() != null
                ? template.progress()
                : new GuiTemplate.Progress(GuiTemplates.sprite("progress_bar/progress_bar_alloying"), 78, 24);
        var left = side(
                recipe,
                true,
                map == MachineRecipeMaps.Anvil
                        || map == MachineRecipeMaps.AnvilBendBig
                        || map == MachineRecipeMaps.AnvilBendSmall);
        var right = side(recipe, false, false);
        int leftColumns = Math.min(3, left.size()), rightColumns = Math.min(3, right.size());
        int leftWidth = leftColumns * 18, rightWidth = rightColumns * 18;
        int centerWidth = Math.max(18, progress.width());
        int contentWidth = leftWidth + rightWidth + centerWidth + 12;
        width = Math.max(140, contentWidth + 8);
        boolean hasSpecial = !RecipeSpecialItems.display(recipe.mSpecialItems).isEmpty();
        int centerHeight = progress.height() + (hasSpecial ? 20 : 0);
        int contentHeight = Math.max(centerHeight, Math.max((left.size() + 2) / 3, (right.size() + 2) / 3) * 18);
        int startX = (width - contentWidth) / 2;
        arrowX = startX + leftWidth + 6 + (centerWidth - progress.width()) / 2;
        arrowY = 4 + (contentHeight - centerHeight) / 2;
        special = new Point(startX + leftWidth + 6 + (centerWidth - 18) / 2 + 1, arrowY + progress.height() + 3);
        var placed = new ArrayList<Slot>();
        place(placed, left, startX, 4 + (contentHeight - (left.size() + 2) / 3 * 18) / 2, leftColumns);
        place(
                placed,
                right,
                startX + leftWidth + centerWidth + 12,
                4 + (contentHeight - (right.size() + 2) / 3 * 18) / 2,
                rightColumns);
        if (hasSpecial) placed.add(new Slot(true, false, -1, special.x(), special.y()));
        slots = List.copyOf(placed);
        captions = RecipeDisplayCaptions.lines(map, recipe);
        captionY = 4 + contentHeight + (captions.isEmpty() ? 0 : 4);
        height = captionY + captions.size() * 12 + 4;
    }

    private static List<Slot> side(Recipe recipe, boolean input, boolean keepEmpty) {
        var result = new ArrayList<Slot>();
        var items = input ? recipe.mInputs : recipe.mOutputs;
        var fluids = input ? recipe.mFluidInputs : recipe.mFluidOutputs;
        for (int i = 0; i < items.length; i++)
            if (keepEmpty || items[i] != null && !items[i].isEmpty()) result.add(new Slot(input, false, i, 0, 0));
        for (int i = 0; i < fluids.length; i++)
            if (fluids[i] != null && fluids[i].getFluid() != net.minecraft.world.level.material.Fluids.EMPTY)
                result.add(new Slot(input, true, i, 0, 0));
        return result;
    }

    private static void place(List<Slot> target, List<Slot> side, int x, int y, int columns) {
        for (int i = 0; i < side.size(); i++) {
            var slot = side.get(i);
            target.add(new Slot(
                    slot.input(), slot.fluid(), slot.index(), x + i % columns * 18 + 1, y + i / columns * 18 + 1));
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public Point special() {
        return special;
    }

    public Point point(boolean input, boolean fluid, int index) {
        for (var slot : slots)
            if (slot.input() == input && slot.fluid() == fluid && slot.index() == index)
                return new Point(slot.x(), slot.y());
        throw new IllegalArgumentException("No visible recipe slot " + input + "/" + fluid + "/" + index);
    }

    public void draw(GuiGraphics graphics) {
        // Recipe viewers own the surrounding panel; only compose the recipe contents here.
        RenderSystem.enableBlend();
        graphics.setColor(1, 1, 1, 1);
        for (var slot : slots) {
            GuiTemplates.sprite(slot.fluid() ? "slot/fluid" : "slot/item").draw(graphics, slot.x() - 1, slot.y() - 1);
            if (template == null || slot.index() < 0) continue;
            var original = slot.fluid()
                    ? MachineGuiLayout.fluid(slot.input(), slot.index())
                    : MachineGuiLayout.item(
                            slot.input(),
                            slot.index(),
                            slot.input() ? map.mInputItemsCount : map.mOutputItemsCount,
                            slot.input() ? map.mInputFluidCount : map.mOutputFluidCount);
            for (var element : template.elements())
                if (element.x() == original.x()
                        && element.y() == original.y()
                        && element.sprite().width() == 16
                        && element.sprite().height() == 16) element.sprite().draw(graphics, slot.x(), slot.y());
        }
        var level = Minecraft.getInstance().level;
        float fraction = level == null ? 0 : level.getGameTime() % 40 / 40f;
        progress.draw(graphics, arrowX, arrowY, fraction);
        RecipeDisplayCaptions.draw(graphics, captions, 4, captionY, width - 8);
    }
}
