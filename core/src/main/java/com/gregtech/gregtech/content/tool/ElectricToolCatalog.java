package com.gregtech.gregtech.content.tool;
import com.gregtech.gregtech.api.material.*;
import java.util.List;
/** GT6 Loader_Tools:356-376 and inherited electric ToolStats, shared by both loaders. */
public final class ElectricToolCatalog {
 private ElectricToolCatalog() {}
 public record Definition(String id,String name,int tier,String headPrefix,List<String> rows,float speed,int quality,int blockCost,int attackCost,float damage) {
  public long capacity(){return voltage()*1000;}
  public long voltage(){return 8L << (2*tier);}
  public long energyPerUse(){return blockCost;}
  public int durabilityMultiplier(){return 1 << (tier-1);}
  public String handleMaterial(){return new String[]{"","Orange","Red","Blue"}[tier];}
  public String chassisMaterial(){return new String[]{"","SteelGalvanized","Aluminium","StainlessSteel"}[tier];}
  public String original(){return name.replace(" ","").toUpperCase(java.util.Locale.ROOT)+"_"+tierName().toUpperCase(java.util.Locale.ROOT);}
  public String tierName(){return new String[]{"ulv","lv","mv","hv"}[tier];}
 }
 private static Definition row(String id,String name,int tier,String head,String pattern,float speed,int quality,int block,int attack,float damage){return new Definition(id,name,tier,head,List.of(pattern.split("/")),speed,quality,block,attack,damage);}
 public static final List<Definition> ALL=List.of(
  row("electric_drill","Drill",1,"toolHeadDrill","fSY/TXW/dVZ",1,0,200,400,1.5F),
  row("electric_chainsaw","Chainsaw",1,"toolHeadChainsaw","dAT/XWX/XVX",2,1,50,400,3),
  row("electric_wrench","Wrench",1,"toolHeadWrench","dAT/XWX/XVX",2,0,50,200,1),
  row("electric_screwdriver","Screwdriver",1,"toolHeadScrewdriver","XdA/TWY/VYX",1,0,100,100,1),
  row("electric_mining_drill","Mining Drill",1,"toolHeadDrill","dAT/XWX/XVX",3,0,25,200,2),
  row("electric_mixer","Mixer",1,"toolHeadDrill","SSY/SXW/hVZ",1,0,200,400,1.5F),
  row("electric_buzzsaw","BuzzSaw",1,"toolHeadBuzzSaw","YXV/TWX/AdY",1,0,50,300,1),
  row("electric_trimmer","Trimmer",1,"toolHeadSword","XAT/ZYA/VWd",.25F,0,100,100,2),
  row("electric_wrench_mv","Wrench",2,"toolHeadWrench","dAT/XWX/XVX",3,1,200,800,1.5F),
  row("electric_mining_drill_mv","Mining Drill",2,"toolHeadDrill","dAT/XWX/XVX",6,1,100,800,2.5F),
  row("electric_chainsaw_mv","Chainsaw",2,"toolHeadChainsaw","dAT/XWX/XVX",3,1,200,1600,3.5F),
  row("electric_wrench_hv","Wrench",3,"toolHeadWrench","dAT/XWX/XVX",4,1,800,3200,2),
  row("electric_mining_drill_hv","Mining Drill",3,"toolHeadDrill","dAT/XWX/XVX",9,1,400,3200,3),
  row("electric_chainsaw_hv","Chainsaw",3,"toolHeadChainsaw","dAT/XWX/XVX",4,1,800,6400,4));
 public static Definition get(String id){return ALL.stream().filter(d->d.id().equals(id)).findFirst().orElseThrow();}
 public static Definition of(String name,int tier){return ALL.stream().filter(d->d.name().equals(name)&&d.tier()==tier).findFirst().orElseThrow();}
 public static boolean validMaterial(GTMaterial material){return material!=null&&material.isValid()&&material.resolve()==material&&material.getToolTypes()>=3&&material.getToolQuality()>=1&&!material.has(MaterialProperty.WOOD)&&!material.has(MaterialProperty.ANTIMATTER)&&!OriginalToolMaterials.inFamily(material,"Rubber")&&!OriginalToolMaterials.inFamily(material,"Plastic");}
}
