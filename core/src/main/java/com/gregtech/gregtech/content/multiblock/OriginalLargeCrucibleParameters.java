package com.gregtech.gregtech.content.multiblock;
import java.util.List;
/** Original eight GT6 vessels; each hull has 100 U mass and holds 432 U. */
public final class OriginalLargeCrucibleParameters {
 private OriginalLargeCrucibleParameters(){}
 public static final long HULL_UNITS=100L*com.gregtech.gregtech.api.material.GTValues.U;
 public static final long CAPACITY_UNITS=432L*com.gregtech.gregtech.api.material.GTValues.U;
 public record Definition(int originalId,String path,String material,int wallId,float hardness,boolean acidProof){}
 public static final List<Definition> DEFINITIONS=List.of(
            new Definition(17309, "large_steel_crucible", "Steel", 18009, 6, false),
            new Definition(17302, "large_stainless_steel_crucible", "StainlessSteel", 18002, 6, true),
            new Definition(17307, "large_invar_crucible", "Invar", 18007, 6, false),
            new Definition(17306, "large_titanium_crucible", "Titanium", 18006, 9, false),
            new Definition(17303, "large_tungstensteel_crucible", "Tungstensteel", 18003, 12.5f, false),
            new Definition(17304, "large_tungsten_crucible", "Tungsten", 18004, 10, true),
            new Definition(17312, "large_tantalum_hafnium_carbide_crucible", "TantalumHafniumCarbide", 18012, 12.5f, false),
            new Definition(17305, "large_adamantium_crucible", "Adamantium", 18005, 100, true));
}
