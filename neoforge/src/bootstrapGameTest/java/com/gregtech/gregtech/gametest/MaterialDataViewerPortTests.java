package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.api.machine.*;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.content.recipe.*;
import com.gregtech.gregtech.content.transport.*;
import com.gregtech.gregtech.content.transport.fluid.FluidTransportDefinitions;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.data.generated.MaterialDataFacts;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("gregtech_data_viewer") @PrefixGameTestTemplate(false)
public final class MaterialDataViewerPortTests {
 private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("gregtech:"+id)));}
 private static Recipe lookup(GameTestHelper h,RecipeMap map,List<ItemStack> items,List<net.neoforged.neoforge.fluids.FluidStack> fluids){return map.findRecipe(items,fluids,false,3,3,h.getLevel(),null,ItemStack.EMPTY);}
 private static List<ItemStack> pipes(){var list=new ArrayList<ItemStack>();for(var spec:FluidTransportDefinitions.pipes())list.add(item(spec.id()));for(var mat:ItemPipeCatalog.ITEM_PIPE_MATS)for(var size:ItemPipeSpec.ItemPipeSize.values())list.add(item("item_pipe_"+size.name().toLowerCase(Locale.ROOT)+"_"+mat.idSuffix()));return list;}
 @GameTest(template="test_empty",timeoutTicks=100)
 public static void nativePreferredPipeFormsAndGroupedScannerInputs(GameTestHelper h){
  var usb=item("usb3_stick");int accepted=0,rejected=0;var seen=new HashSet<Integer>();
  for(var row:MaterialDataViewerRecipes.scans()){
   var material=GTMaterialDataRecipes.scannedMaterial(row.mOutputs[0]).resolve();
   h.assertTrue(row.mFakeRecipe&&seen.add(material.getId()),"one informational scanner page per positive material");
   var alternatives=row.viewerInputAlternatives(0);h.assertTrue(!alternatives.isEmpty(),"grouped native scan forms");
   for(var stack:alternatives){var form=MaterialEquivalence.form(stack);h.assertTrue(form!=null&&form.material().resolve()==material&&MaterialDataFacts.scannable(form.prefix()),"every displayed alternative is a real original SCANNABLE form");}
   alternatives.get(0).setCount(7);h.assertTrue(row.viewerInputAlternatives(0).get(0).getCount()==1,"caller cannot mutate shared viewer inputs");
  }
  for(var stack:pipes()){
   var form=MaterialEquivalence.form(stack);h.assertTrue(form!=null,"real pipe prefix");
   var preferred=GTItems.getStack(form.prefix(),form.material(),3);
   h.assertTrue(preferred.is(stack.getItem())&&preferred.getCount()==3,"original native pipe form target and requested quantity "+stack);
   var real=lookup(h,MachineRecipeMaps.ScannerMolecular,List.of(stack,usb),List.of());
   boolean valid=MaterialDataFacts.scannable(form.prefix());
   h.assertTrue((real!=null)==valid,"original pipe scan gate "+stack);
   if(!valid){rejected++;continue;}
   accepted++;h.assertTrue(!real.mFakeRecipe&&!real.mCanBeBuffered&&real.mEUt==512,"viewer rows never become executable cached recipes");
   var consumed=RecipeInputs.consume(real,List.of(stack,usb),List.of(),1);
   h.assertTrue(consumed!=null&&consumed.items().stream().allMatch(ItemStack::isEmpty)&&GTMaterialDataRecipes.scannedMaterial(real.mOutputs[0]).resolve()==form.material().resolve(),"actual scanner consumes pipe and blank USB and writes the right material");
   h.assertTrue(MaterialDataViewerRecipes.scans().stream().filter(r->GTMaterialDataRecipes.scannedMaterial(r.mOutputs[0]).resolve()==form.material().resolve()).anyMatch(r->r.viewerInputAlternatives(0).stream().anyMatch(s->s.is(stack.getItem()))),"pipe indexed in the grouped viewer input");
  }
  h.assertTrue(accepted==303&&rejected==103&&pipes().size()==406,"40 fluid tiny and 63 restrictive forms are excluded; six fluid sizes and three item sizes scan");
  h.succeed();
 }
 @GameTest(template="test_empty",timeoutTicks=100)
 public static void viewerPrinterAndReplicatorKeepDynamicMediumRules(GameTestHelper h){
  var counts=GTMaterialDataRecipes.coverage();
  h.assertTrue(MaterialDataViewerRecipes.scans().size()==counts[0]&&MaterialDataViewerRecipes.prints().size()==counts[1]&&MaterialDataViewerRecipes.replications().size()==counts[2],"all currently registered material-data domain has bounded viewer coverage");
  int before=MachineRecipeMaps.ScannerMolecular.mRecipeList.size()+MachineRecipeMaps.Printer.mRecipeList.size()+MachineRecipeMaps.Replicator.mRecipeList.size();
  h.assertTrue(MaterialDataViewerRecipes.register()==0&&before==MachineRecipeMaps.ScannerMolecular.mRecipeList.size()+MachineRecipeMaps.Printer.mRecipeList.size()+MachineRecipeMaps.Replicator.mRecipeList.size(),"repeat viewer registration cannot duplicate pages");
  for(var shown:MaterialDataViewerRecipes.prints()){
   h.assertTrue(shown.mFakeRecipe&&shown.isCatalystInput(1),"printer display keeps its scanned medium");
   var real=lookup(h,MachineRecipeMaps.Printer,List.of(shown.mInputs),List.of(shown.mFluidInputs));
   h.assertTrue(real!=null&&!real.mFakeRecipe&&real.mDuration==shown.mDuration&&real.mEUt==shown.mEUt,"real printer matches the material-specific display");
  }
  for(var shown:MaterialDataViewerRecipes.replications()){
   var material=GTMaterialDataRecipes.scannedMaterial(shown.mInputs[0]).resolve();
   h.assertTrue(shown.mFakeRecipe&&shown.isCatalystInput(0)&&material.has(MaterialProperty.UUM)&&!material.has(MaterialProperty.ANTIMATTER),"only original replicable materials, retained USB");
   var real=lookup(h,MachineRecipeMaps.Replicator,List.of(shown.mInputs),List.of(shown.mFluidInputs));
   h.assertTrue(real!=null&&!real.mFakeRecipe&&!real.mCanBeBuffered&&real.mEUt==1&&real.mDuration==shown.mDuration,"native replicator ignores fake pages and resolves current medium");
   h.assertTrue(real.mOutputs.length==shown.mOutputs.length&&real.mFluidOutputs.length==shown.mFluidOutputs.length,"native ambient phase agrees with viewer");
  }
  h.succeed();
 }
}
