package gregtech.platform.neoforge.fluid;

import gregtech.api.material.*;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.registry.GTFluidType;
import gregtech.engine.material.*;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;

/** Native adapter for fluid display payloads, registered drinks and an inexhaustible source capability. */
public final class FluidDisplayItem extends Item {
    private final RegisteredFluids.FluidEntry fluidEntry;

    public FluidDisplayItem(RegisteredFluids.FluidEntry fluidEntry, Properties properties) {
        super(properties);
        this.fluidEntry = fluidEntry;
    }

    @Override
    public net.minecraft.world.InteractionResult onItemUseFirst(
            ItemStack stack, net.minecraft.world.item.context.UseOnContext context) {
        return gregtech.engine.fluid.FluidDisplayInteraction.useFirst(stack, context);
    }

    public RegisteredFluids.FluidEntry fluidEntry() {
        return fluidEntry;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (gregtech.engine.fluid.FluidDisplayBinding.hasPayload(stack)) {
            var fluid = gregtech.engine.fluid.FluidDisplayBinding.resolveCurrent(stack);
            if (!fluid.isEmpty()) {
                // entryForFluid, not FluidStack#getDisplayName: the three world waters report vanilla
                // water's FluidType (see GTWorldWaterFluid), so their type description is "Water" and
                // the GT6 name of the fluid the payload actually carries would be lost. Resolve the
                // GT6 entry of that fluid instead, exactly what the type description used to give.
                var entry = gregtech.content.registry.GTFluids.entryForFluid(fluid.getFluid());
                if (entry == null) return fluid.getDisplayName();
                String path = gregtech.content.registry.GTFluids.sanitizePath(entry.registryName());
                return GTFluidType.describe(entry, path);
            }
        }
        String path = RegisteredFluids.sanitizePath(fluidEntry.registryName());
        return GTFluidType.describe(fluidEntry, path);
    }

    /**
     * Uses the carrier's registered drink entry; this action does not resolve display payloads.
     * For a noncreative player, consumes one carrier and attempts to add an empty bottle.
     */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(
            net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        net.minecraft.world.level.material.Fluid fluid = gregtech.content.food.GTDrinks.fluidOf(fluidEntry);
        if (gregtech.content.food.GTDrinks.rowFor(fluid) == null) {
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            gregtech.content.food.GTDrinks.drink(player, new FluidStack(fluid, 250));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                player.getInventory().add(new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
            }
        }
        return net.minecraft.world.InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var actual = gregtech.engine.fluid.FluidDisplayBinding.resolve(context.registries(), stack);
        long amount = gregtech.engine.fluid.FluidDisplayBinding.hasPayload(stack) ? actual.getAmount() : 0;
        tooltip.addAll(GTFluidType.describeTooltip(actual, amount, flag.isAdvanced()));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        var entry = fluidEntry;
        if (gregtech.engine.fluid.FluidDisplayBinding.hasPayload(stack)) {
            var actual = gregtech.engine.fluid.FluidDisplayBinding.resolveCurrent(stack);
            entry = actual.isEmpty() ? null : gregtech.content.registry.GTFluids.entryForFluid(actual.getFluid());
            if (entry == null) return false;
        }
        if (entry.glint()) return true;
        if (entry.registryName().startsWith("potion.")) return true;
        GTMaterial material = resolveMaterial(entry);
        return material.isValid()
                && (material.has(MaterialProperty.MAGICAL) || material.has(MaterialProperty.ANTIMATTER));
    }

    private GTMaterial resolveMaterial(RegisteredFluids.FluidEntry fluidEntry) {
        if (fluidEntry.materialKey() != null) {
            return GTMaterialRegistry.get(fluidEntry.materialKey()).resolve();
        }
        if (fluidEntry.registryName().startsWith("molten.")) {
            return GTMaterialRegistry.get(fluidEntry.registryName().substring("molten.".length()))
                    .resolve();
        }
        return GTMaterialRegistry.get("NULL");
    }

    public IFluidHandlerItem handler(ItemStack stack) {
        return new FluidHandler(stack);
    }

    /** Inexhaustible per-stack source; this handler does not check player permissions or consume its carrier. */
    private static class FluidHandler implements IFluidHandlerItem {
        private final ItemStack container;

        FluidHandler(ItemStack container) {
            this.container = container;
        }

        private Fluid resolveFluid() {
            var fluid = gregtech.engine.fluid.FluidDisplayBinding.resolveCurrent(container);
            return fluid.isEmpty() ? null : fluid.getFluid();
        }

        private FluidStack contents(int amount) {
            var fluid = gregtech.engine.fluid.FluidDisplayBinding.resolveCurrent(container);
            if (!fluid.isEmpty()) fluid.setAmount(amount);
            return fluid;
        }

        @Override
        public @NotNull ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @NotNull
        @Override
        public FluidStack getFluidInTank(int tank) {
            Fluid resolvedFluid = resolveFluid();
            if (resolvedFluid == null) return FluidStack.EMPTY;
            return contents(Integer.MAX_VALUE);
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            Fluid resolvedFluid = resolveFluid();
            return resolvedFluid != null && stack.getFluid() == resolvedFluid;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            Fluid resolvedFluid = resolveFluid();
            if (resolvedFluid == null
                    || !FluidStack.isSameFluidSameComponents(
                            gregtech.engine.fluid.FluidDisplayBinding.resolveCurrent(container), resource))
                return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @NotNull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) return FluidStack.EMPTY;
            Fluid resolvedFluid = resolveFluid();
            if (resolvedFluid == null) return FluidStack.EMPTY;
            return contents(maxDrain);
        }
    }
}
