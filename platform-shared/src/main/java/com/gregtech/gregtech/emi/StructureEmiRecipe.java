package com.gregtech.gregtech.emi;

import com.gregtech.gregtech.jei.*;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.*;

/** Actual controller geometry and parts, available without JEI installed. */
public final class StructureEmiRecipe implements EmiRecipe {
    private static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath("gregtech", "multiblock_assembly"),
            EmiStack.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BRICKS))) {
        @Override public Component getName() { return Component.translatable("gregtech.jei.assembly"); }
    };
    private final MultiblockInfoData.Info info;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;
    public StructureEmiRecipe(MultiblockInfoData.Info info) {
        this.info = info;
        inputs = MultiblockInfoData.parts(info).stream().map(stack -> (EmiIngredient)EmiStack.of(stack)).toList();
        outputs = List.of(EmiStack.of(info.controller()));

    }
    public static void register(EmiRegistry registry) {
        registry.addCategory(CATEGORY);
        for (var info : MultiblockInfoData.recipes()) {
            registry.addRecipe(new StructureEmiRecipe(info));
            registry.addWorkstation(CATEGORY, EmiStack.of(info.controller()));
        }
    }
    @Override public EmiRecipeCategory getCategory() { return CATEGORY; }
    @Override public ResourceLocation getId() { return ResourceLocation.fromNamespaceAndPath("gregtech",
            "/structure/" + BuiltInRegistries.ITEM.getKey(info.controller().getItem()).getPath()); }
    @Override public List<EmiIngredient> getInputs() { return inputs; }
    @Override public List<EmiStack> getOutputs() { return outputs; }
    @Override public boolean supportsRecipeTree() { return false; }
    @Override public boolean hideCraftable() { return true; }
    @Override public int getDisplayWidth() { return 176; }
    @Override public int getDisplayHeight() { return com.gregtech.gregtech.client.MultiblockPreviewPanel.preferredHeight(); }
    public MultiblockInfoData.Info info() { return info; }
    @Override public void addWidgets(WidgetHolder widgets) {
        var panel=new com.gregtech.gregtech.client.MultiblockPreviewPanel(info,widgets.getHeight());
        widgets.addSlot(outputs.get(0),0,0).recipeContext(this);
        for(int i=0;i<inputs.size();i++)widgets.addSlot(inputs.get(i),20+i*19,0);
        widgets.add(new EmiInteractiveWidget() {
            @Override public Bounds getBounds() {return new Bounds(0,20,176,panel.height()-20);}
            @Override public void render(net.minecraft.client.gui.GuiGraphics g,int x,int y,float delta) {EmiWidgetInput.rendered(this,g);panel.draw(g);}
            @Override public boolean mouseClicked(int x,int y,int button) {return panel.click(x,y,button);}
            @Override public boolean pressed(double x,double y,int button) {
                return y>=PreviewViewport.TOP&&y<panel.previewBottom()&&(button==0||button==1);
            }
            @Override public boolean dragged(int button,double dx,double dy) {return panel.drag(button,dx,dy);}
            @Override public boolean scrolled(double x,double y,double delta) {if(y<PreviewViewport.TOP||y>=panel.previewBottom())return false;panel.scroll(delta);return true;}
            @Override public java.util.List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> getTooltip(int x,int y) {
                return panel.tooltip(y).stream().map(c->net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(c.getVisualOrderText())).toList();
            }
        });
    }
}
