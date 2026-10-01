package com.gregtech.gregtech.jei;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.recipe.ToolAssemblyCatalog;
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

import java.util.List;

/**
 * JEI view for the manual tool recipes ({@link com.gregtech.gregtech.recipe.GTToolAssemblyRecipe}).
 *
 * <p>Every GT6 tool is a data recipe without a fixed grid — the head decides the tool's material and
 * the handle only decides the durability — so the vanilla recipe viewer shows the 43
 * {@code data/gregtech/recipes/tools/*.json} entries as "special" items with no ingredients at all.
 * This category shows the actual contract instead: one ingredient slot per required material form,
 * each cycling through every item that satisfies it, and the assembled tool as the output. The rows
 * come from {@link ToolAssemblyCatalog}, which reads the same requirement table the crafting recipe
 * uses, so the viewer cannot drift from what actually crafts the tool.</p>
 */
public final class ToolAssemblyCategory {
    private ToolAssemblyCategory() {}

    public static final RecipeType<ToolAssemblyCatalog.ToolAssemblyInfo> TYPE =
            RecipeType.create(GregTech.MODID, "tool_assembly", ToolAssemblyCatalog.ToolAssemblyInfo.class);

    /** The JEI category: one slot per required form, the tool as output. */
    public static final class Category implements IRecipeCategory<ToolAssemblyCatalog.ToolAssemblyInfo> {
        private final IDrawable background, icon, slot;

        public Category(IGuiHelper gui) {
            this.background = gui.createBlankDrawable(150, 60);
            this.slot = gui.getSlotDrawable();
            this.icon = gui.createDrawableItemStack(GTToolItem.create(GTToolType.WRENCH,
                    GTMaterialRegistry.get("Iron"), GTMaterialRegistry.get("Iron")));
        }

        @Override public RecipeType<ToolAssemblyCatalog.ToolAssemblyInfo> getRecipeType() { return TYPE; }
        @Override public Component getTitle() { return Component.translatable("gregtech.jei.category.tool_assembly"); }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, ToolAssemblyCatalog.ToolAssemblyInfo info,
                              IFocusGroup focuses) {
            int i = 0;
            for (List<ItemStack> stacks : info.inputs()) {
                builder.addSlot(RecipeIngredientRole.INPUT, 1 + i * 18, 1)
                        .setBackground(slot, -1, -1)
                        .addItemStacks(stacks);
                i++;
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, 1 + i * 18 + 18, 1)
                    .setBackground(slot, -1, -1)
                    .addItemStack(info.output());
        }

        @Override
        public void draw(ToolAssemblyCatalog.ToolAssemblyInfo info, IRecipeSlotsView view, GuiGraphics g,
                         double mx, double my) {
            var font = net.minecraft.client.Minecraft.getInstance().font;
            int y = 24;
            if (info.headAssembly()) {
                g.drawString(font, Component.translatable("gregtech.jei.info.tool_head_and_handle"), 1, y, 0xFF404040, false);
            } else {
                g.drawString(font, Component.translatable("gregtech.jei.info.tool_material_forms",
                        info.inputs().size()), 1, y, 0xFF404040, false);
            }
            g.drawString(font, Component.translatable("gregtech.jei.info.tool_any_material"), 1, y + 10, 0xFF707070, false);
        }
    }
}
