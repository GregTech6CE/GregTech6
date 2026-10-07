package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.api.fluid.*;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.block.machine.TankControllerBlock;
import com.gregtech.gregtech.blockentity.machine.MultiblockTankControllerBlockEntity;
import com.gregtech.gregtech.client.CommonBlockTooltips;
import com.gregtech.gregtech.content.multiblock.OriginalTankTooltipData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import java.util.*;

/** Actual installed controller tooltips, saved item data and native entity loading; no world tick claim. */
final class MultiblockTankDeliveryChecks {
    private MultiblockTankDeliveryChecks() {}
    static JsonObject verify() {
        int original=0,legacy=0,states=0;
        for(var block:BuiltInRegistries.BLOCK) {
            if(!(block instanceof TankControllerBlock tankBlock)) continue;
            var spec=tankBlock.valveSpec();var stack=new ItemStack(block);
            var id=BuiltInRegistries.BLOCK.getKey(block);
            var normal=tooltip(stack,false);
            long capacity=spec==null?(tankBlock.size()==5?1024000L:320000L):spec.capacity();
            for(String key:List.of("gt.lang.structure","gt.lang.pipe.stats.capacity","gt.lang.nogui.funnel.tap.tank",
                    "gt.lang.no.powerconducting.fluids","gt.lang.use.builder.wand.to.ease.building","gt.lang.use.magnifyingglass.to.detail",
                    "gt.lang.use.x.to.toggle.facing.pre","gt.lang.use.x.to.toggle.facing.post"))
                require(count(normal,key)==1,"single inherited/specialized tank row "+key+" "+id);
            var facing=normal.stream().filter(c->CommonBlockTooltips.containsKey(List.of(c),"gt.lang.use.x.to.toggle.facing.pre")).findFirst().orElseThrow();
            require(CommonBlockTooltips.containsKey(List.of(facing),"gt.lang.tool.name.wrench")
                    && CommonBlockTooltips.containsKey(List.of(facing),"gt.lang.use.x.to.toggle.facing.post"),"source facing tool in its own line "+id);
            for(String key:OriginalTankTooltipData.structureKeys(tankBlock.size())) require(count(normal,key)==1,"source tank structure "+id);
            require(normal.stream().anyMatch(c->CommonBlockTooltips.containsKey(List.of(c),"gt.lang.pipe.stats.capacity")
                    && c.getString().endsWith(OriginalTankTooltipData.formatNumber(capacity)+" L")),"source empty liters and decimal grouping "+id);
            require(count(normal,"gt.lang.proof.gas")== (spec!=null&&spec.gasProof()?1:0)
                    && count(normal,"gt.lang.proof.acid")== (spec!=null&&spec.acidProof()?1:0)
                    && count(normal,"gt.lang.proof.plasma")== (spec!=null&&spec.plasmaProof()?1:0)
                    && count(normal,"gt.lang.proof.magic")== (spec!=null&&spec.magicProof()?1:0)
                    && count(normal,"gt.lang.only.simple")== (spec!=null&&spec.simpleOnly()?1:0),"actual conditional tank proof flags "+id);
            require(count(normal,"gt.lang.hazard.meltdown")== (spec!=null?1:0),"only registered material has source melting limit "+id);
            if(spec!=null) {
                require(normal.stream().anyMatch(c->CommonBlockTooltips.containsKey(List.of(c),"gt.lang.hazard.meltdown")
                        && c.getString().endsWith(" ("+spec.meltingPoint()+" K)")),"material melting point without crucible bonus "+id);
                var expected=com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block(id.getPath()).orElseThrow();
                require(ItemMaterialRegistry.get(stack).orElseThrow().components().equals(expected.components()),"exact existing controller REV components "+id);
                require(count(tooltip(stack,true),"tooltip.gregtech.contained_materials")==1
                        && count(normal,"tooltip.gregtech.contained_materials")==0,"single source advanced material header "+id);
                original++;
            } else legacy++;
            for(int sample=0;sample<6;sample++) {
                boolean gas=sample==2, hasFluid=sample!=5;
                long amount=switch(sample) {case 0->2500;case 1->0;case 2->5000;case 3->capacity+12345;case 4->3500000000L;default->0;};
                var savedTank=new FluidTankGT(999999999L);
                var fluid=gas?com.gregtech.gregtech.registry.GTFluids.stack("Steam",1).getFluid():Fluids.WATER;
                if(hasFluid) savedTank.setFluid(new net.minecraftforge.fluids.FluidStack(fluid,1),amount);
                var data=new CompoundTag();var nested=new CompoundTag();savedTank.writeToNBT(nested);
                data.put("gt.tank",nested);data.putInt("gt.size",tankBlock.size()==3?5:3);
                var stored=stack.copy();stored.addTagElement("BlockEntityTag",data);var before=stored.getTagElement("BlockEntityTag").copy();
                var lines=tooltip(stored,false);
                long max=Math.max(capacity,amount);
                if(hasFluid) require(count(lines,"gt.lang.pipe.stats.capacity")==0 && lines.stream().anyMatch(c->c.getString()
                        .startsWith(OriginalTankTooltipData.formatNumber(amount)+" L of ")
                        && c.getString().endsWith(" ("+(gas?"Gaseous":"Liquid")+"); Max: "+OriginalTankTooltipData.formatNumber(max)+" L)")),
                        "source filled/zero identity/long amount with correct capacity "+id+" "+sample);
                else require(lines.stream().anyMatch(c->CommonBlockTooltips.containsKey(List.of(c),"gt.lang.pipe.stats.capacity")
                        && c.getString().endsWith(OriginalTankTooltipData.formatNumber(capacity)+" L")),"saved foreign capacity does not redefine valve "+id);
                require(before.equals(stored.getTagElement("BlockEntityTag")),"tank hover cannot mutate saved contents "+id);
                require(!ItemMaterialRegistry.canRecover(stored),"stored tank cannot be recovered as empty hull "+id);
                var entity=(MultiblockTankControllerBlockEntity)tankBlock.newBlockEntity(BlockPos.ZERO,block.defaultBlockState());
                entity.load(data);
                try {
                    var field=MultiblockTankControllerBlockEntity.class.getDeclaredField("tank");field.setAccessible(true);
                    var actual=(FluidTankGT)field.get(entity);
                    require(actual.baseCapacity()==capacity && actual.getAmount()==amount,"actual native loading retains saved amount and valve capacity "+id+" "+sample);
                    if(hasFluid) {
                        entity.setSize(tankBlock.size());
                        require(actual.getAmount()==amount && actual.baseCapacity()==capacity,"size/proof refresh cannot discard retained amount "+id);
                    }
                    require(actual.getFluidLong().isEmpty()==!hasFluid,"actual native loading retains zero amount identity "+id);
                } catch(ReflectiveOperationException e) {throw new IllegalStateException("native tank data "+id,e);}
                states++;
            }
        }
        require(original==25 && legacy==2 && states==162,"all25 valves, two aliases, six saved states each");
        var out=new JsonObject();out.addProperty("originalControllers",original);out.addProperty("retainedAliases",legacy);
        out.addProperty("savedTooltipAndActualEntityLoadCases",states);
        out.addProperty("scope","installed item tooltips/events and actual native entity data loading; no formed-tank flow or restart claim");return out;
    }
    private static List<Component> tooltip(ItemStack stack,boolean advanced) {return stack.getTooltipLines(null,advanced?TooltipFlag.ADVANCED:TooltipFlag.NORMAL);}
    private static long count(List<Component> lines,String key) {return lines.stream().filter(c->CommonBlockTooltips.containsKey(List.of(c),key)).count();}
    private static void require(boolean ok,String message) {if(!ok)throw new IllegalStateException(message);}
}
