package com.gregtech.gregtech.api.fluid;
import com.gregtech.gregtech.api.material.GTMaterial;
/** Original liquid/dye multipliers without native texture classes. */
public final class FluidTintRules {
 private FluidTintRules(){}
public static int moltenTint(GTMaterial material){
        return switch(material.resolve().getName()){
            case "Iron" -> 0xFFFF4020;
            case "WroughtIron" -> 0xFFFF5028;
            case "Steel" -> 0xFFFF140A;
            case "HSLASteel" -> 0xFFB4501E;
            case "Iridium" -> 0xFFFF80C8;
            case "Naquadah" -> 0xFF00FF00;
            case "NaquadahEnriched" -> 0xFF40FF40;
            case "Naquadria" -> 0xFF80FF80;
            case "Stone" -> 0xFFC06040;
            case "Bedrock" -> 0xFF806040;
            default -> material.getColor()|0xFF000000;
        };
    }
public static int dyeTint(String name){
        return switch(name.replace("_","")){
            case "black"->0xFF202020;case "red"->0xFFFF0000;case "green"->0xFF00FF00;case "brown"->0xFF604000;
            case "blue"->0xFF0000FF;case "purple"->0xFF800080;case "cyan"->0xFF00FFFF;case "lightgray"->0xFFC0C0C0;
            case "gray"->0xFF808080;case "pink"->0xFFFFC0C0;case "lime"->0xFF80FF80;case "yellow"->0xFFFFFF00;
            case "lightblue"->0xFF8080FF;case "magenta"->0xFFFF00FF;case "orange"->0xFFFF8000;default->0xFFFFFFFF;
        };
    }
}
