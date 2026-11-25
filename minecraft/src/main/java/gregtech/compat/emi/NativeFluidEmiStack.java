package gregtech.compat.emi;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import java.util.ArrayList;
import java.util.List;

/**
 * Native water, lava and third-party fluids stay fluid entries in EMI, but only offer the
 * creative fill carrier while cheat mode is on. Survival keeps the read-only index entry.
 */
final class NativeFluidEmiStack extends EmiStack {
    private final EmiStack fluid;
    NativeFluidEmiStack(EmiStack fluid) {
        this.fluid = fluid;
        amount = fluid.getAmount();
        chance = fluid.getChance();
    }
    @Override public EmiStack copy() {
        var copy = new NativeFluidEmiStack(fluid.copy());
        copy.amount = amount;
        copy.chance = chance;
        copy.comparison = comparison;
        copy.setRemainder(getRemainder().copy());
        return copy;
    }
    @Override public boolean isEmpty() { return fluid.isEmpty(); }
//? if >=1.21.1 {
    /*@Override public net.minecraft.core.component.DataComponentPatch getComponentChanges() { return fluid.getComponentChanges(); }
*///?} else {
    @Override public net.minecraft.nbt.CompoundTag getNbt() { return fluid.getNbt(); }
//?}
    @Override public Object getKey() { return fluid.getKey(); }
    @Override public ResourceLocation getId() { return fluid.getId(); }
    @Override public Component getName() { return fluid.getName(); }
    @Override public void render(GuiGraphics graphics, int x, int y, float delta, int flags) {
        fluid.render(graphics, x, y, delta, flags);
    }
    @Override public List<Component> getTooltipText() { return fluid.getTooltipText(); }

    /**
     * EMI only moves stacks into slots in cheat mode, so the payload carrier is gated the same
     * way; the fluid keeps its own amount and components inside the {@code gt.display_fluid} tag.
     */
    @Override public ItemStack getItemStack() {
        if (!dev.emi.emi.api.EmiApi.isCheatMode() || fluid.isEmpty()
                || !(fluid.getKey() instanceof Fluid key) || key == Fluids.EMPTY) return ItemStack.EMPTY;
//? if >=1.21.1 {
        /*var stack = new net.neoforged.neoforge.fluids.FluidStack(
                net.minecraft.core.registries.BuiltInRegistries.FLUID.wrapAsHolder(key), 1000,
                fluid.getComponentChanges());
*///?} else {
        var stack = new net.minecraftforge.fluids.FluidStack(key, 1000, fluid.getNbt());
//?}
        return gregtech.engine.fluid.FluidDisplayBinding.display(
                gregtech.engine.stack.RegistryContext.current(), stack);
    }

    @Override public List<ClientTooltipComponent> getTooltip() {
        var lines = new ArrayList<>(fluid.getTooltip());
        var player = Minecraft.getInstance().player;
        if (player != null && player.getAbilities().instabuild)
            lines.add(ClientTooltipComponent.create(Component.translatable(
                    gregtech.content.fluid.OriginalFluidDisplayRules.PREFIX + "creative_fill")
                    .withStyle(ChatFormatting.AQUA).getVisualOrderText()));
        return lines;
    }

    /** Bookmarks persist the wrapped native fluid stack, never the carrier item. */
    static final class Serializer implements dev.emi.emi.api.stack.serializer.EmiIngredientSerializer<NativeFluidEmiStack> {
        @Override public String getType() { return "gt6ce:native_fluid_display"; }

        @Override public com.google.gson.JsonElement serialize(NativeFluidEmiStack stack) {
            var json = new com.google.gson.JsonObject();
            json.addProperty("type", getType());
            var value = stack.fluid.copy().setAmount(stack.getAmount()).setChance(stack.getChance());
            value.setRemainder(stack.getRemainder());
            json.add("fluid", dev.emi.emi.api.stack.serializer.EmiIngredientSerializer.getSerialized(value));
            return json;
        }

        @Override public EmiStack deserialize(com.google.gson.JsonElement element) {
            if (!element.isJsonObject()) return EmiStack.EMPTY;
            var json = element.getAsJsonObject();
            if (!json.has("fluid")) return EmiStack.EMPTY;
            var fluid = dev.emi.emi.api.stack.serializer.EmiIngredientSerializer.getDeserialized(json.get("fluid"));
            if (!(fluid instanceof EmiStack value) || value.isEmpty()
                    || !(value.getKey() instanceof Fluid)) return EmiStack.EMPTY;
            var result = new NativeFluidEmiStack(value);
            result.setRemainder(value.getRemainder());
            return result;
        }
    }
}
