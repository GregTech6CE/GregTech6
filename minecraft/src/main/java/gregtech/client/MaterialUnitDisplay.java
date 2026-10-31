package gregtech.client;

import gregtech.engine.recipe.MaterialUnitAmount;
import gregtech.engine.recipe.Recipe;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Shared recipe-viewer amount overlay; U is not an inventory stack count. */
public final class MaterialUnitDisplay {
    private MaterialUnitDisplay() {}

    public static void draw(GuiGraphics graphics, int x, int y, long units) {
        var font = Minecraft.getInstance().font;
        String label = MaterialUnitAmount.format(units);
        float scale = Math.min(1f, 17f / Math.max(1, font.width(label)));
        var pose = graphics.pose();
        pose.pushPose();
        // Match item-count decorations so the viewer's slot highlight also covers the unit label.
        pose.translate(x + 17, y + 17, 200);
        pose.scale(scale, scale, 1);
        graphics.drawString(font, label, -font.width(label), -font.lineHeight, 0xFFFFFFFF, true);
        pose.popPose();
    }

    public static List<Component> tooltip(Recipe.MaterialInputDisplay input) {
        return List.of(
                Component.translatable(
                                "gt6ce.recipe.material_units",
                                MaterialUnitAmount.format(input.amount()),
                                MaterialUnitAmount.decimal(input.amount()))
                        .withStyle(ChatFormatting.YELLOW),
                Component.translatable("gt6ce.recipe.material_forms").withStyle(ChatFormatting.GRAY),
                Component.translatable(
                                input.rounding()
                                                == gregtech.api.addon.recipe.MaterialUnitIngredient.Rounding
                                                        .WHOLE_OUTPUTS
                                        ? "gt6ce.recipe.material_batch_whole"
                                        : "gt6ce.recipe.material_batch_exact")
                        .withStyle(ChatFormatting.GRAY));
    }
}
