package com.gregtech.gregtech.gametest;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.addon.AddonMaterials;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeInputs;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.recipe.CraftingMaterialForms;
import com.gregtech.gregtech.recipe.MachineRecipeOverlay;
import com.gregtech.gregtech.content.recipe.NonOreMacerationRows;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.event.server.ServerStoppedEvent;
//?}
/** Native default-fixture source output audit. Global mutations finish synchronously in finally.
 * Public stop-handler/collision checks are NOT cold-start or actual-stop proofs.
 * New policy/ledger accessed reflectively so unchanged tests compile against the red baseline.
 */
@GameTestHolder("gregtech_ore_form_outputs")
@PrefixGameTestTemplate(false)
public final class OreFormOutputRegressionTests {
// Append inside OreFormOutputRegressionTests. Exactly one additional GameTest.
// Reuses imports and tagFluidKeys from TagLifecycleCases; requires GTFluids import.
// No production eligibility, byproduct list, or new ledger helper determines expected output.
// Source original MT.java:3290,3780-3783; Loader_Recipes_Ores.java:383,415-420;
// original GT6_Main six-prefix exclusions + OM crushedPurifiedTiny fallback.
// Literal identities: Dolamide8753 / Dilithium8317 / DiduraniumTrioxide9198 /
// DuraniumHexafluoride9199 / DuraniumHexabromide9201.

    @GameTest(template="test_empty",batch="ore_overlay_lifecycle",timeoutTicks=200)
    public static void literalDolamideSourceFinesAllWaterAndSizeVariants(GameTestHelper h) {
        assertSourceDust(h); // first observed row is baseline-red semantic witness, not a linkage check
        int observed=0;
        for(boolean tiny:new boolean[]{false,true})for(String water:new String[]{"Water","MnWtr","DistW","SpDew"}) {
            Recipe row=sourceLiteralDolamide(h,tiny,water);
            System.out.println("[source-fine-witness] Dolamide/"+tiny+"/"+water+" "+fingerprint(row));
            observed++;
        }
        h.assertTrue(observed==8,"eight explicitly reviewed Dolamide size/water witnesses, not a global cohort census");
        h.succeed();
    }

    private static Recipe assertSourceDust(GameTestHelper h) {
        return sourceLiteralDolamide(h,false,"Water");
    }

    private static Recipe sourceLiteralDolamide(GameTestHelper h,boolean tiny,String water) {
        int fineCount=tiny?1:9,mb=tiny?100:900;
        ItemStack input=sourceExactForm(h,tiny?MaterialPrefix.crushedTiny:MaterialPrefix.crushed,"Dolamide",8753,1);
        var fluid=liquid(water,mb);
        Recipe row=find(MachineRecipeMaps.Sluice,input,fluid);
        h.assertTrue(row!=null,"native Dolamide Sluice exists "+tiny+"/"+water);
        // First content assertion deliberately proves the bug: the excluded manufactured fine is not used.
        ItemStack dust=sourceExactForm(h,MaterialPrefix.dustTiny,"Dilithium",8317,fineCount);
        h.assertTrue(row.mOutputs.length==9&&ItemStack.matches(row.mOutputs[1],dust),
                "source excludes automatic Dilithium crushed forms: first fine must be dustTiny x"+fineCount);
        ItemStack[] outputs={
                sourceExactForm(h,tiny?MaterialPrefix.crushedPurifiedTiny:MaterialPrefix.crushedPurified,"Dolamide",8753,1),
                dust,
                sourceExactForm(h,MaterialPrefix.crushedPurifiedTiny,"DiduraniumTrioxide",9198,fineCount),
                sourceExactForm(h,MaterialPrefix.crushedPurifiedTiny,"DuraniumHexafluoride",9199,fineCount),
                sourceExactForm(h,MaterialPrefix.crushedPurifiedTiny,"DuraniumHexabromide",9201,fineCount),
                sourceExactForm(h,MaterialPrefix.crushedPurifiedTiny,"Dolamide",8753,fineCount),
                sourceExactForm(h,MaterialPrefix.crushedPurifiedTiny,"Dolamide",8753,fineCount),
                sourceExactForm(h,MaterialPrefix.crushedPurifiedTiny,"Dolamide",8753,fineCount),
                sourceExactForm(h,MaterialPrefix.crushedPurifiedTiny,"Dolamide",8753,fineCount)};
        var matching=MachineRecipeMaps.Sluice.mRecipeList.stream().filter(r->r.mInputs.length==1
                &&ItemStack.matches(r.mInputs[0],input)
                &&tagFluidKeys(r.mFluidInputs).equals(tagFluidKeys(new FluidStack[]{fluid}))).toList();
        h.assertTrue(matching.size()==1&&matching.get(0)==row,"one actual accepted source row, not shadowed/duplicate");
        h.assertTrue(find(MachineRecipeMaps.Sluice,input,fluid)==row,"warm source lookup retains exact accepted identity");
        h.assertTrue(row.mInputs.length==1&&ItemStack.matches(row.mInputs[0],input),"source input form/count");
        h.assertTrue(row.mOutputs.length==outputs.length,"all nine independent ordered source slots");
        for(int i=0;i<outputs.length;i++)h.assertTrue(ItemStack.matches(row.mOutputs[i],outputs[i]),"literal source output slot "+i);
        long[] chances={10000,300,300,300,300,300,300,300,300};
        h.assertTrue(Arrays.equals(row.mChances,chances),"exact ordered source chance array");
        h.assertTrue(row.mMaxChances.length==9&&Arrays.stream(row.mMaxChances).allMatch(c->c==10000),"source chance scale");
        h.assertTrue(tagFluidKeys(row.mFluidInputs).equals(tagFluidKeys(new FluidStack[]{fluid})),"exact source water identity/volume");
        h.assertTrue(tagFluidKeys(row.mFluidOutputs).equals(tagFluidKeys(new FluidStack[]{liquid("Sluice",mb)})),"exact source slurry identity/volume");
        h.assertTrue(row.mDuration==(tiny?16:144)&&row.mEUt==16&&row.mSpecialValue==0,"literal source ticks/EUt/special");
        h.assertTrue(row.mEnabled&&!row.mFakeRecipe,"native enabled functional row");
        return row;
    }

    private static ItemStack sourceExactForm(GameTestHelper h,MaterialPrefix prefix,String name,int id,int count) {
        var material=GTMaterialRegistry.get(name).resolve();
        h.assertTrue(material.isValid()&&material.getId()==id&&material.getName().equals(name),"literal source identity "+name+"/"+id);
        ItemStack stack=GTItems.getStack(prefix,material,count);
        h.assertTrue(!stack.isEmpty()&&stack.getCount()==count,"actual source fixture present "+prefix.getName()+"/"+name);
        h.assertTrue(stack.getItem() instanceof com.gregtech.gregtech.api.material.MaterialFormItem form
                &&form.getPrefix()==prefix&&form.getMaterial().resolve()==material,"canonical native typed fixture "+prefix.getName()+"/"+name);
        return stack;
    }

