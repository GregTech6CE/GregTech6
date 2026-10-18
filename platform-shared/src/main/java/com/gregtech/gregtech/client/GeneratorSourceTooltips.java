/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * LargeTurbineSteam/Gas/LargeDynamo.addToolTips and original converter parent. */
package com.gregtech.gregtech.client;

import com.gregtech.gregtech.content.multiblock.OriginalGeneratorTooltipData.Converter;
import com.gregtech.gregtech.content.multiblock.OriginalGeneratorTooltipData.Kind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class GeneratorSourceTooltips {
    private GeneratorSourceTooltips() {}
    public static void append(Converter data,List<Component> lines) {
        if(CommonBlockTooltips.containsKey(lines,"gt.lang.structure"))return;
        lines.add(Component.translatable("gt.lang.structure").withStyle(ChatFormatting.AQUA).append(":"));
        var keys=data.structureKeys();
        for(int i=0;i<keys.size();i++) {
            var line=Component.translatable(keys.get(i)).withStyle(data.kind()==Kind.GAS&&i==3?ChatFormatting.GOLD:ChatFormatting.WHITE);
            if(i==0&&data.kind()!=Kind.DYNAMO)line.append(Component.translatable("gt.multitileentity."+data.wallId()));
            lines.add(line);
        }
        if(data.showsInput())energy(lines,"gt.lang.energy.input",data.input(),data.inputMinimum(),data.inputMaximum(),data.inputUnitKey(),ChatFormatting.GREEN,data.kind()==Kind.STEAM?ChatFormatting.GRAY:ChatFormatting.GREEN);
        energy(lines,"gt.lang.energy.output",data.output(),data.outputMinimum(),data.outputMaximum(),data.outputUnitKey(),ChatFormatting.RED,data.kind()==Kind.DYNAMO?ChatFormatting.YELLOW:ChatFormatting.GREEN);
        if(data.kind()==Kind.STEAM)lines.add(Component.translatable("gt.lang.emits.used.steam").withStyle(ChatFormatting.GOLD)
                .append(" (").append(Component.translatable("gt.lang.face.sides")).append(", 95%)"));
        lines.add(Component.translatable("gt.lang.efficiency").withStyle(ChatFormatting.YELLOW).append(": ")
                .append(Component.literal(MachineTooltips.formatEfficiencyPercent(data.efficiency())+"%").withStyle(ChatFormatting.WHITE)));
        lines.add(Component.translatable("gt.lang.use.builder.wand.to.ease.building").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("gt.lang.use.magnifyingglass.to.detail").withStyle(ChatFormatting.DARK_GRAY));
        StorageBlockTooltips.facing(lines);
        // CommonBlockTooltips / material event append source harvest, blast and exact quantities once.
    }
    private static void energy(List<Component> lines,String key,long rec,long min,long max,String unit,ChatFormatting labelColor,ChatFormatting unitColor) {
        lines.add(Component.translatable(key).withStyle(labelColor).append(": ")
                .append(Component.literal(rec+" ").withStyle(ChatFormatting.WHITE))
                .append(Component.translatable(unit).withStyle(unitColor))
                .append(Component.literal("/t ("+(min>1?min+" to ":"up to ")+max+")").withStyle(ChatFormatting.WHITE)));
    }
}
