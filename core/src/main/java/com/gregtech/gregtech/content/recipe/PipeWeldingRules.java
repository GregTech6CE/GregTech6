package com.gregtech.gregtech.content.recipe;
import com.gregtech.gregtech.api.machine.PipeSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.MaterialWorkability;
/** Original five GT6 curved-plate welding rows and exact work costs, shared by both loaders. */
public final class PipeWeldingRules {
 private PipeWeldingRules(){}
 public record Row(int curved,int circuit,PipeSpec.PipeSize size){}
 public static final java.util.List<Row> ROWS=java.util.List.of(new Row(1,1,PipeSpec.PipeSize.TINY),new Row(1,2,PipeSpec.PipeSize.SMALL),new Row(3,3,PipeSpec.PipeSize.MEDIUM),new Row(6,4,PipeSpec.PipeSize.LARGE),new Row(12,5,PipeSpec.PipeSize.HUGE));
 public static long ticks(GTMaterial material,Row row){long units=MaterialPrefix.plateCurved.getMaterialWeight()*row.curved();return MaterialWorkability.isFurnace(material)?16L*row.curved():Math.max(1,(units*64L*(material.getToolQuality()+1)+GTValues.U-1)/GTValues.U);}
}
