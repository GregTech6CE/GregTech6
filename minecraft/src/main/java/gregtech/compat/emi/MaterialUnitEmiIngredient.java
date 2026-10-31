package gregtech.compat.emi;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import gregtech.client.MaterialUnitDisplay;
import gregtech.engine.recipe.Recipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

/** Stable unit icon, with real-material aliases retained only for recipe search. */
public final class MaterialUnitEmiIngredient implements EmiIngredient {
    private final EmiStack icon;
    private final List<EmiStack> aliases;
    private final Recipe.MaterialInputDisplay input;
    private final EmiIngredient tag;
    private long amount = 1;
    private float chance = 1;

    MaterialUnitEmiIngredient(
            EmiStack icon, List<EmiStack> aliases, Recipe.MaterialInputDisplay input, EmiIngredient tag) {
        this.icon = icon;
        this.aliases = aliases;
        this.input = input;
        this.tag = tag;
    }

    @Override
    public List<EmiStack> getEmiStacks() {
        return aliases;
    }
    /** Only the uses index substitutes this key; rendering and recipe-tree matching retain aliases. */
    public EmiStack indexKey() {
        return icon;
    }

    public boolean openTag() {
        return input.tag() != null && MaterialUnitTagEmiRecipe.open(input.tag());
    }

    @Override
    public EmiIngredient copy() {
        return new MaterialUnitEmiIngredient(icon, aliases, input, tag)
                .setAmount(amount)
                .setChance(chance);
    }

    @Override
    public long getAmount() {
        return amount;
    }

    @Override
    public EmiIngredient setAmount(long value) {
        amount = value;
        return this;
    }

    @Override
    public float getChance() {
        return chance;
    }

    @Override
    public EmiIngredient setChance(float value) {
        chance = value;
        return this;
    }

    @Override
    public void render(GuiGraphics draw, int x, int y, float delta, int flags) {
        icon.render(draw, x, y, delta, flags & ~RENDER_AMOUNT & ~RENDER_INGREDIENT);
        if ((flags & RENDER_AMOUNT) != 0) MaterialUnitDisplay.draw(draw, x, y, input.amount());
    }

    @Override
    public List<ClientTooltipComponent> getTooltip() {
        var result = new ArrayList<>(icon.getTooltip());
        for (var line : MaterialUnitDisplay.tooltip(input))
            result.add(ClientTooltipComponent.create(line.getVisualOrderText()));
        if (tag != null) result.addAll(tag.getTooltip());
        return result;
    }
}
