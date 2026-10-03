package com.gregtech.gregtech.emi;

import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.api.material.MaterialDisplayBinding;
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
    private final List<EmiIngredient> inputs = new ArrayList<>(), catalysts = new ArrayList<>();
    private final List<EmiStack> outputs = new ArrayList<>();

    public MachineEmiRecipe(EmiRecipeCategory category, RecipeMap map, Recipe recipe, ResourceLocation id) {
        this.category = category; this.map = map; this.recipe = recipe; this.id = id;
        for (int i = 0; i < recipe.mInputs.length; i++) {
            var stack = recipe.mInputs[i];
            if (stack == null || stack.isEmpty()) continue;
            (recipe.isCatalystInput(i) ? catalysts : inputs).add(ingredient(stack));
        }
        for (var fluid : recipe.mFluidInputs) if (fluid != null && !fluid.isEmpty()) inputs.add(fluid(fluid));
        for (var special : RecipeSpecialItems.display(recipe.mSpecialItems)) catalysts.add(EmiStack.of(special));
        for (int i = 0; i < recipe.mOutputs.length; i++) {
            var stack = recipe.mOutputs[i];
            if (stack != null && !stack.isEmpty()) outputs.add(EmiStack.of(stack).setChance(recipe.getOutputChance(i) / 10000f));
        }
        for (var fluid : recipe.mFluidOutputs) if (fluid != null && !fluid.isEmpty()) outputs.add(fluid(fluid));
    }
    private static EmiIngredient ingredient(ItemStack stack) {
        var alternatives = new ArrayList<EmiStack>();
        alternatives.add(EmiStack.of(stack));
        for (var alias : MaterialDisplayBinding.alternatives(stack)) alternatives.add(EmiStack.of(alias));
        return EmiIngredient.of(alternatives, stack.getCount());
    }
    private static EmiStack fluid(net.neoforged.neoforge.fluids.FluidStack stack) {
        return EmiStack.of(stack.getFluid(), stack.getComponentsPatch(), stack.getAmount());
    }
    /** Only the visible slot gets the GT icon; indexing and R/U retain the real fluid key/components. */
    private static EmiStack displayFluid(net.neoforged.neoforge.fluids.FluidStack stack) {
        var nativeStack = fluid(stack);
        var client = net.minecraft.client.Minecraft.getInstance();
        if (client.level == null) return nativeStack;
        var display = com.gregtech.gregtech.api.fluid.FluidDisplayBinding.display(client.level.registryAccess(), stack);
        return display.isEmpty() ? nativeStack : new FluidDisplayEmiStack(nativeStack, EmiStack.of(display));
    }
    @Override public EmiRecipeCategory getCategory() { return category; }
    @Override public ResourceLocation getId() { return id; }
    @Override public List<EmiIngredient> getInputs() { return List.copyOf(inputs); }
    @Override public List<EmiIngredient> getCatalysts() { return List.copyOf(catalysts); }
    @Override public List<EmiStack> getOutputs() { return List.copyOf(outputs); }
    @Override public int getDisplayWidth() { return 176; }
    @Override public int getDisplayHeight() { return 146; }
    @Override public void addWidgets(WidgetHolder widgets) {
        widgets.addTexture(MachineGuiLayout.texture(map), 3, 3, 170, 79, 3, 3);
        side(widgets, true, recipe.mInputs, recipe.mFluidInputs);
        side(widgets, false, recipe.mOutputs, recipe.mFluidOutputs);
        var special = RecipeSpecialItems.display(recipe.mSpecialItems);
        if (!special.isEmpty()) widgets.addSlot(EmiIngredient.of(special.stream().map(EmiStack::of).toList()), 79, 42)
                .catalyst(true).appendTooltip(Component.translatable("gregtech.jei.catalyst"));
        widgets.addDrawable(0, 0, 176, 146, (graphics, mouseX, mouseY, delta) ->
                com.gregtech.gregtech.client.RecipeDisplayCaptions.draw(graphics, map, recipe));
    }
    private void side(WidgetHolder widgets, boolean input, ItemStack[] items,
                      net.neoforged.neoforge.fluids.FluidStack[] fluids) {
        int count = input ? map.mInputItemsCount : map.mOutputItemsCount;
        for (int i = 0; i < items.length; i++) {
            if (items[i] == null || items[i].isEmpty()) continue;
            var p = MachineGuiLayout.item(input, i, count, map.mInputFluidCount + map.mOutputFluidCount);
            var slot = widgets.addSlot(input ? ingredient(items[i]) : EmiStack.of(items[i]).setChance(recipe.getOutputChance(i) / 10000f), p.x()-1, p.y()-1);
            if (input && recipe.isCatalystInput(i)) slot.catalyst(true).appendTooltip(Component.translatable("gregtech.jei.catalyst"));
            if (!input) slot.recipeContext(this);
        }
        for (int i = 0; i < fluids.length; i++) {
            if (fluids[i] == null || fluids[i].isEmpty()) continue;
            var p = MachineGuiLayout.fluid(input, i);
            var slot = widgets.addSlot(displayFluid(fluids[i]), p.x()-1, p.y()-1);
            if (!input) slot.recipeContext(this);
        }
    }
}
