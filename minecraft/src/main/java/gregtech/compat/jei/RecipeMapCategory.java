package gregtech.compat.jei;


//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.engine.recipe.*;
import gregtech.client.gui.RecipeGuiLayout;
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

/** Compact shared GUI components with native JEI ingredients and recipe indexing. */
public final class RecipeMapCategory implements IRecipeCategory<Recipe> {
    private final RecipeMap map;
    private IDrawable background;
    private final IDrawable icon;
    private final IGuiHelper helper;
    public RecipeMapCategory(IGuiHelper helper,RecipeMap map,ItemStack icon) {
        this.map=map;
        this.helper=helper;
        background=helper.createBlankDrawable(140,96);
        this.icon=helper.createDrawableItemStack(icon);

    }
    /** JEI fixes dimensions per category; choose the smallest bounds containing its actual rows. */
    public void fitRecipes(java.util.List<Recipe> recipes) {
        int width = 140, height = 26;
        for (var recipe : recipes) {
            var layout = new RecipeGuiLayout(map, recipe);
            width = Math.max(width, layout.width());
            height = Math.max(height, layout.height());
        }
        background = helper.createBlankDrawable(width, height);
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
        var layout = new RecipeGuiLayout(map, recipe);
        addSide(builder,true,recipe.mInputs,recipe.mFluidInputs,recipe,layout);
        addSide(builder,false,recipe.mOutputs,recipe.mFluidOutputs,recipe,layout);
        var special = RecipeSpecialItems.display(recipe.mSpecialItems);
        if (!special.isEmpty()) builder.addSlot(RecipeIngredientRole.CATALYST, layout.special().x(), layout.special().y())
                .setSlotName("special_tool").addItemStacks(special)
                .addTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("gt6ce.jei.catalyst")));
    }
    private void addSide(IRecipeLayoutBuilder builder,boolean input,ItemStack[] items,FluidStack[] fluids,Recipe recipe,RecipeGuiLayout layout) {
        var role=input?RecipeIngredientRole.INPUT:RecipeIngredientRole.OUTPUT;
        for(int i=0;i<items.length;i++) {
            if(items[i]==null||items[i].isEmpty()) continue;
            var point=layout.point(input,false,i);
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
            var point=layout.point(input,true,i);
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
                    tooltip.add(gregtech.engine.fluid.FluidUnits.text("gt6ce.fluid.amount",presence ? 0 : fluid.getAmount())));
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
        new RecipeGuiLayout(map, recipe).draw(graphics);
    }
    @Override public java.util.List<Component> getTooltipStrings(Recipe recipe,IRecipeSlotsView slots,double mouseX,double mouseY) {
        if(map==gregtech.content.data.MachineRecipeMaps.Anvil || map==gregtech.content.data.MachineRecipeMaps.AnvilBendBig || map==gregtech.content.data.MachineRecipeMaps.AnvilBendSmall) {
            for(int i=0;i<recipe.mInputs.length;i++) if(recipe.mInputs[i]==null || recipe.mInputs[i].isEmpty()) {
                var point=new RecipeGuiLayout(map,recipe).point(true,false,i);
                if(mouseX>=point.x()&&mouseX<point.x()+16&&mouseY>=point.y()&&mouseY<point.y()+16)
                    return java.util.List.of(Component.translatable("gt6ce.jei.empty_workpiece"));
            }
        }
        return java.util.List.of();
    }
}
