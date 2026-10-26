package gregtech.content.recipe;

import gregtech.api.material.*;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.content.data.FluidCatalog;
/** Source-bound generated Mercury only. Never treats equal external definitions as factory-owned. */
public final class ReviewedOreGeneratedFluids {
 private ReviewedOreGeneratedFluids(){}
 public static boolean mercury(FluidCatalog.FluidEntry entry){
  String field="GenLiquid_Mercury";
  if(entry==null||!FluidCatalog.canonicalField(field).equals(field)
    ||!FluidCatalog.isGeneratedGasLiquidDefinition(field,entry))return false;
  var material=GTMaterialRegistry.get("Mercury");
  if(material==null||!material.isValid())return false;material=material.resolve();
  if(material.getId()!=800||!material.getName().equals("Mercury")||!GTMaterialRegistry.isCoreMaterial(material)
    ||AddonMaterials.owns(material))return false;
  return entry.registryName().equals("mercury")&&"Mercury".equals(entry.materialKey())
    &&entry.textureMode()==FluidCatalog.FluidTextureMode.DEDICATED&&entry.tintArgb()==0
    &&entry.tintTexture()&&!entry.glint()&&!entry.gas()&&entry.density()==0&&entry.viscosity()==0
    &&entry.luminosity()==0&&entry.temperature()==300&&entry.flags()==FluidCatalog.FluidFlags.LIQUID;
 }
 /** Two source-reviewed ordinary-dust targets; absent/unowned/mutated identities defer. */
 public static boolean wholeDustMetal(gregtech.api.material.GTMaterial material,FluidCatalog.FluidEntry entry){
  if(material==null||entry==null)return false;material=material.resolve();
  if(!GTMaterialRegistry.isCoreMaterial(material)||AddonMaterials.owns(material))return false;
  int temperature;float density;
  if(material.getId()==220&&material.getName().equals("Titanium")){temperature=1941;density=4.54F;}
  else if(material.getId()==260&&material.getName().equals("Iron")){temperature=1811;density=7.874F;}
  else return false;
  String field="GenMolten_"+material.getName();
  return FluidCatalog.isGeneratedMoltenDefinition(field,entry)
    &&material.getTargetSmeltingMaterial()!=null&&material.getTargetSmeltingMaterial().resolve()==material
    &&material.getTargetSmeltingAmount()==gregtech.api.material.GTValues.U
    &&material.getMeltingPoint()==temperature&&material.getDensity()==density
    &&material.has(gregtech.api.material.MaterialProperty.MELTING)
    &&!material.has(gregtech.api.material.MaterialProperty.ANTIMATTER)
    &&entry.registryName().equals("molten."+material.getName().toLowerCase(java.util.Locale.ROOT))
    &&material.getName().equals(entry.materialKey())&&entry.textureMode()==FluidCatalog.FluidTextureMode.GENERIC_MOLTEN
    &&entry.tintTexture()&&!entry.glint()&&!entry.gas()&&entry.density()==0&&entry.viscosity()==0
    &&entry.luminosity()==10&&entry.temperature()==temperature&&entry.flags()==FluidCatalog.FluidFlags.LIQUID;
 }
}
