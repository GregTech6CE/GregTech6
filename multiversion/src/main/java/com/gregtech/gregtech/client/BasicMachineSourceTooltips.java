/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original BasicMachine.addToolTips/addToolTipsSided and LH.addEnergyToolTips. */
package com.gregtech.gregtech.client;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.content.machine.OriginalBasicMachineRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.List;
public final class BasicMachineSourceTooltips {
    private BasicMachineSourceTooltips() {}
    public static void append(BasicMachineSpec spec,List<Component> lines) {
        appendDefaults(spec,lines,OriginalBasicMachineRules.cheapOverclocking(spec.machineName(),spec.tier()),OriginalBasicMachineRules.efficiency(spec.machineName(),spec.tier()),false,"TU");
    }
    public static void appendDefaults(BasicMachineSpec spec,List<Component> lines,boolean cheap,int efficiency,boolean multiblock,String chargedEnergy) {
        if(CommonBlockTooltips.containsKey(lines,"gt.lang.recipes"))return;
        var map=spec.recipeMap();var fc=spec.faceConfig();
        add(lines,"gt.lang.recipes",ChatFormatting.AQUA).append(": ")
            .append(Component.translatable(map.mNameInternal).withStyle(ChatFormatting.WHITE))
            .append(Component.literal(spec.parallelLimit()>1?" (up to "+spec.parallelLimit()+"x processed per run)":"").withStyle(ChatFormatting.WHITE));
        if(cheap)add(lines,"gt.lang.cheap.overclocking",ChatFormatting.YELLOW);
        if(efficiency!=10000)add(lines,"gt.lang.efficiency",ChatFormatting.YELLOW).append(": ")
            .append(Component.literal(MachineTooltips.formatEfficiencyPercent(efficiency)+"%").withStyle(ChatFormatting.WHITE));
        String energy=spec.energyType().equals("TU")?chargedEnergy:spec.energyType();
        if(!energy.equals("TU")) {
            var line=add(lines,"gt.lang.energy.input",ChatFormatting.GREEN).append(": ")
                .append(Component.literal(spec.energyIn()+" ").withStyle(ChatFormatting.WHITE))
                .append(Component.translatable(OriginalBasicMachineRules.unitKey(energy)).withStyle(energyColor(energy)))
                .append(Component.literal("/t").withStyle(ChatFormatting.WHITE));
            if(spec.energyIn()!=spec.energyInMin()||spec.energyIn()!=spec.energyInMax()) {
                line.append(Component.literal(" ("+(spec.energyInMin()<=1?"up to ":spec.energyInMin()+" to ")+spec.energyInMax()).withStyle(ChatFormatting.WHITE));
                if((fc.energyInputs()&63)!=63)for(int side:OriginalBasicMachineRules.FACE_ORDER)if((fc.energyInputs()&(1<<side))!=0)
                    line.append(", ").append(Component.translatable(OriginalBasicMachineRules.faceKey(side)).withStyle(ChatFormatting.WHITE));
                line.append(")");
            }
        }
        io(lines,"gt.lang.item.input",map.mInputItemsCount,fc.itemInputs(),fc.itemAutoInput(),ChatFormatting.GREEN);
        io(lines,"gt.lang.item.output",map.mOutputItemsCount,fc.itemOutputs(),fc.itemAutoOutput(),ChatFormatting.RED);
        io(lines,"gt.lang.fluid.input",map.mInputFluidCount,fc.fluidInputs(),fc.fluidAutoInput(),ChatFormatting.GREEN);
        io(lines,"gt.lang.fluid.output",map.mOutputFluidCount,fc.fluidOutputs(),fc.fluidAutoOutput(),ChatFormatting.RED);
        if(OriginalBasicMachineRules.requiresIgnition(spec.machineName(),spec.tier()))add(lines,"gt.lang.requirement.ignite.fire",ChatFormatting.GOLD);
        add(lines,"gt.lang.use.screwdriver.to.toggle",ChatFormatting.DARK_GRAY);
        if(validAuto(fc.itemAutoInput())||validAuto(fc.fluidAutoInput()))add(lines,"gt.lang.use.monkey.wrench.to.toggle.auto.inputs",ChatFormatting.DARK_GRAY);
        if(validAuto(fc.itemAutoOutput())||validAuto(fc.fluidAutoOutput()))add(lines,"gt.lang.use.monkey.wrench.to.toggle.auto.outputs",ChatFormatting.DARK_GRAY);
        add(lines,"gt.lang.use.soft.hammer.to.reset",ChatFormatting.DARK_GRAY);
        if(multiblock)add(lines,"gt.lang.use.builder.wand.to.ease.building",ChatFormatting.DARK_GRAY);
        add(lines,"gt.lang.use.magnifyingglass.to.detail",ChatFormatting.DARK_GRAY);
        StorageBlockTooltips.facing(lines);
        // CommonBlockTooltips and MaterialTooltipHandler own harvest/blast and exact composition once.
    }
    private static boolean validAuto(int side) {return side>=0&&side<6;}
    private static ChatFormatting energyColor(String unit) {return switch(unit) {
        case "HU"->ChatFormatting.RED;case "RU"->ChatFormatting.GREEN;case "KU"->ChatFormatting.DARK_GREEN;
        case "CU"->ChatFormatting.AQUA;case "LU"->ChatFormatting.YELLOW;case "QU"->ChatFormatting.DARK_PURPLE;case "MU"->ChatFormatting.DARK_GRAY;
        default->ChatFormatting.BLUE;
    };}
    private static void io(List<Component> lines,String key,int slots,int mask,int auto,ChatFormatting color) {
        var row=OriginalBasicMachineRules.io(slots,mask,auto);if(row==null)return;
        var line=add(lines,key,color).append(": ");
        if(row.any()) {
            line.append(Component.translatable(row.faces().isEmpty()?"gt.lang.face.any":OriginalBasicMachineRules.faceKey(row.faces().get(0).side())).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(row.faces().isEmpty()?" (no auto)":" (auto, otherwise any)").withStyle(ChatFormatting.WHITE));
        } else for(int i=0;i<row.faces().size();i++) {
            if(i>0)line.append(Component.literal(", ").withStyle(ChatFormatting.WHITE));
            var face=row.faces().get(i);line.append(Component.translatable(OriginalBasicMachineRules.faceKey(face.side())).withStyle(ChatFormatting.WHITE));
            if(face.automatic())line.append(Component.literal(" (auto)").withStyle(ChatFormatting.WHITE));
        }
    }
    private static MutableComponent add(List<Component> lines,String key,ChatFormatting color) {
        var line=Component.translatable(key).withStyle(color);lines.add(line);return line;
    }
}
