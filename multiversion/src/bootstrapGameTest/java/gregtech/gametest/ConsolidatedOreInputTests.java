package gregtech.gametest;
import gregtech.api.material.*;
import gregtech.internal.material.*;
import gregtech.internal.material.*;
import gregtech.internal.addon.AddonMaterials;
import gregtech.internal.recipe.*;
import gregtech.internal.recipe.*;
import gregtech.content.data.*;
import gregtech.content.recipe.CraftingMaterialForms;
import gregtech.content.recipe.MachineRecipeOverlay;
import gregtech.content.registry.GTItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
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
/** Read-only actual raw-input admission census. Paired source53 identities are literal,
 * not read from the production policy. No map injection, overlay call or world mutation. */
@GameTestHolder("gregtech_ore_alignment_batch")
@PrefixGameTestTemplate(false)
public final class ConsolidatedOreInputTests {
    private record Identity(int id,String name){}
    private static final Identity[] SOURCE_NO={
        new Identity(8366,"ArcaneCompound"),
        new Identity(8198,"Bakelite"),
        new Identity(8418,"Bamboo"),
        new Identity(8275,"Bark"),
        new Identity(8106,"BlackEndSand"),
        new Identity(8620,"Brass"),
        new Identity(8610,"Bronze"),
        new Identity(8501,"Concrete"),
        new Identity(8290,"Dreamwood"),
        new Identity(8751,"Duranium"),
        new Identity(9191,"Eclogite"),
        new Identity(8504,"Endstone"),
        new Identity(8035,"FerricOxyhydroxide"),
        new Identity(8698,"Fireleaf"),
        new Identity(9712,"FishRaw"),
        new Identity(8002,"Flint"),
        new Identity(90,"Fluorine"),
        new Identity(8606,"GildedIron"),
        new Identity(9175,"Graphene"),
        new Identity(8296,"Greatwood"),
        new Identity(8199,"HardPlastic"),
        new Identity(8522,"Holystone"),
        new Identity(9802,"Ice"),
        new Identity(8644,"IronCompressed"),
        new Identity(8645,"IronMagnetic"),
        new Identity(8241,"Leather"),
        new Identity(9189,"Limestone"),
        new Identity(8289,"Livingwood"),
        new Identity(8506,"Marble"),
        new Identity(9700,"MeatRaw"),
        new Identity(8650,"MeteoricSteel"),
        new Identity(8647,"NeodymiumMagnetic"),
        new Identity(8502,"Netherrack"),
        new Identity(8214,"Obsidian"),
        new Identity(8218,"Plastic"),
        new Identity(8197,"PVC"),
        new Identity(8509,"Redrock"),
        new Identity(8104,"RedSand"),
        new Identity(8217,"Rubber"),
        new Identity(8414,"Shimmerwood"),
        new Identity(8297,"Silverwood"),
        new Identity(8291,"Skyroot"),
        new Identity(9801,"Snow"),
        new Identity(8673,"Steeleaf"),
        new Identity(8651,"SteelGalvanized"),
        new Identity(8646,"SteelMagnetic"),
        new Identity(8196,"Teflon"),
        new Identity(8752,"Tritanium"),
        new Identity(8027,"TungsticAcid"),
        new Identity(8286,"Weedwood"),
        new Identity(8105,"WhiteEndSand"),
        new Identity(8221,"Wood"),
        new Identity(8224,"WoodRubber")
    };
    private static final Set<String> RELEVANT=Set.of("oreRaw","crushed","crushedTiny","crushedPurified",
        "crushedPurifiedTiny","crushedCentrifuged","crushedCentrifugedTiny","rawOreChunk");
    @GameTest(template="test_empty",batch="ore_alignment_batch",timeoutTicks=200)
    public static void reviewedRawCohortRequiresSourceOrExternalInput(GameTestHelper h){
        int eligible=0,requested=0,external=0;var failures=new ArrayList<String>();
        h.assertTrue(SOURCE_NO.length==53,"literal paired source53");
        for(var id:SOURCE_NO){
            var m=GTMaterialRegistry.get(id.name()).resolve();
            h.assertTrue(m.isValid()&&m.getId()==id.id()&&m.getName().equals(id.name())&&GTMaterialRegistry.isCoreMaterial(m)&&!AddonMaterials.owns(m),"canonical source fixture "+id);
            var input=stack(MaterialPrefix.oreRaw,m);
            h.assertTrue(!input.isEmpty()&&input.getItem() instanceof MaterialFormItem form&&form.getMaterial().resolve()==m
                &&form.getPrefix()==MaterialPrefix.oreRaw&&BuiltInRegistries.ITEM.getKey(input.getItem()).getNamespace().equals("gt6ce"),"retained canonical raw ID "+id);
            var reasons=overrideReasons(m);if(reasons.isEmpty())eligible++;
            if(reasons.stream().anyMatch(s->s.startsWith("requested:")))requested++;
            if(reasons.stream().anyMatch(s->!s.startsWith("requested:")))external++;
            System.out.println("[nonore-raw-classification] "+id.id()+"/"+id.name()+" overrides="+reasons);
            System.out.println("[nonore-raw-facts] "+id.name()+" target="+m.getTargetCrushingMaterial().resolve().getName()+" amount="+m.getTargetCrushingAmount()
                +" multiplier="+m.getOreMultiplier()+"/"+m.getOreProcessingMultiplier()+" quality="+m.getToolQuality());
            boolean representative=Set.of("Wood","Snow","Ice","PVC").contains(m.getName());
            if(representative)h.assertTrue(m.getTargetCrushingMaterial().resolve()==m&&m.getTargetCrushingAmount()==GTValues.U
                &&m.getOreMultiplier()==1&&m.getOreProcessingMultiplier()==1&&m.getToolQuality()==(m.getName().equals("PVC")?1:0),"literal representative target/U/multiplier/quality "+m.getName());
            for(var map:new RecipeMap[]{MachineRecipeMaps.Crusher,MachineRecipeMaps.Hammer}){
                var rows=map.mRecipeList.stream().filter(row->Arrays.stream(row.mInputs).anyMatch(i->!i.isEmpty()&&i.is(input.getItem()))).toList();
                var selected=map.findRecipe(List.of(input),List.of(),false,1,12);var warm=map.findRecipe(List.of(input),List.of(),false,1,12);
                h.assertTrue(warm==selected,"stable warmed raw lookup "+id+"/"+map.mNameInternal);
                if(selected!=null)h.assertTrue(rows.contains(selected),"raw selected actual accepted row, not hidden provider");
                System.out.println("[nonore-raw-row] "+id.id()+"/"+id.name()+" map="+map.mNameInternal+" accepted="+rows.size()+" selected="+(selected!=null)+" warm="+(warm!=null));
                for(var row:rows)System.out.println("[nonore-raw-detail] "+id.name()+"/"+map.mNameInternal+" "+fingerprint(row));
                if(representative&&!rows.isEmpty()){
                    h.assertTrue(rows.size()==1&&selected==rows.get(0),"literal representative unique selected admission");
                    int count=map==MachineRecipeMaps.Crusher?2:1;var outputs=new ItemStack[count];Arrays.fill(outputs,stack(MaterialPrefix.crushed,m));
                    long duration=(map==MachineRecipeMaps.Crusher?128:32)*(m.getName().equals("PVC")?2:1);
                    OreSourceDifferentialTests.row(h,"literal existing raw representative "+id.name(),map,input,new FluidStack[0],outputs,
                        count==2?new long[]{10000,10000}:new long[]{10000},new FluidStack[0],duration,16);
                    System.out.println("[nonore-raw-literal] "+id.name()+"/"+map.mNameInternal+" verified=true");
                }
                if(reasons.isEmpty()&&(!rows.isEmpty()||selected!=null))failures.add(id.name()+"/"+map.mNameInternal);
            }
        }
        h.assertTrue(eligible==52&&requested==1&&external==0,"controlled raw census52NO+1Bakelite defer, actual "+eligible+"/"+requested+"/"+external);
        h.assertTrue(failures.isEmpty(),"source-NO fabricated raw Crusher/Hammer remain: "+failures);
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_alignment_batch",timeoutTicks=200)
    public static void cinnabarRawChanceAndHammerRemainLiteral(GameTestHelper h){
        var in=OreSourceDifferentialTests.s("oreRaw","Cinnabar",1);var crushed=OreSourceDifferentialTests.s("crushed","Cinnabar",1);
        OreSourceDifferentialTests.row(h,"source raw Cinnabar Crusher",MachineRecipeMaps.Crusher,in,new FluidStack[0],
            new ItemStack[]{crushed,crushed.copy(),OreSourceDifferentialTests.s("gem","Cinnabar",1)},new long[]{10000,10000,3000},new FluidStack[0],128,16);
        OreSourceDifferentialTests.row(h,"source raw Cinnabar Hammer",MachineRecipeMaps.Hammer,in,new FluidStack[0],
            new ItemStack[]{crushed},new long[]{10000},new FluidStack[0],32,16);
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_alignment_batch",timeoutTicks=200)
    public static void acceptedRawRequestUnknownAndAddonRetainBothMaps(GameTestHelper h){
        h.assertTrue(AddonMaterials.acceptedForms().stream().anyMatch(f->f.materialName().equals("Bakelite")&&f.prefixName().equals("oreRaw")),"public namespace-scoped Bakelite oreRaw request actually accepted");
        for(String name:new String[]{"Bakelite","Sand","SampleAlloy"}){
            var m=GTMaterialRegistry.get(name).resolve();if(name.equals("Sand"))h.assertTrue(m.getId()==8100,"reviewed Sand UNKNOWN");
            if(name.equals("SampleAlloy"))h.assertTrue(AddonMaterials.owns(m)&&!GTMaterialRegistry.isCoreMaterial(m),"addon-owned fixture");
            for(var map:new RecipeMap[]{MachineRecipeMaps.Crusher,MachineRecipeMaps.Hammer})snapshot(h,map,stack(MaterialPrefix.oreRaw,m),name+"/raw");
        }
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_alignment_batch",timeoutTicks=200)
    public static void blockAndVanillaCallersRemainAccepted(GameTestHelper h){
        var block=BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("gt6ce:ore_iron"));
        h.assertTrue(block!=net.minecraft.world.item.Items.AIR,"actual standard Iron ore block exists");
        for(var input:new ItemStack[]{new ItemStack(block),new ItemStack(net.minecraft.world.item.Items.IRON_ORE),
            new ItemStack(net.minecraft.world.item.Items.NETHER_QUARTZ_ORE),new ItemStack(net.minecraft.world.item.Items.RAW_COPPER),new ItemStack(net.minecraft.world.item.Items.RAW_COPPER_BLOCK)})
            for(var map:new RecipeMap[]{MachineRecipeMaps.Crusher,MachineRecipeMaps.Hammer})snapshot(h,map,input,BuiltInRegistries.ITEM.getKey(input.getItem()).toString());
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_alignment_batch",timeoutTicks=200)
    public static void legitimateRockDustMortarAndNativeFurnaceRemain(GameTestHelper h){
        for(String name:new String[]{"Wood","Concrete","Limestone"})snapshot(h,MachineRecipeMaps.Crusher,stack(MaterialPrefix.rockGt,GTMaterialRegistry.get(name)),name+"/rock");
        snapshot(h,MachineRecipeMaps.Mortar,stack(MaterialPrefix.plate,GTMaterialRegistry.get("Wood")),"Wood/plateMortar");
        int dustRows=0;
        for(var id:SOURCE_NO){var in=stack(MaterialPrefix.dust,GTMaterialRegistry.get(id.name()));
            h.assertTrue(!in.isEmpty(),"retained legitimate dust form "+id.name());
            for(var map:new RecipeMap[]{MachineRecipeMaps.Melter,MachineRecipeMaps.Smelter,MachineRecipeMaps.Furnace})
                for(var row:map.mRecipeList)if(Arrays.stream(row.mInputs).anyMatch(i->!i.isEmpty()&&i.is(in.getItem())))
                    {dustRows++;System.out.println("[nonore-raw-preserved-dust] "+id.name()+"/"+map.mNameInternal+" "+fingerprint(row));}
        }
        h.assertTrue(dustRows>0,"actual legitimate dust rows remain");
        // Existing source-backed control inspects actual native RecipeManager and exact canonical
        // vanilla Copper ingot output, plus Cinnabar's furnace absence; no desired recipe insertion.
        OreSourceControlTests.furnaceUsesSourceFurnaceNotMeltingFlag(h);
    }

    @GameTest(template="test_empty",batch="ore_alignment_batch",timeoutTicks=400)
    public static void reviewedSluiceCohortRequiresSourceOrExternalInput(GameTestHelper h){
        var failures=new ArrayList<String>();int eligible=0;
        for(var id:SOURCE_NO){var m=GTMaterialRegistry.get(id.name()).resolve();
            h.assertTrue(m.getId()==id.id()&&GTMaterialRegistry.isCoreMaterial(m)&&!AddonMaterials.owns(m),"Sluice source identity "+id);
            boolean noOverride=overrideReasons(m).isEmpty();if(noOverride)eligible++;
            NonOreSluiceAssertions.auditNo(h,m,noOverride,failures);
        }
        h.assertTrue(eligible==52,"controlled Sluice52NO/one accepted startup override");
        h.assertTrue(failures.isEmpty(),"source-NO fabricated Sluice remain: "+failures);h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_alignment_batch",timeoutTicks=400)
    public static void reviewedMagneticClassesUseOppositeSourceProducts(GameTestHelper h){
        var failures=new ArrayList<String>();MagneticBatchAssertions.audit(h,failures);
        h.assertTrue(failures.isEmpty(),"source magnetic class/output mismatches: "+failures);h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_alignment_batch",timeoutTicks=200)
    public static void sourceSluicePositivesUnknownAndOverridesRemain(GameTestHelper h){
        NonOreSluiceAssertions.cinnabar(h);
        for(String n:new String[]{"Sand","Bakelite","SampleAlloy"})NonOreSluiceAssertions.preserved(h,GTMaterialRegistry.get(n));
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_alignment_batch",timeoutTicks=200)
    public static void retainedPackingAndCinnabarThermalRemainLiteral(GameTestHelper h){
        ThermalPreservationControls.verify(h);
        for(String name:new String[]{"PVC","Ice","Cinnabar","Bakelite"})for(String prefix:new String[]{"crushed","crushedPurified","crushedCentrifuged"}){
            var whole=OreSourceDifferentialTests.s(prefix,name,1);var tiny=OreSourceDifferentialTests.s(prefix+"Tiny",name,9);
            for(boolean pack:new boolean[]{false,true}){
                var map=pack?MachineRecipeMaps.Boxinator:MachineRecipeMaps.Unboxinator;
                var inputs=pack?List.of(tiny,new ItemStack(gregtech.content.registry.GTTechnological.selectorTag(9))):List.of(whole);
                var expected=pack?whole:tiny;var row=map.findRecipe(inputs,List.of(),false,map.mInputItemsCount,map.mOutputItemsCount);
                h.assertTrue(row!=null&&map.mRecipeList.contains(row)&&map.findRecipe(inputs,List.of(),false,map.mInputItemsCount,map.mOutputItemsCount)==row,"retained packing selected actual warm row "+name+"/"+prefix+"/"+pack);
                h.assertTrue(row.mOutputs.length==1&&row.mOutputs[0].is(expected.getItem())&&row.mOutputs[0].getCount()==expected.getCount()
                    &&row.mDuration==16&&row.mEUt==16&&row.mFluidInputs.length==0&&row.mFluidOutputs.length==0&&row.getOutputChance(0)==10000,"literal retained9:1 packing "+name+"/"+prefix+"/"+pack);
                System.out.println("[ore-batch-packing] "+name+"/"+prefix+"/"+pack+" verified=true");
            }
        }h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_alignment_batch",timeoutTicks=200)
    public static void wholeDustThermalUsesSourceTargetPermissionAndWork(GameTestHelper h){
        var failures=new ArrayList<String>();WholeDustThermalAssertions.audit(h,failures);
        h.assertTrue(failures.isEmpty(),"source Ti/Fe whole-dust thermal mismatches: "+failures);h.succeed();
    }
    private static void snapshot(GameTestHelper h,RecipeMap map,ItemStack input,String label){
        var row=requireAccepted(h,map,input,List.of(),label);
        h.assertTrue(map.findRecipe(List.of(input),List.of(),false,1,12)==row,"preserved warm lookup "+label);
        System.out.println("[nonore-raw-preserved] "+label+"/"+map.mNameInternal+" "+fingerprint(row));
    }
    private static List<String> overrideReasons(GTMaterial m){
        var reasons=new ArrayList<String>();
        for(var f:AddonMaterials.acceptedForms())if(f.materialName().equals(m.getName())&&RELEVANT.contains(f.prefixName()))reasons.add("requested:"+f.prefixName());
        for(var entry:CraftingMaterialForms.nativeAliases().entrySet()){
            if(!entry.getKey().material().equals(m.getName())||!RELEVANT.contains(entry.getKey().prefix()))continue;
            for(var item:entry.getValue()){
                boolean own=item instanceof MaterialFormItem form&&form.getMaterial().resolve()==m
                        &&form.getPrefix().getName().equals(entry.getKey().prefix())
                        &&BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("gt6ce")
                        &&GTItems.getStack(form.getPrefix(),m,1).getItem()==item;
                if(!own)reasons.add("external-alias:"+BuiltInRegistries.ITEM.getKey(item)+"/"+entry.getKey().prefix());
            }
        }
        for(String name:RELEVANT){
            var prefix=gregtech.internal.prefix.PrefixRegistry.byName(name);if(prefix==null)continue;
            var canonical=stack(prefix,m);if(canonical.isEmpty())continue;
            var data=ItemMaterialRegistry.explicit(canonical.getItem()).orElse(null);
            if(data!=null&&(data.prefix()!=prefix||!data.recoverable()||data.components().size()!=1
                    ||data.material().resolve()!=m||data.amount()!=prefix.getMaterialWeight()))
                reasons.add("canonical-binding-override:"+name);
        }
        // Explicit foreign/mixed composition is authoritative even if alias indexing chose another form.
        for(var entry:ItemMaterialRegistry.entries().entrySet()){
            var data=entry.getValue();if(data.prefix()==null||!RELEVANT.contains(data.prefix().getName()))continue;
            if(data.components().stream().noneMatch(c->c.material().resolve()==m))continue;
            boolean own=entry.getKey() instanceof MaterialFormItem form&&form.getMaterial().resolve()==m&&form.getPrefix()==data.prefix()
                    &&BuiltInRegistries.ITEM.getKey(entry.getKey()).getNamespace().equals("gt6ce")&&data.recoverable()
                    &&data.components().size()==1&&data.amount()==data.prefix().getMaterialWeight();
            if(!own)reasons.add("explicit-override:"+BuiltInRegistries.ITEM.getKey(entry.getKey()));
        }
        return List.copyOf(reasons);
    }
    private static ItemStack stack(MaterialPrefix p,GTMaterial m){return GTItems.getStack(p,m.resolve(),1);}
    private static Recipe requireAccepted(GameTestHelper h,RecipeMap map,ItemStack input,List<FluidStack> fluids,String label){
        h.assertTrue(!input.isEmpty(),label+" actual input exists");Recipe row=map.findRecipe(List.of(input),fluids,false,1,12);
        h.assertTrue(row!=null&&map.mRecipeList.contains(row),label+" actual accepted lookup");return row;
    }
    private static List<String> itemText(ItemStack[] stacks){return Arrays.stream(stacks).map(i->i==null||i.isEmpty()?"<empty>":BuiltInRegistries.ITEM.getKey(i.getItem())+"x"+i.getCount()).toList();}
    private static List<String> fluidText(FluidStack[] stacks){return Arrays.stream(stacks).map(f->f==null||f.isEmpty()?"<empty>":BuiltInRegistries.FLUID.getKey(f.getFluid())+"x"+f.getAmount()).toList();}
    private static String fingerprint(Recipe r){return itemText(r.mInputs)+"->"+itemText(r.mOutputs)+";chance="+Arrays.toString(r.mChances)
            +";fin="+fluidText(r.mFluidInputs)+";fout="+fluidText(r.mFluidOutputs)+";work="+r.mDuration+"/"+r.mEUt+"/"+r.mSpecialValue;}
}
