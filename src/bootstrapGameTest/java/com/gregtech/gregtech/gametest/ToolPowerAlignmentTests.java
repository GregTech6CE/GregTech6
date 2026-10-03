package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.recipe.*;
import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.blockentity.machine.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import java.util.*;

@GameTestHolder("gregtech_alignment") @PrefixGameTestTemplate(false)
public final class ToolPowerAlignmentTests {
    private static ItemStack form(MaterialPrefix prefix, com.gregtech.gregtech.api.material.GTMaterial material) {
        var stack=GTItems.getStack(prefix,material,1);
        if(stack.isEmpty())throw new IllegalStateException("Missing fixture form "+prefix+"/"+material);
        return stack;
    }
    private static ItemStack tool(GTToolType type) { return GTToolHelper.displayTool(type); }
    private static CraftingRecipe recipe(GameTestHelper h, String path) {
        return (CraftingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("gregtech",path)).orElseThrow();
    }
    private static net.minecraft.world.inventory.CraftingContainer grid(int width,int height,List<ItemStack> stacks) {
            var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0) {
                public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}
                public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}
            };
            var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,width,height);
            for(int i=0;i<stacks.size();i++)grid.setItem(i,stacks.get(i));return grid;
        }
    @GameTest(template="test_empty", timeoutTicks=80)
    public static void everyDisplayedManualRowCraftsItsDisplayedResult(GameTestHelper h) {
        int count=0;var failures=new ArrayList<String>();
        for(var type:GTToolType.values()) {
            for(boolean head:new boolean[]{false,true}) {
                var patterns=head?GTToolRecipes.heads(type):GTToolRecipes.shaped(type);
                for(int index=0;index<patterns.size();index++) {
                    String path=(head?"tool_heads/":"tools/")+type.id()+(index==0?"":"_"+index);
                    var pattern=patterns.get(index);
                    if (!GTToolPatternRecipe.hasMaterials(type,pattern)) continue;
                    var row=recipe(h,path);
                    var stacks=new ArrayList<ItemStack>();
                    for(var ingredient:row.getIngredients()) {
                        if(ingredient.isEmpty())stacks.add(ItemStack.EMPTY);
                        else { var stack=ingredient.getItems()[0].copy();
                            if(stack.getItem() instanceof GTToolItem item)stack=tool(item.toolType());
                            stacks.add(stack); }
                    }
                    var grid=grid(pattern.width(),pattern.height(),stacks);
                    if (!row.matches(grid,h.getLevel())) { failures.add(path+" grid="+stacks);continue; }
                    var actual=row.assemble(grid,h.getLevel().registryAccess());
                    if (!ItemStack.isSameItemSameTags(actual,row.getResultItem(h.getLevel().registryAccess()))) failures.add(path+" output disagrees with viewer");
                    var remains=row.getRemainingItems(grid);
                    for(int slot=0;slot<stacks.size();slot++)if(stacks.get(slot).getItem() instanceof GTToolItem item)
                        h.assertTrue(remains.get(slot).getItem()==item&&remains.get(slot).getDamageValue()==item.toolType().damagePerCraft(),"original catalyst wear "+path);
                    count++;
                }
            }
        }
        int catalogRows=0;
        for (var info : ToolAssemblyCatalog.build()) {
            String path="tools/"+info.type().id()+(info.headAssembly()?"_assembly":"");
            var row=recipe(h,path);var stacks=new ArrayList<ItemStack>();int index=0;
            if (info.pattern()!=null) {
                for (int y=0;y<info.pattern().height();y++) for (int x=0;x<info.pattern().width();x++)
                    stacks.add(info.pattern().at(x,y)==' '?ItemStack.EMPTY:info.inputs().get(index++).get(0).copy());
            } else for (var options : info.inputs()) stacks.add(options.get(0).copy());
            var grid=info.pattern()==null?grid(stacks.size(),1,stacks):grid(info.pattern().width(),info.pattern().height(),stacks);
            if (!row.matches(grid,h.getLevel()) || !ItemStack.isSameItemSameTags(row.assemble(grid,h.getLevel().registryAccess()),info.output()))
                failures.add("JEI catalog "+path+" grid="+stacks);
            catalogRows++;
        }
        com.mojang.logging.LogUtils.getLogger().info("TOOL_ALIGNMENT_CATALOG_ROWS {}",catalogRows);
        h.assertTrue(failures.isEmpty(),"Invalid displayed tool rows: "+failures);
        com.mojang.logging.LogUtils.getLogger().info("TOOL_ALIGNMENT_ROWS_SUCCESS {}",count);h.succeed();
    }
    @GameTest(template="test_empty")
    public static void originalSpecialObjectsAndHandleRestrictions(GameTestHelper h) {
        var bronze=form(MaterialPrefix.plate,Materials.Bronze);var rod=form(MaterialPrefix.stick,Materials.Bronze);
        // Loader_Tools: crowbar V is blue dye, wrench head V/W are steel rings/screws.
        var crowbar=grid(3,3,List.of(tool(GTToolType.HARD_HAMMER),new ItemStack(Items.BLUE_DYE),rod.copy(),
            new ItemStack(Items.BLUE_DYE),rod.copy(),new ItemStack(Items.BLUE_DYE),rod.copy(),new ItemStack(Items.BLUE_DYE),tool(GTToolType.FILE)));
        h.assertTrue(recipe(h,"tools/crowbar").matches(crowbar,h.getLevel()),"crowbar uses independent blue dye");
        var screw=form(MaterialPrefix.screw,Materials.Steel);var ring=form(MaterialPrefix.ring,Materials.Steel);
        var wrenchHead=grid(3,3,List.of(tool(GTToolType.HARD_HAMMER),bronze.copy(),screw.copy(),bronze.copy(),ring,
            bronze.copy(),screw.copy(),bronze.copy(),tool(GTToolType.SCREWDRIVER)));
        h.assertTrue(recipe(h,"tool_heads/wrench").matches(wrenchHead,h.getLevel()),"bronze wrench head uses steel ring and screws");
        var pick=recipe(h,"tools/pickaxe_assembly");
        h.assertTrue(pick.matches(grid(2,1,List.of(form(MaterialPrefix.toolHeadPickaxe,Materials.Bronze),new ItemStack(Items.STICK))),h.getLevel()),"vanilla wooden stick is normal bronze handle");
        h.assertTrue(!pick.matches(grid(2,1,List.of(form(MaterialPrefix.toolHeadPickaxe,Materials.Bronze),form(MaterialPrefix.stick,Materials.Iron))),h.getLevel()),"iron is not a wood/plastic handle");
        var knife=grid(2,2,List.of(tool(GTToolType.FILE),bronze.copy(),tool(GTToolType.HARD_HAMMER),rod.copy()));
        h.assertTrue(recipe(h,"tools/knife").matches(knife,h.getLevel()),"one-piece bronze knife uses bronze handle");
        h.succeed();
    }
    private static Block block(String id) {var key=ResourceLocation.fromNamespaceAndPath("gregtech",id);if(!BuiltInRegistries.BLOCK.containsKey(key))throw new IllegalStateException("Missing block "+id);return BuiltInRegistries.BLOCK.get(key);}
    private static IFluidHandler fluid(GameTestHelper h, BlockPos pos, Direction side) { return h.getLevel().getBlockEntity(pos).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,side).resolve().orElse(null); }
    @GameTest(template="test_empty")
    public static void turbineNeedsNoInstalledRotorAndOnlyRearSteam(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,2,1));var state=block("steam_turbine_bronze").defaultBlockState().setValue(DirectionalBlock.FACING,Direction.EAST);
        h.getLevel().setBlockAndUpdate(pos,state);var node=(EnergyNodeBlockEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(!node.hasRotor()&&!node.installRotor(form(MaterialPrefix.rotor,Materials.Bronze)),"no runtime rotor installation");
        h.assertTrue(fluid(h,pos,Direction.EAST)==null&&fluid(h,pos,Direction.UP)==null,"only rear fluid inlet");
        var inlet=fluid(h,pos,Direction.WEST);
        h.assertTrue(inlet.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,100),IFluidHandler.FluidAction.EXECUTE)==0,"water cannot produce RU");
        h.assertTrue(inlet.fill(new FluidStack(GTFluids.still("Steam").get(),96),IFluidHandler.FluidAction.EXECUTE)==96,"96 L actual steam accepted");
        h.assertTrue(inlet.drain(96,IFluidHandler.FluidAction.EXECUTE).isEmpty(),"steam cannot be extracted from source inlet");
        node.machineControl(Direction.WEST).setEnabled(false);
        h.assertTrue(inlet.fill(new FluidStack(GTFluids.still("Steam").get(),96),IFluidHandler.FluidAction.EXECUTE)==0,"stopping closes the steam inlet");
        EnergyNodeBlockEntity.serverTick(h.getLevel(),pos,state,node);
        h.assertTrue(node.stored()==0&&inlet.getFluidInTank(0).isEmpty(),"blocked output spends source waste energy immediately");
        EnergyNodeBlockEntity.serverTick(h.getLevel(),pos,state,node);
        h.assertTrue(node.stored()==0,"second staged half is also spent");h.succeed();
    }
    @GameTest(template="test_empty", timeoutTicks=120)
    public static void realBoilerTurbineDynamoPowersElectrolyzer(GameTestHelper h) {
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(1,1,1));
        level.setBlockAndUpdate(base,block("steam_boiler_bronze").defaultBlockState());
        level.setBlockAndUpdate(base.above(),block("steam_turbine_bronze").defaultBlockState().setValue(DirectionalBlock.FACING,Direction.UP));
        level.setBlockAndUpdate(base.above(2),block("electric_dynamo_lv").defaultBlockState().setValue(DirectionalBlock.FACING,Direction.UP));
        level.setBlockAndUpdate(base.above(3),block("electrolyzer_galvanized_steel").defaultBlockState());
        // Real automatic output needs somewhere to go; machines eject into open air otherwise.
        var outputPositions=java.util.Arrays.stream(Direction.values()).filter(d -> d.getAxis().isHorizontal())
            .map(d -> base.above(3).relative(d)).toList();
        for (var outputPos : outputPositions) level.setBlockAndUpdate(outputPos,Blocks.CHEST.defaultBlockState());
        var boiler=(BoilerTankBlockEntity)level.getBlockEntity(base);
        var turbine=(EnergyNodeBlockEntity)level.getBlockEntity(base.above());
        var dynamo=(EnergyNodeBlockEntity)level.getBlockEntity(base.above(2));
        var machine=(BasicMachineBlockEntity)level.getBlockEntity(base.above(3));
        var fixture=machine.recipeMap().mRecipeList.stream().filter(r->r.mEnabled&&!r.mFakeRecipe&&r.mEUt>0&&r.mEUt<=22
            &&com.gregtech.gregtech.api.recipe.MachineWorkCost.calculate(r.mEUt,r.mDuration,1,false,10000,16,64,false).minimumPower()<=22
            &&r.mInputs.length<=machine.recipeMap().mInputItemsCount&&r.mFluidInputs.length<=machine.getTanksInput().length
            &&r.mOutputs.length<=machine.recipeMap().mOutputItemsCount&&r.mFluidOutputs.length<=machine.getTanksOutput().length
            &&r.mInputs.length>0&&r.mOutputs.length>0&&Arrays.stream(r.mInputs).allMatch(s->!s.isEmpty()&&s.getCount()<=64)
            &&Arrays.stream(r.mFluidInputs).allMatch(f->f.getAmount()<=8000)&&Arrays.stream(r.mChances).allMatch(c->c>=10000))
            .min(Comparator.comparingLong(r->r.mDuration)).orElseThrow(()->new IllegalStateException("No real LV electrolyzer fixture"));
        var inventory=machine.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new);
        for(int i=0;i<fixture.mInputs.length;i++)h.assertTrue(inventory.insertItem(i,fixture.mInputs[i].copy(),false).isEmpty(),"fixture input insertion");
        for(int i=0;i<fixture.mFluidInputs.length;i++)h.assertTrue(machine.getTanksInput()[i].fill(fixture.mFluidInputs[i].copy(),IFluidHandler.FluidAction.EXECUTE)==fixture.mFluidInputs[i].getAmount(),"fixture fluid insertion");
        com.mojang.logging.LogUtils.getLogger().info("STEAM_FIXTURE eu={} duration={} inputs={} outputs={} fluidsOut={}",fixture.mEUt,fixture.mDuration,Arrays.toString(fixture.mInputs),Arrays.toString(fixture.mOutputs),Arrays.toString(fixture.mFluidOutputs));
        int powered=0;long peak=0;
        for(int tick=0;tick<5000;tick++) {
            // External heat/water supply; every litre of steam is made by the registered boiler.
            boiler.waterTank().fill(new FluidStack(GTFluids.still("DistW").get(),4000),IFluidHandler.FluidAction.EXECUTE);
            boiler.doEnergyInjection(GregTechTags.Energy.HU,Direction.DOWN,80,1,true);
            BoilerTankBlockEntity.serverTick(level,boiler.getBlockPos(),boiler.getBlockState(),boiler);
            EnergyNodeBlockEntity.serverTick(level,turbine.getBlockPos(),turbine.getBlockState(),turbine);
            EnergyNodeBlockEntity.serverTick(level,dynamo.getBlockPos(),dynamo.getBlockState(),dynamo);
            peak=Math.max(peak,machine.getEnergyTick());if(machine.getEnergyTick()>0)powered++;
            BasicMachineBlockEntity.serverTick(level,machine.getBlockPos(),machine.getBlockState(),machine);
            boolean output=false;for(int slot=machine.recipeMap().mInputItemsCount;slot<inventory.getSlots();slot++)output|=!inventory.getStackInSlot(slot).isEmpty();
            for (var outputPos : outputPositions) if (level.getBlockEntity(outputPos) instanceof net.minecraft.world.Container chest)
                for (int slot=0;slot<chest.getContainerSize();slot++) output |= chest.getItem(slot).is(fixture.mOutputs[0].getItem());
            if(output){h.assertTrue(powered>0&&peak>=16&&!turbine.hasRotor(),"actual steam/RU/EU chain processed electrolyzer job without rotor");
                com.mojang.logging.LogUtils.getLogger().info("STEAM_ELECTROLYZER_CHAIN_SUCCESS ticks={} poweredTicks={} peakEU={} recipeEU={} duration={} input={} output={}",tick,powered,peak,fixture.mEUt,fixture.mDuration,Arrays.toString(fixture.mInputs),Arrays.toString(fixture.mOutputs));h.succeed();return;}
        }
        throw new IllegalStateException("Steam chain stalled: boilerSteam="+boiler.steamTank().getAmount()+" turbine="+turbine.stored()+" dynamo="+dynamo.stored()+" progress="+machine.getProgressPercent()+" peakEU="+peak+" poweredTicks="+powered+" fixtureEU="+fixture.mEUt+" duration="+fixture.mDuration+" machineNBT="+machine.saveWithoutMetadata());
    }
}
