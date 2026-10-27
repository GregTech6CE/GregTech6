package gregtech.compat.jei;

import gregtech.compat.common.MoldChiselDisplay;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class MoldChiselCategory implements IRecipeCategory<MoldChiselDisplay.Info> {
    public static final RecipeType<MoldChiselDisplay.Info> TYPE =
            RecipeType.create("gt6ce", "mold_chisel", MoldChiselDisplay.Info.class);
    private final IDrawable background, icon;

    public MoldChiselCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(MoldChiselDisplay.WIDTH, MoldChiselDisplay.HEIGHT);
        icon = helper.createDrawableItemStack(MoldChiselDisplay.chisel());
    }

    @Override public RecipeType<MoldChiselDisplay.Info> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("gt6ce.jei.category.mold_chisel"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, MoldChiselDisplay.Info info, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, MoldChiselDisplay.CHISEL_X, MoldChiselDisplay.CHISEL_Y)
                .addItemStack(MoldChiselDisplay.chisel());
        builder.addSlot(RecipeIngredientRole.OUTPUT, MoldChiselDisplay.OUTPUT_X, MoldChiselDisplay.OUTPUT_Y)
                .addItemStacks(info.outputs());
    }

    @Override public void draw(MoldChiselDisplay.Info info, IRecipeSlotsView slots, GuiGraphics graphics, double x, double y) {
        MoldChiselDisplay.draw(graphics, info);
    }

    @Override public List<Component> getTooltipStrings(MoldChiselDisplay.Info info, IRecipeSlotsView slots, double x, double y) {
        return MoldChiselDisplay.tooltip(info, x, y);
    }
}
