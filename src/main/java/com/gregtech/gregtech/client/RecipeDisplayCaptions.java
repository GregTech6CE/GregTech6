package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.api.recipe.RecipePowerStats;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** One source-derived machine caption renderer shared by JEI and EMI. */
public final class RecipeDisplayCaptions {
    private static final String ENERGY_UNIT = "GU";
    private RecipeDisplayCaptions() {}
    public static void draw(GuiGraphics graphics, RecipeMap map, Recipe recipe) {
        var stats = RecipePowerStats.of(recipe, map.mPower);
        if (recipe.mEUt != 0) {
            caption(graphics, Component.translatable(stats.generating() ? "gregtech.jei.gain" : "gregtech.jei.costs", stats.costs().toString(), ENERGY_UNIT), 86);
            if (!map.mCombinePower)
                caption(graphics, Component.translatable(stats.generating() ? "gregtech.jei.output" : "gregtech.jei.usage", stats.usage().toString(), ENERGY_UNIT), 98);
            caption(graphics, Component.translatable("gregtech.jei.tier", stats.tier().toString(), ENERGY_UNIT), 110);
            if(map!=com.gregtech.gregtech.data.MachineRecipeMaps.Fusion) caption(graphics, Component.translatable("gregtech.jei.power", stats.power()), 122);
        } else {
            caption(graphics, Component.translatable("gregtech.jei.tier_unspecified"), 110);
        }
        // Instant maps (the crucibles) convert on contact, so a duration line would be a lie.
        if (!map.mInstantRecipes)
            caption(graphics, Component.translatable("gregtech.jei.time_" + stats.timeUnit(), stats.time()), 134);
        if(map==com.gregtech.gregtech.data.MachineRecipeMaps.Fusion)
            caption(graphics,Component.translatable("gregtech.fusion.startup",recipe.mSpecialValue),122);
        else if(map.hasSpecialValueLabel() && recipe.mSpecialValue!=0)
            // GT6 NEI special value, e.g. the crucible's "Temperature: 1811 K".
            caption(graphics,Component.translatable("gregtech.jei.special_value",
                    recipe.mSpecialValue*map.mSpecialValueMultiplier,map.mSpecialValuePost.trim()),122);
    }
    private static void caption(GuiGraphics graphics,Component text,int y) {
        var font=net.minecraft.client.Minecraft.getInstance().font;
        float scale=Math.min(1f,168f/Math.max(1,font.width(text)));
        graphics.pose().pushPose();graphics.pose().translate(4,y,0);graphics.pose().scale(scale,scale,1);
        graphics.drawString(font,text,0,0,0x555555,false);graphics.pose().popPose();
    }
}
