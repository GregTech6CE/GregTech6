/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original LargeHeatExchanger/LightningRod plus MultiBlockBase/FacingSingle parent tooltips. */
package com.gregtech.gregtech.client;

import com.gregtech.gregtech.content.multiblock.OriginalUtilityControllerData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.List;

/** Default source descriptions; shared exact material quantities are added by the material event. */
public final class UtilityControllerTooltips {
    private UtilityControllerTooltips() {}
    public static void heat(List<Component> lines) {
        if(CommonBlockTooltips.containsKey(lines,"gt.lang.structure"))return;
        structure(lines,OriginalUtilityControllerData.HEAT_STRUCTURE,false);
        lines.add(Component.translatable("gt.lang.recipes").withStyle(ChatFormatting.AQUA).append(": ")
                .append(Component.translatable(OriginalUtilityControllerData.HEAT_FUEL_KEY).withStyle(ChatFormatting.WHITE)));
        lines.add(Component.translatable("gt.lang.efficiency").withStyle(ChatFormatting.YELLOW).append(": ")
                .append(Component.literal(MachineTooltips.formatEfficiencyPercent(OriginalUtilityControllerData.HEAT_EFFICIENCY)+"%").withStyle(ChatFormatting.WHITE)));
        // The source min/recommended/max are identical, so LH omits the otherwise supplied top-side suffix.
        lines.add(Component.translatable("gt.lang.energy.output").withStyle(ChatFormatting.RED).append(": ")
                .append(Component.literal(OriginalUtilityControllerData.HEAT_RATE+" ").withStyle(ChatFormatting.WHITE))
                .append(Component.translatable("gt.td.short.energy.heat").withStyle(ChatFormatting.RED))
                .append(Component.literal("/t").withStyle(ChatFormatting.WHITE)));
        lines.add(Component.translatable("gt.lang.nogui.funnel.tap.tank").withStyle(ChatFormatting.GOLD));
        parentTools(lines);
    }
    public static void lightning(List<Component> lines) {
        if(CommonBlockTooltips.containsKey(lines,"gt.lang.structure"))return;
        structure(lines,OriginalUtilityControllerData.LIGHTNING_STRUCTURE,true);
        lines.add(Component.translatable("gt.lang.energy.output").withStyle(ChatFormatting.GREEN).append(": ")
                .append(Component.literal(OriginalUtilityControllerData.LIGHTNING_PACKET+" ").withStyle(ChatFormatting.WHITE))
                .append(Component.translatable("gt.td.short.energy.electricity").withStyle(ChatFormatting.BLUE))
                .append(Component.literal("/p (up to "+OriginalUtilityControllerData.LIGHTNING_AMPS+" Amps)").withStyle(ChatFormatting.WHITE)));
        lines.add(Component.literal(OriginalUtilityControllerData.LIGHTNING_CAPACITY+" ").withStyle(ChatFormatting.WHITE)
                .append(Component.translatable("gt.td.short.energy.electricity").withStyle(ChatFormatting.BLUE))
                .append(Component.literal(" per Lightning Strike").withStyle(ChatFormatting.GRAY)));
        parentTools(lines);
    }
    public static void vonDaGraagg(List<Component> lines) {
        if(CommonBlockTooltips.containsKey(lines,"gt.lang.structure"))return;
        lines.add(Component.translatable("gt.lang.structure").withStyle(ChatFormatting.AQUA).append(":"));
        var keys=OriginalUtilityControllerData.VON_DA_GRAAGG_STRUCTURE;
        for(int i=0;i<keys.size();i++)lines.add(Component.translatable(keys.get(i)).withStyle(i==3?ChatFormatting.AQUA:ChatFormatting.WHITE));
        lines.add(Component.translatable("gt.lang.energy.input").withStyle(ChatFormatting.GREEN).append(": ")
                .append(Component.literal(com.gregtech.gregtech.content.multiblock.VonDaGraaggRules.INPUT_RECOMMENDED+" ").withStyle(ChatFormatting.WHITE))
                .append(Component.translatable("gt.td.short.energy.electricity").withStyle(ChatFormatting.BLUE))
                .append(Component.literal("/t ("+com.gregtech.gregtech.content.multiblock.VonDaGraaggRules.INPUT_MIN+" to "+com.gregtech.gregtech.content.multiblock.VonDaGraaggRules.CAPACITY+", ").withStyle(ChatFormatting.WHITE))
                .append(Component.translatable("gt.lang.face.bottom").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(")").withStyle(ChatFormatting.WHITE)));
        parentTools(lines);
    }
    private static void structure(List<Component> lines,List<String> keys,boolean lightning) {
        lines.add(Component.translatable("gt.lang.structure").withStyle(ChatFormatting.AQUA).append(":"));
        for(int i=0;i<keys.size();i++)lines.add(Component.translatable(keys.get(i)).withStyle(
                lightning&&i==7?ChatFormatting.YELLOW:lightning&&i==8?ChatFormatting.GOLD:ChatFormatting.WHITE));
    }
    private static void parentTools(List<Component> lines) {
        lines.add(Component.translatable("gt.lang.use.builder.wand.to.ease.building").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("gt.lang.use.magnifyingglass.to.detail").withStyle(ChatFormatting.DARK_GRAY));
        StorageBlockTooltips.facing(lines);
    }
}
