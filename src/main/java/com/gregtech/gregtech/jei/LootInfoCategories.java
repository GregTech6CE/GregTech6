package com.gregtech.gregtech.jei;

import com.gregtech.gregtech.content.loot.LootViewerData;
import com.gregtech.gregtech.client.LootPageCaptions;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Separate lookup pages for actual GT weighted loot and original mob-drop conditions. */
public final class LootInfoCategories {
    private LootInfoCategories() {}
    public static final RecipeType<LootViewerData.Row> LOOT = RecipeType.create("gregtech", "loot_tables", LootViewerData.Row.class);
    public static final RecipeType<LootViewerData.Row> MOBS = RecipeType.create("gregtech", "mob_drops", LootViewerData.Row.class);
    public static final class Category implements IRecipeCategory<LootViewerData.Row> {
        private final RecipeType<LootViewerData.Row> type;
        private final IDrawable background, icon, slot;
        public Category(IGuiHelper gui, boolean mobs) {
            type = mobs ? MOBS : LOOT;
            background = gui.createBlankDrawable(LootPageCaptions.WIDTH, LootPageCaptions.HEIGHT);
            icon = gui.createDrawableItemStack(new ItemStack(mobs ? Items.ZOMBIE_SPAWN_EGG : Items.CHEST));
            slot = gui.getSlotDrawable();
        }
        @Override public RecipeType<LootViewerData.Row> getRecipeType() { return type; }
        @Override public Component getTitle() { return Component.translatable("gregtech.jei.category." + type.getUid().getPath()); }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, LootViewerData.Row row, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 24, 22).setBackground(slot, -1, -1).addItemStacks(row.inputs());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 126, 22).setBackground(slot, -1, -1).addItemStack(row.output())
                .addTooltipCallback((slot, tooltip) -> tooltip.addAll(LootPageCaptions.lines(row)));
        }
        @Override public void draw(LootViewerData.Row row, IRecipeSlotsView view, GuiGraphics graphics, double x, double y) {
            LootPageCaptions.draw(graphics, row);
        }
    }
}
