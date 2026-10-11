package com.gregtech.gregtech.integration.client;
import com.google.gson.JsonObject;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.content.machine.OriginalBasicMachineRules;
import com.gregtech.gregtech.client.CommonBlockTooltips;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.List;
final class BasicMachineSourceDeliveryChecks {
    static JsonObject verify() {
        int count=0,magnetic=0;
        for(var b:BuiltInRegistries.BLOCK)if(b instanceof BasicMachineBlock block&&OriginalBasicMachineRules.handles(block.basicSpec().machineName(),block.basicSpec().tier())) {
            var spec=block.basicSpec();var stack=new ItemStack(b);var lines=stack.getTooltipLines(null,TooltipFlag.NORMAL);
            require(count(lines,"gt.lang.recipes")==1&&count(lines,spec.recipeMap().mNameInternal)==1,"source localized recipe once "+spec.id());
            require(net.minecraft.client.resources.language.I18n.exists(spec.recipeMap().mNameInternal),"recipe language loaded "+spec.id());
            require(count(lines,"gt.lang.energy.input")== (spec.energyType().equals("TU")?0:1),"source TU omission "+spec.id());
            if(spec.energyType().equals("MU")) {
                require(count(lines,"gt.td.short.energy.magnetic")==1&&net.minecraft.client.resources.language.I18n.exists("gt.td.short.energy.magnetic"),"loaded original magnetic unit "+spec.id());
                require(lines.stream().flatMap(c->c.getSiblings().stream()).anyMatch(c->c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("gt.td.short.energy.magnetic")&&c.getStyle().getColor()!=null&&c.getStyle().getColor().getValue()==net.minecraft.ChatFormatting.DARK_GRAY.getColor()),"original TD magnetic dark-gray color "+spec.id());
                magnetic++;
            }
            require(count(lines,"gt.lang.cheap.overclocking")== (OriginalBasicMachineRules.cheapOverclocking(spec.machineName(),spec.tier())?1:0),"actual source cheap flag "+spec.id());
            require(count(lines,"gt.lang.efficiency")== (OriginalBasicMachineRules.efficiency(spec.machineName(),spec.tier())==10000?0:1),"actual source efficiency "+spec.id());
            require(count(lines,"gt.lang.requirement.ignite.fire")== (OriginalBasicMachineRules.requiresIgnition(spec.machineName(),spec.tier())?1:0),"actual source ignition "+spec.id());
            var fc=spec.faceConfig();
            var map=spec.recipeMap();
            String[] ioKeys={"gt.lang.item.input","gt.lang.item.output","gt.lang.fluid.input","gt.lang.fluid.output"};
            int[] slots={map.mInputItemsCount,map.mOutputItemsCount,map.mInputFluidCount,map.mOutputFluidCount};
            int[] masks={fc.itemInputs(),fc.itemOutputs(),fc.fluidInputs(),fc.fluidOutputs()};
            int[] auto={fc.itemAutoInput(),fc.itemAutoOutput(),fc.fluidAutoInput(),fc.fluidAutoOutput()};
            for(int i=0;i<ioKeys.length;i++)require(count(lines,ioKeys[i])==(OriginalBasicMachineRules.io(slots[i],masks[i],auto[i])==null?0:1),"actual conditional source IO "+ioKeys[i]+" "+spec.id());
            require(count(lines,"gt.lang.use.monkey.wrench.to.toggle.auto.inputs")== (fc.itemAutoInput()>=0||fc.fluidAutoInput()>=0?1:0),"conditional source input tools "+spec.id());
            require(count(lines,"gt.lang.use.monkey.wrench.to.toggle.auto.outputs")== (fc.itemAutoOutput()>=0||fc.fluidAutoOutput()>=0?1:0),"conditional source output tools "+spec.id());
            require(count(lines,"tooltip.gregtech.machine.recipes")==0,"no old generic duplicates "+spec.id());
            require(count(stack.getTooltipLines(null,TooltipFlag.ADVANCED),"tooltip.gregtech.contained_materials")==1&&count(lines,"tooltip.gregtech.contained_materials")==0,"one exact advanced material section "+spec.id());count++;
        }
        require(count==247&&magnetic==10,"actual all247 source basic items including10 magnetic processors");var result=new JsonObject();result.addProperty("sourceBasicMachines",count);result.addProperty("magneticMachines",magnetic);result.addProperty("exactAdvancedMaterialSections",count);result.addProperty("scope","installed actual tooltip calls/events at title screen; not player hover or world operations");return result;
    }
    private static long count(List<Component> lines,String key) {return lines.stream().filter(c->CommonBlockTooltips.containsKey(List.of(c),key)).count();}
    private static void require(boolean ok,String message) {if(!ok)throw new IllegalStateException(message);}
}
