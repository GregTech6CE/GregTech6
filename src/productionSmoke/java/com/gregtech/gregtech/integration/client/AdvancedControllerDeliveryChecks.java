package com.gregtech.gregtech.integration.client;
import com.google.gson.JsonObject;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.content.multiblock.OriginalAdvancedControllerData;
import com.gregtech.gregtech.client.CommonBlockTooltips;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.List;
final class AdvancedControllerDeliveryChecks {
    static JsonObject verify() {
        int controllers=0,rows=0;
        for(var block:BuiltInRegistries.BLOCK) {
            var id=BuiltInRegistries.BLOCK.getKey(block);
            if(!id.getNamespace().equals("gregtech"))continue;
            var spec=block instanceof BasicMachineBlock basic&&OriginalAdvancedControllerData.handles(basic.basicSpec().machineName())?basic.basicSpec():id.getPath().equals("implosion_compressor_main")?com.gregtech.gregtech.content.machine.BasicMachineDefinitions.from(com.gregtech.gregtech.content.multiblock.OriginalMultiblockMachineParameters.implosionCompressor()):null;
            if(spec==null)continue;
            require(new ItemStack(block).getHoverName().getString().equals(net.minecraft.client.resources.language.I18n.get("block.gregtech."+id.getPath())),"original localized controller name "+id);
            var stack=new ItemStack(block);var lines=stack.getTooltipLines(null,TooltipFlag.NORMAL);
            require(count(lines,"gt.lang.structure")==1&&count(lines,"gt.lang.recipes")==1&&count(lines,spec.recipeMap().mNameInternal)==1,"advanced source structure/recipes once "+id);
            for(var key:OriginalAdvancedControllerData.structureKeys(spec.machineName())){require(count(lines,key)==1&&net.minecraft.client.resources.language.I18n.exists(key),"source specialized row and loaded language "+key);rows++;}
            require(count(lines,"gt.lang.energy.input")== (spec.machineName().equals("implosioncompressor")?0:1),"source charged LU /QU and hidden TU "+id);
            require(count(lines,"gt.lang.energy.output")==0,"original parent does not add emitted-energy row "+id);
            require(count(lines,"gt.lang.cheap.overclocking")== (spec.machineName().equals("largemassfab")?1:0),"source cheap flag "+id);
            require(count(lines,"gt.lang.efficiency")==0&&count(lines,"gt.lang.use.builder.wand.to.ease.building")==1,"source full efficiency omitted and inherited builder wand "+id);
            require(count(lines,"gt.lang.use.monkey.wrench.to.toggle.auto.inputs")==0&&count(lines,"gt.lang.use.monkey.wrench.to.toggle.auto.outputs")== (spec.machineName().equals("fusionreactor")?0:1),"source conditional auto tools "+id);
            require(count(stack.getTooltipLines(null,TooltipFlag.ADVANCED),"tooltip.gregtech.contained_materials")==1,"one precise advanced material section "+id);controllers++;
        }
        require(controllers==3&&rows==17,"actual three registered controller items /17 specialized rows");
        var result=new JsonObject();result.addProperty("controllers",controllers);result.addProperty("sourceStructureRows",rows);result.addProperty("scope","installed actual tooltip calls/events only; not player hover, natural processing or restart");return result;
    }
    private static long count(List<Component> lines,String key) {return lines.stream().filter(c->CommonBlockTooltips.containsKey(List.of(c),key)).count();}
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
}
