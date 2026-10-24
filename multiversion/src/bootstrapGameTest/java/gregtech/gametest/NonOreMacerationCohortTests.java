package gregtech.gametest;
import gregtech.api.material.*;
import gregtech.api.addon.AddonMaterials;
import gregtech.api.recipe.*;
import gregtech.data.*;
import gregtech.recipe.CraftingMaterialForms;
import gregtech.recipe.MachineRecipeOverlay;
import gregtech.registry.GTItems;
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
/** API-neutral bounded producer tests. Literal identities come from paired source proof, not the
 * production policy helper. No recipe injection, signature removal or global ORES inference.
 * Requires the supported Bakelite/crushed startup form request in the sample fixture.
 */
@GameTestHolder("gregtech_nonore_cohort_audit")
@PrefixGameTestTemplate(false)
public final class NonOreMacerationCohortTests {
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
    private static final MaterialPrefix[] SIX={MaterialPrefix.crushed,MaterialPrefix.crushedTiny,
        MaterialPrefix.crushedPurified,MaterialPrefix.crushedPurifiedTiny,
        MaterialPrefix.crushedCentrifuged,MaterialPrefix.crushedCentrifugedTiny};
    private static final Set<String> RELEVANT=Set.of("oreRaw","crushed","crushedTiny","crushedPurified",
        "crushedPurifiedTiny","crushedCentrifuged","crushedCentrifugedTiny","rawOreChunk");

