package com.gregtech.gregtech.item;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.registry.GTFluidType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Item proxy for each GT6 fluid that carries tooltip data and acts as a fluid container. */
public class FluidItem extends Item {
    private final RegisteredFluids.FluidEntry fluidEntry;

    public FluidItem(RegisteredFluids.FluidEntry fluidEntry, Properties properties) {
        super(properties);
        this.fluidEntry = fluidEntry;
    }

    public RegisteredFluids.FluidEntry fluidEntry() {
        return fluidEntry;
    }

    @Override
    public Component getName(ItemStack stack) {
        if(com.gregtech.gregtech.api.fluid.FluidDisplayBinding.hasPayload(stack)) {
            var fluid=com.gregtech.gregtech.api.fluid.FluidDisplayBinding.resolve(stack);
            if(!fluid.isEmpty()) {
                // entryForFluid, not FluidStack#getDisplayName: the three world waters report vanilla
                // water's FluidType (see GTWorldWaterFluid), so their type description is "Water" and
                // the GT6 name of the fluid the payload actually carries would be lost. Resolve the
                // GT6 entry of that fluid instead, exactly what the type description used to give.
                var entry=com.gregtech.gregtech.registry.GTFluids.entryForFluid(fluid.getFluid());
                if(entry==null) return fluid.getDisplayName();
                String path = com.gregtech.gregtech.registry.GTFluids.sanitizePath(entry.registryName());
                return GTFluidType.describe(entry, path);
            }
        }
        String path = RegisteredFluids.sanitizePath(fluidEntry.registryName());
        return GTFluidType.describe(fluidEntry, path);
    }

    /**
     * Drinking, the port of GT6's bottles ({@code MultiItemBottles}, {@code EnumAction.drink}): GT6 gives every
     * drinkable fluid a {@code FoodStatDrink} entry ({@code DrinksGT}), and a bottle is 250&nbsp;mB of it. The
     * port's per-fluid item carries the same fluid, so right-clicking a drink drinks it; the bottle is consumed
     * and an empty glass bottle is handed back, like GT6's bottle. Fluids without a drink entry do nothing.
     */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        net.minecraft.world.level.material.Fluid fluid =
                com.gregtech.gregtech.content.food.GTDrinks.fluidOf(fluidEntry);
        if (com.gregtech.gregtech.content.food.GTDrinks.rowFor(fluid) == null) {
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            com.gregtech.gregtech.content.food.GTDrinks.drink(player, new FluidStack(fluid, 250));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                player.getInventory().add(new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
            }
        }
        return net.minecraft.world.InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        var actual=com.gregtech.gregtech.api.fluid.FluidDisplayBinding.resolve(stack);
        long amount = com.gregtech.gregtech.api.fluid.FluidDisplayBinding.hasPayload(stack) ? actual.getAmount() : 0;
        tooltip.addAll(GTFluidType.describeTooltip(actual, amount, flag.isAdvanced()));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        var entry = fluidEntry;
        if (com.gregtech.gregtech.api.fluid.FluidDisplayBinding.hasPayload(stack)) {
            var actual = com.gregtech.gregtech.api.fluid.FluidDisplayBinding.resolve(stack);
            entry = actual.isEmpty() ? null : com.gregtech.gregtech.registry.GTFluids.entryForFluid(actual.getFluid());
            if (entry == null) return false;
        }
        if (entry.glint()) return true;
        if (entry.registryName().startsWith("potion.")) return true;
        GTMaterial material = resolveMaterial(entry);
        return material.isValid() && (material.has(MaterialProperty.MAGICAL) || material.has(MaterialProperty.ANTIMATTER));
    }

    private GTMaterial resolveMaterial(RegisteredFluids.FluidEntry fluidEntry) {
        if (fluidEntry.materialKey() != null) {
            return GTMaterialRegistry.get(fluidEntry.materialKey()).resolve();
        }
        if (fluidEntry.registryName().startsWith("molten.")) {
            return GTMaterialRegistry.get(fluidEntry.registryName().substring("molten.".length())).resolve();
        }
        return GTMaterialRegistry.get("NULL");
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidHandler(stack);
    }

    /** Per-stack IFluidHandlerItem — creative-only infinite fluid source. Never consumed. */
    private static class FluidHandler implements IFluidHandlerItem, ICapabilityProvider {
        private final ItemStack container;
        private final net.minecraftforge.common.util.LazyOptional<IFluidHandlerItem> holder;

        FluidHandler(ItemStack container) {
            this.container = container;
            this.holder = net.minecraftforge.common.util.LazyOptional.of(() -> this);
        }

        private Fluid resolveFluid() {
            var fluid=com.gregtech.gregtech.api.fluid.FluidDisplayBinding.resolve(container);
            return fluid.isEmpty()?null:fluid.getFluid();
        }

        private FluidStack contents(int amount) {
            var fluid=com.gregtech.gregtech.api.fluid.FluidDisplayBinding.resolve(container);
            if(!fluid.isEmpty()) fluid.setAmount(amount);
            return fluid;
        }

        @Override
        public @NotNull ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() { return 1; }

        @NotNull
        @Override
        public FluidStack getFluidInTank(int tank) {
            Fluid f = resolveFluid();
            if (f == null) return FluidStack.EMPTY;
            return contents(Integer.MAX_VALUE);
        }

        @Override
        public int getTankCapacity(int tank) { return Integer.MAX_VALUE; }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            Fluid f = resolveFluid();
            return f != null && stack.getFluid() == f;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            Fluid f = resolveFluid();
            if (f == null || !com.gregtech.gregtech.api.fluid.FluidDisplayBinding.resolve(container).isFluidEqual(resource)) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @NotNull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) return FluidStack.EMPTY;
            Fluid f = resolveFluid();
            if (f == null) return FluidStack.EMPTY;
            return contents(maxDrain);
        }

        @Override
        public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
                @NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
            if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM) {
                return holder.cast();
            }
            return net.minecraftforge.common.util.LazyOptional.empty();
        }
    }
}
