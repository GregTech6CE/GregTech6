package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.addon.AddonMaterials;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Read-only source oracle: original Ores45-49 / W49-53. No GameTest entrypoint,
 * row insertion, h.succeed(), reload or expected values derived from generated tokens.
 * Caller reports the combined semantic failures only after all nine roles are observed.
 */
final class SlurryRecyclingAssertions {
 private SlurryRecyclingAssertions() {}
 private static final String[] WATERS={"Water","MnWtr","DistW","SpDew"};
 static void audit(GameTestHelper h,List<String> failures) {
  var sand=GTMaterialRegistry.get("SluiceSand");
  h.assertTrue(sand!=null&&sand.isValid(),"slurry fixture requires registered SluiceSand");sand=sand.resolve();
  h.assertTrue(sand.getId()==8102&&sand.getName().equals("SluiceSand")&&GTMaterialRegistry.isCoreMaterial(sand)
   &&!AddonMaterials.owns(sand),"slurry fixture canonical source SluiceSand8102");
  var whole=form(h,sand,MaterialPrefix.dust,1);var tiny=form(h,sand,MaterialPrefix.dustTiny,1);
  var slurry100=NonOreSluiceAssertions.fluid(h,"Sluice",100);
  var slurry900=NonOreSluiceAssertions.fluid(h,"Sluice",900);
  var distilled50=NonOreSluiceAssertions.fluid(h,"DistW",50);
  // Resolve all required real fluid identities before source assertions; missing fixtures are not expected RED.
  var water100=new LinkedHashMap<String,FluidStack>();var water900=new LinkedHashMap<String,FluidStack>();
  for(String name:WATERS){water100.put(name,NonOreSluiceAssertions.fluid(h,name,100));water900.put(name,NonOreSluiceAssertions.fluid(h,name,900));}
  for(String name:WATERS)for(boolean small:new boolean[]{true,false}){
   var input=small?tiny:whole;var fluid=(small?water100:water900).get(name);var output=small?slurry100:slurry900;
   String label="Mixer/"+(small?"tiny":"whole")+"/"+name;
   var rows=MachineRecipeMaps.Mixer.mRecipeList.stream().filter(r->Arrays.stream(r.mInputs).anyMatch(s->sandForm(s))
    &&Arrays.stream(r.mFluidInputs).anyMatch(f->sameFluid(f,fluid))).toList();
   observe(h,failures,label,MachineRecipeMaps.Mixer,rows,new ItemStack[]{input},new FluidStack[]{fluid},
    new ItemStack[0],new FluidStack[]{output},16);
  }
  var drying=MachineRecipeMaps.Drying.mRecipeList.stream().filter(r->Arrays.stream(r.mFluidInputs).anyMatch(f->sameFluid(f,slurry100))).toList();
  observe(h,failures,"Drying/tiny",MachineRecipeMaps.Drying,drying,new ItemStack[0],new FluidStack[]{slurry100},
   new ItemStack[]{tiny},new FluidStack[]{distilled50},100);
 }
 private static ItemStack form(GameTestHelper h,GTMaterial sand,MaterialPrefix prefix,int count){
  var stack=GTItems.getStack(prefix,sand,count);
  h.assertTrue(!stack.isEmpty()&&stack.getCount()==count&&stack.getItem() instanceof MaterialFormItem f
   &&f.getPrefix()==prefix&&f.getMaterial().resolve()==sand
   &&BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("gt6ce"),"slurry fixture exact native "+prefix.getName()+"/SluiceSand");
  return stack;
 }
 private static boolean sandForm(ItemStack s){
  return !s.isEmpty()&&s.getItem() instanceof MaterialFormItem f&&f.getMaterial().resolve().getId()==8102
   &&f.getMaterial().resolve().getName().equals("SluiceSand");
 }
 private static void observe(GameTestHelper h,List<String> failures,String label,RecipeMap map,List<Recipe> rows,
   ItemStack[] inputs,FluidStack[] fluids,ItemStack[] outputs,FluidStack[] fluidOutputs,long ticks){
  var selected=lookup(map,inputs,fluids);var warm=lookup(map,inputs,fluids);
  boolean exact=rows.size()==1&&selected==rows.get(0)&&warm==selected&&matches(rows.get(0),inputs,fluids,outputs,fluidOutputs,ticks);
  System.out.println("[slurry-recycling-role] "+label+" accepted="+rows.size()+" selected="+(selected!=null)+" warm="+(selected==warm)+" exact="+exact);
  for(var row:rows){
   var actual=lookup(map,row.mInputs,row.mFluidInputs);var actualWarm=lookup(map,row.mInputs,row.mFluidInputs);
   System.out.println("[slurry-recycling-row] "+label+" selected="+(row==selected)+" actualSelected="+(actual==row)+" actualWarm="+(actual==actualWarm)+" "+fingerprint(row));
   if(actual!=actualWarm)failures.add(label+": unstable actual-input warm selection");
  }
  if(!exact)failures.add(label+": expected literal source item/fluid/work vector; selected="+(selected==null?"none":fingerprint(selected)));
  // The already-correct full Water vector is an explicit preservation witness, not an inferred skipped role.
  if(label.equals("Mixer/whole/Water")&&exact)System.out.println("[slurry-recycling-preserved] "+label+" "+fingerprint(selected));
 }
 private static Recipe lookup(RecipeMap map,ItemStack[] inputs,FluidStack[] fluids){
  return map.findRecipe(Arrays.asList(inputs),Arrays.asList(fluids),false,map.mInputItemsCount,map.mOutputItemsCount);
 }
 private static boolean matches(Recipe r,ItemStack[] in,FluidStack[] fin,ItemStack[] out,FluidStack[] fout,long ticks){
  if(!r.mEnabled||r.mFakeRecipe||r.mDuration!=ticks||r.mEUt!=16||r.mSpecialValue!=0
   ||!sameItems(r.mInputs,in)||!sameItems(r.mOutputs,out)||!sameFluids(r.mFluidInputs,fin)||!sameFluids(r.mFluidOutputs,fout))return false;
  for(int i=0;i<out.length;i++)if(r.getOutputChance(i)!=10000)return false;
  return true;
 }
 private static boolean sameItems(ItemStack[] a,ItemStack[] b){
  if(a.length!=b.length)return false;for(int i=0;i<a.length;i++)if(a[i].isEmpty()||a[i].getItem()!=b[i].getItem()||a[i].getCount()!=b[i].getCount())return false;return true;
 }
 private static boolean sameFluid(FluidStack a,FluidStack b){return !a.isEmpty()&&a.getFluid()==b.getFluid()&&a.getAmount()==b.getAmount();}
 private static boolean sameFluids(FluidStack[] a,FluidStack[] b){
  if(a.length!=b.length)return false;for(int i=0;i<a.length;i++)if(!sameFluid(a[i],b[i]))return false;return true;
 }
 private static String fingerprint(Recipe r){
  var result=new ArrayList<String>();
  for(var s:r.mInputs)result.add("IN:"+BuiltInRegistries.ITEM.getKey(s.getItem())+"x"+s.getCount());
  for(var s:r.mOutputs)result.add("OUT:"+BuiltInRegistries.ITEM.getKey(s.getItem())+"x"+s.getCount());
  for(var f:r.mFluidInputs)result.add("FIN:"+BuiltInRegistries.FLUID.getKey(f.getFluid())+"x"+f.getAmount());
  for(var f:r.mFluidOutputs)result.add("FOUT:"+BuiltInRegistries.FLUID.getKey(f.getFluid())+"x"+f.getAmount());
  result.add("CHANCES:"+Arrays.toString(r.mChances));result.add("WORK:"+r.mDuration+"/"+r.mEUt+"/"+r.mSpecialValue);
  result.add("FLAGS:"+r.mEnabled+"/"+r.mFakeRecipe);return result.toString();
 }
}
