package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.api.addon.AddonMaterials;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import java.util.*;
/** Literal paired source Anvil oracle. No registration/mutation or test completion. */
final class AnvilSourceAssertions {
 private record Expected(int id,String name,MaterialPrefix input,int targetId,String target,int quality,long ticks,boolean raw){}
 private static final Expected[] ROWS={
  new Expected(8500,"Stone",MaterialPrefix.rockGt,8500,"Stone",1,72,false),
  new Expected(8503,"NetherBrick",MaterialPrefix.rockGt,8502,"Netherrack",1,72,false),
  new Expected(9114,"Cinnabar",MaterialPrefix.oreRaw,9114,"Cinnabar",0,30,true)};
 static void audit(GameTestHelper h,List<String> failures){
  var map=MachineRecipeMaps.Anvil;
  for(var expected:ROWS){
   var material=GTMaterialRegistry.get(expected.name).resolve();var target=GTMaterialRegistry.get(expected.target).resolve();
   var input=GTItems.getStack(expected.input,material,1);
   var output=GTItems.getStack(expected.raw?MaterialPrefix.crushed:MaterialPrefix.dustSmall,target,expected.raw?1:9);
   var byproduct=expected.raw?GTItems.getStack(MaterialPrefix.crushedTiny,target,6):ItemStack.EMPTY;
   String key=expected.name+"/"+expected.input.getName();
   boolean canonical=material.getId()==expected.id&&material.getName().equals(expected.name)&&GTMaterialRegistry.isCoreMaterial(material)&&!AddonMaterials.owns(material)
    &&target.getId()==expected.targetId&&target.getName().equals(expected.target)&&!AddonMaterials.owns(target);
   boolean inputIdentity=!input.isEmpty()&&input.getItem() instanceof MaterialFormItem form&&form.getMaterial().resolve()==material&&form.getPrefix()==expected.input
    &&BuiltInRegistries.ITEM.getKey(input.getItem()).getNamespace().equals("gt6ce");
   var data=input.isEmpty()?null:ItemMaterialRegistry.explicit(input.getItem()).orElse(null);
   boolean ordinary=data==null||(data.recoverable()&&data.prefix()==expected.input&&data.components().size()==1&&data.material().resolve()==material&&data.amount()==expected.input.getMaterialWeight());
   boolean conditions=!material.has(MaterialProperty.ANTIMATTER)&&material.getToolQuality()==expected.quality
    &&(!expected.raw||(com.gregtech.gregtech.data.generated.MaterialWorkability.isMortarGrindable(material)&&material.getTargetCrushingMaterial().resolve()==material));
   System.out.println("[anvil-source-facts] "+key+" canonical="+canonical+" inputIdentity="+inputIdentity+" ordinary="+ordinary+" conditions="+conditions
    +" quality="+material.getToolQuality()+" pulver="+material.getTargetPulverMaterial()+" expectedPulver="+expected.target+" prefixWeight="+expected.input.getMaterialWeight());
   var rows=input.isEmpty()?List.<Recipe>of():map.mRecipeList.stream().filter(r->Arrays.stream(r.mInputs).anyMatch(s->!s.isEmpty()&&s.is(input.getItem()))).toList();
   Recipe cold=input.isEmpty()?null:map.findRecipe(List.of(input,ItemStack.EMPTY),List.of(),false,map.mInputItemsCount,map.mOutputItemsCount);
   Recipe warm=input.isEmpty()?null:map.findRecipe(List.of(input,ItemStack.EMPTY),List.of(),false,map.mInputItemsCount,map.mOutputItemsCount);
   Recipe blocked=input.isEmpty()?null:map.findRecipe(List.of(input,new ItemStack(Items.APPLE)),List.of(),false,map.mInputItemsCount,map.mOutputItemsCount);
   System.out.println("[anvil-source-census] "+key+" accepted="+rows.size()+" cold="+describe(cold)+" warmSame="+(warm==cold)+" occupiedSecond="+describe(blocked));
   for(var r:rows)System.out.println("[anvil-source-row] "+key+" selected="+(r==cold)+" "+describe(r));
   if(!canonical||!inputIdentity||!ordinary||!conditions||output.isEmpty()||(expected.raw&&byproduct.isEmpty())){failures.add(key+": source fixture/ownership/input condition mismatch");continue;}
   if(material.getTargetPulverMaterial().resolve()!=target)failures.add(key+": imported pulver target differs from paired source "+expected.target);
   if(cold==null||warm!=cold||!rows.contains(cold)){failures.add(key+": actual stable accepted winner missing");continue;}
   if(com.gregtech.gregtech.content.tool.AnvilWorkInputs.consume(new ItemStack[]{input,ItemStack.EMPTY},cold.mInputs)==null
    ||com.gregtech.gregtech.content.tool.AnvilWorkInputs.consume(new ItemStack[]{input,new ItemStack(Items.APPLE)},cold.mInputs)!=null)
    failures.add(key+": actual Anvil transaction must accept empty and reject occupied second slot");
   if(cold.mInputs.length!=2||!cold.mInputs[0].is(input.getItem())||cold.mInputs[0].getCount()!=1||!cold.mInputs[1].isEmpty()
    ||cold.mFluidInputs.length!=0||cold.mFluidOutputs.length!=0)failures.add(key+": exact source input/empty-slot/fluid shape");
   if(cold.mOutputs.length!=(expected.raw?2:1)||!same(cold.mOutputs[0],output)
    ||(expected.raw&&!same(cold.mOutputs[1],byproduct)))failures.add(key+": exact source target/forms/counts");
   for(int i=0;i<cold.mOutputs.length;i++)if(cold.getOutputChance(i)!=10000)failures.add(key+": source deterministic output chance "+i);
   if(cold.mDuration!=expected.ticks||cold.mEUt!=16)failures.add(key+": source work expected "+expected.ticks+"/16 actual "+cold.mDuration+"/"+cold.mEUt);
  }
 }
 private static boolean same(ItemStack a,ItemStack b){return !a.isEmpty()&&a.is(b.getItem())&&a.getCount()==b.getCount();}
 private static String describe(Recipe r){if(r==null)return "NONE";return "in="+Arrays.toString(r.mInputs)+" out="+Arrays.toString(r.mOutputs)+" fin="+Arrays.toString(r.mFluidInputs)+" fout="+Arrays.toString(r.mFluidOutputs)+" ticks="+r.mDuration+" eut="+r.mEUt;}
}
