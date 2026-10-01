package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.content.recipe.*;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class VanillaBlockProcessingTests {
    @GameTest(template="test_empty")
    public static void blockProcessingIsReachableAndConsumesExactCoolant(GameTestHelper h) {
        var entries=VanillaBlockProcessingRecipes.INSTANCE.entries();
        h.assertTrue(entries.size()>800,"Broad vanilla block processing coverage");
        for(var entry:entries) {
            var r=entry.recipe();
            var items=Arrays.asList(r.mInputs); var fluids=Arrays.asList(r.mFluidInputs);
            h.assertTrue(entry.map().findRecipe(items,fluids,false,entry.map().mInputItemsCount,entry.map().mOutputItemsCount)==r,"Unshadowed machine lookup: "+entry.id());
            var result=RecipeInputs.consume(r,items,fluids,1);
            h.assertTrue(result!=null && result.items().stream().allMatch(ItemStack::isEmpty),"Consumes source: "+entry.id());
            h.assertTrue(result.fluids().stream().allMatch(net.minecraftforge.fluids.FluidStack::isEmpty),"Exact coolant consumed");
            if(!fluids.isEmpty()) {
                var shortFluid=fluids.get(0).copy(); shortFluid.shrink(1);
                h.assertTrue(RecipeInputs.consume(r,items,List.of(shortFluid),1)==null,"Missing coolant must block recipe");
            }
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void sawCoolantsKeepSourceTimingAndFoodSafety(GameTestHelper h) {
        var water=row("saw/glass/Water"); var distilled=row("saw/glass/DistW"); var oil=row("saw/glass/Lubricant");
        h.assertTrue(water.mDuration==128 && water.mFluidInputs[0].getAmount()==200,"Water costs 4x");
        h.assertTrue(distilled.mDuration==96 && distilled.mFluidInputs[0].getAmount()==150,"Distilled costs 3x");
        h.assertTrue(oil.mDuration==32 && oil.mFluidInputs[0].getAmount()==50,"Lubricant costs 1x");
        h.assertTrue(water.mOutputs[0].is(Items.GLASS_PANE)&&water.mOutputs[0].getCount()==9,"GT6 nine panes per block");
        h.assertTrue(VanillaBlockProcessingRecipes.INSTANCE.entries().stream().filter(e->e.id().startsWith("saw/melon/")).count()==4,"Food only accepts four potable water variants");
        h.assertTrue(row("hammer/quartz_stairs").mOutputs[0].getCount()==4,"Modern stonecutter cannot multiply quartz");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void furniturePreservesPartsAndRejectsInventoryNbt(GameTestHelper h) {
        var r=row("saw/trapped_chest/Water");
        h.assertTrue(r.mOutputs[0].is(Items.OAK_PLANKS)&&r.mOutputs[0].getCount()==8&&r.mOutputs[1].is(Items.TRIPWIRE_HOOK),"Chest wood and hook recovered");
        var chest=r.mInputs[0].copy();
        var contents=new net.minecraft.nbt.ListTag(); contents.add(new ItemStack(Items.DIAMOND,32).save(new net.minecraft.nbt.CompoundTag()));
        chest.getOrCreateTagElement("BlockEntityTag").put("Items",contents);
        h.assertTrue(MachineRecipeMaps.Cutter.findRecipe(List.of(chest),Arrays.asList(r.mFluidInputs),false,1,3)==null,"Filled chest cannot enter dismantling recipe");
        var bed=row("saw/blue_bed/Water");
        h.assertTrue(bed.mOutputs[1].is(Items.BLUE_WOOL)&&bed.mOutputs[1].getCount()==3,"Bed retains wool color");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void woodProcessingClosesTheBarkPaperAndCreosoteChains(GameTestHelper h) {
        var entries=VanillaWoodProcessingRecipes.INSTANCE.entries();
        h.assertTrue(entries.size()==424,"Eight wood species with 53 machine recipes each");
        for(var e:entries) {
            var r=e.recipe();
            h.assertTrue(e.map().findRecipe(Arrays.asList(r.mInputs),Arrays.asList(r.mFluidInputs),false,e.map().mInputItemsCount,e.map().mOutputItemsCount)==r,"Wood recipe reachable: "+e.id());
            h.assertTrue(RecipeInputs.consume(r,Arrays.asList(r.mInputs),Arrays.asList(r.mFluidInputs),1)!=null,"Wood recipe executable");
        }
        var coke=entries.stream().filter(e->e.id().equals("coke/oak_log")).findFirst().orElseThrow().recipe();
        h.assertTrue(coke.mEUt==0&&coke.mDuration==3600&&coke.mOutputs[0].is(Items.CHARCOAL)&&coke.mFluidOutputs[0].getAmount()==250,"GT6 wood coke oven charcoal/creosote");
        var saw=entries.stream().filter(e->e.id().equals("saw/oak_log/Lubricant")).findFirst().orElseThrow().recipe();
        h.assertTrue(saw.mOutputs[0].is(Items.OAK_PLANKS)&&saw.mOutputs[0].getCount()==6,"Industrial saw beats hand crafting");
        var bark=saw.mOutputs[1];
        h.assertTrue(MachineRecipeMaps.Bath.findRecipe(List.of(bark),List.of(DyeProcessingRecipes.fluid("Water",125)),false,2,2)!=null,"Bark byproduct reaches existing paper bath");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void compositionAgreesWithDebarkingAndModernQuartzRecovery(GameTestHelper h) {
        var log=com.gregtech.gregtech.api.material.ItemMaterialRegistry.get(new ItemStack(Items.OAK_LOG)).orElseThrow();
        var stripped=com.gregtech.gregtech.api.material.ItemMaterialRegistry.get(new ItemStack(Items.STRIPPED_OAK_LOG)).orElseThrow();
        long u=com.gregtech.gregtech.api.material.GTValues.U;
        h.assertTrue(log.components().stream().mapToLong(c->c.amount()).sum()==9*u,"GT6 raw wood plus bark mass");
        h.assertTrue(stripped.components().stream().mapToLong(c->c.amount()).sum()==8*u,"Debarking removes exactly one unit");
        var quartz=com.gregtech.gregtech.api.material.ItemMaterialRegistry.get(new ItemStack(Items.QUARTZ_STAIRS)).orElseThrow();
        h.assertTrue(quartz.amount()==4*u,"Shredder/crucible cannot restore legacy six-quartz stair yield");
        h.succeed();
    }
    private static Recipe row(String id) {
        return VanillaBlockProcessingRecipes.INSTANCE.entries().stream().filter(e->e.id().equals(id)).findFirst().orElseThrow().recipe();
    }
}
