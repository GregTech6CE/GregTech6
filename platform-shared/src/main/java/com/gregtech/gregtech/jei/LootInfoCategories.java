package com.gregtech.gregtech.jei;
import com.gregtech.gregtech.content.loot.LootViewerData;
import com.gregtech.gregtech.client.LootPageCaptions;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
/** Source pages with native JEI scroll grids; every rule remains indexed as an output. */
public final class LootInfoCategories {
    private LootInfoCategories() {}
    public static final RecipeType<LootViewerData.Group> LOOT = RecipeType.create("gregtech", "loot_tables", LootViewerData.Group.class);
    public static final RecipeType<LootViewerData.Group> MOBS = RecipeType.create("gregtech", "mob_drops", LootViewerData.Group.class);
    public static final class Category implements IRecipeCategory<LootViewerData.Group> {
        private final RecipeType<LootViewerData.Group> type;
        private final IDrawable background, icon, slot;
        public Category(IGuiHelper gui, boolean mobs) {
            type = mobs ? MOBS : LOOT;
            background = gui.createBlankDrawable(LootPageCaptions.WIDTH, LootPageCaptions.HEIGHT);
            icon = gui.createDrawableItemStack(new ItemStack(mobs ? Items.ZOMBIE_SPAWN_EGG : Items.CHEST));
            slot = gui.getSlotDrawable();
        }
        @Override public RecipeType<LootViewerData.Group> getRecipeType() { return type; }
        @Override public Component getTitle() { return Component.translatable("gregtech.jei.category." + type.getUid().getPath()); }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, LootViewerData.Group group, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 3, 19).setBackground(slot, -1, -1).addItemStacks(group.inputs());
            for (var row : group.rows()) builder.addSlot(RecipeIngredientRole.OUTPUT).addItemStack(row.output())
                .addTooltipCallback((view, tooltip) -> tooltip.addAll(LootPageCaptions.lines(row)));
        }
        @Override public void createRecipeExtras(IRecipeExtrasBuilder builder, LootViewerData.Group group, IFocusGroup focuses) {
            builder.addScrollGridWidget(builder.getRecipeSlots().getSlots(RecipeIngredientRole.OUTPUT),
                LootPageCaptions.COLUMNS, LootPageCaptions.VISIBLE_ROWS).setPosition(LootPageCaptions.GRID_X, LootPageCaptions.GRID_Y);
        }
        @Override public void draw(LootViewerData.Group group, IRecipeSlotsView view, GuiGraphics graphics, double x, double y) {
            LootPageCaptions.draw(graphics, group);
        }
    }
}
