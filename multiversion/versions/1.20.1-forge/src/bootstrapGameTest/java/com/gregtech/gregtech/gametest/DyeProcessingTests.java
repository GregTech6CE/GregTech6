package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.recipe.RecipeInputs;
import com.gregtech.gregtech.content.recipe.DyeProcessingRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class DyeProcessingTests {
    @GameTest(template="test_empty") public static void allDyeRecipesConsumeActualRegisteredInputs(GameTestHelper h){
        h.assertTrue(DyeProcessingRecipes.entries().size()==348,"304 dye mixers, 26 flowers, and 18 oil/foam precursors");
        for(var entry:DyeProcessingRecipes.entries()){
            var recipe=entry.recipe();
            h.assertTrue(entry.map().mRecipeList.contains(recipe),"Published to machine map and JEI");
            var items=Arrays.stream(recipe.mInputs).map(ItemStack::copy).toList();
            var fluids=Arrays.stream(recipe.mFluidInputs).map(f->f.copy()).toList();
            var result=RecipeInputs.consume(recipe,items,fluids,1);
            h.assertTrue(result!=null&&result.items().stream().allMatch(ItemStack::isEmpty)&&result.fluids().stream().allMatch(f->f.isEmpty()),"All exact item/fluid quantities consumed");
            if(!fluids.isEmpty()){
                var shortInput=new ArrayList<>(fluids);var first=shortInput.get(0).copy();first.shrink(1);shortInput.set(0,first);
                h.assertTrue(RecipeInputs.consume(recipe,items,shortInput,1)==null,"One mB short must not process");
            }
        }
        h.succeed();
    }
    @GameTest(template="test_empty") public static void redDyeChainKeepsOriginalAmountsAndResidueChance(GameTestHelper h){
        var flower=DyeProcessingRecipes.entries().stream().filter(e->e.map()==MachineRecipeMaps.Squeezer&&e.recipe().mInputs[0].is(Items.POPPY)).findFirst().orElseThrow().recipe();
        h.assertTrue(flower.mFluidOutputs[0].getAmount()==288&&flower.getOutputChance(0)==2000,"Poppy: 288 mB and 20 percent remains");
        var chemical=DyeProcessingRecipes.entries().stream().map(e->e.recipe()).filter(r->r.mFluidOutputs[0].isFluidEqual(DyeProcessingRecipes.fluid("Dye_Chemical_Red",1))).findFirst().orElseThrow();
        h.assertTrue(chemical.mFluidInputs[0].getAmount()==216&&chemical.mFluidInputs[1].getAmount()==20&&chemical.mFluidOutputs[0].getAmount()==288,"Oil conversion: 216 + 20 -> 288 mB");
        var owned=DyeProcessingRecipes.entries().stream().map(e->e.recipe()).filter(r->r.mFluidOutputs[0].isFluidEqual(DyeProcessingRecipes.fluid("CFoam_DyedOwned_Red",1))&&r.mDuration==64).findFirst().orElseThrow();
        h.assertTrue(owned.mInputs[0].getCount()==1&&owned.mFluidInputs[0].getAmount()==400&&owned.mFluidOutputs[0].getAmount()==400,"Full palladium dust processes four foam units");
        h.succeed();
    }
}
