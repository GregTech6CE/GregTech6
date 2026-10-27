package gregtech.compat.jei;


//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.engine.recipe.*;
import gregtech.engine.recipe.*;
import gregtech.engine.fluid.FluidDisplayBinding;
//? if >=1.21.1 {
/*import mezz.jei.api.neoforge.NeoForgeTypes;
*///?} else {
import mezz.jei.api.forge.ForgeTypes;
//?}
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
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import java.util.Locale;

/** The actual GT6 machine panel, with the same slots as the live container. */
public final class RecipeMapCategory implements IRecipeCategory<Recipe> {
    private final RecipeMap map;
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
//? if >=1.21.1 {
        /*return RecipeType.create("gt6ce",map.mNameInternal.toLowerCase(Locale.ROOT),Recipe.class);
*///?} else {
        return RecipeType.create(GregTech.NAMESPACE,map.mNameInternal.toLowerCase(Locale.ROOT),Recipe.class);
//?}
    }
    @Override public RecipeType<Recipe> getRecipeType() {return recipeType(map);}
    @Override public Component getTitle() {return Component.translatableWithFallback(map.mNameInternal, map.mNameLocal);}
    @Override public IDrawable getBackground() {return background;}
    @Override public IDrawable getIcon() {return icon;}
    @Override public void setRecipe(IRecipeLayoutBuilder builder,Recipe recipe,IFocusGroup focuses) {
        addSide(builder,true,recipe.mInputs,recipe.mFluidInputs,recipe);
        addSide(builder,false,recipe.mOutputs,recipe.mFluidOutputs,recipe);
        var special = RecipeSpecialItems.display(recipe.mSpecialItems);
        if (!special.isEmpty()) builder.addSlot(RecipeIngredientRole.CATALYST, 80, 43)
                .setSlotName("special_tool").setBackground(slot, -1, -1).addItemStacks(special)
                .addTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("gt6ce.jei.catalyst")));
    }
    private void addSide(IRecipeLayoutBuilder builder,boolean input,ItemStack[] items,FluidStack[] fluids,Recipe recipe) {
        var role=input?RecipeIngredientRole.INPUT:RecipeIngredientRole.OUTPUT;
        int count=input?map.mInputItemsCount:map.mOutputItemsCount;
        for(int i=0;i<items.length;i++) {
            if(items[i]==null||items[i].isEmpty()) continue;
            var point=MachineGuiLayout.item(input,i,count,input ? map.mInputFluidCount : map.mOutputFluidCount);
            var itemSlot=builder.addSlot(role,point.x(),point.y()).setSlotName(role.name()+"_item_"+i);
            var choices=input?recipe.viewerInputAlternatives(i):java.util.List.<ItemStack>of();
            if (input && !recipe.mExactItemInputs && !recipe.hasItemInputGuard(i)) {
                var displayed = choices.isEmpty() ? java.util.List.of(items[i]) : choices;
                var expanded = new java.util.ArrayList<ItemStack>();
                for (var candidate : displayed) {
                    var preferred = gregtech.engine.material.MaterialUnification.canonical(candidate);
                    if (preferred.getItem() != candidate.getItem()) expanded.add(preferred);
                    expanded.add(candidate);
                }
                choices = expanded;
            }
            if(choices.isEmpty())itemSlot.addItemStack(items[i]);else itemSlot.addItemStacks(choices);
            var inputNote=input?recipe.viewerInputNote(i):"";
            if(!inputNote.isEmpty()) itemSlot.addTooltipCallback((view,tooltip)->tooltip.add(
                    Component.translatable(inputNote)));
            var materialAliases=gregtech.content.recipe.CrucibleDisplayItems.isCrucible(map)
                    ? gregtech.content.recipe.CrucibleDisplayItems.aliases(items[i])
                    : gregtech.engine.material.MaterialDisplayBinding.alternatives(items[i]);
            if(!materialAliases.isEmpty())builder.addInvisibleIngredients(role).addItemStacks(materialAliases);
            int chance=recipe.getOutputChance(i);
            boolean showChance=!input&&(chance<10000||gregtech.engine.addon.ExclusiveOutputMaps.enabled(map));
            if(showChance) itemSlot.addTooltipCallback((view,tooltip)->tooltip.add(
                    Component.translatable("gt6ce.jei.chance",chance/100.0).withStyle(net.minecraft.ChatFormatting.YELLOW)));
        }
        for(int i=0;i<fluids.length;i++) {
            var fluid=fluids[i];
            if(fluid==null||fluid.getFluid()==net.minecraft.world.level.material.Fluids.EMPTY) continue;
            boolean presence=input&&recipe.isPresenceFluidInput(i);
            boolean catalyst=input&&(presence||recipe.isCatalystFluidInput(i));
            var fluidRole=catalyst?RecipeIngredientRole.CATALYST:role;
            var shown=fluid;
            var point=MachineGuiLayout.fluid(input,i);
//? if >=1.21.1 {
            /*var display=FluidDisplayBinding.display(net.minecraft.client.Minecraft.getInstance().level.registryAccess(),shown);
*///?} else {
            var display=FluidDisplayBinding.display(shown);
//?}
            var fluidSlot=builder.addSlot(fluidRole,point.x(),point.y()).setSlotName(fluidRole.name()+"_fluid_"+i);
            if (input) {
                var containers = gregtech.engine.fluid.FluidContainerDisplayBinding.containers(shown);
                if (!containers.isEmpty()) builder.addInvisibleIngredients(fluidRole).addItemStacks(containers);
            }
            if(!display.isEmpty()) fluidSlot.addItemStack(display).addTooltipCallback((view,tooltip)->
                    tooltip.add(Component.translatable("gt6ce.fluid.amount",presence ? 0 : fluid.getAmount())));
//? if >=1.21.1 {
            /*else fluidSlot.addIngredient(NeoForgeTypes.FLUID_STACK,shown).setFluidRenderer(Math.max(1000,shown.getAmount()),false,16,16);
            builder.addInvisibleIngredients(fluidRole).addIngredient(NeoForgeTypes.FLUID_STACK,shown);
*///?} else {
            else fluidSlot.addIngredient(ForgeTypes.FLUID_STACK,shown).setFluidRenderer(Math.max(1000,shown.getAmount()),false,16,16);
            builder.addInvisibleIngredients(fluidRole).addIngredient(ForgeTypes.FLUID_STACK,shown);
//?}
            if(catalyst) fluidSlot.addTooltipCallback((view,tooltip)->tooltip.add(Component.translatable(
                    presence ? "gt6ce.jei.fluid_presence" : "gt6ce.jei.catalyst")));
            // Generic creative proxies still find all compatible fluid variants via U/R.
            if(!display.isEmpty()) builder.addInvisibleIngredients(fluidRole).addItemStack(new ItemStack(display.getItem()));
        }
    }
    @Override public void draw(Recipe recipe,IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY) {
        // Crop the rounded frame without moving any item/fluid slot coordinates.
        graphics.blit(texture,4,3,4,3,169,79,256,256);
        gregtech.client.gui.MachineGuiChrome.brand(graphics,texture);
        var client=net.minecraft.client.Minecraft.getInstance();
        long tick=client.level==null?0:client.level.getGameTime();
        graphics.blit(texture,78,24,176,0,(int)(tick%40)*20/40,18,256,256);
        // Draw backgrounds from the same coordinates to cover legacy maps lacking their own original GUI.
        for(boolean input:new boolean[]{true,false}) {
            int count=input?map.mInputItemsCount:map.mOutputItemsCount;
            for(int i=0;i<count;i++) {
                var point=MachineGuiLayout.item(input,i,count,input ? map.mInputFluidCount : map.mOutputFluidCount);
                slot.draw(graphics,point.x()-1,point.y()-1);
            }
            int fluids=input?map.mInputFluidCount:map.mOutputFluidCount;
            for(int i=0;i<fluids;i++) {var point=MachineGuiLayout.fluid(input,i);slot.draw(graphics,point.x()-1,point.y()-1);}
        }
        gregtech.client.RecipeDisplayCaptions.draw(graphics, map, recipe);
    }
    @Override public java.util.List<Component> getTooltipStrings(Recipe recipe,IRecipeSlotsView slots,double mouseX,double mouseY) {
        if(map==gregtech.content.data.MachineRecipeMaps.Anvil || map==gregtech.content.data.MachineRecipeMaps.AnvilBendBig || map==gregtech.content.data.MachineRecipeMaps.AnvilBendSmall) {
            for(int i=0;i<recipe.mInputs.length;i++) if(recipe.mInputs[i]==null || recipe.mInputs[i].isEmpty()) {
                var point=MachineGuiLayout.item(true,i,map.mInputItemsCount,0);
                if(mouseX>=point.x()&&mouseX<point.x()+16&&mouseY>=point.y()&&mouseY<point.y()+16)
                    return java.util.List.of(Component.translatable("gt6ce.jei.empty_workpiece"));
            }
        }
        return java.util.List.of();
    }
}
