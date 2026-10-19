package com.gregtech.gregtech.emi;
import com.gregtech.gregtech.api.fluid.FluidContainerDisplayBinding;
import com.gregtech.gregtech.item.WoodenBucketItem;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import java.util.List;
/** Supplemental lookup only: the displayed result remains the real empty bucket. */
final class WoodenBucketLookupRecipe implements EmiRecipe {
    private final CraftingRecipe recipe;
    private final ItemStack output;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;
    private WoodenBucketLookupRecipe(CraftingRecipe recipe, ItemStack output) {
        this.recipe = recipe; this.output = output;
        inputs = recipe.getIngredients().stream().filter(i -> !i.isEmpty()).map(EmiIngredient::of).toList();
        outputs = FluidContainerDisplayBinding.bucketVariants(output).stream()
                .filter(s -> !((WoodenBucketItem)s.getItem()).content().empty()).map(EmiStack::of).toList();
    }
    static void register(EmiRegistry registry) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return;
        for (var entry : registry.getRecipeManager().getRecipes()) {
//? if >=1.21.1 {
            /*var nativeRecipe = entry.value();
*///?} else {
            var nativeRecipe = entry;
//?}
            if (!(nativeRecipe instanceof CraftingRecipe crafting)) continue;
            var output = crafting.getResultItem(level.registryAccess());
            if (output.getItem() instanceof WoodenBucketItem bucket && bucket.content().empty())
                registry.addRecipe(new WoodenBucketLookupRecipe(crafting, output));
        }
    }
    @Override public EmiRecipeCategory getCategory() { return VanillaEmiRecipeCategories.CRAFTING; }
    @Override public ResourceLocation getId() { return ResourceLocation.fromNamespaceAndPath("gt6ce", "wooden_bucket_lookup/" + ((WoodenBucketItem)output.getItem()).band().id()); }
    @Override public List<EmiIngredient> getInputs() { return inputs; }
    @Override public List<EmiStack> getOutputs() { return outputs; }
    @Override public int getDisplayWidth() { return 118; }
    @Override public int getDisplayHeight() { return 54; }
    @Override public boolean supportsRecipeTree() { return false; }
    @Override public void addWidgets(WidgetHolder widgets) {
        int width = 1;
        while (width < 3 && !recipe.canCraftInDimensions(width, 3)) width++;
        for (int i = 0; i < recipe.getIngredients().size(); i++)
            widgets.addSlot(EmiIngredient.of(recipe.getIngredients().get(i)), i % width * 18, i / width * 18);
        widgets.addSlot(EmiStack.of(output), 92, 18);
    }
}
