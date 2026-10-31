package gregtech.compat.emi;

import dev.emi.emi.api.render.EmiRender;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;

/** Explicit viewer aliases: no attempt to rediscover them by scanning every item tag. */
final class MachineEmiIngredient implements EmiIngredient {
    private final List<EmiStack> alternatives;
    private long amount;
    private float chance = 1;
    private EmiStack renderedSource, rendered;
    private long renderedAmount;
    private float renderedChance;

    /** Reuse the current decorated candidate until rotation or amount/chance actually changes. */
    private EmiStack decorated() {
        var source = current();
        if (source != renderedSource || amount != renderedAmount || chance != renderedChance) {
            renderedSource = source;
            renderedAmount = amount;
            renderedChance = chance;
            rendered = source.copy().setAmount(source.getAmount() * amount).setChance(chance);
        }
        return rendered;
    }

    MachineEmiIngredient(List<EmiStack> alternatives, long amount) {
        if (alternatives.isEmpty()) throw new IllegalArgumentException("Empty machine ingredient");
        this.alternatives = List.copyOf(alternatives);
        this.amount = amount;
    }

    @Override
    public List<EmiStack> getEmiStacks() {
        return alternatives;
    }

    @Override
    public EmiIngredient copy() {
        return new MachineEmiIngredient(alternatives, amount).setChance(chance);
    }

    @Override
    public long getAmount() {
        return amount;
    }

    @Override
    public EmiIngredient setAmount(long amount) {
        this.amount = amount;
        return this;
    }

    @Override
    public float getChance() {
        return chance;
    }

    @Override
    public EmiIngredient setChance(float chance) {
        this.chance = chance;
        return this;
    }

    private EmiStack current() {
        return alternatives.get((int) (System.currentTimeMillis() / 1000 % alternatives.size()));
    }

    @Override
    public void render(GuiGraphics draw, int x, int y, float delta, int flags) {
        decorated().render(draw, x, y, delta, flags & ~RENDER_INGREDIENT);
        if ((flags & RENDER_INGREDIENT) != 0) EmiRender.renderIngredientIcon(this, draw, x, y);
    }

    @Override
    public List<ClientTooltipComponent> getTooltip() {
        var tooltip = new ArrayList<ClientTooltipComponent>();
        tooltip.add(ClientTooltipComponent.create(
                Component.translatable("tooltip.emi.accepts").getVisualOrderText()));
        tooltip.addAll(decorated().getTooltip());
        return tooltip;
    }
}
