/* Copyright GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * MultiTileEntitySmeltery.addToolTips. Common harvest/blast/material rows are appended once by shared handlers. */
package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.machine.CrucibleSpec;
import com.gregtech.gregtech.api.machine.crucible.CrucibleHazards;
import com.gregtech.gregtech.content.multiblock.OriginalLargeCrucibleParameters;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public final class CrucibleTooltips {
    private CrucibleTooltips() {}
    public static void appendLargeCrucible(CrucibleSpec spec, List<Component> tooltip) {
        tooltip.add(Component.translatable("gt.lang.structure").append(":").withStyle(ChatFormatting.AQUA));
        for (String key : List.of("gt.tooltip.multiblock.crucible.1", "gt.tooltip.multiblock.crucible.2",
                "gt.tooltip.multiblock.crucible.3", "gt.tooltip.multiblock.crucible.4"))
            tooltip.add(Component.translatable(key).withStyle(ChatFormatting.WHITE));
        appendThermal(spec, OriginalLargeCrucibleParameters.meltDownTemperatureK(spec.meltingPointK()), tooltip);
        tooltip.add(Component.translatable("gt.tooltip.multiblock.crucible.5").withStyle(ChatFormatting.WHITE));
        appendHazards(spec, CrucibleHazards.LARGE, tooltip);
        tooltip.add(Component.translatable("gt.lang.use.shovel.to.empty").withStyle(ChatFormatting.DARK_GRAY));
    }
    public static void appendSmeltingCrucible(CrucibleSpec spec, List<Component> tooltip, TooltipFlag flag) {
        appendThermal(spec, spec.meltDownTemperatureK(), tooltip);
        tooltip.add(Component.translatable("gt.tooltip.crucible.1").withStyle(ChatFormatting.WHITE));
        appendHazards(spec, CrucibleHazards.SMALL, tooltip);
        tooltip.add(Component.translatable("gt.lang.use.thermometer.to.measure").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("gt.lang.use.shovel.to.empty").withStyle(ChatFormatting.DARK_GRAY));
    }
    private static void appendThermal(CrucibleSpec spec, long limitK, List<Component> tooltip) {
        tooltip.add(Component.translatable("gt.lang.energy.convert.from").append(" 1 ")
                .append(Component.translatable("gt.td.short.energy.heat")).append(" ")
                .append(Component.translatable("gt.lang.energy.convert.to")).append(" +1 K ")
                .append(Component.translatable("gt.lang.energy.convert.per"))
                .append(" " + CrucibleSpec.KG_PER_ENERGY + "kg (at least " + CrucibleSpec.MIN_HU_PER_TICK
                        + " Units per Tick required!)").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("gt.lang.thermal.mass")
                .append(" " + spec.smeltingThermalMassKg() + " kg").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("gt.lang.hazard.meltdown")
                .append(" (" + limitK + " K)").withStyle(ChatFormatting.DARK_RED));
    }
    private static void appendHazards(CrucibleSpec spec, CrucibleHazards.Profile profile, List<Component> tooltip) {
        if (spec.acidProof()) tooltip.add(Component.translatable("gt.lang.proof.acid").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("gt.lang.hazard.fire").append(" (" + profile.displayedFireRange() + "m)").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("gt.lang.hazard.contact").withStyle(ChatFormatting.DARK_RED));
    }
}
