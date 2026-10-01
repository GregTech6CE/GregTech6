package com.gregtech.gregtech.api.machine;
/** Original mold/basin/crossing/faucet identities, meta offsets and hull amounts. */
public final class SmelteryCompanionDefinitions{
 private SmelteryCompanionDefinitions(){}
 public enum Kind{MOLD("mold_",50,CrucibleSpec.MOLD_HULL_UNITS),BASIN("mold_basin_",750,CrucibleSpec.BASIN_HULL_UNITS),CROSSING("crucible_crossing_",850,CrucibleSpec.CROSSING_HULL_UNITS),FAUCET("crucible_faucet_",700,CrucibleSpec.FAUCET_HULL_UNITS);final String prefix;final int offset;final long hull;Kind(String p,int o,long h){prefix=p;offset=o;hull=h;}}
 public static CrucibleSpec of(CrucibleSpec base,Kind kind){return copy(base,kind.prefix+base.id().substring("smelting_crucible_".length()),kind.offset,kind.hull);}
 public static CrucibleSpec copy(CrucibleSpec base,String id,int offset,long hull){return new CrucibleSpec(id,base.material(),base.gt6MetaId()+offset,base.meltingPointK(),base.boilingPointK(),base.hullDensity(),base.hardness(),base.blastResistance(),base.acidProof(),hull);}
 public static java.util.List<CrucibleSpec> all(Kind kind){return OriginalCrucibleDefinitions.all().stream().map(base->of(base,kind)).toList();}
}
