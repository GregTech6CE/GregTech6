package gregtech.client;

import gregtech.internal.recipe.Recipe;
import gregtech.internal.recipe.RecipeMap;
import gregtech.internal.recipe.RecipePowerStats;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** One source-derived machine caption renderer shared by JEI and EMI. */
public final class RecipeDisplayCaptions {
    private static final String ENERGY_UNIT = "GU";
    private RecipeDisplayCaptions() {}
    public static void draw(GuiGraphics graphics, RecipeMap map, Recipe recipe) {
        var stats = RecipePowerStats.of(recipe, map.mPower);
        var visibility=gregtech.internal.recipe.RecipeCaptionVisibility.of(recipe.mEUt,map.mCombinePower,map.mShowVoltageAmperage);
        if (recipe.mEUt != 0) {
            caption(graphics, Component.translatable(stats.generating() ? "gt6ce.jei.gain" : "gt6ce.jei.costs", stats.costs().toString(), ENERGY_UNIT), 86);
            if (visibility.usage())
                caption(graphics, Component.translatable(stats.generating() ? "gt6ce.jei.output" : "gt6ce.jei.usage", stats.usage().toString(), ENERGY_UNIT), 98);
            if (visibility.tier()) caption(graphics, Component.translatable("gt6ce.jei.tier", stats.tier().toString(), ENERGY_UNIT), 110);
            if(visibility.power() && map!=gregtech.content.data.MachineRecipeMaps.Fusion) caption(graphics, Component.translatable("gt6ce.jei.power", stats.power()), 122);
        } else if(visibility.tier()) {
            caption(graphics, Component.translatable("gt6ce.jei.tier_unspecified"), 110);
        }
        // Instant maps (the crucibles) convert on contact, so a duration line would be a lie.
        if (!map.mInstantRecipes)
            caption(graphics, Component.translatable("gt6ce.jei.time_" + stats.timeUnit(), stats.time()), 134);
        if(map==gregtech.content.data.MachineRecipeMaps.Fusion)
            caption(graphics,Component.translatable("gt6ce.fusion.startup",recipe.mSpecialValue),122);
        else if(map.hasSpecialValueLabel() && recipe.mSpecialValue!=0)
            // GT6 NEI special value, e.g. the crucible's "Temperature: 1811 K".
            caption(graphics,Component.translatable("gt6ce.jei.special_value",
                    recipe.mSpecialValue*map.mSpecialValueMultiplier,map.mSpecialValuePost.trim()),122);
    }
    private static void caption(GuiGraphics graphics,Component text,int y) {
        var font=net.minecraft.client.Minecraft.getInstance().font;
        float scale=Math.min(1f,168f/Math.max(1,font.width(text)));
        graphics.pose().pushPose();graphics.pose().translate(4,y,0);graphics.pose().scale(scale,scale,1);
        graphics.drawString(font,text,0,0,0x555555,false);graphics.pose().popPose();
    }
}
