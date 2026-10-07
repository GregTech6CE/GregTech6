/* Copyright GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * MultiTileEntitySmeltery.addToolTips. Common harvest/blast/material rows are appended once by shared handlers. */
package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.machine.CrucibleSpec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public final class CrucibleTooltips {
    private CrucibleTooltips() {}
    public static void appendSmeltingCrucible(CrucibleSpec spec, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gt.lang.energy.convert.from").append(" 1 ")
                .append(Component.translatable("gt.td.short.energy.heat")).append(" ")
                .append(Component.translatable("gt.lang.energy.convert.to")).append(" +1 K ")
                .append(Component.translatable("gt.lang.energy.convert.per"))
                .append(" " + CrucibleSpec.KG_PER_ENERGY + "kg (at least " + CrucibleSpec.MIN_HU_PER_TICK
                        + " Units per Tick required!)").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("gt.lang.thermal.mass")
                .append(" " + spec.smeltingThermalMassKg() + " kg").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("gt.lang.hazard.meltdown")
                .append(" (" + spec.meltDownTemperatureK() + " K)").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("gt.tooltip.crucible.1").withStyle(ChatFormatting.WHITE));
        if (spec.acidProof()) tooltip.add(Component.translatable("gt.lang.proof.acid").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("gt.lang.hazard.fire").append(" (4m)").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("gt.lang.hazard.contact").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("gt.lang.use.thermometer.to.measure").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("gt.lang.use.shovel.to.empty").withStyle(ChatFormatting.DARK_GRAY));
    }
}
