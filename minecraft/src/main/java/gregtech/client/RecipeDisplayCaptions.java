package gregtech.client;

import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.recipe.RecipePowerStats;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** One source-derived machine caption renderer shared by JEI and EMI. */
public final class RecipeDisplayCaptions {
    private static final String ENERGY_UNIT = "GU";

    private RecipeDisplayCaptions() {}

    public static java.util.List<Component> lines(RecipeMap map, Recipe recipe) {
        var lines = new java.util.ArrayList<Component>();
        boolean informational = map == gregtech.content.data.MachineRecipeMaps.ByProductList
                || map == gregtech.content.data.MachineRecipeMaps.DidYouKnow;
        boolean showTier = !informational && !gregtech.content.recipe.CrucibleDisplayItems.isCrucible(map);
        var stats = RecipePowerStats.of(recipe, map.mPower);
        var visibility = gregtech.engine.recipe.RecipeCaptionVisibility.of(
                recipe.mEUt, map.mCombinePower, map.mShowVoltageAmperage);
        if (recipe.mEUt != 0) {
            lines.add(Component.translatable(
                    stats.generating() ? "gt6ce.jei.gain" : "gt6ce.jei.costs",
                    stats.costs().toString(),
                    ENERGY_UNIT));
            if (visibility.usage())
                lines.add(Component.translatable(
                        stats.generating() ? "gt6ce.jei.output" : "gt6ce.jei.usage",
                        stats.usage().toString(),
                        ENERGY_UNIT));
            if (showTier && visibility.tier())
                lines.add(Component.translatable("gt6ce.jei.tier", stats.tier().toString(), ENERGY_UNIT));
            if (visibility.power() && map != gregtech.content.data.MachineRecipeMaps.Fusion)
                lines.add(Component.translatable("gt6ce.jei.power", stats.power()));
        }
        // Instant maps (the crucibles) convert on contact, so a duration line would be a lie.
        if (!map.mInstantRecipes && !informational)
            lines.add(Component.translatable("gt6ce.jei.time_" + stats.timeUnit(), stats.time()));
        if (map == gregtech.content.data.MachineRecipeMaps.Fusion)
            lines.add(Component.translatable("gt6ce.fusion.startup", recipe.mSpecialValue));
        else if (map.hasSpecialValueLabel() && recipe.mSpecialValue != 0)
            // GT6 NEI special value, e.g. the crucible's "Temperature: 1811 K".
            lines.add(Component.translatable(
                    "gt6ce.jei.special_value",
                    recipe.mSpecialValue * map.mSpecialValueMultiplier,
                    map.mSpecialValuePost.trim()));
        return java.util.List.copyOf(lines);
    }

    public static void draw(GuiGraphics graphics, RecipeMap map, Recipe recipe) {
        draw(graphics, lines(map, recipe), 4, 86, 168);
    }

    public static void draw(GuiGraphics graphics, java.util.List<Component> lines, int x, int y, int width) {
        for (var line : lines) {
            caption(graphics, line, x, y, width);
            y += 12;
        }
    }

    private static void caption(GuiGraphics graphics, Component text, int x, int y, int width) {
        var font = net.minecraft.client.Minecraft.getInstance().font;
        float scale = Math.min(1f, (float) width / Math.max(1, font.width(text)));
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, 0, 0, 0x555555, false);
        graphics.pose().popPose();
    }
}
