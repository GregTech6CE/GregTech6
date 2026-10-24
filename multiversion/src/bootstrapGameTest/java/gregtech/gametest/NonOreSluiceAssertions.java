package gregtech.gametest;

import gregtech.api.material.*;
import gregtech.internal.material.*;
import gregtech.internal.material.*;
import gregtech.internal.recipe.Recipe;
import gregtech.content.recipe.NonOreMacerationRows;
import gregtech.content.data.*;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Shared read-only native oracles for the combined raw/Sluice batch. No standalone GameTests,
 * row injection, fake reload or production-policy-derived expected eligibility.
 * Source Ores383/385-398/415-420 and FL.waters687-689; literal identity/phase bindings below.
 */
final class NonOreSluiceAssertions {
    private NonOreSluiceAssertions() {}
    static final String[] WATERS={"Water","MnWtr","DistW","SpDew"};
    private static final long[] CHANCES={10000,300,300,300,300,300,300,300,300};

    /** Called from the independent literal53 census; caller supplies its independently classified
     * startup/external override decision. Collect all actual observations before final absence fail.
     * Returns actual accepted candidate count, not a presumed53x8 native admission census. */
    static int auditNo(GameTestHelper h,GTMaterial material,boolean noOverride,List<String> failures) {
        int accepted=0;
        for(boolean tiny:new boolean[]{false,true})for(String water:WATERS) {
            ItemStack input=input(h,material,tiny);FluidStack fluid=fluid(h,water,tiny?100:900);
            if(noOverride)h.assertTrue(BuiltInRegistries.ITEM.getKey(input.getItem()).getNamespace().equals("gt6ce"),
                    "source-NO comparison requires canonical builtin input, not external replacement");
            String label=key(material,tiny,water);
            List<Recipe> rows=rows(input,fluid);
            Recipe selected=lookup(material,tiny,water);
            h.assertTrue(lookup(material,tiny,water)==selected,"stable warm Sluice selection "+label);
            if(selected!=null)h.assertTrue(rows.contains(selected),"Sluice selection is actual admitted row "+label);
            System.out.println("[nonore-sluice-row] "+label+" accepted="+rows.size()+" selected="+(selected!=null)+" noOverride="+noOverride);
            for(Recipe row:rows){
                accepted++;shape(h,row,input,fluid,tiny,label);
                System.out.println("[nonore-sluice-detail] "+label+" "+fingerprint(row));
            }
            if(Set.of("Snow","Ice","PVC").contains(material.getName())&&!rows.isEmpty())
                literalSelf(h,material,tiny,water,false);
            if(noOverride&&(!rows.isEmpty()||selected!=null))failures.add(label);
        }
        return accepted;
    }

    static void absent(GameTestHelper h,GTMaterial material) {
        var failures=new ArrayList<String>();auditNo(h,material,true,failures);
        h.assertTrue(failures.isEmpty(),"owned source-NO Sluice rows/ warm lookups must be absent: "+failures);
    }

    /** Real current map preservation for UNKNOWN/addon/startup overrides, not full source parity.
     * Caller/checker compares these exact row fingerprints with its baseline receipts. */
    static Map<String,Recipe> preserved(GameTestHelper h,GTMaterial material) {
        Map<String,Recipe> selected=new LinkedHashMap<>();
        for(boolean tiny:new boolean[]{false,true})for(String water:WATERS) {
            var input=input(h,material,tiny);var fluid=fluid(h,water,tiny?100:900);
            String label=key(material,tiny,water);var rows=rows(input,fluid);var row=lookup(material,tiny,water);
            h.assertTrue(rows.size()==1&&row==rows.get(0)&&lookup(material,tiny,water)==row,"preserved unique cold/warm Sluice admission "+label);
            shape(h,row,input,fluid,tiny,label);selected.put(label,row);
            System.out.println("[nonore-sluice-preserved] "+label+" "+fingerprint(row));
        }
        return Map.copyOf(selected);
    }

