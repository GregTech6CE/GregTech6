package com.gregtech.gregtech.content.data;
import com.gregtech.gregtech.data.MaterialPrefix;import java.util.List;
/** Source1 scanner/printer/replicator work and material-form preference. Units remain at the platform boundary. */
public final class MaterialDataRules {private MaterialDataRules(){}
 public static final long SCANNER_POWER=512,PRINTER_POWER=VisualDocumentRules.POWER,PRINTER_TICKS=VisualDocumentRules.bookPrint(0).ticks(),PRINTER_MANY_TICKS=VisualDocumentRules.bookPrint(51).ticks();
 /** RecipeMapReplicator:91/110: duration per nucleon, at one QU per tick. */
 public static final long REPLICATOR_TICKS_PER_NUCLEON=256, REPLICATOR_POWER=1;
 /** UT.NBT:2261: original data tooltip figure (distinct from raw recipe work). */
 public static final long DATA_ENERGY_PER_NUCLEON=65536;
 public static final int ENVIRONMENT_KELVIN=293;
 /** Compatibility name from the earlier port; its value is duration, not packet power. */
 @Deprecated public static final long REPLICATOR_POWER_PER_NUCLEON=REPLICATOR_TICKS_PER_NUCLEON;
 public enum AmbientPhase { SOLID, LIQUID, GAS }
 public static AmbientPhase ambientPhase(com.gregtech.gregtech.api.material.GTMaterial material) {
     if(material.getMeltingPoint()>ENVIRONMENT_KELVIN)return AmbientPhase.SOLID;
     return material.getBoilingPoint()<=ENVIRONMENT_KELVIN?AmbientPhase.GAS:AmbientPhase.LIQUID;
 }
 /** Both book sizes use 16 GU/t; the old name remains available to integrations. */
 public static final long PRINTER_MANY_POWER=PRINTER_POWER;
 public static final int DYE_MB=72,DYE_MANY_MB=144,MANY_PAGES=50;
 public static final List<MaterialPrefix> FORMS=List.of(MaterialPrefix.gem,MaterialPrefix.plateGem,MaterialPrefix.ingot,MaterialPrefix.plate,MaterialPrefix.nugget,MaterialPrefix.chunkGt,MaterialPrefix.dust,MaterialPrefix.dustTiny,MaterialPrefix.dustSmall,MaterialPrefix.stick);
 public static long units(MaterialPrefix form){return form==MaterialPrefix.nugget||form==MaterialPrefix.dustTiny?9:form==MaterialPrefix.chunkGt||form==MaterialPrefix.dustSmall?4:form==MaterialPrefix.stick?2:1;}
}
