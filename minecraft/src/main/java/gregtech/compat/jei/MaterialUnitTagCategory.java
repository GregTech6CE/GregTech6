package gregtech.compat.jei;

import gregtech.compat.common.MaterialUnitTagDisplay;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Paginated tag contents, indexed as both recipes and uses of the unit icon. */
final class MaterialUnitTagCategory implements IRecipeCategory<MaterialUnitTagCategory.Page> {
    static final List<String> FAMILIES = List.of("smeltable", "extrudable");
    private static final int PAGE_SIZE = 45;

    record Page(MaterialUnitTagDisplay.Entry entry, List<ItemStack> members, int number, int total) {}

    private final String family;
    private final IDrawable background, icon, slot;

    MaterialUnitTagCategory(IGuiHelper helper, String family) {
        this.family = family;
        background = helper.createBlankDrawable(162, 126);
        icon = helper.createDrawableItemStack(new ItemStack(Items.IRON_INGOT));
        slot = helper.getSlotDrawable();
    }

    static RecipeType<Page> type(String family) {
        return RecipeType.create("gt6ce", "material_inputs/" + family, Page.class);
    }

    static List<Page> pages(String family, List<MaterialUnitTagDisplay.Entry> entries) {
        var pages = new ArrayList<Page>();
        for (var entry : entries)
            if (entry.family().equals(family)) {
                int total = (entry.members().size() + PAGE_SIZE - 1) / PAGE_SIZE;
                for (int page = 0; page < total; page++)
                    pages.add(new Page(
                            entry,
                            entry.members()
                                    .subList(
                                            page * PAGE_SIZE,
                                            Math.min(
                                                    (page + 1) * PAGE_SIZE,
                                                    entry.members().size())),
                            page + 1,
                            total));
            }
        return pages;
    }

    @Override
    public RecipeType<Page> getRecipeType() {
        return type(family);
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gt6ce.recipe.material_inputs." + family);
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Page page, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1)
                .addItemStack(page.entry().unit())
                .addTooltipCallback((view, lines) ->
                        lines.add(Component.literal("#" + page.entry().tag().location())));
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
                .addItemStack(page.entry().unit());
        for (int i = 0; i < page.members().size(); i++)
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 1 + i % 9 * 18, 37 + i / 9 * 18)
                    .setBackground(slot, -1, -1)
                    .addItemStack(page.members().get(i));
    }

    @Override
    public void draw(Page page, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = net.minecraft.client.Minecraft.getInstance().font;
        String name = font.plainSubstrByWidth(page.entry().unit().getHoverName().getString(), 140);
        graphics.drawString(font, name, 22, 5, 0x404040, false);
        graphics.drawString(font, page.number() + " / " + page.total(), 1, 23, 0x606060, false);
    }
}
