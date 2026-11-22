/* Copyright GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * MultiTileEntityTank.addToolTips and its structure/facing superclass chain. */
package gregtech.client;

import gregtech.content.multiblock.OriginalTankTooltipData;
import gregtech.content.multiblock.TankValveParameters;
import gregtech.engine.fluid.FluidHazard;
import gregtech.engine.fluid.FluidHazards;
import gregtech.engine.fluid.FluidTankGT;
import gregtech.engine.fluid.MultiblockTankData;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class MultiblockTankTooltips {

    private MultiblockTankTooltips() {}

    public static void append(
            int size,
            TankValveParameters spec,
            ItemStack stack,
            @org.jetbrains.annotations.Nullable net.minecraft.core.HolderLookup.Provider lookup,
            List<Component> lines) {
        add(lines, "gt.lang.structure", ChatFormatting.AQUA).append(":");
        for (var key : OriginalTankTooltipData.structureKeys(size)) add(lines, key, ChatFormatting.WHITE);
        long capacity = spec.capacity();
        var tank = new FluidTankGT(capacity);
        var data = CommonBlockTooltips.blockData(stack);
        MultiblockTankData.readContents(
                tank,
                data,
                capacity,
                lookup == null
                        ? net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(
                                net.minecraft.core.registries.BuiltInRegistries.REGISTRY)
                        : lookup);
        var fluid = tank.getFluidLong();
        if (fluid.isEmpty()) {
            add(lines, "gt.lang.pipe.stats.capacity", ChatFormatting.AQUA)
                    .append(OriginalTankTooltipData.formatNumber(tank.baseCapacity()) + " "
                            + gregtech.engine.fluid.FluidUnits.unit());
        } else {
            lines.add(Component.literal(OriginalTankTooltipData.formatNumber(tank.getAmount()) + " "
                            + gregtech.engine.fluid.FluidUnits.unit() + " of ")
                    .append(fluid.getDisplayName())
                    .append(" (" + (FluidHazards.isGas(fluid.getFluid()) ? "Gaseous" : "Liquid") + "); Max: "
                            + OriginalTankTooltipData.formatNumber(tank.capacity()) + " "
                            + gregtech.engine.fluid.FluidUnits.unit() + ")")
                    .withStyle(ChatFormatting.AQUA));
        }
        add(lines, "gt.lang.nogui.funnel.tap.tank", ChatFormatting.GOLD);
        add(lines, "gt.lang.no.powerconducting.fluids", ChatFormatting.GOLD);
        if (spec != null) {
            if (spec.simpleOnly()) add(lines, "gt.lang.only.simple", ChatFormatting.GOLD);
            if (spec.protections().contains(FluidHazard.GAS)) add(lines, "gt.lang.proof.gas", ChatFormatting.GOLD);
            if (spec.protections().contains(FluidHazard.ACID)) add(lines, "gt.lang.proof.acid", ChatFormatting.GOLD);
            if (spec.protections().contains(FluidHazard.PLASMA))
                add(lines, "gt.lang.proof.plasma", ChatFormatting.GOLD);
            if (spec.protections().contains(FluidHazard.MAGIC)) add(lines, "gt.lang.proof.magic", ChatFormatting.GOLD);
            add(lines, "gt.lang.hazard.meltdown", ChatFormatting.DARK_RED).append(" (" + spec.meltingPoint() + " K)");
        }
        add(lines, "gt.lang.use.builder.wand.to.ease.building", ChatFormatting.DARK_GRAY);
        add(lines, "gt.lang.use.magnifyingglass.to.detail", ChatFormatting.DARK_GRAY);
        StorageBlockTooltips.facing(lines);
    }

    private static net.minecraft.network.chat.MutableComponent add(
            List<Component> lines, String key, ChatFormatting color) {
        var line = gregtech.engine.fluid.FluidUnits.text(key).withStyle(color);
        lines.add(line);
        return line;
    }
}