    private static Recipe find(RecipeMap map,ItemStack item,FluidStack fluid) {
        if(item.isEmpty()||(fluid!=null&&fluid.isEmpty()))throw new AssertionError("Missing native recipe lookup fixture");
        return map.findRecipe(List.of(item),fluid==null?List.of():List.of(fluid),false,map.mInputItemsCount,map.mOutputItemsCount);
    }

    private static FluidStack liquid(String field,int mb) {
        String[] binding=switch(field) {
            case "Water"->new String[]{"water","minecraft:water"};
            case "MnWtr"->new String[]{"potion.mineralwater","gt6ce:potion_mineralwater"};
            case "DistW"->new String[]{"ic2distilledwater","gt6ce:ic2distilledwater"};
            case "SpDew"->new String[]{"spectral_dew","gt6ce:spectral_dew"};
            case "Sluice"->new String[]{"sluicejuice","gt6ce:sluicejuice"};
            default->throw new AssertionError("Unreviewed source liquid "+field);
        };
        var entry=com.gregtech.gregtech.data.RegisteredFluids.get(field);
        if(entry==null||!entry.registryName().equals(binding[0]))throw new AssertionError("Source fluid catalog mismatch "+field);
        var stack=com.gregtech.gregtech.registry.GTFluids.stack(field,mb);
        if(stack==null||stack.isEmpty()||stack.getAmount()!=mb||!BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString().equals(binding[1]))
            throw new AssertionError("Native source fluid mismatch "+field+" expected "+binding[1]+" x"+mb);
        return stack;
    }

