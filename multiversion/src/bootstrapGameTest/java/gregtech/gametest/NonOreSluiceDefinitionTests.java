package gregtech.gametest;

import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.recipe.*;
import gregtech.internal.recipe.*;
import gregtech.content.recipe.DormantOreRows;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Fixed-only real native map contract for immutable Sluice descriptor data. The temporary map is
 * not a gameplay/addon-pipeline surrogate: actual owned Sluice lifecycle is verified separately. */
@GameTestHolder("gregtech_ore_alignment_batch")
@PrefixGameTestTemplate(false)
public final class NonOreSluiceDefinitionTests {
    @GameTest(template="test_empty",batch="ore_alignment_batch",timeoutTicks=200)
    public static void waterKeysRetainFluidOutputsCopiesAndExactOwnership(GameTestHelper h) {
        var map=new RecipeMap(new ArrayList<>(),"gt.recipe.test.sluice_definitions","Sluice definitions","test",
                1,9,1,1,1,1,2,false,false,true,true);
        var ledger=new DormantOreRows();var material=GTMaterialRegistry.get("PVC").resolve();
        try {
            Recipe startup=map.addRecipe1(false,16,32,new ItemStack(Items.APPLE),
                    NonOreSluiceAssertions.fluid(h,"Water",900),NonOreSluiceAssertions.fluid(h,"Sluice",17),new ItemStack(Items.NETHER_STAR));
            h.assertTrue(startup!=null,"independent fluid-aware collision incumbent admitted");
            for(boolean tiny:new boolean[]{false,true})for(String field:NonOreSluiceAssertions.WATERS) {
                int mb=tiny?100:900;ItemStack input=new ItemStack(tiny?Items.CARROT:Items.APPLE);
                ItemStack[] outputs=new ItemStack[9];outputs[0]=new ItemStack(Items.DIAMOND);
                for(int i=1;i<9;i++)outputs[i]=new ItemStack(Items.COAL,tiny?1:9);
                long[] chances={10000,300,300,300,300,300,300,300,300};
                FluidStack water=NonOreSluiceAssertions.fluid(h,field,mb),slurry=NonOreSluiceAssertions.fluid(h,"Sluice",mb);
                FluidStack[] in={water},out={slurry};
                ledger.define(material,(tiny?"sluice/tiny/":"sluice/whole/")+BuiltInRegistries.FLUID.getKey(water.getFluid()),
                        map,true,input,outputs,chances,in,out,tiny?16:144,16);
                // Mutate both nested values and containers after capture; neither may leak in.
                input.setCount(32);outputs[0].setCount(32);outputs[1]=ItemStack.EMPTY;chances[1]=1;
                water.setAmount(1);slurry.setAmount(2);in[0]=FluidStack.EMPTY;out[0]=FluidStack.EMPTY;
            }
            ledger.seal();h.assertTrue(ledger.definitionCount()==8&&ledger.owned().isEmpty(),"eight distinct definitions, no eager admission");
            h.assertTrue(ledger.reconcile(m->false)==0&&map.mRecipeList.size()==1,"NO retains independent row only");
            h.assertTrue(ledger.reconcile(m->true)==7,"eight keys admit seven actual rows and preserve collision winner");
            var first=check(h,map,ledger,startup);
            map.captureBaseline(ledger.owned());map.restoreBaseline();
            h.assertTrue(map.mRecipeList.size()==1&&select(map,false,"Water")==startup,"baseline excludes only accepted owned instances");
            h.assertTrue(ledger.reconcile(m->true)==7,"all noncolliding water keys replay");
            var second=check(h,map,ledger,startup);
            h.assertTrue(first.stream().noneMatch(second::contains),"replay creates fresh accepted identities");
            Recipe changed=select(map,true,"DistW");changed.mFluidOutputs[0].setAmount(7);
            ledger.clear();h.assertTrue(ledger.reconcile(m->true)==7,"replay after output mutation uses immutable capture");
            check(h,map,ledger,startup);
            Recipe stale=select(map,false,"MnWtr");h.assertTrue(map.unlink(stale),"owned row removed by independent caller");
            Recipe replacement=map.addRecipe1(false,16,48,new ItemStack(Items.APPLE),
                    NonOreSluiceAssertions.fluid(h,"MnWtr",900),NonOreSluiceAssertions.fluid(h,"Sluice",77),new ItemStack(Items.EMERALD));
            h.assertTrue(replacement!=null,"independent same-input/fluid replacement admitted");
            ledger.clear();
            h.assertTrue(select(map,false,"MnWtr")==replacement&&select(map,false,"Water")==startup&&map.mRecipeList.size()==2,
                    "stale owned handle never deletes independent replacement or startup winner");
            h.assertTrue(ledger.reconcile(m->true)==6,"both independent per-water winners survive later collision admission");
            h.assertTrue(select(map,false,"MnWtr")==replacement&&select(map,false,"Water")==startup,"warm lookup still selects both winners");
            ledger.reconcile(m->false);
            h.assertTrue(map.mRecipeList.size()==2&&ledger.owned().isEmpty()&&ledger.definitionCount()==8,"deactivation clears exact six rows, retains definitions and independent rows");
            for(boolean tiny:new boolean[]{false,true})for(String field:NonOreSluiceAssertions.WATERS) {
                Recipe expected=!tiny&&field.equals("Water")?startup:!tiny&&field.equals("MnWtr")?replacement:null;
                h.assertTrue(select(map,tiny,field)==expected&&select(map,tiny,field)==expected,"all eight warmed keys cleared/preserved exactly "+tiny+"/"+field);
            }
        } finally {ledger.clear();RecipeMap.unregister(map);}
        h.succeed();
    }
    private static Set<Recipe> check(GameTestHelper h,RecipeMap map,DormantOreRows ledger,Recipe startup) {
        Set<Recipe> result=Collections.newSetFromMap(new IdentityHashMap<>());
        for(boolean tiny:new boolean[]{false,true})for(String field:NonOreSluiceAssertions.WATERS) {
            Recipe row=select(map,tiny,field);
            if(!tiny&&field.equals("Water")){
                h.assertTrue(row==startup&&!ledger.owned().contains(row),"startup collision winner is never claimed");continue;
            }
            int mb=tiny?100:900;
            h.assertTrue(row!=null&&map.mRecipeList.contains(row)&&ledger.owned().contains(row)&&select(map,tiny,field)==row,"actual accepted cold/warm fluid row "+tiny+"/"+field);
            h.assertTrue(row.mInputs.length==1&&row.mInputs[0].getCount()==1&&row.mInputs[0].is(tiny?Items.CARROT:Items.APPLE),"captured input copied");
            h.assertTrue(row.mOutputs.length==9&&row.mOutputs[0].is(Items.DIAMOND)&&row.mOutputs[0].getCount()==1,"captured nine-output primary");
            for(int i=1;i<9;i++)h.assertTrue(row.mOutputs[i].is(Items.COAL)&&row.mOutputs[i].getCount()==(tiny?1:9)&&row.getOutputChance(i)==300,"independent fine/chance slot "+i);
            h.assertTrue(row.getOutputChance(0)==10000&&row.mFluidInputs.length==1&&row.mFluidOutputs.length==1,"captured complete chance/fluid arrays");
            h.assertTrue(row.mFluidInputs[0].getFluid()==NonOreSluiceAssertions.fluid(h,field,mb).getFluid()&&row.mFluidInputs[0].getAmount()==mb,
                    "deep-copied actual water identity/amount");
            h.assertTrue(row.mFluidOutputs[0].getFluid()==NonOreSluiceAssertions.fluid(h,"Sluice",mb).getFluid()&&row.mFluidOutputs[0].getAmount()==mb,
                    "deep-copied actual slurry identity/amount");
            h.assertTrue(row.mDuration==(tiny?16:144)&&row.mEUt==16,"captured work preserved");result.add(row);
        }
        h.assertTrue(result.size()==7,"every noncolliding variant separately accepted");return result;
    }
    private static Recipe select(RecipeMap map,boolean tiny,String water){
        var fluid=gregtech.content.registry.GTFluids.stack(water,tiny?100:900);
        return map.findRecipe(List.of(new ItemStack(tiny?Items.CARROT:Items.APPLE)),List.of(fluid),false,1,9);
    }
}
