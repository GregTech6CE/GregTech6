/* Copyright (c) 2025 GregTech-6 Team; Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from the twelve original processing controllers and MultiTileEntityBasicMachine.addToolTips. */
package com.gregtech.gregtech.client;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.content.multiblock.OriginalLargeRecipeMachineData;
import com.gregtech.gregtech.content.multiblock.LargeMachineProcessingRules;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.List;
public final class LargeRecipeMachineTooltips {
    private LargeRecipeMachineTooltips() {}
    public static void append(BasicMachineSpec spec,List<Component> lines) {
        add(lines,"gt.lang.structure",ChatFormatting.AQUA).append(":");
        for(var key:OriginalLargeRecipeMachineData.structureKeys(spec.machineName())) add(lines,key,ChatFormatting.WHITE);
        add(lines,"gt.lang.recipes",ChatFormatting.AQUA).append(": ")
                .append(Component.translatable(OriginalLargeRecipeMachineData.recipeKey(spec.machineName())).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(spec.parallelLimit()>1?" (up to "+spec.parallelLimit()+"x processed per run)":"").withStyle(ChatFormatting.WHITE));
        boolean timed=spec.energyType().equals("TU");
        if(!timed)add(lines,"gt.lang.cheap.overclocking",ChatFormatting.YELLOW);
        int efficiency=LargeMachineProcessingRules.efficiency(spec.machineName());
        if(efficiency!=10000)add(lines,"gt.lang.efficiency",ChatFormatting.YELLOW).append(": ")
                .append(Component.literal(efficiency/100+"."+String.format(java.util.Locale.ROOT,"%02d",efficiency%100)+"%").withStyle(ChatFormatting.WHITE));
        if(!timed) {
            String unit=switch(spec.energyType()) {case "RU"->"gt.td.short.energy.kinetic_rotation";case "HU"->"gt.td.short.energy.heat";default->"gt.td.short.energy.electricity";};
            var color=switch(spec.energyType()) {case "RU"->ChatFormatting.GREEN;case "HU"->ChatFormatting.RED;default->ChatFormatting.BLUE;};
            add(lines,"gt.lang.energy.input",ChatFormatting.GREEN).append(": ")
                    .append(Component.literal(spec.energyIn()+" ").withStyle(ChatFormatting.WHITE))
                    .append(Component.translatable(unit).withStyle(color))
                    .append(Component.literal("/t ("+(spec.energyInMin()<=1?"up to ":spec.energyInMin()+" to ")+spec.energyInMax()+")").withStyle(ChatFormatting.WHITE));
        }
        var map=MachineRecipeMaps.byMachineName(spec.machineName());
        if(map.mInputItemsCount>0)io(lines,"gt.lang.item.input",false,spec,ChatFormatting.GREEN);
        if(map.mOutputItemsCount>0)io(lines,"gt.lang.item.output",true,spec,ChatFormatting.RED);
        if(map.mInputFluidCount>0)io(lines,"gt.lang.fluid.input",false,spec,ChatFormatting.GREEN);
        if(map.mOutputFluidCount>0)io(lines,"gt.lang.fluid.output",true,spec,ChatFormatting.RED);
        add(lines,"gt.lang.use.screwdriver.to.toggle",ChatFormatting.DARK_GRAY);
        add(lines,"gt.lang.use.monkey.wrench.to.toggle.auto.outputs",ChatFormatting.DARK_GRAY);
        add(lines,"gt.lang.use.soft.hammer.to.reset",ChatFormatting.DARK_GRAY);
        add(lines,"gt.lang.use.builder.wand.to.ease.building",ChatFormatting.DARK_GRAY);
        add(lines,"gt.lang.use.magnifyingglass.to.detail",ChatFormatting.DARK_GRAY);
        StorageBlockTooltips.facing(lines);
    }
    private static void io(List<Component> lines,String key,boolean output,BasicMachineSpec spec,ChatFormatting color) {
        var line=add(lines,key,color).append(": ");
        line.append(Component.translatable(output?(spec.machineName().equals("largefermenter")?"gt.lang.face.back":"gt.lang.face.bottom"):"gt.lang.face.any").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(output?" (auto, otherwise any)":" (no auto)").withStyle(ChatFormatting.WHITE));
    }
    private static MutableComponent add(List<Component> lines,String key,ChatFormatting color) {
        var line=Component.translatable(key).withStyle(color);lines.add(line);return line;
    }
}
