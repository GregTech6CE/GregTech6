package gregtech.compat.emi;

import gregtech.engine.recipe.*;
import gregtech.engine.recipe.*;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/** Visible native item/fluid inputs, outputs and non-consumable special tools. */
public final class MachineEmiRecipe implements EmiRecipe {
    private final EmiRecipeCategory category;
    private final RecipeMap map;
    private final Recipe recipe;
    private final ResourceLocation id;
    private final List<EmiIngredient> inputs, catalysts;
    private final List<EmiStack> outputs;
    private final MachineEmiIngredients ingredients;

    public MachineEmiRecipe(EmiRecipeCategory category, RecipeMap map, Recipe recipe, ResourceLocation id,
                            MachineEmiIngredients ingredients) {
        this.category = category; this.map = map; this.recipe = recipe; this.id = id; this.ingredients = ingredients;
        var inputs = new ArrayList<EmiIngredient>(); var catalysts = new ArrayList<EmiIngredient>();
        var outputs = new ArrayList<EmiStack>();
        for (int i = 0; i < recipe.mInputs.length; i++) {
            var stack = recipe.mInputs[i];
            if (stack == null || stack.isEmpty()) continue;
            var ingredient = ingredients.ingredient(recipe,i);
            if (gregtech.content.recipe.CrucibleDisplayItems.isCrucible(map)) {
                var search = new ArrayList<EmiStack>();
                search.add(EmiStack.of(stack.copyWithCount(1)));
                for (var alias : gregtech.content.recipe.CrucibleDisplayItems.aliases(stack)) search.add(EmiStack.of(alias));
                ingredient = new MachineEmiIngredient(search, stack.getCount());
            }
            (recipe.isCatalystInput(i) ? catalysts : inputs).add(ingredient);
        }
        for (int i = 0; i < recipe.mFluidInputs.length; i++) {
            var fluid = recipe.mFluidInputs[i];
            if (fluid == null || fluid.isEmpty()) continue;
            var choices = new ArrayList<EmiStack>();
            choices.add(fluid(fluid));
            for (var container : gregtech.engine.fluid.FluidContainerDisplayBinding.containers(fluid)) choices.add(EmiStack.of(container));
            ((recipe.isCatalystFluidInput(i) || recipe.isPresenceFluidInput(i)) ? catalysts : inputs).add(EmiIngredient.of(choices));
        }
        for (var special : RecipeSpecialItems.display(recipe.mSpecialItems)) catalysts.add(EmiStack.of(special));
        for (int i = 0; i < recipe.mOutputs.length; i++) {
            var stack = recipe.mOutputs[i];
            if (stack != null && !stack.isEmpty()) {
                outputs.add(EmiStack.of(stack).setChance(recipe.getOutputChance(i) / 10000f));
                if (gregtech.content.recipe.CrucibleDisplayItems.isCrucible(map))
                    for (var alias : gregtech.content.recipe.CrucibleDisplayItems.aliases(stack)) outputs.add(EmiStack.of(alias));
            }
        }
        for (var fluid : recipe.mFluidOutputs) if (fluid != null && !fluid.isEmpty()) outputs.add(fluid(fluid));
        this.inputs = List.copyOf(inputs); this.catalysts = List.copyOf(catalysts); this.outputs = List.copyOf(outputs);
    }
//? if >=1.21.1 {
    /*private static EmiStack fluid(net.neoforged.neoforge.fluids.FluidStack stack) {
        return EmiStack.of(stack.getFluid(), stack.getComponentsPatch(), stack.getAmount());
*///?} else {
    private static EmiStack fluid(net.minecraftforge.fluids.FluidStack stack) {
        return EmiStack.of(stack.getFluid(), stack.getTag(), stack.getAmount());
//?}
    }
    /** Only the visible slot gets the GT icon; indexing and R/U retain the real fluid key/components. */
//? if >=1.21.1 {
    /*private static EmiStack displayFluid(net.neoforged.neoforge.fluids.FluidStack stack) {
*///?} else {
    private static EmiStack displayFluid(net.minecraftforge.fluids.FluidStack stack) {
//?}
        var nativeStack = fluid(stack);
        var client = net.minecraft.client.Minecraft.getInstance();
        if (client.level == null) return nativeStack;
//? if >=1.21.1 {
        /*var display = gregtech.engine.fluid.FluidDisplayBinding.display(client.level.registryAccess(), stack);
*///?} else {
        var display = gregtech.engine.fluid.FluidDisplayBinding.display(stack);
//?}
        return display.isEmpty() ? nativeStack : new FluidDisplayEmiStack(nativeStack, EmiStack.of(display));
    }
    @Override public EmiRecipeCategory getCategory() { return category; }
    @Override public ResourceLocation getId() { return id; }
    @Override public List<EmiIngredient> getInputs() { return inputs; }
    @Override public List<EmiIngredient> getCatalysts() { return catalysts; }
    @Override public List<EmiStack> getOutputs() { return outputs; }
    @Override public boolean supportsRecipeTree() { return !recipe.mFakeRecipe; }
    @Override public int getDisplayWidth() { return 176; }
    @Override public int getDisplayHeight() { return Math.max(64,Math.min(146,net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight()-140)); }
    @Override public void addWidgets(WidgetHolder widgets) {
        // EMI may clamp the requested height. Fit drawing and slot hitboxes together.
        float scale=Math.min(1f,Math.min(widgets.getWidth()/176f,widgets.getHeight()/146f));
        float offset=(widgets.getWidth()-176*scale)/2;
        // The new rounded frame still has a white corner at x=3: omit one more column.
        var texture=new dev.emi.emi.api.widget.TextureWidget(MachineGuiLayout.texture(map),4,3,169,79,4,3);
        widgets.addDrawable(0,0,widgets.getWidth(),widgets.getHeight(),(graphics,mouseX,mouseY,delta)->{
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(offset,0,0);graphics.pose().scale(scale,scale,1);
                texture.render(graphics,(int)((mouseX-offset)/scale),(int)(mouseY/scale),delta);
                gregtech.client.gui.MachineGuiChrome.brand(graphics,MachineGuiLayout.texture(map));
                gregtech.client.RecipeDisplayCaptions.draw(graphics,map,recipe);
            } finally {graphics.pose().popPose();}
        });
        side(widgets, true, recipe.mInputs, recipe.mFluidInputs,scale,offset);
        side(widgets, false, recipe.mOutputs, recipe.mFluidOutputs,scale,offset);
        var special = RecipeSpecialItems.display(recipe.mSpecialItems);
        if (!special.isEmpty()) widgets.add(new FittedSlot(EmiIngredient.of(special.stream().map(EmiStack::of).toList()),79,42,scale,offset))
                .catalyst(true).appendTooltip(Component.translatable("gt6ce.jei.catalyst"));
    }
    /** Retain SlotWidget identity, recipe context, shortcuts and tooltips at the fitted coordinates. */
    private static final class FittedSlot extends dev.emi.emi.api.widget.SlotWidget {
        private final float scale,offset;
        private boolean drawing;
        private FittedSlot(EmiIngredient ingredient,int x,int y,float scale,float offset) {
            super(ingredient,x,y);this.scale=scale;this.offset=offset;
        }
        @Override public dev.emi.emi.api.widget.Bounds getBounds() {
            var raw=super.getBounds();if(drawing)return raw;
            int left=(int)Math.floor(offset+raw.x()*scale),top=(int)Math.floor(raw.y()*scale);
            return new dev.emi.emi.api.widget.Bounds(left,top,(int)Math.ceil(offset+raw.right()*scale)-left,(int)Math.ceil(raw.bottom()*scale)-top);
        }
        @Override public void render(net.minecraft.client.gui.GuiGraphics graphics,int mouseX,int mouseY,float delta) {
            graphics.pose().pushPose();drawing=true;
            try {
                graphics.pose().translate(offset,0,0);graphics.pose().scale(scale,scale,1);
                super.render(graphics,(int)Math.floor((mouseX-offset)/scale),(int)Math.floor(mouseY/scale),delta);
            } finally {drawing=false;graphics.pose().popPose();}
        }
    }
    private void side(WidgetHolder widgets, boolean input, ItemStack[] items,
//? if >=1.21.1 {
                      /*net.neoforged.neoforge.fluids.FluidStack[] fluids,float scale,float offset) {
*///?} else {
                      net.minecraftforge.fluids.FluidStack[] fluids,float scale,float offset) {
//?}
        int count = input ? map.mInputItemsCount : map.mOutputItemsCount;
        for (int i = 0; i < items.length; i++) {
            if (items[i] == null || items[i].isEmpty()) continue;
            var p = MachineGuiLayout.item(input, i, count, input ? map.mInputFluidCount : map.mOutputFluidCount);
            int chance = input ? 0 : recipe.getOutputChance(i);
            var slot = widgets.add(new FittedSlot(input ? ingredients.ingredient(recipe,i) : EmiStack.of(items[i]).setChance(chance / 10000f),p.x()-1,p.y()-1,scale,offset));
            if (input && recipe.isCatalystInput(i)) slot.catalyst(true).appendTooltip(Component.translatable("gt6ce.jei.catalyst"));
            if (!input) {
                slot.recipeContext(this);
                if (gregtech.engine.addon.ExclusiveOutputMaps.enabled(map))
                    slot.appendTooltip(Component.translatable("gt6ce.jei.chance", chance / 100.0));
            }
        }
        for (int i = 0; i < fluids.length; i++) {
            if (fluids[i] == null || fluids[i].isEmpty()) continue;
            var p = MachineGuiLayout.fluid(input, i);
            var slot = widgets.add(new FittedSlot(displayFluid(fluids[i]),p.x()-1,p.y()-1,scale,offset));
            if (input && (recipe.isCatalystFluidInput(i) || recipe.isPresenceFluidInput(i)))
                slot.catalyst(true).appendTooltip(Component.translatable(recipe.isPresenceFluidInput(i)
                        ? "gt6ce.jei.fluid_presence" : "gt6ce.jei.catalyst"));
            if (!input) slot.recipeContext(this);
        }
    }
}