    /** Existing source-positive Cinnabar nine-slot oracle, including all eight independent fines.
     * This helper intentionally never calls h.succeed() on behalf of its enclosing test. */
    static void cinnabar(GameTestHelper h) {
        var material=GTMaterialRegistry.get("Cinnabar").resolve();
        h.assertTrue(material.getId()==9114&&material.getName().equals("Cinnabar"),"literal Cinnabar identity");
        for(boolean tiny:new boolean[]{false,true})for(String water:WATERS) {
            int amount=tiny?1:9;ItemStack[] outputs=new ItemStack[9];
            outputs[0]=form(h,tiny?MaterialPrefix.crushedPurifiedTiny:MaterialPrefix.crushedPurified,material,1);
            String[] products={"Redstone","Sulfur","Glowstone","Selenium","Cinnabar","Cinnabar","Cinnabar","Cinnabar"};
            for(int i=0;i<products.length;i++)outputs[i+1]=form(h,MaterialPrefix.crushedPurifiedTiny,GTMaterialRegistry.get(products[i]).resolve(),amount);
            var fluid=fluid(h,water,tiny?100:900);
            OreSourceDifferentialTests.row(h,"literal Cinnabar Sluice "+water+"/"+tiny,MachineRecipeMaps.Sluice,input(h,material,tiny),
                    new FluidStack[]{fluid},outputs,CHANCES,new FluidStack[]{fluid(h,"Sluice",tiny?100:900)},tiny?16:144,16);
            shape(h,lookup(material,tiny,water),input(h,material,tiny),fluid,tiny,key(material,tiny,water));
        }
    }

    /** Literal preserved self-product row after a real tag continuation. The amount is independent
     * of the constructor/ledger and stays900/100mB even after repeated admission. */
    static Recipe literalSelf(GameTestHelper h,GTMaterial material,boolean tiny,String water,boolean owned) {
        h.assertTrue(material.getByProducts().isEmpty(),"reviewed empty byproduct fixture "+material.getName());
        int amount=tiny?1:9;ItemStack[] outputs=new ItemStack[9];
        outputs[0]=form(h,tiny?MaterialPrefix.crushedPurifiedTiny:MaterialPrefix.crushedPurified,material,1);
        for(int i=1;i<outputs.length;i++)outputs[i]=form(h,MaterialPrefix.crushedPurifiedTiny,material,amount);
        var input=input(h,material,tiny);var fluid=fluid(h,water,tiny?100:900);String label=key(material,tiny,water);
        OreSourceDifferentialTests.row(h,"literal self Sluice "+label,MachineRecipeMaps.Sluice,input,new FluidStack[]{fluid},
                outputs,CHANCES,new FluidStack[]{fluid(h,"Sluice",tiny?100:900)},tiny?16:144,16);
        Recipe row=lookup(material,tiny,water);
        h.assertTrue(row!=null&&lookup(material,tiny,water)==row,"literal Sluice cold/warm selection "+label);
        if(owned)h.assertTrue(NonOreMacerationRows.owned().contains(row),"exact accepted Sluice handle owned "+label);
        shape(h,row,input,fluid,tiny,label);
        System.out.println("[nonore-sluice-literal] "+label+" "+fingerprint(row));return row;
    }

