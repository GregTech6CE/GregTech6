package com.gregtech.gregtech.emi;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
//? if >=1.21.1 {
/*import net.minecraft.core.component.DataComponentPatch;
*///?} else {
import net.minecraft.nbt.CompoundTag;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

/** A visible GT fluid item with the actual fluid key and components for EMI recipe queries. */
final class FluidDisplayEmiStack extends EmiStack {
    private final EmiStack fluid, icon;
    private final boolean indexIcon;
    FluidDisplayEmiStack(EmiStack fluid, EmiStack icon) {
        this(fluid, icon, false);
    }
    FluidDisplayEmiStack(EmiStack fluid, EmiStack icon, boolean indexIcon) {
        this.indexIcon = indexIcon;
        this.fluid = fluid;
        this.icon = icon;
        amount = fluid.getAmount();
        chance = fluid.getChance();
    }
    @Override public EmiStack copy() {
        var copy = new FluidDisplayEmiStack(fluid.copy(), icon.copy(), indexIcon);
        copy.amount = amount;
        copy.chance = chance;
        copy.comparison = comparison;
        copy.setRemainder(getRemainder().copy());
        return copy;
    }
    @Override public boolean isEmpty() { return fluid.isEmpty(); }
//? if >=1.21.1 {
    /*@Override public DataComponentPatch getComponentChanges() { return fluid.getComponentChanges(); }
*///?} else {
    @Override public CompoundTag getNbt() { return fluid.getNbt(); }
//?}
    @Override public Object getKey() { return fluid.getKey(); }
    @Override public ResourceLocation getId() { return indexIcon ? icon.getId() : fluid.getId(); }
    @Override public net.minecraft.world.item.ItemStack getItemStack() { return indexIcon ? icon.getItemStack() : net.minecraft.world.item.ItemStack.EMPTY; }
    @Override public Component getName() { return icon.getName(); }
    @Override public void render(GuiGraphics graphics, int x, int y, float delta, int flags) {
        icon.render(graphics, x, y, delta, flags & ~RENDER_AMOUNT);
    }
    @Override public List<Component> getTooltipText() {
        var lines = new ArrayList<>(icon.getTooltipText());
        lines.add(Component.translatable("gt6ce.fluid.amount", amount));
        return lines;
    }
    @Override public List<ClientTooltipComponent> getTooltip() {
        var lines = new ArrayList<>(icon.getTooltip());
        lines.add(ClientTooltipComponent.create(Component.translatable("gt6ce.fluid.amount", amount).getVisualOrderText()));
        return lines;
    }
}
