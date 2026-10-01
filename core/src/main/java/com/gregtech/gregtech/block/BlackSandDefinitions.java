package com.gregtech.gregtech.block;
import java.util.Map;import java.util.List;
/** Exact three original falling black-sand identities/materials/dust colors. */
public final class BlackSandDefinitions {
 private BlackSandDefinitions(){}public record Spec(String material,int dustColor){}
 public static final List<String> IDS=List.of("sand_magnetite","sand_basalt_magnetite","sand_granite_magnetite");
 public static final Map<String,Spec> SPECS=Map.of("sand_magnetite",new Spec("Magnetite",0x1E1E1E),"sand_basalt_magnetite",new Spec("BasalticMineralSand",0x283228),"sand_granite_magnetite",new Spec("GraniticMineralSand",0x283C3C));
}