    @GameTest(template="test_empty",batch="nonore_maceration_cohort",timeoutTicks=200)
    public static void sourceNoCohortReportsAcceptedRowsAndSuppressesOnlyEligibleMacerationWashing(GameTestHelper h){
        h.assertTrue(SOURCE_NO.length==53,"exact paired-source cohort");
        int suppressed=0,requested=0,external=0;var failures=new ArrayList<String>();
        for(var identity:SOURCE_NO){
            var m=GTMaterialRegistry.get(identity.name()).resolve();
            h.assertTrue(m.isValid()&&m.getId()==identity.id()&&m.getName().equals(identity.name())
                    &&GTMaterialRegistry.isCoreMaterial(m)&&!AddonMaterials.owns(m),"canonical source identity "+identity);
            var raw=stack(MaterialPrefix.oreRaw,m);h.assertTrue(!raw.isEmpty(),"retained legacy raw ID "+m.getName());
            List<String> reasons=overrideReasons(m);
            boolean startup=reasons.stream().anyMatch(s->s.startsWith("requested:"));
            boolean foreign=reasons.stream().anyMatch(s->!s.startsWith("requested:"));
            if(startup)requested++;if(foreign)external++;if(reasons.isEmpty())suppressed++;
            System.out.println("[nonore-cohort-classification] "+identity.id()+"/"+identity.name()+" source=NO overrides="+reasons);
            for(var prefix:SIX){
                ItemStack input=stack(prefix,m);h.assertTrue(!input.isEmpty(),"retained legacy form "+identity+"/"+prefix.getName());
                for(var map:new RecipeMap[]{MachineRecipeMaps.Crusher,MachineRecipeMaps.Shredder})
                    inspect(h,failures,m,prefix,map,input,List.of(),reasons.isEmpty());
            }
            inspect(h,failures,m,MaterialPrefix.crushed,MachineRecipeMaps.Bath,stack(MaterialPrefix.crushed,m),
                    List.of(new FluidStack(Fluids.WATER,1000)),reasons.isEmpty());
            inspect(h,failures,m,MaterialPrefix.crushedPurified,MachineRecipeMaps.Centrifuge,
                    stack(MaterialPrefix.crushedPurified,m),List.of(),reasons.isEmpty());
        }
        h.assertTrue(suppressed==52&&requested==1&&external==0,
                "controlled native fixture coverage expected52 baselineNO+1 Bakelite startup defer; got "+suppressed+"/"+requested+"/"+external);
        h.assertTrue(failures.isEmpty(),"bounded source-owned input failures="+failures.size()+": "+failures.stream().limit(24).toList());
        h.succeed();
    }
    @GameTest(template="test_empty",batch="nonore_maceration_cohort",timeoutTicks=200)
    public static void acceptedStartupRequestKnownOreSandUnknownAndAddonKeepTheirChains(GameTestHelper h){
        h.assertTrue(AddonMaterials.acceptedForms().stream().anyMatch(f->f.materialName().equals("Bakelite")&&f.prefixName().equals("crushed")),
                "supported actual Bakelite request accepted, not invented runtime metadata");
        for(String name:new String[]{"Bakelite","Copper","Sand","SampleAlloy"}){
            var m=GTMaterialRegistry.get(name).resolve();
            if(name.equals("Sand"))h.assertTrue(m.getId()==8100,"Sand identity remains explicitly UNKNOWN in paired review");
            if(name.equals("SampleAlloy"))h.assertTrue(AddonMaterials.owns(m)&&!GTMaterialRegistry.isCoreMaterial(m),"addon-owned positive control");
            requireAccepted(h,MachineRecipeMaps.Crusher,stack(MaterialPrefix.crushedTiny,m),List.of(),name+" tiny Crusher");
            requireAccepted(h,MachineRecipeMaps.Shredder,stack(MaterialPrefix.crushedTiny,m),List.of(),name+" tiny Shredder");
            requireAccepted(h,MachineRecipeMaps.Bath,stack(MaterialPrefix.crushed,m),List.of(new FluidStack(Fluids.WATER,1000)),name+" Bath");
            requireAccepted(h,MachineRecipeMaps.Centrifuge,stack(MaterialPrefix.crushedPurified,m),List.of(),name+" Centrifuge");
        }
        h.succeed();
    }
    @GameTest(template="test_empty",batch="nonore_maceration_cohort",timeoutTicks=200)
    public static void dustRecyclingRocksWoodMortarAndForcedIceFormsSurviveOverlay(GameTestHelper h){
        for(String name:new String[]{"Concrete","Limestone"})requireAccepted(h,MachineRecipeMaps.Crusher,
                stack(MaterialPrefix.rockGt,GTMaterialRegistry.get(name)),List.of(),name+" legitimate rock");
        requireAccepted(h,MachineRecipeMaps.Mortar,stack(MaterialPrefix.plate,GTMaterialRegistry.get("Wood")),List.of(),"Wood plate Mortar");
        for(var prefix:new MaterialPrefix[]{MaterialPrefix.gem,MaterialPrefix.gemChipped,MaterialPrefix.gemFlawed})
            h.assertTrue(!stack(prefix,GTMaterialRegistry.get("Ice")).isEmpty(),"forced Ice form preserved "+prefix.getName());
        var before=dustRows();h.assertTrue(!before.isEmpty(),"actual legacy dust processing/recycling rows present");
        System.out.println("[nonore-dust-snapshot] "+new com.google.gson.Gson().toJson(before));
        MachineRecipeOverlay.apply(h.getLevel().getRecipeManager());
        h.assertTrue(before.equals(dustRows()),"existing accepted dust rows/quantities/work survive bounded ore replay");
        h.succeed();
    }
    private static void inspect(GameTestHelper h,List<String> failures,GTMaterial m,MaterialPrefix prefix,RecipeMap map,
            ItemStack input,List<FluidStack> fluids,boolean absent){
        var accepted=map.mRecipeList.stream().filter(r->Arrays.stream(r.mInputs).anyMatch(i->!i.isEmpty()&&i.is(input.getItem()))).toList();
        Recipe selected=map.findRecipe(List.of(input),fluids,false,1,12);
        System.out.println("[nonore-cohort-row] "+m.getName()+"/"+prefix.getName()+" map="+map.mNameInternal+" accepted="+accepted.size()+" selected="+(selected!=null));
        for(var row:accepted)System.out.println("[nonore-cohort-detail] "+m.getName()+" "+map.mNameInternal+" "+fingerprint(row));
        if(absent&&(!accepted.isEmpty()||selected!=null))failures.add(m.getName()+"/"+prefix.getName()+"/"+map.mNameInternal);
        if(selected!=null)h.assertTrue(map.mRecipeList.contains(selected),"selected row actually accepted (not an unclassified dynamic provider)");
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
            var prefix=gregtech.api.prefix.PrefixRegistry.byName(name);if(prefix==null)continue;
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
    private static List<String> dustRows(){
        var rows=new ArrayList<String>();
        for(var id:SOURCE_NO){var input=stack(MaterialPrefix.dust,GTMaterialRegistry.get(id.name()));
            if(input.isEmpty())continue;
            for(var map:new RecipeMap[]{MachineRecipeMaps.Melter,MachineRecipeMaps.Smelter,MachineRecipeMaps.Furnace})
                for(var row:map.mRecipeList)if(Arrays.stream(row.mInputs).anyMatch(i->!i.isEmpty()&&i.is(input.getItem())))
                    rows.add(id.name()+"/"+map.mNameInternal+"/"+fingerprint(row));
        }
        Collections.sort(rows);return List.copyOf(rows);
    }
    private static List<String> itemText(ItemStack[] stacks){return Arrays.stream(stacks).map(i->i==null||i.isEmpty()?"<empty>":BuiltInRegistries.ITEM.getKey(i.getItem())+"x"+i.getCount()).toList();}
    private static List<String> fluidText(FluidStack[] stacks){return Arrays.stream(stacks).map(f->f==null||f.isEmpty()?"<empty>":BuiltInRegistries.FLUID.getKey(f.getFluid())+"x"+f.getAmount()).toList();}
    private static String fingerprint(Recipe r){return itemText(r.mInputs)+"->"+itemText(r.mOutputs)+";chance="+Arrays.toString(r.mChances)
            +";fin="+fluidText(r.mFluidInputs)+";fout="+fluidText(r.mFluidOutputs)+";work="+r.mDuration+"/"+r.mEUt+"/"+r.mSpecialValue;}
}
