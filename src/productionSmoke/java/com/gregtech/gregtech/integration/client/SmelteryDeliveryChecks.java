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
        require(vessels==39 && companions==156 && positive==155 && undefined==40 && shapes==156,"complete installed smeltery coverage");
        var result=new JsonObject();result.addProperty("vessels",vessels);result.addProperty("companions",companions);
        result.addProperty("exactSourceCompositions",positive);result.addProperty("undefinedSourceAmountsPreserved",undefined);
        result.addProperty("savedShapeTooltipAndProtectionCases",shapes);result.addProperty("nativeThermalParameterMethods",vessels);
        result.addProperty("scope","installed tooltip calls/events, saved item state, entity thermal parameter methods and recovery parsing; no world ticking or restart");
        return result;
    }
    private static long count(List<Component> lines,String key) { return lines.stream().filter(c->CommonBlockTooltips.containsKey(List.of(c),key)).count(); }
    private static List<Component> tooltip(ItemStack stack,boolean advanced) { return stack.getTooltipLines(null,advanced?TooltipFlag.ADVANCED:TooltipFlag.NORMAL); }
    private static void write(ItemStack stack,CompoundTag data) { stack.addTagElement("BlockEntityTag",data); }
    private static CompoundTag read(ItemStack stack) { return stack.getTagElement("BlockEntityTag"); }
    private static void require(boolean value,String message) { if(!value) throw new IllegalStateException(message); }
}
