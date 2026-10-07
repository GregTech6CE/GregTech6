package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.client.CommonBlockTooltips;
import com.gregtech.gregtech.content.multiblock.OriginalControllerTooltipData;
import com.gregtech.gregtech.content.multiblock.OriginalControllerTooltipData.Family;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;

/** Actual installed item, block, event and native controller parameters; no ticking-world claim. */
final class ProcessControllerDeliveryChecks {
    private ProcessControllerDeliveryChecks() {}
    static JsonObject verify() {
        var paths=Map.of("coke_oven_main",Family.COKE_OVEN,"distillation_tower_main",Family.DISTILLATION_TOWER,
                "logistics_core",Family.LOGISTICS_CORE,"bedrock_mining_drill_controller",Family.BEDROCK_DRILL);
        for(var entry:paths.entrySet()) {
            var id=ResourceLocation.parse("gregtech:"+entry.getKey());var family=entry.getValue();
            var block=BuiltInRegistries.BLOCK.get(id);var stack=new ItemStack(block);
            require(stack.getItem() instanceof BlockItem,"installed original controller "+id);
            var normal=stack.getTooltipLines(Item.TooltipContext.EMPTY,null,TooltipFlag.NORMAL);
            require(count(normal,"gt.lang.structure")==1,"source heading once "+id);
            int index=0;
            for(var key:OriginalControllerTooltipData.structureKeys(family)) {
                require(count(normal,key)==1,"source structure row once "+key);
                var line=normal.stream().filter(c->CommonBlockTooltips.containsKey(List.of(c),key)).findFirst().orElseThrow();
                var color=family==Family.LOGISTICS_CORE && index++>=4?ChatFormatting.YELLOW:ChatFormatting.WHITE;
                require(line.getStyle().getColor().equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(color)),"source structure color "+key);
            }
            for(var key:List.of("gt.lang.use.builder.wand.to.ease.building","gt.lang.use.magnifyingglass.to.detail",
                    "gt.lang.use.x.to.toggle.facing.pre","gt.lang.use.x.to.toggle.facing.post"))
                require(count(normal,key)==1,"inherited tool text once "+key+" "+id);
            var facing=normal.stream().filter(c->CommonBlockTooltips.containsKey(List.of(c),"gt.lang.use.x.to.toggle.facing.pre")).findFirst().orElseThrow();
            require(CommonBlockTooltips.containsKey(List.of(facing),"gt.lang.tool.name.wrench")
                    && CommonBlockTooltips.containsKey(List.of(facing),"gt.lang.use.x.to.toggle.facing.post"),"source facing tool in its own line "+id);
            var expected=com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block(entry.getKey()).orElseThrow();
            require(ItemMaterialRegistry.get(stack).orElseThrow().components().equals(expected.components()),"native exact source REV components "+id);
            require(count(stack.getTooltipLines(Item.TooltipContext.EMPTY,null,TooltipFlag.ADVANCED),"tooltip.gregtech.contained_materials")==1
                    && count(normal,"tooltip.gregtech.contained_materials")==0,"advanced quantities once "+id);
            if(block instanceof BasicMachineBlock machine) {
                var spec=machine.basicSpec();
                require(count(normal,"gt.lang.recipes")==1 && count(normal,"gt.lang.use.screwdriver.to.toggle")==1
                        && count(normal,"gt.lang.use.soft.hammer.to.reset")==1,"source basic inheritance once "+id);
                require(count(normal,"tooltip.gregtech.machine.recipes")==0
                        && count(normal,"tooltip.gregtech.machine.tool.screwdriver")==0,"old port generic rows not duplicated "+id);
                var entity=(com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity)
                        machine.newBlockEntity(BlockPos.ZERO,block.defaultBlockState());
                require(entity.getEnergySizeInputMin(spec.energyTag(),Direction.NORTH)==spec.energyInMin()
                        && entity.getEnergySizeInputMax(spec.energyTag(),Direction.NORTH)==spec.energyInMax(),"actual native source energy range "+id);
                if(family==Family.COKE_OVEN) {
                    require(count(normal,"gt.lang.energy.input")==0 && count(normal,"gt.lang.requirement.ignite.fire")==1
                            && count(normal,"gt.lang.fluid.output")==1 && count(normal,"gt.lang.item.input")==1
                            && count(normal,"gt.lang.item.output")==1,"TU suppresses energy line; source ignition and IO "+id);
                    require(normal.stream().anyMatch(c->CommonBlockTooltips.containsKey(List.of(c),"gt.lang.recipes")
                            && c.getString().endsWith(" (up to 16x processed per run)")),"source parallel16 "+id);
                } else {
                    require(count(normal,"gt.lang.energy.input")==1 && count(normal,"gt.lang.cheap.overclocking")==1
                            && count(normal,"gt.lang.item.input")==0 && count(normal,"gt.lang.fluid.output")==0,
                            "tower overrides sided tooltip and retains cheap overclocking "+id);
                    require(count(normal,"gt.lang.use.monkey.wrench.to.toggle.auto.inputs")==0
                            && count(normal,"gt.lang.use.monkey.wrench.to.toggle.auto.outputs")==1,"original back auto output only "+id);
                    require(spec.faceConfig().energyInputs()==63 && spec.faceConfig().itemInputs()==63
                            && spec.faceConfig().fluidInputs()==63,"actual registered default face masks "+id);
                }
            } else {
                require(count(normal,"gt.lang.energy.input")==1 && count(normal,"gt.lang.recipes")==0,
                        "standalone source chain without invented recipe row "+id);
                var input=OriginalControllerTooltipData.standaloneEnergy(family);
                require(normal.stream().anyMatch(c->CommonBlockTooltips.containsKey(List.of(c),"gt.lang.energy.input")
                        && c.getString().contains(input.minimum()+" to "+input.maximum())),"source displayed standalone range "+id);
            }
        }
        var result=new JsonObject();result.addProperty("originalControllers",paths.size());
        result.addProperty("exactMaterialRecords",paths.size());result.addProperty("sourceStructureRows",22);
        result.addProperty("scope","actual installed tooltip methods/events and native parameter methods; no world tick/restart claim");return result;
    }
    private static long count(List<Component> lines,String key) {return lines.stream().filter(c->CommonBlockTooltips.containsKey(List.of(c),key)).count();}
    private static void require(boolean ok,String message) {if(!ok)throw new IllegalStateException(message);}
}
