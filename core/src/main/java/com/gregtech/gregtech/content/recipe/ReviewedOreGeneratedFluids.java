package com.gregtech.gregtech.content.recipe;
import com.gregtech.gregtech.api.addon.AddonMaterials;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.data.FluidCatalog;
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
}
