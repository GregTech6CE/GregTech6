package gregtech.gametest;

import gregtech.internal.recipe.RecipeInputs;
import gregtech.content.recipe.DyeProcessingRecipes;
import gregtech.content.data.MachineRecipeMaps;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("gt6ce") @PrefixGameTestTemplate(false)
public final class DyeProcessingTests {
    @GameTest(template="test_empty") public static void allDyeRecipesConsumeActualRegisteredInputs(GameTestHelper h){
        int colors=gregtech.content.data.RegisteredFluids.DYE_OREDICTS_POST.length;
        int coolants=gregtech.content.recipe.SawingCoolants.variants(false).size();
        int expected=colors*(27+3*coolants)+26+18+9;
        h.assertTrue(DyeProcessingRecipes.entries().size()==expected,"source color families, coolant variants, flowers and precursors: expected "+expected+", got "+DyeProcessingRecipes.entries().size());
        for(var entry:DyeProcessingRecipes.entries()){
            var recipe=entry.recipe();
            h.assertTrue(entry.map().mRecipeList.contains(recipe),"Published to machine map and JEI");
            var items=Arrays.stream(recipe.mInputs).map(ItemStack::copy).toList();
            var fluids=Arrays.stream(recipe.mFluidInputs).map(f->f.copy()).toList();
            var result=RecipeInputs.consume(recipe,items,fluids,1);
            h.assertTrue(result!=null&&result.fluids().stream().allMatch(f->f.isEmpty()),"All exact item/fluid quantities consumed");
            for(int i=0;i<items.size();i++) h.assertTrue(recipe.isCatalystInput(i)
                    ? ItemStack.isSameItemSameTags(items.get(i),result.items().get(i))&&items.get(i).getCount()==result.items().get(i).getCount()
                    : result.items().get(i).isEmpty(),"only declared item catalysts remain");
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
        var chemical=DyeProcessingRecipes.entries().stream().map(e->e.recipe()).filter(r->r.mFluidOutputs.length>0&&r.mFluidOutputs[0].isFluidEqual(DyeProcessingRecipes.fluid("Dye_Chemical_Red",1))).findFirst().orElseThrow();
        h.assertTrue(chemical.mFluidInputs[0].getAmount()==216&&chemical.mFluidInputs[1].getAmount()==20&&chemical.mFluidOutputs[0].getAmount()==288,"Oil conversion: 216 + 20 -> 288 mB");
        var owned=DyeProcessingRecipes.entries().stream().map(e->e.recipe()).filter(r->r.mFluidOutputs.length>0&&r.mFluidOutputs[0].isFluidEqual(DyeProcessingRecipes.fluid("CFoam_DyedOwned_Red",1))&&r.mDuration==64).findFirst().orElseThrow();
        h.assertTrue(owned.mInputs[0].getCount()==1&&owned.mFluidInputs[0].getAmount()==400&&owned.mFluidOutputs[0].getAmount()==400,"Full palladium dust processes four foam units");
        h.succeed();
    }
}
