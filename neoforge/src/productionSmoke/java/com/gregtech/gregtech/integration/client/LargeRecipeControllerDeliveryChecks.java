package com.gregtech.gregtech.integration.client;
import com.google.gson.JsonObject;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.client.CommonBlockTooltips;
import com.gregtech.gregtech.content.machine.OriginalMachineMaterialData;
import com.gregtech.gregtech.content.multiblock.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.*;
final class LargeRecipeControllerDeliveryChecks {
    static JsonObject verify() {
        int count=0,rows=0;
        for(var b:BuiltInRegistries.BLOCK)if(b instanceof BasicMachineBlock block&&OriginalLargeRecipeMachineData.handles(block.basicSpec().machineName())) {
            var spec=block.basicSpec();var stack=new ItemStack(b);var lines=stack.getTooltipLines(Item.TooltipContext.EMPTY,null,TooltipFlag.NORMAL);
            require(count(lines,"gt.lang.structure")==1&&count(lines,"gt.lang.recipes")==1,"source controller headings once "+spec.id());
            for(var key:OriginalLargeRecipeMachineData.structureKeys(spec.machineName())) {require(count(lines,key)==1,"original structure row once "+key);rows++;}
            for(var key:List.of("gt.lang.use.screwdriver.to.toggle","gt.lang.use.monkey.wrench.to.toggle.auto.outputs","gt.lang.use.soft.hammer.to.reset","gt.lang.use.builder.wand.to.ease.building","gt.lang.use.magnifyingglass.to.detail","gt.lang.use.x.to.toggle.facing.pre"))require(count(lines,key)==1,"source inherited tools once "+key);
            require(count(lines,"gt.lang.use.monkey.wrench.to.toggle.auto.inputs")==0&&count(lines,"tooltip.gregtech.machine.recipes")==0,"no old port extra instructions "+spec.id());
            require(count(lines,"gt.lang.energy.input")== (spec.energyType().equals("TU")?0:1),"source TU suppresses energy input "+spec.id());
            require(count(lines,"gt.lang.efficiency")== (LargeMachineProcessingRules.efficiency(spec.machineName())==10000?0:1),"source registered efficiency "+spec.id());
            var expected=OriginalMachineMaterialData.find(spec.machineName(),1).orElseThrow();
            require(ItemMaterialRegistry.get(stack).orElseThrow().components().equals(expected.components()),"actual exact native material registration "+spec.id());
            require(count(stack.getTooltipLines(Item.TooltipContext.EMPTY,null,TooltipFlag.ADVANCED),"tooltip.gregtech.contained_materials")==1&&count(lines,"tooltip.gregtech.contained_materials")==0,"one advanced quantity section "+spec.id());
            var machine=(BasicMachineBlockEntity)block.newBlockEntity(BlockPos.ZERO,b.defaultBlockState());
            require(machine.getEnergySizeInputMin(spec.energyTag(),null)==spec.energyInMin()&&machine.getEnergySizeInputMax(spec.energyTag(),null)==spec.energyInMax(),"actual source native input range "+spec.id());
            for(var tank:machine.getTanksOutput())require(tank.baseCapacity()==Long.MAX_VALUE,"source unlimited outputs "+spec.id());
            count++;
        }
        var pack=new com.gregtech.gregtech.data.MultiblockRecipePack(new net.minecraft.server.packs.PackLocationInfo("gregtech:delivery",Component.literal("Delivery"),net.minecraft.server.packs.repository.PackSource.BUILT_IN,Optional.empty()));
        for(var entry:com.gregtech.gregtech.data.MultiblockCraftingRecipes.ENTRIES)if(entry.blockId().startsWith("large")&&OriginalLargeRecipeMachineData.handles(entry.blockId().substring(0,entry.blockId().indexOf('_')))) {
            var resource=pack.getResource(net.minecraft.server.packs.PackType.SERVER_DATA,net.minecraft.resources.ResourceLocation.parse("gregtech:recipe/machines/multiblock/"+entry.blockId()+".json"));
            require(resource!=null,"actual native recipe pack resolves controller "+entry.blockId());
            try(var input=resource.get()) {
                var data=com.google.gson.JsonParser.parseString(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                if(entry.blockId().equals("largeautoclave_stainless_steel"))require(data.getAsJsonObject("key").getAsJsonObject("M").get("item").getAsString().equals("gregtech:tank_wall_dense"),"actual source18022 dense-wall ingredient");
                if(entry.blockId().equals("largebath_stainless_steel"))require(data.getAsJsonObject("key").getAsJsonObject("A").get("item").getAsString().equals("gregtech:compact_robot_arm_mv"),"actual fixed sourceMV robot arm");
            }catch(java.io.IOException error) {throw new IllegalStateException("native controller recipe read",error);}
        }
        require(count==12&&rows==43,"all twelve original family items and43 structure rows");
        var result=new JsonObject();result.addProperty("controllers",count);result.addProperty("sourceStructureRows",rows);result.addProperty("exactMaterialRecords",count);return result;
    }
    private static long count(List<Component> lines,String key) {return lines.stream().filter(c->CommonBlockTooltips.containsKey(List.of(c),key)).count();}
    private static void require(boolean ok,String message) {if(!ok)throw new IllegalStateException(message);}
}
