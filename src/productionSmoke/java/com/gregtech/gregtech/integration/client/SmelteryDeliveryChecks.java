package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.api.machine.OriginalSmelteryDefinitions;
import com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput;
import com.gregtech.gregtech.api.machine.crucible.MoldShapes;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.block.machine.MoldItemData;
import com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock;
import com.gregtech.gregtech.client.CommonBlockTooltips;
import com.gregtech.gregtech.data.BlockHarvestPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;

/** Actual installed item tooltips, native entity parameter methods and protected recovery inputs.
 * No world ticking, screen hover or independent save-restart claim. */
final class SmelteryDeliveryChecks {
    private SmelteryDeliveryChecks() {}
    static JsonObject verify() {
        int vessels=0, companions=0, positive=0, undefined=0, shapes=0;
        for (var spec : OriginalSmelteryDefinitions.all()) {
            var id=ResourceLocation.parse("gregtech:"+spec.id());
            require(BuiltInRegistries.ITEM.containsKey(id),"native smeltery item exists "+id);
            var item=BuiltInRegistries.ITEM.get(id);
            var stack=new ItemStack(item);
            var block=((BlockItem)item).getBlock();
            require(block.getExplosionResistance()==spec.blastResistance(),"actual source resistance "+id);
            require(BlockHarvestPolicy.tool(block)==BlockHarvestPolicy.Tool.PICKAXE,"source pickaxe policy "+id);
            var normal=tooltip(stack,false);
            require(count(normal,"gt.lang.proof.acid")==(spec.acidProof()
                    && !spec.id().startsWith("crucible_crossing_")?1:0),"actual acid-proof row "+id);
            if (spec.id().startsWith("smelting_crucible_")) {
                vessels++;
                for(String key:List.of("gt.lang.energy.convert.from","gt.lang.energy.convert.to","gt.lang.energy.convert.per",
                        "gt.lang.thermal.mass","gt.lang.hazard.meltdown","gt.tooltip.crucible.1","gt.lang.hazard.fire",
                        "gt.lang.hazard.contact","gt.lang.use.thermometer.to.measure","gt.lang.use.shovel.to.empty"))
                    require(count(normal,key)==1,"single original vessel row "+key+" "+id);
                require(normal.stream().anyMatch(c->CommonBlockTooltips.containsKey(List.of(c),"gt.lang.thermal.mass")
                        && c.getString().endsWith(" "+spec.smeltingThermalMassKg()+" kg")),"visible source thermal mass "+id);
                var entity=((SmeltingCrucibleBlock)block).newBlockEntity(BlockPos.ZERO,block.defaultBlockState());
                try {
                    var method=entity.getClass().getDeclaredMethod("thermalMassKg");method.setAccessible(true);
                    require(Math.abs(((Number)method.invoke(entity)).doubleValue()-spec.smeltingThermalMassKg())<1e-9,
                            "actual native entity uses separate7U thermal hull "+id);
                } catch(ReflectiveOperationException error) { throw new IllegalStateException("native thermal method "+id,error); }
            } else {
                companions++;
                boolean crossing=spec.id().startsWith("crucible_crossing_");
                // Crossings have no addToolTips; their acid property is not a specialized tooltip.
                if (!crossing) require(count(normal,"gt.lang.hazard.meltdown")==1
                        && normal.stream().anyMatch(c->CommonBlockTooltips.containsKey(List.of(c),"gt.lang.hazard.meltdown")
                        && c.getString().endsWith(" ("+spec.meltDownTemperatureK()+" K)")),"source companion limit "+id);
                if (spec.id().startsWith("mold_") && !spec.id().startsWith("mold_basin_")) {
                    require(count(normal,"gt.lang.recipes.mold.select")==1,"unselected mold row "+id);
                    for(int shape:new int[]{1,0x1ffffff,0b00000_11111_11111_11111_00000,1<<30}) {
                        var saved=MoldItemData.withShape(stack.copy(),shape);
                        var before=read(saved).copy();
                        var lines=tooltip(saved,false);
                        var recipe=MoldShapes.recipe(shape);
                        require(count(lines,"gt.lang.recipes.mold.select")==0 && count(lines,"gt.lang.recipes.mold")==1
                                && count(lines,"oredict.prefix."+MoldShapes.sourcePrefixName(recipe))==1,
                                "saved mold reports actual source product "+id+" "+shape);
                        require(lines.stream().anyMatch(c->CommonBlockTooltips.containsKey(List.of(c),"gt.lang.recipes.mold")
                                && c.getString().endsWith(com.gregtech.gregtech.client.MaterialTooltips.displayUnits(
                                MoldShapes.requiredMaterialUnits(shape))+" Units")),"saved mold amount "+id);
                        require(before.equals(read(saved)),"saved shape hover is read-only "+id);
                        require(!ItemMaterialRegistry.canRecover(saved) && CrucibleItemInput.parse(saved).isEmpty(),
                                "configured native mold cannot be consumed as empty shell "+id);
                        shapes++;
                    }
                }
            }
            var expected=com.gregtech.gregtech.content.machine.MachineConstructionMaterials.block(spec.id());
            var actual=ItemMaterialRegistry.get(stack);
            if (expected.isPresent()) {
                require(actual.isPresent() && actual.get().components().equals(expected.get().components()),"exact registered source components "+id);
                var parsed=CrucibleItemInput.parse(stack);
                for(var part:expected.get().components()) require(parsed.stream().anyMatch(p->p.material.resolve()==part.material().resolve()
                        && p.amount==part.amount()),"native crucible parser keeps original component "+id+" "+part);
                require(count(tooltip(stack,true),"tooltip.gregtech.contained_materials")==1
                        && count(normal,"tooltip.gregtech.contained_materials")==0,"single advanced composition header "+id);
                var saved=stack.copy();write(saved,new CompoundTag());
                require(!ItemMaterialRegistry.canRecover(saved) && CrucibleItemInput.parse(saved).isEmpty(),"protected block-item data "+id);
                positive++;
            } else {
                require(actual.isEmpty() && CrucibleItemInput.parse(stack).isEmpty(),"original undefined REV amount has no fabricated recovery "+id);
                undefined++;
            }
        }
        int large=0;
        var variants=new ArrayList<>(com.gregtech.gregtech.content.multiblock.LargeCrucibleSpecs.all());
        variants.add(com.gregtech.gregtech.content.multiblock.LargeCrucibleSpecs.LEGACY);
        for(var variant:variants) {
            var id=ResourceLocation.parse("gregtech:"+variant.path());
            require(BuiltInRegistries.ITEM.containsKey(id),"large vessel item exists "+id);
            var stack=new ItemStack(BuiltInRegistries.ITEM.get(id));
            var block=(com.gregtech.gregtech.block.machine.LargeCrucibleControllerBlock)((BlockItem)stack.getItem()).getBlock();
            var entity=(com.gregtech.gregtech.blockentity.machine.LargeCrucibleControllerBlockEntity)
                    block.newBlockEntity(BlockPos.ZERO,block.defaultBlockState());
            long limit=com.gregtech.gregtech.content.multiblock.OriginalLargeCrucibleParameters.meltDownTemperatureK(variant.material().getMeltingPoint());
            require(entity.getMeltDownLimitK()==limit,"native large limit truncates1.10 "+id);
            var normal=tooltip(stack,false);
            for(String key:List.of("gt.lang.structure","gt.tooltip.multiblock.crucible.1","gt.tooltip.multiblock.crucible.2",
                    "gt.tooltip.multiblock.crucible.3","gt.tooltip.multiblock.crucible.4","gt.tooltip.multiblock.crucible.5",
                    "gt.lang.energy.convert.from","gt.lang.thermal.mass","gt.lang.hazard.meltdown","gt.lang.hazard.fire",
                    "gt.lang.hazard.contact","gt.lang.use.shovel.to.empty"))
                require(count(normal,key)==1,"single source large row "+key+" "+id);
            require(count(normal,"gt.lang.proof.acid")== (variant.acidProof()?1:0),"native large acid condition "+id);
            require(count(normal,"gt.lang.use.thermometer.to.measure")==0,"source large vessel has no thermometer row "+id);
            require(normal.stream().anyMatch(c->CommonBlockTooltips.containsKey(List.of(c),"gt.lang.hazard.fire")
                    && c.getString().endsWith(" (6m)")),"large source fire range "+id);
            require(normal.stream().anyMatch(c->CommonBlockTooltips.containsKey(List.of(c),"gt.lang.hazard.meltdown")
                    && c.getString().endsWith(" ("+limit+" K)")),"visible large source meltdown limit "+id);
            require(normal.stream().anyMatch(c->CommonBlockTooltips.containsKey(List.of(c),"gt.lang.thermal.mass")
                    && c.getString().endsWith(" "+variant.crucible().smeltingThermalMassKg()+" kg")),"source large100U thermal hull "+id);
            try {
                var method=entity.getClass().getDeclaredMethod("hazardProfile");method.setAccessible(true);
                require(method.invoke(entity).equals(com.gregtech.gregtech.api.machine.crucible.CrucibleHazards.LARGE),"native5-radius damage4 profile "+id);
            } catch(ReflectiveOperationException error) {throw new IllegalStateException("large native parameter "+id,error);}
            if(variant.originalId()>0) {
                var data=ItemMaterialRegistry.get(stack).orElseThrow();
                require(data.components().size()==1 && data.components().get(0).material().resolve()==variant.material().resolve()
                        && data.components().get(0).amount()==4L*com.gregtech.gregtech.api.material.GTValues.U,
                        "controller REV4U remains separate from100U thermal mass "+id);
                require(count(tooltip(stack,true),"tooltip.gregtech.contained_materials")==1,"single large advanced composition row "+id);
            }
            large++;
        }
        require(large==9,"eight original large controllers and retained alias");
        require(vessels==39 && companions==156 && positive==155 && undefined==40 && shapes==156,"complete installed smeltery coverage");
        var result=new JsonObject();result.addProperty("vessels",vessels);result.addProperty("companions",companions);
        result.addProperty("exactSourceCompositions",positive);result.addProperty("undefinedSourceAmountsPreserved",undefined);
        result.addProperty("savedShapeTooltipAndProtectionCases",shapes);result.addProperty("nativeThermalParameterMethods",vessels);
        result.addProperty("largeVesselTooltipAndNativeParameterMethods",large);
        result.addProperty("scope","installed tooltip calls/events, saved item state, entity thermal parameter methods and recovery parsing; no world ticking or restart");
        return result;
    }
    private static long count(List<Component> lines,String key) { return lines.stream().filter(c->CommonBlockTooltips.containsKey(List.of(c),key)).count(); }
    private static List<Component> tooltip(ItemStack stack,boolean advanced) { return stack.getTooltipLines(null,advanced?TooltipFlag.ADVANCED:TooltipFlag.NORMAL); }
    private static void write(ItemStack stack,CompoundTag data) { stack.addTagElement("BlockEntityTag",data); }
    private static CompoundTag read(ItemStack stack) { return stack.getTagElement("BlockEntityTag"); }
    private static void require(boolean value,String message) { if(!value) throw new IllegalStateException(message); }
}