    // Paired GT6_Main145-150 / W161-166 literal source oracle, not the new production policy.
    private static final Map<Integer,String> SOURCE_NO=Map.ofEntries(
        Map.entry(2220,"Adamantium"),Map.entry(260,"Iron"),Map.entry(140,"Silicon"),Map.entry(130,"Aluminium"),
        Map.entry(220,"Titanium"),Map.entry(740,"Tungsten"),Map.entry(90,"Fluorine"),Map.entry(730,"Tantalum"),
        Map.entry(410,"Niobium"),Map.entry(8317,"Dilithium"));
    private static boolean oracleNo(GTMaterial m) {
        m=m.resolve();return GTMaterialRegistry.isCoreMaterial(m)&&!AddonMaterials.owns(m)
            &&m.getName().equals(SOURCE_NO.get(m.getId()));
    }
    private static ItemStack oracleFine(GTMaterial m,int count,boolean sluice) {
        m=m.resolve();
        // This case is the default native no-external override fixture. Alias lifecycle cases are separate.
        if(!oracleNo(m)&&!(m.getId()==8500&&m.getName().equals("Stone"))) {
            if(sluice) {var s=GTItems.getStack(MaterialPrefix.crushedPurifiedTiny,m,count);if(!s.isEmpty())return s;}
            var s=GTItems.getStack(MaterialPrefix.crushedCentrifugedTiny,m,count);if(!s.isEmpty())return s;
        }
        return GTItems.getStack(MaterialPrefix.dustTiny,m,count);
    }
    @GameTest(template="test_empty",batch="ore_overlay_lifecycle",timeoutTicks=400)
    public static void sourceOutputDynamicNativeCohortHasExactSourceSlots(GameTestHelper h) {
        assertSourceDust(h);
        Set<Recipe> expected=Collections.newSetFromMap(new IdentityHashMap<>());
        Set<GTMaterial> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        List<String> observed=new ArrayList<>();
        for(var alias:GTMaterialRegistry.allMaterials()) {
            var m=alias.resolve();if(!m.isValid()||!seen.add(m)||NonOreMacerationRows.reviewed(m))continue;
            var by=m.getByProducts();
            List<GTMaterial> sluice=new ArrayList<>();
            for(int i=0;i<8;i++)sluice.add(i<by.size()&&by.get(i)!=null&&by.get(i).isValid()?by.get(i):m);
            boolean sluiceForms=MaterialPrefix.crushed.isValidFor(m)&&cohortForms(m,MaterialPrefix.crushed,MaterialPrefix.crushedTiny,
                    MaterialPrefix.crushedPurified,MaterialPrefix.crushedPurifiedTiny);
            if(sluiceForms&&sluice.stream().anyMatch(OreFormOutputRegressionTests::oracleNo))
                for(boolean tiny:new boolean[]{false,true})for(String water:List.of("Water","MnWtr","DistW","SpDew")) {
                    var in=GTItems.getStack(tiny?MaterialPrefix.crushedTiny:MaterialPrefix.crushed,m,1);
                    var row=find(MachineRecipeMaps.Sluice,in,liquid(water,tiny?100:900));
                    cohortCheck(h,row,m,sluice,tiny,true,water);expected.add(row);
                    observed.add("Sluice/"+m.getId()+"/"+tiny+"/"+water);
                }

        }
        h.assertTrue(!expected.isEmpty()&&sourceOwned().equals(expected),"exact dynamically measured native source cohort, no invented fixed census");
        Collections.sort(observed);System.out.println("ORE_FORM_SOURCE_COHORT count="+observed.size()+" emptyInteriorFineSlots="+expected.stream().mapToLong(row->Arrays.stream(row.mOutputs).skip(1).filter(ItemStack::isEmpty).count()).sum()+" rows="+observed);
        h.succeed();
    }
    private static boolean cohortForms(GTMaterial m,MaterialPrefix... forms) {
        for(var p:forms)if(GTItems.getStack(p,m,1).isEmpty())return false;return true;
    }
    private static void cohortCheck(GameTestHelper h,Recipe r,GTMaterial m,List<GTMaterial> products,boolean tiny,boolean sluice,String water) {
        h.assertTrue(r!=null,"source row exists "+m.getName()+" tiny="+tiny+" sluice="+sluice);
        int n=sluice?9:6,count=sluice?(tiny?1:9):(tiny?2:18);
        var fines=products.stream().map(product->oracleFine(product,count,sluice)).toList();
        // Original Recipe895 / W1116 trim trailing nulls; interior empty slots keep their index.
        while(n>1&&fines.get(n-2).isEmpty())n--;
        h.assertTrue(r.mOutputs.length==n&&r.mChances.length==n,"exact output/chance slot arity");
        var prefix=sluice?(tiny?MaterialPrefix.crushedPurifiedTiny:MaterialPrefix.crushedPurified):
                (tiny?MaterialPrefix.crushedCentrifugedTiny:MaterialPrefix.crushedCentrifuged);
        h.assertTrue(ItemStack.matches(r.mOutputs[0],GTItems.getStack(prefix,m,1)),"main output untouched");
        for(int i=0;i<n-1;i++)h.assertTrue(ItemStack.matches(r.mOutputs[i+1],fines.get(i)),
                "source fine exact item/count/empty slot "+m.getName()+"/"+i);
        h.assertTrue(r.mChances[0]==10000,"main certainty");
        for(int i=1;i<n;i++)h.assertTrue(r.mChances[i]==(sluice?300:600),"source fine chance "+i);
        h.assertTrue(r.mDuration==(tiny?16:144)&&r.mEUt==16&&r.mSpecialValue==0,"source work unchanged");
        h.assertTrue(r.mInputs.length==1&&r.mInputs[0].getCount()==1,"input count unchanged");
        h.assertTrue(r.mFluidInputs.length==(sluice?1:0)&&r.mFluidOutputs.length==(sluice?1:0),"fluid arity");
        if(sluice) {
            h.assertTrue(tagFluidKeys(r.mFluidInputs).equals(tagFluidKeys(new FluidStack[]{liquid(water,tiny?100:900)})),"water preserved");
            h.assertTrue(tagFluidKeys(r.mFluidOutputs).equals(tagFluidKeys(new FluidStack[]{liquid("Sluice",tiny?100:900)})),"slurry preserved");
        }
    }
    @GameTest(template="test_empty",batch="ore_overlay_lifecycle",timeoutTicks=400)
    public static void sourceOutputConservativeProvenanceKeepsPublicForms(GameTestHelper h) throws Exception {
        assertSourceDust(h);
        var prefixes=List.of(MaterialPrefix.crushed,MaterialPrefix.crushedTiny,MaterialPrefix.crushedPurified,
                MaterialPrefix.crushedPurifiedTiny,MaterialPrefix.crushedCentrifuged,MaterialPrefix.crushedCentrifugedTiny);
        Map<String,ItemStack> originals=new TreeMap<>();
        for(var fact:SOURCE_NO.entrySet()) {
            var m=GTMaterialRegistry.get(fact.getValue()).resolve();h.assertTrue(m.getId()==fact.getKey(),"source identity");
            for(var p:prefixes)originals.put(m.getName()+"/"+p.getName(),GTItems.getStack(p,m,1).copy());
            for(var p:List.of(MaterialPrefix.oreRaw,MaterialPrefix.rockGt,MaterialPrefix.dust,MaterialPrefix.dustTiny)) {
                h.assertTrue(!policyExcluded(m,p),"raw/rock/dust not source-excluded");
                originals.put(m.getName()+"/"+p.getName(),GTItems.getStack(p,m,1).copy());
            }
        }
        var iron=GTMaterialRegistry.get("Iron").resolve();var prefix=MaterialPrefix.crushedPurifiedTiny;
        var nativeStack=GTItems.getStack(prefix,iron,1);
        var previous=ItemMaterialRegistry.explicit(nativeStack.getItem()).orElseThrow();
        var raw=ownershipRawControls(h);
        try {
            h.assertTrue(policyBuiltin(iron,prefix,nativeStack),"default exact native provenance");
            for(var changed:List.of(
                    new ItemComposition(prefix,List.of(MaterialComponent.of(iron,prefix.getMaterialWeight())),"test unrecoverable",false),
                    new ItemComposition(prefix,GTMaterialRegistry.get("Copper"),prefix.getMaterialWeight()),
                    new ItemComposition(prefix,List.of(MaterialComponent.of(iron,prefix.getMaterialWeight()),MaterialComponent.of(GTMaterialRegistry.get("Copper"),1)),"test mixed",true))) {
                ItemMaterialRegistry.register(nativeStack.getItem(),changed);
                h.assertTrue(!policyBuiltin(iron,prefix,nativeStack),"changed explicit metadata remains UNKNOWN");
            }
        } finally {ItemMaterialRegistry.register(nativeStack.getItem(),previous);}
        MachineRecipeOverlay.apply(h.getLevel().getServer().getRecipeManager());
        assertSourceDust(h);ownershipAssertRaw(h,raw);
        for(var e:originals.entrySet()) {
            var names=e.getKey().split("/");var m=GTMaterialRegistry.get(names[0]);
            var p=(MaterialPrefix)MaterialPrefix.class.getField(names[1]).get(null);
            h.assertTrue(ItemStack.matches(e.getValue(),GTItems.getStack(p,m,1)),"shipped form presence/identity unchanged "+e.getKey());
        }
        var sample=GTMaterialRegistry.get("SampleAlloy").resolve();
        h.assertTrue(AddonMaterials.owns(sample)&&!policyExcluded(sample,prefix),"actual addon-owned form not suppressed");
        h.assertTrue(!policyExcluded(GTMaterialRegistry.get("Copper"),prefix),"unlisted core identity unaffected");
        System.out.println("ORE_FORM_PUBLIC_PRESENCE present="+originals.values().stream().filter(s->!s.isEmpty()).count()+" totalObserved="+originals.size());
        h.succeed();
    }
    private static boolean policyExcluded(GTMaterial m,MaterialPrefix p)throws Exception {
        return (Boolean)Class.forName("com.gregtech.gregtech.content.recipe.ReviewedCrushedFormGeneration")
                .getMethod("sourceExcluded",GTMaterial.class,MaterialPrefix.class).invoke(null,m,p);
    }
    private static boolean policyBuiltin(GTMaterial m,MaterialPrefix p,ItemStack s)throws Exception {
        return (Boolean)Class.forName("com.gregtech.gregtech.content.recipe.ReviewedCrushedFormGeneration")
                .getMethod("excludedBuiltin",GTMaterial.class,MaterialPrefix.class,ItemStack.class).invoke(null,m,p,s);
    }
    @SuppressWarnings("unchecked") private static Set<Recipe> sourceOwned() {
        try {return (Set<Recipe>)Class.forName("com.gregtech.gregtech.content.recipe.SourceOreOutputRows").getMethod("owned").invoke(null);}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static void sourceClear(){sourceCall("clear");}
    private static void sourceRebuild(){sourceCall("rebuild");}
    private static void sourceCall(String method) {
        try {Class.forName("com.gregtech.gregtech.content.recipe.SourceOreOutputRows").getMethod(method).invoke(null);}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }

// REPLACEMENT fragment: exactly three tests, synchronous native tag/explicit metadata lifecycle.
// Same imports/common helpers as original TagLifecycleCases, plus RecipeInputs, Items, ItemComposition.
// Explicit fixture mutations restore captured PREEXISTING metadata, never reflection/remove/invented ownership.
// Native parsed-tag update only; no disk reload/cold-start claims. Whole catalog comparisons are actual native measurements.

    @GameTest(template="test_empty", batch="ore_overlay_lifecycle", timeoutTicks=400)
    public static void unsupportedLateNativeFormTagDefersToUsableCanonical(GameTestHelper h) {
        assertSourceDust(h);
        Item probe=tagProbe(h,"nonore_raw_probe");
        var original=tagSnapshot();
        var before=tagDolamideRows(h,tagFine(MaterialPrefix.dustTiny));
        try {
            tagBind(original,Map.of(tagFineKey(),List.of(probe)));
            tagApply(h);
            h.assertTrue(CraftingMaterialForms.taggedForms(new ItemStack(probe)).size()==1,"real late native purifiedTiny tag bound");
            ItemStack canonical=new ItemStack(tagFine(MaterialPrefix.crushedPurifiedTiny));
            h.assertTrue(!RecipeInputs.matches(canonical,new ItemStack(probe)),"raw tag alone is not a supported machine input alias");
            tagDownstream(h,new ItemStack(probe),false);
            var deferred=tagDolamideRows(h,tagFine(MaterialPrefix.crushedPurifiedTiny));
            tagExceptFirstFine(h,before,deferred);
            for(Recipe row:deferred)tagDownstream(h,row.mOutputs[1],true); // UNKNOWN retains usable old canonical, never emits raw probe
            tagRestore(original);tagApply(h);
            tagSameRows(h,before,tagDolamideRows(h,tagFine(MaterialPrefix.dustTiny)),"raw tag removal restores same source rows");
        } finally {
            tagRestore(original);tagApply(h);tagAssertRestored(h,original);
            tagSameRows(h,before,tagDolamideRows(h,tagFine(MaterialPrefix.dustTiny)),"raw tag finally restoration");
        }
        h.succeed();
    }

    @GameTest(template="test_empty", batch="ore_overlay_lifecycle", timeoutTicks=600)
    public static void compatibleExplicitFineAndAmbiguityUseActualMachineContract(GameTestHelper h) {
        assertSourceDust(h);
        Item first=Items.REDSTONE,second=Items.GLOWSTONE_DUST,probe=tagProbe(h,"nonore_raw_probe");
        ItemComposition firstOriginal=tagExistingComposition(h,first),secondOriginal=tagExistingComposition(h,second);
        var original=tagSnapshot();var before=tagDolamideRows(h,tagFine(MaterialPrefix.dustTiny));
        try {
            tagRegisterFine(first);tagApply(h);
            var usable=tagDolamideRows(h,first);tagExceptFirstFine(h,before,usable);
            for(Recipe row:usable)tagDownstream(h,row.mOutputs[1],true);
            tagRegisterFine(second);tagApply(h);
            var ambiguous=tagDolamideRows(h,tagFine(MaterialPrefix.crushedPurifiedTiny));
            tagExceptFirstFine(h,before,ambiguous);
            for(Recipe row:ambiguous)tagDownstream(h,row.mOutputs[1],true);
            tagDownstream(h,new ItemStack(first),true);tagDownstream(h,new ItemStack(second),true);
            // Remove only our reversible metadata override; never unregister an item or rewrite global form policy.
            ItemMaterialRegistry.register(second,secondOriginal);
            TagKey<Item> conflicting=TagKey.create(Registries.ITEM,sourceResource("forge","tiny_dusts/dilithium"));
            tagBind(original,Map.of(tagFineKey(),List.of(probe),conflicting,List.of(probe)));
            tagApply(h);
            h.assertTrue(CraftingMaterialForms.taggedForms(new ItemStack(probe)).size()==2,"genuine multiple native form tags");
            h.assertTrue(!RecipeInputs.matches(new ItemStack(tagFine(MaterialPrefix.crushedPurifiedTiny)),new ItemStack(probe)),"ambiguous raw tag still unusable");
            var unaffected=tagDolamideRows(h,first);tagExceptFirstFine(h,before,unaffected);
            for(Recipe row:unaffected)tagDownstream(h,row.mOutputs[1],true);
        } finally {
            ItemMaterialRegistry.register(first,firstOriginal);ItemMaterialRegistry.register(second,secondOriginal);
            tagRestore(original);tagApply(h);tagAssertRestored(h,original);
            h.assertTrue(ItemMaterialRegistry.explicit(first).orElseThrow()==firstOriginal&&ItemMaterialRegistry.explicit(second).orElseThrow()==secondOriginal,"exact preexisting metadata restored");
            tagSameRows(h,before,tagDolamideRows(h,tagFine(MaterialPrefix.dustTiny)),"explicit and ambiguous tag restoration");
        }
        h.succeed();
    }

    @GameTest(template="test_empty", batch="ore_overlay_lifecycle", timeoutTicks=800)
    public static void compatibleFineRepeatedOverlayAndNativeTagRemovalPreserveWarmRows(GameTestHelper h) {
        assertSourceDust(h);
        Item explicit=Items.REDSTONE,probe=tagProbe(h,"nonore_raw_probe");
        ItemComposition metadata=tagExistingComposition(h,explicit);
        var original=tagSnapshot();var before=tagDolamideRows(h,tagFine(MaterialPrefix.dustTiny));var clean=tagCatalog();
        try {
            tagRegisterFine(explicit);
            tagBind(original,Map.of(tagFineKey(),List.of(probe)));
            tagApply(h);
            var tagged=tagCatalog();var previous=tagDolamideRows(h,explicit);
            for(int round=0;round<3;round++) {
                tagApply(h);var current=tagDolamideRows(h,explicit);
                tagSameRows(h,previous,current,"same-binding replay "+round);
                h.assertTrue(previous.get(0)!=current.get(0),"retire warmed source identity "+round);
                h.assertTrue(tagged.equals(tagCatalog()),"actual full catalog fingerprints/multiplicity stable "+round);
                for(Recipe row:current)tagDownstream(h,row.mOutputs[1],true);
                previous=current;
            }
            tagRestore(original);tagApply(h);
            h.assertTrue(CraftingMaterialForms.taggedForms(new ItemStack(probe)).isEmpty(),"actual bound alias removed");
            tagSameRows(h,previous,tagDolamideRows(h,explicit),"unsupported tag removal cannot change compatible explicit winner");
            ItemMaterialRegistry.register(explicit,metadata);tagApply(h);
            tagSameRows(h,before,tagDolamideRows(h,tagFine(MaterialPrefix.dustTiny)),"explicit binding restoration returns dust, not obsolete warmed alias");
        } finally {
            ItemMaterialRegistry.register(explicit,metadata);tagRestore(original);tagApply(h);tagAssertRestored(h,original);
            h.assertTrue(ItemMaterialRegistry.explicit(explicit).orElseThrow()==metadata,"exact preexisting metadata restored after replay");
            tagSameRows(h,before,tagDolamideRows(h,tagFine(MaterialPrefix.dustTiny)),"replay restoration");
            h.assertTrue(clean.equals(tagCatalog()),"all actual catalog row fingerprints restored");
        }
        h.succeed();
    }

    private static ItemComposition tagExistingComposition(GameTestHelper h,Item item) {
        var original=ItemMaterialRegistry.explicit(item).orElse(null);
        h.assertTrue(original!=null,"fixture requires PREEXISTING reversible explicit composition "+BuiltInRegistries.ITEM.getKey(item));
        h.assertTrue(!(item instanceof com.gregtech.gregtech.api.material.MaterialFormItem),"fixture is a plain foreign registered item");
        h.assertTrue(original.prefix()==MaterialPrefix.dust,"fixture is existing vanilla dust, not an ingot with exact recovery precedence");
        for(RecipeMap map:new RecipeMap[]{MachineRecipeMaps.Crusher,MachineRecipeMaps.Shredder})
            for(Recipe row:map.mRecipeList)
                h.assertTrue(!row.mEnabled||row.mInputs.length!=1||!row.mInputs[0].is(item),
                        "fixture must have no preexisting enabled exact single-item route in "+map.mNameInternal);
        return original;
    }
    private static void tagRegisterFine(Item item) {
        ItemMaterialRegistry.register(item,new ItemComposition(MaterialPrefix.crushedPurifiedTiny,
                GTMaterialRegistry.get("Dilithium").resolve(),MaterialPrefix.crushedPurifiedTiny.getMaterialWeight()));
    }
    private static void tagDownstream(GameTestHelper h,ItemStack supplied,boolean expectedUsable) {
        ItemStack canonical=new ItemStack(tagFine(MaterialPrefix.crushedPurifiedTiny));
        h.assertTrue(RecipeInputs.matches(canonical,supplied)==expectedUsable,"actual machine form contract agrees with selected fine");
        for(RecipeMap map:new RecipeMap[]{MachineRecipeMaps.Crusher,MachineRecipeMaps.Shredder}) {
            Recipe expected=map.findRecipe(List.of(canonical),List.of(),false,map.mInputItemsCount,map.mOutputItemsCount);
            h.assertTrue(expected!=null&&expected.mInputs.length==1&&ItemStack.matches(expected.mInputs[0],canonical),"real canonical Dilithium purifiedTiny downstream row exists");
            h.assertTrue(expected.mDuration==64&&expected.mEUt==16&&expected.mFluidInputs.length==0,"actual legacy pulverizing work/input fluids");
            h.assertTrue(expected.mOutputs.length>0&&ItemStack.matches(expected.mOutputs[0],new ItemStack(tagFine(MaterialPrefix.dustTiny))),"actual downstream dustTiny output");
            ItemStack offered=supplied.copy();
            h.assertTrue((map.findRecipe(List.of(offered),List.of(),false,map.mInputItemsCount,map.mOutputItemsCount)==expected)==expectedUsable,"actual canonical downstream route admission/rejection");
            var remainder=RecipeInputs.consume(expected,List.of(offered),List.of(),1);
            h.assertTrue((remainder!=null)==expectedUsable,"real canonical recipe consumption contract");
            if(expectedUsable) {
                Recipe selected=map.findRecipe(List.of(offered),List.of(),false,map.mInputItemsCount,map.mOutputItemsCount);
                h.assertTrue(selected==expected,"selected Sluice fine reaches exact existing canonical-input downstream recipe");
                h.assertTrue(map.findRecipe(List.of(offered),List.of(),false,map.mInputItemsCount,map.mOutputItemsCount)==expected,"warmed downstream lookup stable");
                h.assertTrue(remainder.items().size()==1&&remainder.items().get(0).getCount()==offered.getCount()-1,"consumes exactly one fine, retains remainder");
                h.assertTrue(offered.getCount()==supplied.getCount(),"planning never mutates source Sluice output stack");
            }
        }
    }

    private static Item tagProbe(GameTestHelper h,String path) {
        var id=sourceResource("addon_sample",path);
        Item result=BuiltInRegistries.ITEM.get(id);
        h.assertTrue(BuiltInRegistries.ITEM.getKey(result).equals(id),"real registered fixture "+id);
        h.assertTrue(ItemMaterialRegistry.explicit(result).isEmpty(),"fixture has no explicit metadata "+id);
        h.assertTrue(CraftingMaterialForms.form(new ItemStack(result))==null,"fixture starts with no native form "+id);
        h.assertTrue(CraftingMaterialForms.taggedForms(new ItemStack(result)).isEmpty(),"fixture starts with no form tags "+id);
        return result;
    }
    private static Item tagFine(MaterialPrefix prefix) {
        return GTItems.getStack(prefix,GTMaterialRegistry.get("Dilithium").resolve(),1).getItem();
    }
    private static TagKey<Item> tagFineKey() {
        return TagKey.create(Registries.ITEM,sourceResource("gt6ce","crushed_purified_tiny/dilithium"));
    }
    private static Map<TagKey<Item>,List<Holder<Item>>> tagSnapshot() {
        Map<TagKey<Item>,List<Holder<Item>>> result=new HashMap<>();
        BuiltInRegistries.ITEM.getTags().forEach(pair->result.put(pair.getFirst(),pair.getSecond().stream().toList()));
        return result;
    }
    private static void tagBind(Map<TagKey<Item>,List<Holder<Item>>> original,Map<TagKey<Item>,List<Item>> additions) {
        // Supplying explicit empty lists prevents stale named-holder memberships from a previous stage.
        Map<TagKey<Item>,List<Holder<Item>>> bindings=new HashMap<>();
        BuiltInRegistries.ITEM.getTagNames().forEach(key->bindings.put(key,List.of()));
        bindings.putAll(original);
        additions.forEach((key,items)->{
            List<Holder<Item>> values=new ArrayList<>(original.getOrDefault(key,List.of()));
            for(Item item:items)if(!values.contains(item.builtInRegistryHolder()))values.add(item.builtInRegistryHolder());
            bindings.put(key,List.copyOf(values));
        });
        BuiltInRegistries.ITEM.bindTags(bindings);
    }
    private static void tagRestore(Map<TagKey<Item>,List<Holder<Item>>> original) { tagBind(original,Map.of()); }
    private static void tagApply(GameTestHelper h) {
        MachineRecipeOverlay.apply(h.getLevel().getServer().getRecipeManager());
    }
    private static void tagAssertRestored(GameTestHelper h,Map<TagKey<Item>,List<Holder<Item>>> original) {
        var restored=tagSnapshot();
        original.forEach((key,holders)->h.assertTrue(holders.equals(restored.get(key)),"original native tag binding restored "+key));
        restored.forEach((key,holders)->h.assertTrue(original.containsKey(key)||holders.isEmpty(),"new tag has no residual members "+key));
        // Registry deliberately retains an empty named tag handle; membership and aliases, not key removal, are guaranteed.
    }
    private static List<Recipe> tagDolamideRows(GameTestHelper h,Item fine) {
        var material=GTMaterialRegistry.get("Dolamide").resolve();
        List<Recipe> rows=new ArrayList<>();
        for(boolean tiny:new boolean[]{false,true})for(String water:new String[]{"Water","MnWtr","DistW","SpDew"}) {
            var input=GTItems.getStack(tiny?MaterialPrefix.crushedTiny:MaterialPrefix.crushed,material,1);
            var fluid=liquid(water,tiny?100:900);
            Recipe row=find(MachineRecipeMaps.Sluice,input,fluid);
            h.assertTrue(row!=null,"native Dolamide Sluice "+tiny+"/"+water);
            h.assertTrue(find(MachineRecipeMaps.Sluice,input,fluid)==row,"warmed lookup stays exact accepted identity");
            h.assertTrue(row.mOutputs.length==9 && row.mChances.length==9,"nine slots/chances retained");
            h.assertTrue(row.mInputs.length==1&&ItemStack.matches(row.mInputs[0],input),"literal canonical input retained");
            var main=GTItems.getStack(tiny?MaterialPrefix.crushedPurifiedTiny:MaterialPrefix.crushedPurified,material,1);
            h.assertTrue(ItemStack.matches(row.mOutputs[0],main),"literal canonical main output retained");
            h.assertTrue(tagFluidKeys(row.mFluidInputs).equals(tagFluidKeys(new FluidStack[]{fluid})),"literal source fluid input");
            h.assertTrue(tagFluidKeys(row.mFluidOutputs).equals(tagFluidKeys(new FluidStack[]{liquid("Sluice",tiny?100:900)})),"literal source fluid output");
            h.assertTrue(row.mOutputs[1].is(fine)&&row.mOutputs[1].getCount()==(tiny?1:9),"literal Dilithium fine item/count");
            h.assertTrue(row.mChances[0]==10000 && Arrays.stream(row.mChances,1,9).allMatch(chance->chance==300),"literal source chances");
            h.assertTrue(row.mDuration==(tiny?16:144)&&row.mEUt==16,"literal source work");
            rows.add(row);
        }
        return rows;
    }
    private static void tagExceptFirstFine(GameTestHelper h,List<Recipe> before,List<Recipe> after) {
        h.assertTrue(before.size()==after.size(),"source row cardinality");
        for(int i=0;i<before.size();i++) {
            Recipe a=before.get(i),b=after.get(i);
            h.assertTrue(a.mInputs.length==b.mInputs.length&&a.mOutputs.length==b.mOutputs.length,"item arity preserved");
            for(int j=0;j<a.mInputs.length;j++)h.assertTrue(ItemStack.matches(a.mInputs[j],b.mInputs[j]),"input preserved");
            for(int j=0;j<a.mOutputs.length;j++)if(j!=1)h.assertTrue(ItemStack.matches(a.mOutputs[j],b.mOutputs[j]),"main/other fine slot preserved "+j);
            h.assertTrue(Arrays.equals(a.mChances,b.mChances)&&Arrays.equals(a.mMaxChances,b.mMaxChances),"chances unchanged");
            h.assertTrue(tagFluidKeys(a.mFluidInputs).equals(tagFluidKeys(b.mFluidInputs))&&tagFluidKeys(a.mFluidOutputs).equals(tagFluidKeys(b.mFluidOutputs)),"fluids unchanged");
            h.assertTrue(a.mDuration==b.mDuration&&a.mEUt==b.mEUt&&a.mSpecialValue==b.mSpecialValue,"work unchanged");
            h.assertTrue(Objects.equals(a.mSpecialItems,b.mSpecialItems)&&a.mEnabled==b.mEnabled&&a.mHidden==b.mHidden
                    &&a.mFakeRecipe==b.mFakeRecipe&&a.mCanBeBuffered==b.mCanBeBuffered&&a.mNeedsEmptyOutput==b.mNeedsEmptyOutput
                    &&a.mMaterialRecovery==b.mMaterialRecovery&&a.mRequiresEmptyContainerInputs==b.mRequiresEmptyContainerInputs
                    &&a.mExactItemInputs==b.mExactItemInputs&&a.mExplicitCatalystsOnly==b.mExplicitCatalystsOnly,"flags unchanged");
        }
    }
    private static List<String> tagFluidKeys(FluidStack[] fluids) {
        return Arrays.stream(fluids).map(f->BuiltInRegistries.FLUID.getKey(f.getFluid())+"x"+f.getAmount()).toList();
    }
    private static void tagSameRows(GameTestHelper h,List<Recipe> a,List<Recipe> b,String label) {
        h.assertTrue(a.stream().map(OreFormOutputRegressionTests::fingerprint).toList().equals(
                b.stream().map(OreFormOutputRegressionTests::fingerprint).toList()),label);
    }
    private static Map<String,List<String>> tagCatalog() {
        Map<String,List<String>> result=new TreeMap<>();
        RecipeMap.RECIPE_MAPS.forEach((name,map)->result.put(name,map.mRecipeList.stream()
                .map(OreFormOutputRegressionTests::fingerprint).sorted().toList()));
        return result;
    }

// Paste members into OreFormOutputRegressionTests, namespace gregtech_ore_form_outputs.
// Required imports: java.util.*; Recipe; RecipeMap; MachineRecipeMaps; ItemStack; Items;
// GameTest; GameTestHelper; loader-specific FluidStack and ServerStoppedEvent.
// Shared helpers: assertSourceDust(h): Recipe (Dolamide whole/Water900, literal source oracle),
// sourceOwned(): Set<Recipe>, sourceClear(), sourceRebuild() (reflective; baseline compiles),
// fingerprint(Recipe): immutable value with equals/hashCode covering ALL row fields/stack data.
// No tag mutation; no async callback; both methods restore synchronously before returning.

@GameTest(template="test_empty", batch="ore_overlay_lifecycle", timeoutTicks=400)
public static void sourceOutputPublicCollisionWinnerSurvivesExactCleanup(GameTestHelper h) throws Exception {
    Recipe original = assertSourceDust(h); // baseline must fail on semantic fine output first
    var manager = h.getLevel().getServer().getRecipeManager();
    RecipeMap map = MachineRecipeMaps.Sluice;
    Recipe fixture = null;
    try {
        // Capture baseline before fixture registration, via supported overlay only.
        com.gregtech.gregtech.recipe.MachineRecipeOverlay.apply(manager);
        original = assertSourceDust(h);
        h.assertTrue(sourceOwned().contains(original), "actual Dolamide producer admission is owned");
        h.assertTrue(ownershipLookup(map, original) == original && ownershipLookup(map, original) == original,
                "warm real production lookup before retirement");
        var unaffected = ownershipCensus(sourceOwned(), null);
        var raw = ownershipRawControls(h);
        var retired = ownershipIdentityCopy(sourceOwned());
        sourceClear();
        ownershipAssertAbsent(h, retired, "exact source handles retired before independent admission");
        h.assertTrue(ownershipLookup(map, original) == null, "warm source lookup evicted by clear");
        ItemStack[] outputs = Arrays.stream(original.mOutputs).map(ItemStack::copy).toArray(ItemStack[]::new);
        outputs[1] = new ItemStack(Items.NETHER_STAR);
        fixture = map.addRecipe(false,
                Arrays.stream(original.mInputs).map(ItemStack::copy).toArray(ItemStack[]::new),
                outputs, original.mSpecialItems, original.mChances.clone(),
                Arrays.stream(original.mFluidInputs).map(FluidStack::copy).toArray(FluidStack[]::new),
                Arrays.stream(original.mFluidOutputs).map(FluidStack::copy).toArray(FluidStack[]::new),
                37, original.mEUt, original.mSpecialValue);
        h.assertTrue(fixture != null && fixture != original, "public same-signature independent winner accepted");
        Object winnerFingerprint = fingerprint(fixture);
        h.assertTrue(ownershipLookup(map, original) == fixture && ownershipLookup(map, original) == fixture,
                "public winner selected through warmed native lookup");
        for (int round = 0; round < 2; round++) {
            sourceRebuild();
            h.assertTrue(!sourceOwned().contains(fixture), "collision winner never claimed by source ledger");
            h.assertTrue(ownershipLookup(map, original) == fixture && ownershipLookup(map, original) == fixture,
                    "exact independent winner survives source replay and warmed lookup round " + round);
            h.assertTrue(fingerprint(fixture).equals(winnerFingerprint), "winner complete row unchanged");
            h.assertTrue(ownershipCensus(sourceOwned(), fixture).equals(unaffected),
                    "all non-source rows in affected maps retain complete fingerprint multiset");
            ownershipAssertRaw(h, raw);
            var accepted = ownershipIdentityCopy(sourceOwned());
            sourceClear();
            ownershipAssertAbsent(h, accepted, "clear removes accepted source identities only");
            h.assertTrue(ownershipLookup(map, original) == fixture && !sourceOwned().contains(fixture),
                    "independent winner survives ledger clear with warm cache");
        }
        // Runtime public registration/collision proof; NOT a cold onRecipesReady addon profile.
    } finally {
        if (fixture != null) map.unlink(fixture); // only the exact fixture; never delete by signature
        com.gregtech.gregtech.recipe.MachineRecipeOverlay.apply(manager);
        assertSourceDust(h);
    }
    h.succeed();
}

@GameTest(template="test_empty", batch="ore_overlay_lifecycle", timeoutTicks=400)
public static void sourceOutputBaselineAndSameJvmStopHandlerDoNotResurrectOwnedRows(GameTestHelper h) throws Exception {
    assertSourceDust(h); // semantic baseline failure precedes reflective ledger access
    var server = h.getLevel().getServer();
    var manager = server.getRecipeManager();
    try {
        com.gregtech.gregtech.recipe.MachineRecipeOverlay.apply(manager);
        Recipe initial = assertSourceDust(h);
        var initialOwned = ownershipIdentityCopy(sourceOwned());
        h.assertTrue(initialOwned.contains(initial), "real source factory owns witness before stop handler");
        var completeBefore = ownershipCensus(Collections.emptySet(), null);
        var raw = ownershipRawControls(h);
        h.assertTrue(ownershipLookup(MachineRecipeMaps.Sluice, initial) == initial
                && ownershipLookup(MachineRecipeMaps.Sluice, initial) == initial, "warm source lookup");
        // Invoke only the public handler synchronously. Does NOT stop this server or prove a cold JVM/new world.
        com.gregtech.gregtech.recipe.ExternalOreRecipeLifecycle.stopped(new ServerStoppedEvent(server));
        h.assertTrue(sourceOwned().isEmpty(), "stop handler empties source accepted-identity ledger");
        ownershipAssertAbsent(h, initialOwned, "stop handler removes exact source identities");
        h.assertTrue(ownershipLookup(MachineRecipeMaps.Sluice, initial) == null, "stop handler invalidates warmed lookup");
        ownershipAssertRaw(h, raw);
        MachineRecipeMaps.Sluice.restoreBaseline();
        MachineRecipeMaps.MagneticSeparator.restoreBaseline();
        ownershipAssertAbsent(h, initialOwned, "baseline excludes retired source identities");
        h.assertTrue(ownershipLookup(MachineRecipeMaps.Sluice, initial) == null,
                "baseline alone cannot resurrect source witness before admission stage");
        for (int round = 0; round < 2; round++) {
            var previous = ownershipIdentityCopy(sourceOwned());
            com.gregtech.gregtech.recipe.MachineRecipeOverlay.apply(manager);
            Recipe restored = assertSourceDust(h);
            h.assertTrue(restored != initial && sourceOwned().contains(restored), "fresh owned admission after lifecycle replay");
            ownershipAssertAbsent(h, initialOwned, "initial identities never resurrect after full overlay");
            ownershipAssertAbsent(h, previous, "repeated overlay retires previous accepted objects");
            h.assertTrue(ownershipLookup(MachineRecipeMaps.Sluice, restored) == restored
                    && ownershipLookup(MachineRecipeMaps.Sluice, restored) == restored, "fresh cache selection after replay");
            h.assertTrue(ownershipCensus(Collections.emptySet(), null).equals(completeBefore),
                    "affected maps restore entire full-row fingerprint multisets, not only counts");
            ownershipAssertRaw(h, raw);
        }
    } finally {
        com.gregtech.gregtech.recipe.MachineRecipeOverlay.apply(manager);
        assertSourceDust(h);
    }
    h.succeed();
}

private static Set<Recipe> ownershipIdentityCopy(Set<Recipe> rows) {
    Set<Recipe> result = Collections.newSetFromMap(new IdentityHashMap<>());
    result.addAll(rows);
    return result;
}
private static Recipe ownershipLookup(RecipeMap map, Recipe row) {
    return map.findRecipe(Arrays.stream(row.mInputs).map(ItemStack::copy).toList(),
            Arrays.stream(row.mFluidInputs).map(FluidStack::copy).toList(), false, row.mInputs.length, 16);
}
private static void ownershipAssertAbsent(GameTestHelper h, Set<Recipe> retired, String phase) {
    // Read-only membership checks; handles originate solely in sourceOwned(), never signature inference.
    for (RecipeMap map : List.of(MachineRecipeMaps.Sluice, MachineRecipeMaps.MagneticSeparator))
        for (Recipe row : map.mRecipeList) h.assertTrue(!retired.contains(row), phase + ": " + map.mNameInternal);
}
private static Map<String, Map<Object,Integer>> ownershipCensus(Set<Recipe> excluded, Recipe fixture) {
    var result = new LinkedHashMap<String, Map<Object,Integer>>();
    for (RecipeMap map : List.of(MachineRecipeMaps.Sluice, MachineRecipeMaps.MagneticSeparator)) {
        Map<Object,Integer> rows = new HashMap<>();
        for (Recipe row : map.mRecipeList)
            if (!excluded.contains(row) && row != fixture) rows.merge(fingerprint(row), 1, Integer::sum);
        result.put(map.mNameInternal, Map.copyOf(rows));
    }
    return Map.copyOf(result);
}
private record OwnershipRawControl(RecipeMap map, Recipe row, Object fingerprint) {}
private static List<OwnershipRawControl> ownershipRawControls(GameTestHelper h) {
    var result = new ArrayList<OwnershipRawControl>();
    ItemStack raw = OreSourceDifferentialTests.s("oreRaw", "Cinnabar", 1);
    for (RecipeMap map : List.of(MachineRecipeMaps.Crusher, MachineRecipeMaps.Hammer)) {
        Recipe row = map.findRecipe(List.of(raw.copy()), List.of(), false, 1, 16);
        h.assertTrue(row != null, "real raw Cinnabar control exists: " + map.mNameInternal);
        result.add(new OwnershipRawControl(map, row, fingerprint(row)));
    }
    return List.copyOf(result);
}
private static void ownershipAssertRaw(GameTestHelper h, List<OwnershipRawControl> controls) {
    for (var control : controls) {
        h.assertTrue(ownershipLookup(control.map(), control.row()) == control.row(), "raw route exact identity preserved");
        h.assertTrue(fingerprint(control.row()).equals(control.fingerprint()), "raw complete row preserved");
    }
}

// Members for OreFormOutputRegressionTests. Requires Recipe, ItemStack, loader FluidStack;
// uses fully-qualified names otherwise. No production resolver/source-policy helper is consulted.
// This is a semantic snapshot, never an ownership key. Unknown special payload types fail closed.
private static String fingerprint(Recipe row) {
    if (row == null) return "null-recipe";
    var fields = new java.util.ArrayList<String>();
    fields.add(row.getClass().getName());
    fields.add(fpObject(row.mInputs)); fields.add(fpObject(row.mOutputs));
    fields.add(fpObject(row.mFluidInputs)); fields.add(fpObject(row.mFluidOutputs));
    fields.add(fpObject(row.mChances)); fields.add(fpObject(row.mMaxChances));
    fields.add(Long.toString(row.mDuration)); fields.add(Long.toString(row.mEUt));
    fields.add(Long.toString(row.mSpecialValue)); fields.add(fpObject(row.mSpecialItems));
    fields.add("flags=" + row.mEnabled + "," + row.mHidden + "," + row.mFakeRecipe + ","
            + row.mCanBeBuffered + "," + row.mNeedsEmptyOutput + "," + row.mMaterialRecovery + ","
            + row.mRequiresEmptyContainerInputs + "," + row.mExactItemInputs + "," + row.mExplicitCatalystsOnly);
    fields.add(fpObject(row.explicitCatalystInputs()));
    fields.add(fpObject(row.explicitCatalystFluidInputs()));
    fields.add(fpObject(row.explicitPresenceFluidInputs()));
    for (int slot = 0; slot < row.mInputs.length; slot++) {
        fields.add("effective-catalyst=" + slot + ":" + row.isCatalystInput(slot));
        fields.add("viewer=" + slot + ":" + fpObject(row.viewerInputAlternatives(slot)));
    }
    return fpSequence("recipe", fields);
}
private static String fpItem(ItemStack stack) {
    if (stack == null) return "null-item";
    if (stack.isEmpty()) return "empty-item";
//? if >=1.21.1 {
/*    var server = java.util.Objects.requireNonNull(
            net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer(), "fingerprint requires running native server");
    return "item:" + fpNbt(stack.save(server.registryAccess()));
*///?} else {
    return "item:" + fpNbt(stack.save(new net.minecraft.nbt.CompoundTag()));
//?}
}
private static String fpFluid(FluidStack stack) {
    if (stack == null) return "null-fluid";
    if (stack.isEmpty()) return "empty-fluid";
//? if >=1.21.1 {
/*    var server = java.util.Objects.requireNonNull(
            net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer(), "fingerprint requires running native server");
    return "fluid:" + fpNbt(stack.save(server.registryAccess()));
*///?} else {
    return "fluid:" + fpNbt(stack.writeToNBT(new net.minecraft.nbt.CompoundTag()));
//?}
}
private static String fpNbt(net.minecraft.nbt.Tag tag) {
    if (tag == null) return "null-nbt";
    if (tag instanceof net.minecraft.nbt.CompoundTag compound) {
        var fields = new java.util.ArrayList<String>();
        var keys = new java.util.ArrayList<>(compound.getAllKeys());
        java.util.Collections.sort(keys);
        for (String key : keys) { fields.add(key); fields.add(fpNbt(compound.get(key))); }
        return fpSequence("compound", fields);
    }
    if (tag instanceof net.minecraft.nbt.ListTag list) {
        var fields = new java.util.ArrayList<String>();
        for (int i = 0; i < list.size(); i++) fields.add(fpNbt(list.get(i)));
        return fpSequence("list:" + list.getElementType(), fields);
    }
    // Primitive scalar/array SNBT is ordered, exact and type-tagged; compound order handled above.
    return tag.getId() + ":" + tag.toString();
}
private static String fpSequence(String kind, java.util.List<String> values) {
    var result = new StringBuilder(kind).append('[').append(values.size()).append(':');
    for (String value : values) result.append(value.length()).append(':').append(value);
    return result.append(']').toString();
}
private static String fpObject(Object value) {
    if (value == null) return "null";
    if (value instanceof ItemStack item) return fpItem(item);
    if (value instanceof FluidStack fluid) return fpFluid(fluid);
    if (value instanceof net.minecraft.nbt.Tag tag) return fpNbt(tag);
    if (value instanceof net.minecraft.world.item.Item item)
        return "item-id:" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
    if (value instanceof net.minecraft.world.level.material.Fluid fluid)
        return "fluid-id:" + net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid);
    if (value instanceof String || value instanceof Number || value instanceof Boolean
            || value instanceof Character || value instanceof net.minecraft.resources.ResourceLocation)
        return value.getClass().getName() + ":" + value;
    if (value instanceof Enum<?> enumeration)
        return "enum:" + enumeration.getDeclaringClass().getName() + ":" + enumeration.name();
    if (value.getClass().isArray()) {
        var entries = new java.util.ArrayList<String>();
        for (int i = 0; i < java.lang.reflect.Array.getLength(value); i++)
            entries.add(fpObject(java.lang.reflect.Array.get(value, i)));
        return fpSequence("array:" + value.getClass().getComponentType().getName(), entries);
    }
    if (value instanceof java.util.Map<?, ?> map) {
        var entries = new java.util.ArrayList<String>();
        map.forEach((key, entry) -> entries.add(fpSequence("entry", java.util.List.of(fpObject(key), fpObject(entry)))));
        java.util.Collections.sort(entries);
        return fpSequence("map", entries);
    }
    if (value instanceof java.util.Collection<?> collection) {
        var entries = new java.util.ArrayList<String>();
        for (Object entry : collection) entries.add(fpObject(entry));
        if (value instanceof java.util.Set<?>) java.util.Collections.sort(entries);
        return fpSequence(value instanceof java.util.Set<?> ? "set" : "sequence", entries);
    }
    throw new IllegalArgumentException("Unhandled recipe special payload; add explicit semantic codec: " + value.getClass().getName());
}

    private static ResourceLocation sourceResource(String namespace,String path) {
//? if >=1.21.1 {
/*        return ResourceLocation.fromNamespaceAndPath(namespace,path);
*///?} else {
        return new ResourceLocation(namespace,path);
//?}
    }
}
