package gregtech.gametest;

import gregtech.internal.recipe.*;
import gregtech.internal.recipe.*;
import gregtech.internal.tool.*;
import gregtech.content.recipe.*;
import gregtech.content.data.*;
import gregtech.content.item.*;
import gregtech.content.registry.*;
import gregtech.content.block.energy.ElectricWireBlock;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.inventory.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class ManufacturingTests {
    private static TransientCraftingContainer grid() {
        var menu=new AbstractContainerMenu(null,0) {
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}
            public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}
        };
        return new TransientCraftingContainer(menu,3,3);
    }
    private static ItemStack usable(ItemStack stack) {
        if(stack.getItem() instanceof GTToolItem)return GTToolItem.create(GTToolHelper.getType(stack),gregtech.content.material.Materials.Steel,
                gregtech.internal.material.GTMaterialRegistry.get("Wood"));
        return stack.copy();
    }
    @GameTest(template="test_blueprint_empty")
    public static void wireCraftingPreservesConductorAmount(GameTestHelper h) {
        int count=0;
        for(var raw:h.getLevel().getRecipeManager().getRecipes())if(raw.getId().getNamespace().equals("gt6ce")&&(raw.getId().getPath().startsWith("wire_working/")||raw.getId().getPath().startsWith("wire_sizes/"))) {
            var recipe=(ShapelessRecipe)raw;var grid=grid();int slot=0;long amount=0;String material=null;
            for(var ingredient:recipe.getIngredients()) {
                h.assertTrue(ingredient.getItems().length>0,"wire ingredient resolves");
                var stack=ingredient.getItems()[0].copy();grid.setItem(slot++,stack);
                if(stack.getItem() instanceof BlockItem item&&item.getBlock() instanceof ElectricWireBlock block) {
                    amount+=block.spec().materialAmount();material=block.spec().id();
                }
            }
            if(!recipe.matches(grid,h.getLevel())&&slot==1) {
                var input=grid.getItem(0).copy();
                for(int offset=1;offset<9&&!recipe.matches(grid,h.getLevel());offset++) {
                    grid.clearContent();grid.setItem(offset,input.copy());
                }
            }
            h.assertTrue(recipe.matches(grid,h.getLevel()),"wire crafting matches at its source selector position: "+recipe.getId());
            var output=recipe.assemble(grid,h.getLevel().registryAccess());var block=(ElectricWireBlock)((BlockItem)output.getItem()).getBlock();
            h.assertTrue(block.spec().id().equals(material)&&block.spec().materialAmount()*output.getCount()==amount,"wire crafting conserves metal and family");
            count++;
        }
        var families=new TreeMap<String,TreeSet<Integer>>();
        int oldUnpack=0;
        for(var entry:GTWires.allWires()) {
            var spec=entry.get().spec();families.computeIfAbsent(spec.id(),key->new TreeSet<>()).add(spec.size());
            if(spec.size()>1)oldUnpack++;
        }
        int insulation=(int)GTWires.allCables().stream().filter(e->e.get().spec().size()<=2).count();
        int converted=0;
        for(var sizes:families.values())for(int big:sizes)for(int small:sizes)if(small<big&&big%small==0) {
            converted++;if(big/small<10)converted++;
        }
        h.assertTrue(count==oldUnpack+insulation+converted,"source unpack/insulation and canonical wire-size routes loaded: "+count+" / "+(oldUnpack+insulation+converted));h.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void moldsHaveUniqueOrientationAndSurviveNetwork(GameTestHelper h) {
        var molds=h.getLevel().getRecipeManager().getRecipes().stream().filter(r->r.getId().getNamespace().equals("gt6ce")&&r.getId().getPath().startsWith("extruder_shapes/"))
                .map(r->(gregtech.content.recipe.ToolShapedRecipe)r).toList();
        // 64 extruder shapes + the 14 shape items GT6 crafts the same way (food molds and slicer
        // blades, MultiItemTechnological:336-380), all registered as gregtech:tool_shaped recipes
        h.assertTrue(molds.size()==78,"78 molds loaded; got "+molds.size());
        for(var recipe:molds) {
            var grid=grid();
            for(int y=0;y<recipe.getHeight();y++)for(int x=0;x<recipe.getWidth();x++) {
                var ingredient=recipe.getIngredients().get(x+y*recipe.getWidth());
                if(!ingredient.isEmpty())grid.setItem(x+y*3,usable(ingredient.getItems()[0]));
            }
            h.assertTrue(recipe.matches(grid,h.getLevel()),"mold pattern works: "+recipe.getId());
            var colliding=molds.stream().filter(other->other.matches(grid,h.getLevel())).toList();
            h.assertTrue(colliding.size()==1,"mold patterns cannot collide through mirroring: "+recipe.getId()
                    +" also matches "+colliding.stream().map(r->r.getId().toString()).filter(id->!id.equals(recipe.getId().toString())).toList());
            var remainder=recipe.getRemainingItems(grid);
            for(int slot=0;slot<9;slot++) {
                var stack=grid.getItem(slot);
                if(stack.getItem() instanceof TechItem)h.assertTrue(remainder.get(slot).isEmpty(),"blank/previous mold consumed when cutting a new shape");
                if(stack.getItem() instanceof GTToolItem)h.assertTrue(remainder.get(slot).getDamageValue()==GTToolHelper.getType(stack).damagePerCraft(),"each shaping tool wears correctly");
            }
            var buffer=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                gregtech.content.recipe.ToolShapedRecipe.SERIALIZER.toNetwork(buffer,recipe);
                var copy=gregtech.content.recipe.ToolShapedRecipe.SERIALIZER.fromNetwork(recipe.getId(),buffer);
                h.assertTrue(copy.matches(grid,h.getLevel())&&buffer.readableBytes()==0,"shape network codec includes mirror rule");
                for(int y=0;y<3;y++){var a=grid.getItem(y*3);var b=grid.getItem(y*3+2);grid.setItem(y*3,b);grid.setItem(y*3+2,a);}
                h.assertTrue(copy.matches(grid,h.getLevel())==recipe.matches(grid,h.getLevel()),"network keeps orientation behavior");
            } finally {buffer.release();}
        }
        h.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void wireMachinesAndSelectorsConserveMaterials(GameTestHelper h) {
        h.assertTrue(WireProcessingRecipes.entries().size()>1500,"wire machine routes populated");
        for(var entry:WireProcessingRecipes.entries()) {
            var recipe=entry.recipe();var items=Arrays.stream(recipe.mInputs).map(ItemStack::copy).toList();
            var remaining=RecipeInputs.consume(recipe,items,List.of(),1);h.assertTrue(remaining!=null,"wire machine recipe executes");
            long input=0;for(var stack:items) {
                if(stack.getItem() instanceof BlockItem item&&item.getBlock() instanceof ElectricWireBlock wire)input+=wire.spec().materialAmount()*stack.getCount();
                else if(stack.getItem() instanceof BlockItem item&&item.getBlock() instanceof gregtech.content.block.energy.SignalWireBlock)input+=gregtech.api.material.GTValues.U/2*stack.getCount();
                else if(entry.map()==MachineRecipeMaps.Wiremill&&stack.getItem() instanceof MaterialItem item)input+=item.getPrefix().getMaterialWeight()*stack.getCount();
            }
            var result=recipe.mOutputs[0];var block=((BlockItem)result.getItem()).getBlock();
            long amount=block instanceof ElectricWireBlock wire?wire.spec().materialAmount():gregtech.api.material.GTValues.U/2;
            h.assertTrue(input==amount*result.getCount(),"machine conserves conductor amount");
            if(entry.map()==MachineRecipeMaps.Loom) {
                h.assertTrue(remaining.items().get(1).getCount()==1,"loom selector preserved");
                h.assertTrue(RecipeInputs.consume(recipe,List.of(items.get(0)),List.of(),1)==null,"loom selector required");
            }
        }
        h.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void grapheneAndPolymersAreExecutable(GameTestHelper h) {
        h.assertTrue(GrapheneNanofabricationRecipes.recipes().size()==45,"45 original graphene recipes");
        for(var recipe:GrapheneNanofabricationRecipes.recipes()) {
            var items=Arrays.stream(recipe.mInputs).map(ItemStack::copy).toList();
            var rest=RecipeInputs.consume(recipe,items,List.of(),1);
            h.assertTrue(rest!=null&&rest.items().get(0).getCount()==1&&rest.items().get(1).isEmpty(),"nanofab consumes carbon, retains selector");
            long input=((MaterialItem)items.get(1).getItem()).getPrefix().getMaterialWeight()*items.get(1).getCount();
            var output=recipe.mOutputs[0];long unit=output.getItem() instanceof MaterialItem item?item.getPrefix().getMaterialWeight():
                    ((ElectricWireBlock)((BlockItem)output.getItem()).getBlock()).spec().materialAmount();
            h.assertTrue(input==unit*output.getCount(),"nanofabrication conserves carbon");
        }
        h.assertTrue(PolymerFormingRecipes.recipes().size()==93,"93 polymer recipes");
        for(var recipe:PolymerFormingRecipes.recipes()) {
            var items=Arrays.stream(recipe.mInputs).map(ItemStack::copy).toList();var fluids=Arrays.stream(recipe.mFluidInputs).map(net.minecraftforge.fluids.FluidStack::copy).toList();
            var rest=RecipeInputs.consume(recipe,items,fluids,1);h.assertTrue(rest!=null,"polymer inputs execute");
            if(items.size()==2)h.assertTrue(rest.items().get(0).isEmpty()&&rest.items().get(1).getCount()==1&&recipe.mDuration==64L*recipe.mInputs[0].getCount(),"polymer keeps mold and uses original low-heat duration");
            else h.assertTrue(recipe.mEUt==0&&recipe.mDuration==256&&recipe.mFluidInputs[0].getAmount()==16&&rest.fluids().get(0).isEmpty(),"latex coagulation costs");
        }
        h.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void plasticChemistryKeepsCatalystAndFeedsForming(GameTestHelper h) {
        var plastic=gregtech.internal.material.GTMaterialRegistry.get("Plastic");
        var powder=GTItems.getStack(MaterialPrefix.dust,plastic,1);
        var reaction=MachineRecipeMaps.Mixer.mRecipeList.stream().filter(r->r.mOutputs.length==1&&r.mOutputs[0].is(powder.getItem())&&r.mInputs.length==1).findFirst().orElseThrow();
        h.assertTrue(reaction.isCatalystInput(0),"original MgCl2 catalyst is retained");
        var fluids=Arrays.stream(reaction.mFluidInputs).map(f->{var copy=f.copy();copy.setAmount(f.getAmount()*3);return copy;}).toList();
        var rest=RecipeInputs.consume(reaction,List.of(reaction.mInputs[0].copy()),fluids,3);
        h.assertTrue(rest!=null&&rest.items().get(0).getCount()==1&&rest.fluids().stream().allMatch(net.minecraftforge.fluids.FluidStack::isEmpty),"three plastic batches use one catalyst and exact fluids");
        h.assertTrue(RecipeInputs.consume(reaction,List.of(),fluids,1)==null,"catalyst still required");
        var ingot=GTItems.getStack(MaterialPrefix.ingot,plastic,1);
        var forming=MachineRecipeMaps.Extruder.findRecipe(List.of(powder,new ItemStack(GTTechnological.get("low_heat_extruder_shape_ingot"))),List.of(),false,2,1);
        h.assertTrue(forming!=null&&forming.mOutputs[0].is(ingot.getItem()),"plastic powder forms ingot with low-heat mold");
        var plating=MachineRecipeMaps.Extruder.findRecipe(List.of(ingot,new ItemStack(GTTechnological.get("low_heat_extruder_shape_plate"))),List.of(),false,2,1);
        h.assertTrue(plating!=null&&plating.mOutputs[0].is(GTItems.getStack(MaterialPrefix.plate,plastic,1).getItem()),"plastic ingot feeds circuit-board sheet forming");
        h.succeed();
    }

}