    static Recipe lookup(GTMaterial material,boolean tiny,String water) {
        ItemStack input=GTItems.getStack(tiny?MaterialPrefix.crushedTiny:MaterialPrefix.crushed,material.resolve(),1);
        FluidStack fluid=GTFluids.stack(water,tiny?100:900);
        if(input.isEmpty()||fluid==null||fluid.isEmpty())throw new AssertionError("Missing Sluice fixture "+key(material,tiny,water));
        return MachineRecipeMaps.Sluice.findRecipe(List.of(input),List.of(fluid),false,1,9);
    }
    static FluidStack fluid(GameTestHelper h,String field,int mb) {
        String id=switch(field){
            case "Water"->"minecraft:water";case "MnWtr"->"gt6ce:potion_mineralwater";
            case "DistW"->"gt6ce:ic2distilledwater";case "SpDew"->"gt6ce:spectral_dew";
            case "Sluice"->"gt6ce:sluicejuice";default->throw new AssertionError("Unreviewed fluid "+field);
        };
        var result=GTFluids.stack(field,mb);
        h.assertTrue(result!=null&&!result.isEmpty()&&result.getAmount()==mb
                &&BuiltInRegistries.FLUID.getKey(result.getFluid()).toString().equals(id),"actual canonical fluid "+field+"/"+id+"/"+mb);
        return result;
    }
    private static void shape(GameTestHelper h,Recipe row,ItemStack input,FluidStack fluid,boolean tiny,String label) {
        h.assertTrue(row!=null&&row.mEnabled&&!row.mFakeRecipe,"real enabled Sluice row "+label);
        h.assertTrue(row.mInputs.length==1&&row.mInputs[0].is(input.getItem())&&row.mInputs[0].getCount()==1,"one actual Sluice input "+label);
        h.assertTrue(row.mFluidInputs.length==1&&row.mFluidInputs[0].getFluid()==fluid.getFluid()
                &&row.mFluidInputs[0].getAmount()==(tiny?100:900),"exact Sluice water type/volume "+label);
        h.assertTrue(row.mFluidOutputs.length==1&&row.mFluidOutputs[0].getFluid()==fluid(h,"Sluice",tiny?100:900).getFluid()
                &&row.mFluidOutputs[0].getAmount()==(tiny?100:900),"exact Sluice slurry type/volume "+label);
        h.assertTrue(row.mOutputs.length==9&&Arrays.stream(row.mOutputs).noneMatch(ItemStack::isEmpty),"all nine separate Sluice output slots "+label);
        for(int i=0;i<9;i++)h.assertTrue(row.getOutputChance(i)==CHANCES[i],"literal independent Sluice chance "+label+"/"+i);
        h.assertTrue(row.mDuration==(tiny?16:144)&&row.mEUt==16&&row.mSpecialValue==0,"literal Sluice work "+label);
    }
    private static ItemStack input(GameTestHelper h,GTMaterial m,boolean tiny) {
        return form(h,tiny?MaterialPrefix.crushedTiny:MaterialPrefix.crushed,m,1);
    }
    private static ItemStack form(GameTestHelper h,MaterialPrefix p,GTMaterial m,int count) {
        ItemStack stack=GTItems.getStack(p,m.resolve(),count);
        h.assertTrue(!stack.isEmpty()&&stack.getCount()==count&&stack.getItem() instanceof MaterialFormItem form
                &&form.getPrefix()==p&&form.getMaterial().resolve()==m.resolve(),"actual typed Sluice form "+p.getName()+"/"+m.getName()+"/"+count);
        return stack;
    }
    private static List<Recipe> rows(ItemStack input,FluidStack fluid) {
        return MachineRecipeMaps.Sluice.mRecipeList.stream().filter(r->Arrays.stream(r.mInputs).anyMatch(i->!i.isEmpty()&&i.is(input.getItem()))
                &&Arrays.stream(r.mFluidInputs).anyMatch(f->!f.isEmpty()&&f.getFluid()==fluid.getFluid())).toList();
    }
    private static String key(GTMaterial material,boolean tiny,String water){return material.getName()+"/"+(tiny?"tiny":"whole")+"/"+water;}
    private static String fingerprint(Recipe row) {
        var result=new ArrayList<String>();
        for(var s:row.mInputs)result.add("IN:"+BuiltInRegistries.ITEM.getKey(s.getItem())+"x"+s.getCount());
        for(var s:row.mOutputs)result.add("OUT:"+BuiltInRegistries.ITEM.getKey(s.getItem())+"x"+s.getCount());
        for(var f:row.mFluidInputs)result.add("FIN:"+BuiltInRegistries.FLUID.getKey(f.getFluid())+"x"+f.getAmount());
        for(var f:row.mFluidOutputs)result.add("FOUT:"+BuiltInRegistries.FLUID.getKey(f.getFluid())+"x"+f.getAmount());
        result.add("CHANCES:"+Arrays.toString(row.mChances));result.add("WORK:"+row.mDuration+"/"+row.mEUt+"/"+row.mSpecialValue);
        result.add("FLAGS:"+row.mEnabled+"/"+row.mFakeRecipe);return result.toString();
    }
}
