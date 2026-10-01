package com.gregtech.gregtech.content.tool;
import com.gregtech.gregtech.api.material.*;
/** Original four LV tools and Loader_Tools assembly patterns, one definition for both loaders. */
public final class ElectricToolCatalog {
 private ElectricToolCatalog(){}
 public record Definition(String id,String name,long capacity,long energyPerUse,String headPrefix,java.util.List<String> rows){}
 public static final java.util.List<Definition> ALL=java.util.List.of(new Definition("electric_drill","Drill",100000,100,"toolHeadDrill",java.util.List.of("fSY", "TXW", "dVZ")),
new Definition("electric_chainsaw","Chainsaw",100000,100,"toolHeadChainsaw",java.util.List.of("dAT", "XWX", "XVX")),
new Definition("electric_wrench","Wrench",50000,50,"toolHeadWrench",java.util.List.of("dAT", "XWX", "XVX")),
new Definition("electric_screwdriver","Screwdriver",50000,50,"toolHeadScrewdriver",java.util.List.of("XdA", "TWY", "VYX")));
 public static Definition get(String id){return ALL.stream().filter(d->d.id().equals(id)).findFirst().orElseThrow();}
 public static boolean validMaterial(GTMaterial material){return material!=null&&material.isValid()&&material.resolve()==material&&material.getToolTypes()>=3&&material.getToolQuality()>=1&&!material.has(MaterialProperty.WOOD)&&!material.has(MaterialProperty.ANTIMATTER);}
}
