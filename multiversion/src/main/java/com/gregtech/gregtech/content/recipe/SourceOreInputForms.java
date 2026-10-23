package com.gregtech.gregtech.content.recipe;
import com.gregtech.gregtech.api.addon.AddonMaterials;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialFormItem;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
/** Recipe-local guard for reviewed source NO; does not unregister legacy items or alter target links.
 * Evidence must come from a separately reviewed ID+canonical-name profile, not missing MaterialForms.
 * External ore adapters and explicit recipes stay independent. Not wired into frozen stage1.
 */
public final class SourceOreInputForms {
 public enum Evidence { YES, NO, UNKNOWN }
 private SourceOreInputForms() {}
 public static boolean allowGeneratedInput(GTMaterial material,MaterialPrefix prefix,ItemStack input,Evidence evidence){
  if(input==null||input.isEmpty())return false;
  if(evidence!=Evidence.NO||!oreOnly(prefix))return true;
  material=material.resolve();
  if(AddonMaterials.owns(material)||!GTMaterialRegistry.isCoreMaterial(material))return true;
  // A real externally registered ore input remains admissible despite builtin ORES_NO.
  if(!BuiltInRegistries.ITEM.getKey(input.getItem()).getNamespace().equals("gt6ce"))return true;
  if(!(input.getItem() instanceof MaterialFormItem form)||form.getPrefix()!=prefix
       ||form.getMaterial().resolve()!=material)return true; // uncertain provenance retains legacy
  String name=material.getName();
  if(AddonMaterials.acceptedForms().stream().anyMatch(f->f.materialName().equals(name)&&f.prefixName().equals(prefix.getName())))return true;
  return false; // only a manufactured builtin legacy ore input lacking source generation evidence
 }
 /** Source MT Stone8500 -> stone factory G_STONE, without ORES. Missing profile rows are UNKNOWN.
  * Applies only to generated recipe outputs; legitimate explicit external forms remain authoritative. */
 public static ItemStack reviewedOreOutput(MaterialPrefix prefix,GTMaterial material,int count){
  material=material.resolve();
  ItemStack canonical=com.gregtech.gregtech.registry.GTItems.getStack(prefix,material,count);
  Evidence evidence=material.getId()==8500&&material.getName().equals("Stone")?Evidence.NO:Evidence.UNKNOWN;
  if(evidence!=Evidence.NO||AddonMaterials.owns(material)||!GTMaterialRegistry.isCoreMaterial(material))return canonical;
  String name=material.getName();
  if(AddonMaterials.acceptedForms().stream().anyMatch(f->f.materialName().equals(name)&&f.prefixName().equals(prefix.getName())))return canonical;
  if(!canonical.isEmpty()&&!BuiltInRegistries.ITEM.getKey(canonical.getItem()).getNamespace().equals("gt6ce"))return canonical;
  ItemStack external=ItemStack.EMPTY;
  for(var item:com.gregtech.gregtech.api.material.ItemMaterialRegistry.entries().keySet()){
   if(BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("gt6ce"))continue;
   ItemStack stack=new ItemStack(item,count);
   var form=com.gregtech.gregtech.api.material.MaterialEquivalence.form(stack);
   if(form==null||form.prefix()!=prefix||form.material()!=material)continue;
   // Ambiguous externally owned targets are UNKNOWN; do not replace their existing selection.
   if(!external.isEmpty()&&external.getItem()!=item)return canonical;
   external=stack;
  }
  if(!external.isEmpty())return external;
  return allowGeneratedInput(material,prefix,canonical,evidence)?canonical:ItemStack.EMPTY;
 }
 private static boolean oreOnly(MaterialPrefix prefix){
  return prefix==MaterialPrefix.oreRaw||prefix==MaterialPrefix.crushed||prefix==MaterialPrefix.crushedTiny
   ||prefix==MaterialPrefix.crushedPurified||prefix==MaterialPrefix.crushedPurifiedTiny
   ||prefix==MaterialPrefix.crushedCentrifuged||prefix==MaterialPrefix.crushedCentrifugedTiny;
 }
}
