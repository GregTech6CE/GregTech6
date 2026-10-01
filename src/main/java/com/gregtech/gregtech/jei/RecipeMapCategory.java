package com.gregtech.gregtech.jei;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.api.fluid.FluidDisplayBinding;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import java.util.Locale;

/** The actual GT6 machine panel, with the same slots as the live container. */
public final class RecipeMapCategory implements IRecipeCategory<Recipe> {
    private final RecipeMap map;
    private static final String ENERGY_UNIT = "GU";
    private final IDrawable background,icon,slot;
    private final ResourceLocation texture;
    public RecipeMapCategory(IGuiHelper helper,RecipeMap map,ItemStack icon) {
        this.map=map;
        texture=MachineGuiLayout.texture(map);
        background=helper.createBlankDrawable(176,146);
        this.icon=helper.createDrawableItemStack(icon);
        slot=helper.getSlotDrawable();
    }
    public static RecipeType<Recipe> recipeType(RecipeMap map) {
        return RecipeType.create(GregTech.MODID,map.mNameInternal.toLowerCase(Locale.ROOT),Recipe.class);
    }
    @Override public RecipeType<Recipe> getRecipeType() {return recipeType(map);}
    @Override public Component getTitle() {return Component.literal(map.mNameLocal);}
    @Override public IDrawable getBackground() {return background;}
    @Override public IDrawable getIcon() {return icon;}
    @Override public void setRecipe(IRecipeLayoutBuilder builder,Recipe recipe,IFocusGroup focuses) {
        addSide(builder,true,recipe.mInputs,recipe.mFluidInputs,recipe);
        addSide(builder,false,recipe.mOutputs,recipe.mFluidOutputs,recipe);
    }
    private void addSide(IRecipeLayoutBuilder builder,boolean input,ItemStack[] items,FluidStack[] fluids,Recipe recipe) {
        var role=input?RecipeIngredientRole.INPUT:RecipeIngredientRole.OUTPUT;
        int count=input?map.mInputItemsCount:map.mOutputItemsCount;
        for(int i=0;i<items.length;i++) {
            if(items[i]==null||items[i].isEmpty()) continue;
            var point=MachineGuiLayout.item(input,i,count,map.mInputFluidCount+map.mOutputFluidCount);
            var itemSlot=builder.addSlot(role,point.x(),point.y()).setSlotName(role.name()+"_item_"+i).addItemStack(items[i]);
            var materialAliases=com.gregtech.gregtech.api.material.MaterialDisplayBinding.alternatives(items[i]);
            if(!materialAliases.isEmpty())builder.addInvisibleIngredients(role).addItemStacks(materialAliases);
            int chance=recipe.getOutputChance(i);
            if(!input&&chance<10000) itemSlot.addTooltipCallback((view,tooltip)->tooltip.add(
                    Component.translatable("gregtech.jei.chance",chance/100.0).withStyle(net.minecraft.ChatFormatting.YELLOW)));
        }
        for(int i=0;i<fluids.length;i++) {
            var fluid=fluids[i];if(fluid==null||fluid.isEmpty()) continue;
            var point=MachineGuiLayout.fluid(input,i);
            var display=FluidDisplayBinding.display(fluid);
            var fluidSlot=builder.addSlot(role,point.x(),point.y()).setSlotName(role.name()+"_fluid_"+i);
            if(!display.isEmpty()) fluidSlot.addItemStack(display).addTooltipCallback((view,tooltip)->
                    tooltip.add(Component.translatable("gregtech.fluid.amount",fluid.getAmount())));
            else fluidSlot.addIngredient(ForgeTypes.FLUID_STACK,fluid).setFluidRenderer(Math.max(1000,fluid.getAmount()),false,16,16);
            builder.addInvisibleIngredients(role).addIngredient(ForgeTypes.FLUID_STACK,fluid);
            // Generic creative proxies still find all compatible fluid variants via U/R.
            if(!display.isEmpty()) builder.addInvisibleIngredients(role).addItemStack(new ItemStack(display.getItem()));
        }
    }
    @Override public void draw(Recipe recipe,IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY) {
        // Keep the machine slot coordinates, but omit the outer three-pixel GUI frame.
        graphics.blit(texture,3,3,3,3,170,79,256,256);
        var client=net.minecraft.client.Minecraft.getInstance();
        long tick=client.level==null?0:client.level.getGameTime();
        graphics.blit(texture,78,24,176,0,(int)(tick%40)*20/40,18,256,256);
        // Draw backgrounds from the same coordinates to cover legacy maps lacking their own original GUI.
        for(boolean input:new boolean[]{true,false}) {
            int count=input?map.mInputItemsCount:map.mOutputItemsCount;
            for(int i=0;i<count;i++) {
                var point=MachineGuiLayout.item(input,i,count,map.mInputFluidCount+map.mOutputFluidCount);
                slot.draw(graphics,point.x()-1,point.y()-1);
            }
            int fluids=input?map.mInputFluidCount:map.mOutputFluidCount;
            for(int i=0;i<fluids;i++) {var point=MachineGuiLayout.fluid(input,i);slot.draw(graphics,point.x()-1,point.y()-1);}
        }
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
    @Override public java.util.List<Component> getTooltipStrings(Recipe recipe,IRecipeSlotsView slots,double mouseX,double mouseY) {
        if(map==com.gregtech.gregtech.data.MachineRecipeMaps.Anvil || map==com.gregtech.gregtech.data.MachineRecipeMaps.AnvilBendBig || map==com.gregtech.gregtech.data.MachineRecipeMaps.AnvilBendSmall) {
            for(int i=0;i<recipe.mInputs.length;i++) if(recipe.mInputs[i]==null || recipe.mInputs[i].isEmpty()) {
                var point=MachineGuiLayout.item(true,i,map.mInputItemsCount,0);
                if(mouseX>=point.x()&&mouseX<point.x()+16&&mouseY>=point.y()&&mouseY<point.y()+16)
                    return java.util.List.of(Component.translatable("gregtech.jei.empty_workpiece"));
            }
        }
        return java.util.List.of();
    }
    private void caption(GuiGraphics graphics,Component text,int y) {
        var font=net.minecraft.client.Minecraft.getInstance().font;
        float scale=Math.min(1f,168f/Math.max(1,font.width(text)));
        graphics.pose().pushPose();graphics.pose().translate(4,y,0);graphics.pose().scale(scale,scale,1);
        graphics.drawString(font,text,0,0,0x555555,false);graphics.pose().popPose();
    }
}
