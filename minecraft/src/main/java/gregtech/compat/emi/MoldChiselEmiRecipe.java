package gregtech.compat.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import gregtech.compat.common.MoldChiselDisplay;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class MoldChiselEmiRecipe implements EmiRecipe {
    private final EmiRecipeCategory category;
    private final MoldChiselDisplay.Info info;
    private final List<EmiStack> outputs;

    private MoldChiselEmiRecipe(EmiRecipeCategory category, MoldChiselDisplay.Info info) {
        this.category = category;
        this.info = info;
        outputs = info.outputs().stream().map(EmiStack::of).toList();
    }

    public static void register(EmiRegistry registry) {
        var category =
                new EmiRecipeCategory(
                        ResourceLocation.fromNamespaceAndPath("gt6ce", "mold_chisel"),
                        EmiStack.of(MoldChiselDisplay.chisel())) {
                    @Override
                    public Component getName() {
                        return Component.translatable("gt6ce.jei.category.mold_chisel");
                    }
                };
        registry.addCategory(category);
        for (var mold : MoldChiselDisplay.molds()) registry.addWorkstation(category, EmiStack.of(mold));
        for (var info : MoldChiselDisplay.recipes()) registry.addRecipe(new MoldChiselEmiRecipe(category, info));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", "/mold_chisel/" + info.id());
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.of();
    }

    @Override
    public List<EmiIngredient> getCatalysts() {
        return List.of(EmiStack.of(MoldChiselDisplay.chisel()));
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public boolean hideCraftable() {
        return true;
    }

    @Override
    public int getDisplayWidth() {
        return MoldChiselDisplay.WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return MoldChiselDisplay.HEIGHT;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addDrawable(
                0,
                0,
                getDisplayWidth(),
                getDisplayHeight(),
                (graphics, x, y, delta) -> MoldChiselDisplay.draw(graphics, info));
        widgets.addSlot(
                        EmiStack.of(MoldChiselDisplay.chisel()),
                        MoldChiselDisplay.CHISEL_X - 1,
                        MoldChiselDisplay.CHISEL_Y - 1)
                .drawBack(false)
                .catalyst(true);
        widgets.addSlot(EmiIngredient.of(outputs), MoldChiselDisplay.OUTPUT_X - 1, MoldChiselDisplay.OUTPUT_Y - 1)
                .drawBack(false)
                .recipeContext(this);
        for (int row = 0; row < 5; row++)
            for (int col = 0; col < 5; col++) {
                int x = MoldChiselDisplay.GRID_X + col * MoldChiselDisplay.CELL;
                int y = MoldChiselDisplay.GRID_Y + row * MoldChiselDisplay.CELL;
                widgets.addTooltipText(
                        MoldChiselDisplay.tooltip(info, x, y), x, y, MoldChiselDisplay.CELL, MoldChiselDisplay.CELL);
            }
    }
}
