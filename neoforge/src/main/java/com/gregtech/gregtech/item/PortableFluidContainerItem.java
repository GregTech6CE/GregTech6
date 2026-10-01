package com.gregtech.gregtech.item;

import com.gregtech.gregtech.api.fluid.PortableFluidContainerSpec;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

import org.jetbrains.annotations.Nullable;
import java.util.List;

/** Finite, persistent fluid storage. Both machine interaction and automation use the same handler. */
public final class PortableFluidContainerItem extends BlockItem implements com.gregtech.gregtech.api.inventory.ContainerShapeLike {
    private final PortableFluidContainerSpec spec;
    public PortableFluidContainerItem(net.minecraft.world.level.block.Block block, PortableFluidContainerSpec spec, Properties properties) {
        super(block, properties);
        this.spec = spec;
    }
    public PortableFluidContainerSpec spec() { return spec; }
    @Override public String shapeId() { return spec.shapeId(); }
    public IFluidHandlerItem handler(ItemStack stack) {
        return new FluidHandlerItemStack(com.gregtech.gregtech.registry.GTFluidComponents.VESSEL_CONTENTS,stack, spec.capacity()) {
            @Override public boolean canFillFluidType(FluidStack fluid) { return accepts(fluid); }
            @Override public int getTankCapacity(int tank) { return com.gregtech.gregtech.content.tool.PortableContainerLimits.capacity(stack,spec); }
            @Override public int fill(FluidStack fluid,FluidAction action) {
                int room=Math.max(0,getTankCapacity(0)-getFluid().getAmount());
                if(room==0||fluid.isEmpty())return 0;
                return super.fill(fluid.copyWithAmount(Math.min(room,fluid.getAmount())),action);
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
            if(entry.hasFlag(RegisteredFluids.FluidFlags.POWER_CONDUCTING))return false;
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
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || !context.getLevel().mayInteract(player, context.getClickedPos())
                || !player.mayUseItemAt(context.getClickedPos(), context.getClickedFace(), stack)) return InteractionResult.PASS;
        if (player.isShiftKeyDown() || context.getLevel().isClientSide) return InteractionResult.PASS;
        return FluidUtil.interactWithFluidHandler(player, context.getHand(), context.getLevel(),
                context.getClickedPos(), context.getClickedFace()) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var fluid = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        tooltip.add(Component.translatable("tooltip.gregtech.portable_fluid.contents",
                fluid.isEmpty() ? Component.translatable("tooltip.gregtech.portable_fluid.empty") : fluid.getDisplayName(),
                fluid.getAmount(), com.gregtech.gregtech.content.tool.PortableContainerLimits.capacity(stack,spec)));
        if(com.gregtech.gregtech.content.tool.PortableContainerLimits.adjustable(spec))
            tooltip.add(Component.translatable("tooltip.gregtech.portable_fluid.limit_use"));
        tooltip.add(Component.translatable("tooltip.gregtech.portable_fluid.temperature", spec.maxTemperature()));
        tooltip.add(Component.translatable("tooltip.gregtech.portable_fluid.use"));
    }
}
