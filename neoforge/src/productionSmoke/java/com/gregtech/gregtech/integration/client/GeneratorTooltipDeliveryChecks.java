package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.client.CommonBlockTooltips;
import com.gregtech.gregtech.content.machine.MachineConstructionMaterials;
import com.gregtech.gregtech.content.multiblock.OriginalGeneratorTooltipData;
import com.gregtech.gregtech.content.multiblock.OriginalGeneratorTooltipData.Kind;
import com.gregtech.gregtech.data.BlockHarvestPolicy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.List;

final class GeneratorTooltipDeliveryChecks {
    static JsonObject verify() {
        int items=0,rows=0;
        for(var data:OriginalGeneratorTooltipData.ALL)for(var path:List.of(data.sourcePath(),data.legacyPath())) {
            var id=ResourceLocation.parse("gregtech:"+path);
            require(BuiltInRegistries.ITEM.containsKey(id),"source generator item exists "+id);
            var stack=new ItemStack(BuiltInRegistries.ITEM.get(id));
            var block=((BlockItem)stack.getItem()).getBlock();
            var lines=stack.getTooltipLines(Item.TooltipContext.EMPTY,null,TooltipFlag.NORMAL);
            require(count(lines,"gt.lang.structure")==1,"source structure once "+id);
            for(var key:data.structureKeys()) {require(count(lines,key)==1&&net.minecraft.client.resources.language.I18n.exists(key),"loaded source structure row "+key);rows++;}
            require(count(lines,"gt.lang.energy.input")== (data.showsInput()?1:0)&&count(lines,"gt.lang.energy.output")==1,"source conditional energy rows "+id);
            require(count(lines,data.outputUnitKey())==1&&(!data.showsInput()||count(lines,data.inputUnitKey())==1),"source localized energy units "+id);
            require(count(lines,"gt.lang.efficiency")==1&&lines.stream().anyMatch(c->c.getString().contains(data.kind()==Kind.DYNAMO?"75.00%":"66.66%")),"source efficiency truncation "+id);
            require(count(lines,"gt.lang.emits.used.steam")== (data.kind()==Kind.STEAM?1:0),"source used-steam row only on steam variant "+id);
            require(count(lines,"gt.lang.use.builder.wand.to.ease.building")==1&&count(lines,"gt.lang.use.magnifyingglass.to.detail")==1&&count(lines,"gt.lang.use.x.to.toggle.facing.pre")==1,"source parent tools once "+id);
            require(count(lines,"gt.tooltip.axial.steam")==0&&count(lines,"gt.tooltip.axial.dynamo")==0&&count(lines,"gt.tooltip.axial.control")==0,"old generic rows replaced "+id);
            require(BlockHarvestPolicy.source(block).orElseThrow().sourceId()==data.originalId(),"source harvest identity on actual block "+id);
            var expected=MachineConstructionMaterials.block(path).orElseThrow();
            require(ItemMaterialRegistry.get(stack).orElseThrow().components().equals(expected.components()),"actual source controller material registration "+id);
            require(count(stack.getTooltipLines(Item.TooltipContext.EMPTY,null,TooltipFlag.ADVANCED),"tooltip.gregtech.contained_materials")==1,"precise quantities shown once in advanced mode "+id);
            require(net.minecraft.client.resources.language.I18n.exists("block.gregtech."+path)&&stack.getHoverName().getString().equals(net.minecraft.client.resources.language.I18n.get("block.gregtech."+path)),"original localized source name "+id);
            items++;
        }
        require(items==24&&rows==88,"12 source converters plus12 preserved identities /88 structure rows");
        var result=new JsonObject();result.addProperty("items",items);result.addProperty("sourceStructureRows",rows);result.addProperty("materialAndHarvestAliases",12);result.addProperty("scope","installed actual tooltip calls/events and registrations; not natural generation/recycling or player hover/restart");return result;
    }
    private static long count(List<Component> lines,String key) {return lines.stream().filter(c->CommonBlockTooltips.containsKey(List.of(c),key)).count();}
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
}
