package gregtech.content.item;

import gregtech.engine.fluid.PortableFluidContainerSpec;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.registry.GTFluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

*///?} else {
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.registries.ForgeRegistries;
//?}
import org.jetbrains.annotations.Nullable;
import java.util.List;

/** Finite, persistent fluid storage. Both machine interaction and automation use the same handler. */
public final class PortableFluidContainerItem extends BlockItem implements gregtech.engine.inventory.ContainerShapeLike {
    private final PortableFluidContainerSpec spec;

    public PortableFluidContainerItem(net.minecraft.world.level.block.Block block, PortableFluidContainerSpec spec, Properties properties) {
        super(block, properties);
        this.spec = spec;
    }

    public PortableFluidContainerSpec spec() {
        return spec;
    }

    @Override
    public String shapeId() {
        return spec.shapeId();
    }
//? if >=1.21.1 {
    /*public IFluidHandlerItem handler(ItemStack stack) {
        return new FluidHandlerItemStack(gregtech.content.registry.GTFluidComponents.VESSEL_CONTENTS, stack, spec.capacity()) {
*///?} else {
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidHandlerItemStack(stack, spec.capacity()) {
//?}
            @Override
            public boolean canFillFluidType(FluidStack fluid) {
                return accepts(fluid);
            }

            @Override
            public int getTankCapacity(int tank) {
                return gregtech.content.tool.PortableContainerLimits.capacity(stack, spec);
            }

            @Override
            public int fill(FluidStack fluid, FluidAction action) {
                int availableCapacity = Math.max(0, getTankCapacity(0) - getFluid().getAmount());
                if (availableCapacity == 0 || fluid.isEmpty()) return 0;
//? if >=1.21.1 {
                /*return super.fill(fluid.copyWithAmount(Math.min(availableCapacity, fluid.getAmount())), action);
*///?} else {
                return super.fill(new FluidStack(fluid, Math.min(availableCapacity, fluid.getAmount())), action);
//?}
            }
        };
    }

    public boolean accepts(FluidStack fluid) {
        if (fluid.isEmpty()) return false;
        var type = fluid.getFluid().getFluidType();
        // entryForFluid, not the FluidType key: the three world waters report vanilla water's
        // FluidType (see GTWorldWaterFluid), so the type key would lose their GT6 entry.
        var entry = GTFluids.entryForFluid(fluid.getFluid());
        boolean gas = type.isLighterThanAir();
        boolean acid = false, magic = false, plasma = false;
        if (entry != null) {
            if (entry.hasFlag(RegisteredFluids.FluidFlags.POWER_CONDUCTING)) return false;
            gas |= entry.gas();
            magic = entry.hasFlag(RegisteredFluids.FluidFlags.MAGIC);
            plasma = entry.hasFlag(RegisteredFluids.FluidFlags.PLASMA);
            if (entry.materialKey() != null) {
                var material = GTMaterialRegistry.get(entry.materialKey()).resolve();
                acid = material.has(MaterialProperty.ACID);
                magic |= material.has(MaterialProperty.MAGICAL);
            }
        }
        return spec.accepts(type.getTemperature(fluid), gas, acid, magic, plasma);
    }

    /** Only GT6 cup, jug and thermos implement the food-vessel interfaces. */
    public boolean drinkVessel() {
        return shapeId().equals("cup") || shapeId().equals("jug") || shapeId().equals("thermos");
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        // Fluid transfer is handled first by onItemUseFirst; normal use must remain available to drink.
        if (drinkVessel() && context.getPlayer() != null && !context.getPlayer().isShiftKeyDown())
            return InteractionResult.PASS;
        return super.useOn(context);
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() && drinkVessel() && stack.getCount() == 1) {
            var hit = getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY);
            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                var pos = hit.getBlockPos();
                var fluid = level.getFluidState(pos);
                var container = FluidUtil.getFluidHandler(stack.copyWithCount(1)).orElse(null);
                if (fluid.isSource() && container != null && level.mayInteract(player, pos)
                        && player.mayUseItemAt(pos, hit.getDirection(), stack)
                        && container.fill(new FluidStack(fluid.getType(), 1000),
//? if >=1.21.1 {
                        /*net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE) == 1000) {
*///?} else {
                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE) == 1000) {
//?}
                    if (level.isClientSide) return net.minecraft.world.InteractionResultHolder.success(stack);
                    // The platform transaction removes a source only after the full bucket fits.
                    var pickedUp = FluidUtil.tryPickUpFluid(stack.copyWithCount(1), player, level, pos, hit.getDirection());
                    if (pickedUp.isSuccess())
                        return net.minecraft.world.InteractionResultHolder.success(pickedUp.getResult());
                }
            }
        }
        if (!drinkVessel() || stack.getCount() != 1 || !gregtech.content.food.GTDrinks.canDrink(player,
                FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY)))
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        player.startUsingItem(hand);
        return net.minecraft.world.InteractionResultHolder.consume(stack);
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) {
        var row = gregtech.content.food.GTDrinks.rowFor(FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY));
        return row != null && row.useAction().equals("eat") ? UseAnim.EAT : UseAnim.DRINK;
    }
