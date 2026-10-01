package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.fluid.PortableFluidContainerSpec;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.blockentity.tool.PortableContainerBlockEntity;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class CapsuleCasingTests {
    private static ItemStack item(String id){return new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech",id)));}
    @GameTest(template="test_empty") public static void allCapsulesKeepFluidAcrossPlacement(GameTestHelper h){
        int count=0;var p=new BlockPos(1,2,1);
        for(var spec:PortableFluidContainerSpec.values())if(spec.shapeId().equals("cell")) {
            var stack=item("fluid_"+spec.id());
            h.assertTrue(stack.getItem() instanceof BlockItem&&stack.getMaxStackSize()==64,"original placeable capsule stack limit: "+spec.id());
            var handler=stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(IllegalStateException::new);
            h.assertTrue(handler.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1200),FluidAction.EXECUTE)==1000,"1000 L physical capacity: "+spec.id());
            var block=((BlockItem)stack.getItem()).getBlock();h.setBlock(p,block);
            block.setPlacedBy(h.getLevel(),h.absolutePos(p),block.defaultBlockState(),h.makeMockPlayer(),stack);
            var be=(PortableContainerBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));be.load(be.saveWithoutMetadata());
            var drops=net.minecraft.world.level.block.Block.getDrops(be.getBlockState(),h.getLevel(),h.absolutePos(p),be);
            h.assertTrue(drops.size()==1&&ItemStack.matches(stack,drops.get(0)),"fill/place/save/drop keeps exact contents: "+spec.id());count++;
        }
        h.assertTrue(count==40,"all forty source capsule variants");h.succeed();
    }
    @GameTest(template="test_empty") public static void capsuleResistanceAndBulkFilling(GameTestHelper h){
        var aluminium=item("fluid_cell_aluminium");
        var cap=aluminium.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(IllegalStateException::new);
        h.assertTrue(cap.fill(new FluidStack(GTFluids.still("Steam").get(),1000),FluidAction.EXECUTE)==0,"power-conducting steam rejected");
        h.assertTrue(cap.fill(new FluidStack(GTFluids.still("Helium").get(),1000),FluidAction.EXECUTE)==1000,"ordinary gas accepted");
        var bulk=item("fluid_cell_aluminium");bulk.setCount(64);
        h.assertTrue(bulk.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(IllegalStateException::new).fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),FluidAction.EXECUTE)==0,"direct bulk handler cannot duplicate fluid into 64 cells");
        h.assertTrue(PortableFluidContainerSpec.CELL_INFINITY.accepts(Integer.MAX_VALUE,true,true,true,true),"Infinity original proof flags and temperature");
        h.assertTrue(!PortableFluidContainerSpec.CELL_TUNGSTEN.accepts(300,true,false,false,true)&&PortableFluidContainerSpec.CELL_ADAMANTIUM.accepts(300,true,true,true,true),"plasma resistance differs by material");
        h.assertTrue(PortableFluidContainerSpec.CELL_WAX_MAGIC.maxTemperature()==2700,"magic wax special temperature overrides melting point");h.succeed();
    }
    @GameTest(template="test_empty") public static void capsuleExtrusionPreservesUnits(GameTestHelper h){
        int count=0;var materials=new java.util.HashSet<String>();
        for(var recipe:MachineRecipeMaps.Extruder.mRecipeList)if(recipe.mOutputs.length==1&&recipe.mOutputs[0].getItem() instanceof com.gregtech.gregtech.item.PortableFluidContainerItem vessel&&vessel.spec().shapeId().equals("cell")){
            boolean simple=com.gregtech.gregtech.content.recipe.CapsuleCellRecipes.simpleExtrusion(vessel.spec());
            h.assertTrue(recipe.mEUt==(simple?16:96),"simple and hot extrusion have distinct energy requirements");
            if(simple)h.assertTrue(recipe.mDuration==(recipe.mOutputs[0].getCount()==1?8:64),"original simple extrusion duration");
            else h.assertTrue(!ForgeRegistries.ITEMS.getKey(recipe.mInputs[1].getItem()).getPath().startsWith("low_heat_"),"hot material cannot use low heat mold");
            var prefix=com.gregtech.gregtech.item.MaterialItem.getPrefix(recipe.mInputs[0]);
            h.assertTrue(prefix!=null&&prefix.getMaterialWeight()*9==com.gregtech.gregtech.api.material.GTValues.U*recipe.mOutputs[0].getCount(),"extrusion conserves actual material units");
            materials.add(vessel.spec().id());
            h.assertTrue(recipe.mInputs.length==2&&recipe.mInputs[1].getItem() instanceof com.gregtech.gregtech.item.TechItem mold&&mold.isCatalyst(),"extruder mold is reusable");count++;
        }
        h.assertTrue(count>56&&materials.size()==40,"all forty source capsules now have manufacturing routes, got "+count+" / "+materials.size());h.succeed();
    }
    @GameTest(template="test_empty") public static void hotCapsuleWorkAndEmptyOnlyRecycling(GameTestHelper h){
        var tungsten=GTMaterialRegistry.get("Tungsten");
        long expected=Math.max(16,1+(long)((3695-293)*19.25*111.111111/(75*96)));
        h.assertTrue(com.gregtech.gregtech.content.recipe.CapsuleCellRecipes.hotExtrusionTicks(tungsten,com.gregtech.gregtech.api.material.GTValues.U)==expected,"source tungsten heat and mass work formula");
        h.assertTrue(PortableFluidContainerSpec.CELL_KREKNORITE.material()==GTMaterialRegistry.get("Trinium"),"legacy registry ID retains item identity but Ke resolves Trinium");
        int count=0;
        for(var recipe:MachineRecipeMaps.Shredder.mRecipeList)if(recipe.mInputs.length==1&&recipe.mInputs[0].getItem() instanceof com.gregtech.gregtech.item.PortableFluidContainerItem vessel&&vessel.spec().shapeId().equals("cell")){
            var input=recipe.mInputs[0].copy();
            h.assertTrue(com.gregtech.gregtech.api.recipe.RecipeInputs.consume(recipe,java.util.List.of(input),java.util.List.of(),1)!=null,"empty capsule recyclable");
            input.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(IllegalStateException::new).fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1),FluidAction.EXECUTE);
            h.assertTrue(com.gregtech.gregtech.api.recipe.RecipeInputs.consume(recipe,java.util.List.of(input),java.util.List.of(),1)==null,"even one L prevents recycling and fluid destruction");
            h.assertTrue(com.gregtech.gregtech.item.MaterialItem.isMaterialItem(recipe.mOutputs[0],MaterialPrefix.dustTiny,vessel.spec().material())&&recipe.mOutputs[0].getCount()==1,"one ninth U recovered without duplication");count++;
        }
        h.assertTrue(count==40,"all forty capsules have empty-shell recycling");h.succeed();
    }
    @GameTest(template="test_empty") public static void placedCapsuleUpdateCarriesCurrentFluid(GameTestHelper h){
        var p=new BlockPos(1,2,1);var block=((BlockItem)item("fluid_cell_aluminium").getItem()).getBlock();h.setBlock(p,block);
        var be=(PortableContainerBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));
        var handler=be.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(IllegalStateException::new);
        handler.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),FluidAction.EXECUTE);
        handler.drain(250,FluidAction.EXECUTE);
        var clientCopy=new PortableContainerBlockEntity(be.getBlockPos(),be.getBlockState());
        clientCopy.load(be.getUpdatePacket().getTag());
        h.assertTrue(FluidUtil.getFluidContained(clientCopy.contents()).orElseThrow().getAmount()==750,"client update contains current fluid after automation drain");
        handler.drain(750,FluidAction.EXECUTE);clientCopy.load(be.getUpdateTag());
        h.assertTrue(FluidUtil.getFluidContained(clientCopy.contents()).isEmpty(),"empty update clears stale fluid window");
        var bounds=be.getBlockState().getShape(h.getLevel(),be.getBlockPos()).bounds();
        h.assertTrue(bounds.minX==5/16d&&bounds.maxX==11/16d&&bounds.maxY==12/16d,"source capsule selection bounds");h.succeed();
    }
    @GameTest(template="test_empty") public static void leadCasingGradesAndCoreCrafting(GameTestHelper h){
        var lead=GTMaterialRegistry.get("Lead");int[] units={8,14,26,56};int[] limits={8,4,2,1};int grade=0;
        for(var prefix:new BlockMaterialPrefix[]{BlockMaterialPrefix.casingMachine,BlockMaterialPrefix.casingMachineDouble,BlockMaterialPrefix.casingMachineQuadruple,BlockMaterialPrefix.casingMachineDense}){
            var result=GTBlocks.getStack(prefix,lead);h.assertTrue(!result.isEmpty()&&result.getMaxStackSize()==limits[grade],"existing casing grade with original stack limit");
            var routes=MachineRecipeMaps.Welder.mRecipeList.stream().filter(r->r.mOutputs.length==1&&r.mOutputs[0].is(result.getItem())).toList();
            h.assertTrue(routes.size()==2,"long and normal rod alternatives for lead casing grade "+grade);
            // GT6 Loader_Recipes_Handlers:349-358 keys the welding time on tEasyHeatable = Or(FURNACE):
            // the easy pass is 16 ticks per material unit, everything else pays 64 * units * (quality + 1).
            long expected=com.gregtech.gregtech.data.generated.MaterialWorkability.isFurnace(lead)
                    ?16L*units[grade]:64L*units[grade]*(lead.getToolQuality()+1);
            for(var r:routes)h.assertTrue(r.mDuration==expected&&r.mEUt==16&&r.mInputs[0].getCount()==6,"source welding work and six plates");
            grade++;
        }
        var id=ResourceLocation.fromNamespaceAndPath("gregtech","nuclear/reactor_core_2x2");
        var recipe=h.getLevel().getRecipeManager().byKey(id).orElseThrow();
        h.assertTrue(recipe.getIngredients().get(4).test(GTBlocks.getStack(BlockMaterialPrefix.casingMachineDense,lead)),"core uses dense lead casing, not large machine wall");
        h.assertTrue(recipe.getIngredients().get(0).test(item("compact_electric_piston_ev"))&&recipe.getIngredients().get(1).test(item("circuit_master")),"source EV piston and T5 circuit");h.succeed();
    }
    @GameTest(template="test_empty") public static void geigerCraftingRejectsFilledCellAfterNetwork(GameTestHelper h){
        var menu=new AbstractContainerMenu(null,0){public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}};
        var grid=new TransientCraftingContainer(menu,3,3);
        var id=ResourceLocation.fromNamespaceAndPath("gregtech","nuclear/geiger_counter_empty");
        var recipe=(com.gregtech.gregtech.recipe.ToolShapedRecipe)h.getLevel().getRecipeManager().byKey(id).orElseThrow();
        for(int i=0;i<9;i++){
            var stack=recipe.getIngredients().get(i).getItems()[0].copy();
            if(stack.getItem() instanceof com.gregtech.gregtech.item.GTToolItem)stack=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolHelper.getType(stack),GTMaterialRegistry.get("Steel"),GTMaterialRegistry.get("Wood"));
            grid.setItem(i,stack);
        }
        h.assertTrue(recipe.matches(grid,h.getLevel()),"empty aluminium capsule crafts empty Geiger counter");
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{
            com.gregtech.gregtech.recipe.ToolShapedRecipe.SERIALIZER.toNetwork(buffer,recipe);
            var copy=com.gregtech.gregtech.recipe.ToolShapedRecipe.SERIALIZER.fromNetwork(id,buffer);
            h.assertTrue(copy.matches(grid,h.getLevel())&&buffer.readableBytes()==0,"network codec preserves full recipe");
            grid.getItem(1).getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(IllegalStateException::new).fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,500),FluidAction.EXECUTE);
            h.assertTrue(!copy.matches(grid,h.getLevel())&&!recipe.matches(grid,h.getLevel()),"filled capsule rejected on both sides instead of losing fluid");
        }finally{buffer.release();}h.succeed();
    }
}
