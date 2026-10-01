package com.gregtech.gregtech.content.data;
import com.gregtech.gregtech.data.MaterialPrefix;import java.util.List;
/** Source1 scanner/printer/replicator work and material-form preference. Units remain at the platform boundary. */
public final class MaterialDataRules {private MaterialDataRules(){}
 public static final long SCANNER_POWER=512,PRINTER_POWER=512,PRINTER_MANY_POWER=1024,PRINTER_TICKS=16,REPLICATOR_POWER_PER_NUCLEON=256;
 public static final int DYE_MB=72,DYE_MANY_MB=144,MANY_PAGES=50;
 public static final List<MaterialPrefix> FORMS=List.of(MaterialPrefix.gem,MaterialPrefix.plateGem,MaterialPrefix.ingot,MaterialPrefix.plate,MaterialPrefix.nugget,MaterialPrefix.chunkGt,MaterialPrefix.dust,MaterialPrefix.dustTiny,MaterialPrefix.dustSmall,MaterialPrefix.stick);
 public static long units(MaterialPrefix form){return form==MaterialPrefix.nugget||form==MaterialPrefix.dustTiny?9:form==MaterialPrefix.chunkGt||form==MaterialPrefix.dustSmall?4:form==MaterialPrefix.stick?2:1;}
}