//? if >=1.21.1 {
    /*@Override public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
*///?} else {
    @Override public int getUseDuration(ItemStack stack) {
//?}
        var row = gregtech.content.food.GTDrinks.rowFor(FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY));
        return Math.max(32, row == null ? 32 : row.foodLevel() * 8);
    }

    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity entity) {
        if (level.isClientSide || !drinkVessel() || stack.getCount() != 1
                || !(entity instanceof net.minecraft.world.entity.player.Player player)) return stack;
        var tank = FluidUtil.getFluidHandler(stack).orElse(null);
        if (tank == null) return stack;
//? if >=1.21.1 {
        /*var action = net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;
*///?} else {
        var action = net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;
//?}
        var portion = tank.drain(250, action);
        if (gregtech.content.food.GTDrinks.drink(player, portion)) {
//? if >=1.21.1 {
            /*tank.drain(250, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
*///?} else {
            tank.drain(250, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
//?}
        }
        return tank.getContainer();
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || !context.getLevel().mayInteract(player, context.getClickedPos())
                || !player.mayUseItemAt(context.getClickedPos(), context.getClickedFace(), stack)) return InteractionResult.PASS;
        if (player.isShiftKeyDown() || context.getLevel().isClientSide) return InteractionResult.PASS;
        var target = context.getLevel().getBlockEntity(context.getClickedPos());
        if (target instanceof gregtech.content.blockentity.machine.MoldBlockEntity
                || target instanceof gregtech.content.blockentity.machine.MoldBasinBlockEntity)
            return InteractionResult.PASS;
        return FluidUtil.interactWithFluidHandler(player, context.getHand(), context.getLevel(),
                context.getClickedPos(), context.getClickedFace()) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
//? if >=1.21.1 {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
*///?} else {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
//?}
        var fluid = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        tooltip.add(gregtech.client.TooltipHelper.stat("tooltip.gt6ce.portable_fluid.contents", net.minecraft.ChatFormatting.AQUA,
                fluid.isEmpty() ? Component.translatable("tooltip.gt6ce.portable_fluid.empty") : fluid.getDisplayName(),
                fluid.getAmount(), gregtech.content.tool.PortableContainerLimits.capacity(stack, spec)));
        if (gregtech.content.tool.PortableContainerLimits.adjustable(spec))
            tooltip.add(Component.translatable("tooltip.gt6ce.portable_fluid.limit_use").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        tooltip.add(gregtech.client.TooltipHelper.stat("tooltip.gt6ce.portable_fluid.temperature", net.minecraft.ChatFormatting.GOLD, spec.maxTemperature()));
        tooltip.add(Component.translatable("tooltip.gt6ce.portable_fluid.use").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
