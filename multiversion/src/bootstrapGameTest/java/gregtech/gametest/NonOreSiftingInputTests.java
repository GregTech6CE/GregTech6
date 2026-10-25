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
/** Source-input admission baseline. Actual maps only; no desired-row insertion.
 * Dedicated controlled profile: no startup/foreign ore-form overrides for these three materials.
 * Grade items are not forbidden; only fabricated builtin purified inputs are under test.
 */
@GameTestHolder("gregtech_nonore_sifting_audit")
@PrefixGameTestTemplate(false)
public final class NonOreSiftingInputTests {
 private static final Set<String> RELEVANT=Set.of("oreRaw","crushed","crushedTiny","crushedPurified",
  "crushedPurifiedTiny","crushedCentrifuged","crushedCentrifugedTiny","rawOreChunk");
 @GameTest(template="test_empty",batch="nonore_sifting",timeoutTicks=200)
 public static void flintPurifiedInputsRequireSourceOrExternalOrigin(GameTestHelper h){absent(h,"Flint",8002);h.succeed();}
 @GameTest(template="test_empty",batch="nonore_sifting",timeoutTicks=200)
 public static void icePurifiedInputsRequireSourceOrExternalOrigin(GameTestHelper h){absent(h,"Ice",9802);h.succeed();}
 @GameTest(template="test_empty",batch="nonore_sifting",timeoutTicks=200)
 public static void obsidianPurifiedInputsRequireSourceOrExternalOrigin(GameTestHelper h){absent(h,"Obsidian",8214);h.succeed();}
 private static void absent(GameTestHelper h,String name,int id){
  var m=GTMaterialRegistry.get(name).resolve();
  h.assertTrue(m.isValid()&&m.getId()==id&&m.getName().equals(name)&&GTMaterialRegistry.isCoreMaterial(m)&&!AddonMaterials.owns(m),"canonical source-NO fixture "+name);
  var overrides=overrideReasons(m);h.assertTrue(overrides.isEmpty(),"controlled fixture must not suppress legitimate external overrides: "+name+overrides);
  var failures=new ArrayList<String>();var map=MachineRecipeMaps.Sifting;
  for(var prefix:new MaterialPrefix[]{MaterialPrefix.crushedPurified,MaterialPrefix.crushedPurifiedTiny}){
   var input=stack(prefix,m);h.assertTrue(!input.isEmpty(),"retained legacy purified input "+name+"/"+prefix.getName());
   h.assertTrue(input.getItem() instanceof MaterialFormItem form&&form.getMaterial().resolve()==m&&form.getPrefix()==prefix
    &&BuiltInRegistries.ITEM.getKey(input.getItem()).getNamespace().equals("gt6ce"),"exact fabricated builtin input provenance");
   var rows=map.mRecipeList.stream().filter(row->Arrays.stream(row.mInputs).anyMatch(i->!i.isEmpty()&&i.is(input.getItem()))).toList();
   Recipe first=map.findRecipe(List.of(input),List.of(),false,1,12),warm=map.findRecipe(List.of(input),List.of(),false,1,12);
   System.out.println("[nonore-sifting-input] "+id+"/"+name+"/"+prefix.getName()+" accepted="+rows.size()+" selected="+(first!=null)+" warm="+(warm!=null));
   for(var row:rows)System.out.println("[nonore-sifting-detail] "+name+"/"+prefix.getName()+" "+fingerprint(row));
   if(!rows.isEmpty()||first!=null||warm!=null)failures.add(prefix.getName());
  }
  h.assertTrue(failures.isEmpty(),"source-NO fabricated Sifting inputs remain: "+name+failures);
 }
 @GameTest(template="test_empty",batch="nonore_sifting",timeoutTicks=200)
 public static void cinnabarWholeAndTinySiftingKeepLiteralSourceRows(GameTestHelper h){
  var out=new ItemStack[]{OreSourceDifferentialTests.s("gemLegendary","Cinnabar",1),OreSourceDifferentialTests.s("gemExquisite","Cinnabar",1),
   OreSourceDifferentialTests.s("gemFlawless","Cinnabar",1),OreSourceDifferentialTests.s("gem","Cinnabar",1),
   OreSourceDifferentialTests.s("gemFlawed","Cinnabar",2),OreSourceDifferentialTests.s("gemChipped","Cinnabar",4),OreSourceDifferentialTests.s("dust","Cinnabar",1)};
  OreSourceDifferentialTests.row(h,"paired original Sifting whole",MachineRecipeMaps.Sifting,OreSourceDifferentialTests.s("crushedPurified","Cinnabar",1),
   new FluidStack[0],out,new long[]{9,90,360,1350,1800,3600,4500},new FluidStack[0],144,16);
  OreSourceDifferentialTests.row(h,"paired original Sifting tiny",MachineRecipeMaps.Sifting,OreSourceDifferentialTests.s("crushedPurifiedTiny","Cinnabar",1),
   new FluidStack[0],out,new long[]{1,10,40,150,200,400,500},new FluidStack[0],16,16);
  h.succeed();
 }
 @GameTest(template="test_empty",batch="nonore_sifting",timeoutTicks=200)
 public static void legacyInputIdsAndLegitimateIceGemFormsRemain(GameTestHelper h){
  for(String name:new String[]{"Flint","Ice","Obsidian"}){
   var m=GTMaterialRegistry.get(name).resolve();
   for(var p:new MaterialPrefix[]{MaterialPrefix.oreRaw,MaterialPrefix.crushed,MaterialPrefix.crushedTiny,MaterialPrefix.crushedPurified,
     MaterialPrefix.crushedPurifiedTiny,MaterialPrefix.crushedCentrifuged,MaterialPrefix.crushedCentrifugedTiny,MaterialPrefix.dust,MaterialPrefix.gem})
    h.assertTrue(!stack(p,m).isEmpty(),"do not delete saved IDs or legitimate form "+name+"/"+p.getName());
  }
  for(var p:new MaterialPrefix[]{MaterialPrefix.gemChipped,MaterialPrefix.gemFlawed})
   h.assertTrue(!stack(p,GTMaterialRegistry.get("Ice")).isEmpty(),"forced Ice gem form retained "+p.getName());
  // Actual pre-fix native rows contain these existing save IDs. Source fallback selection
  // must not be implemented by unregistering them, even if an external grade becomes preferred.
  for(String id:new String[]{"gt6ce:gem_legendary_ice","gt6ce:gem_exquisite_ice","gt6ce:gem_flawless_ice"})
   h.assertTrue(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(id))!=net.minecraft.world.item.Items.AIR,
           "retain observed legacy upper-grade registry ID "+id);
  h.succeed();
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
    private static List<String> itemText(ItemStack[] stacks){return Arrays.stream(stacks).map(i->i==null||i.isEmpty()?"<empty>":BuiltInRegistries.ITEM.getKey(i.getItem())+"x"+i.getCount()).toList();}
    private static List<String> fluidText(FluidStack[] stacks){return Arrays.stream(stacks).map(f->f==null||f.isEmpty()?"<empty>":BuiltInRegistries.FLUID.getKey(f.getFluid())+"x"+f.getAmount()).toList();}
    private static String fingerprint(Recipe r){return itemText(r.mInputs)+"->"+itemText(r.mOutputs)+";chance="+Arrays.toString(r.mChances)
            +";fin="+fluidText(r.mFluidInputs)+";fout="+fluidText(r.mFluidOutputs)+";work="+r.mDuration+"/"+r.mEUt+"/"+r.mSpecialValue;}
}
