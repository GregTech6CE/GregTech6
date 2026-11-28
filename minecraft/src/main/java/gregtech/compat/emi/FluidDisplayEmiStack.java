package gregtech.compat.emi;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
//? if >=1.21.1 {
/*import net.minecraft.core.component.DataComponentPatch;
import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    @Override public ResourceLocation getId() { return fluid.getId(); }
    @Override public net.minecraft.world.item.ItemStack getItemStack() { return indexIcon ? icon.getItemStack() : net.minecraft.world.item.ItemStack.EMPTY; }
    @Override public Component getName() { return fluid.getName(); }
    @Override public void render(GuiGraphics graphics, int x, int y, float delta, int flags) {
        icon.render(graphics, x, y, delta, flags & ~RENDER_AMOUNT);
    }
    @Override public List<Component> getTooltipText() {
        var type = (net.minecraft.world.level.material.Fluid) fluid.getKey();
//? if >=1.21.1 {
        /*var stack = new FluidStack(type.builtInRegistryHolder(), 1, fluid.getComponentChanges());
*///?} else {
        var stack = new FluidStack(type, 1, fluid.getNbt());
//?}
        return gregtech.client.gui.FluidSlotPresentation.viewerTooltip(stack, amount);
    }
    @Override public List<ClientTooltipComponent> getTooltip() {
        return getTooltipText().stream().map(line -> ClientTooltipComponent.create(line.getVisualOrderText())).toList();
    }

    /** Bookmarks persist the fluid identity and its display icon separately, just like live queries. */
    static final class Serializer implements dev.emi.emi.api.stack.serializer.EmiIngredientSerializer<FluidDisplayEmiStack> {
        @Override public String getType() { return "gt6ce:fluid_display"; }

        @Override public com.google.gson.JsonElement serialize(FluidDisplayEmiStack stack) {
            var json = new com.google.gson.JsonObject();
            json.addProperty("type", getType());
            var value = stack.fluid.copy().setAmount(stack.getAmount()).setChance(stack.getChance());
            value.setRemainder(stack.getRemainder());
            json.add("fluid", dev.emi.emi.api.stack.serializer.EmiIngredientSerializer.getSerialized(value));
            json.add("icon", dev.emi.emi.api.stack.serializer.EmiIngredientSerializer.getSerialized(stack.icon));
            json.addProperty("index_icon", stack.indexIcon);
            return json;
        }

        @Override public EmiStack deserialize(com.google.gson.JsonElement element) {
            if (!element.isJsonObject()) return EmiStack.EMPTY;
            var json = element.getAsJsonObject();
            if (!json.has("fluid") || !json.has("icon")) return EmiStack.EMPTY;
            var fluid = dev.emi.emi.api.stack.serializer.EmiIngredientSerializer.getDeserialized(json.get("fluid"));
            var icon = dev.emi.emi.api.stack.serializer.EmiIngredientSerializer.getDeserialized(json.get("icon"));
            if (!(fluid instanceof EmiStack value) || value.isEmpty()
                    || !(value.getKey() instanceof net.minecraft.world.level.material.Fluid)) return EmiStack.EMPTY;
            if (!(icon instanceof EmiStack image) || image.isEmpty()) return value;
            var result = new FluidDisplayEmiStack(value, image,
                    json.has("index_icon") && json.get("index_icon").getAsBoolean());
            result.setRemainder(value.getRemainder());
            return result;
        }
    }
}
