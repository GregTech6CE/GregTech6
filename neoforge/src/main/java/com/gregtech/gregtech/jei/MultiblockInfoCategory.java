package com.gregtech.gregtech.jei;

import com.gregtech.gregtech.api.multiblock.*;
import com.gregtech.gregtech.content.multiblock.BoilerStructure;
import com.gregtech.gregtech.registry.GTMultiblocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Assembly diagrams are derived from the same geometry used by the controllers. */
public final class MultiblockInfoCategory extends MultiblockInfoData implements IRecipeCategory<MultiblockInfoCategory.Info> {

    public static final RecipeType<Info> TYPE=RecipeType.create("gregtech","multiblock_assembly",Info.class);
    private final IDrawable background,icon,slot;
    private final Map<Info, com.gregtech.gregtech.client.MultiblockPreviewPanel> views = new IdentityHashMap<>();
    private com.gregtech.gregtech.client.MultiblockPreviewPanel view(Info info) {
        return views.computeIfAbsent(info, com.gregtech.gregtech.client.MultiblockPreviewPanel::new);
    }
    public MultiblockInfoCategory(IGuiHelper helper) {
        background=new IDrawable() {
            @Override public int getWidth() {return com.gregtech.gregtech.client.MultiblockPreviewPanel.WIDTH;}
            @Override public int getHeight() {return com.gregtech.gregtech.client.MultiblockPreviewPanel.preferredHeight();}
            @Override public void draw(GuiGraphics graphics,int x,int y) {}
        };
        icon=helper.createDrawableItemStack(new ItemStack(GTMultiblocks.LARGE_BOILER_MAIN.get()));
        slot=helper.getSlotDrawable();
    }


    @Override public RecipeType<Info> getRecipeType() {return TYPE;}
    @Override public Component getTitle() {return Component.translatable("gregtech.jei.assembly");}
    @Override public IDrawable getBackground() {return background;}
    @Override public IDrawable getIcon() {return icon;}
    @Override public void createRecipeExtras(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder builder, Info info, IFocusGroup focuses) {
        var view = new com.gregtech.gregtech.client.MultiblockPreviewPanel(info);
        views.put(info, view);
        builder.addGuiEventListener(new mezz.jei.api.gui.inputs.IJeiGuiEventListener() {
            public net.minecraft.client.gui.navigation.ScreenRectangle getArea() {
                return new net.minecraft.client.gui.navigation.ScreenRectangle(PreviewViewport.LEFT, PreviewViewport.TOP,
                        PreviewViewport.RIGHT - PreviewViewport.LEFT, view.previewBottom() - PreviewViewport.TOP);
            }
            public boolean mouseClicked(double x,double y,int button) { return button==0 || button==1; }
            public boolean mouseReleased(double x,double y,int button) { return button==0 || button==1; }
            public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
                return view.drag(button,dx,dy);
            }
            public boolean mouseScrolled(double x,double y,double delta) { view.scroll(delta); return true; }
        });
    }
    @Override public void setRecipe(IRecipeLayoutBuilder builder,Info info,IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.OUTPUT,1,1).setBackground(slot,-1,-1).addItemStack(info.controller());
        var parts=MultiblockInfoData.parts(info);
        for(int i=0;i<parts.size();i++) builder.addSlot(RecipeIngredientRole.INPUT,21+i*19,1).setBackground(slot,-1,-1).addItemStack(parts.get(i));
    }
    @Override public void draw(Info info,IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY) { view(info).draw(graphics); }
    @Override public boolean handleInput(Info info,double x,double y,com.mojang.blaze3d.platform.InputConstants.Key input) {
        return input.getType()==com.mojang.blaze3d.platform.InputConstants.Type.MOUSE && view(info).click(x,y,input.getValue());
    }
    @Override public List<Component> getTooltipStrings(Info info,IRecipeSlotsView slots,double mouseX,double mouseY) {return view(info).tooltip(mouseY);}
}
