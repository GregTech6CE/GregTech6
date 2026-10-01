package com.gregtech.gregtech.content.energy;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.content.material.Materials;
import java.util.List;
/** Original GT6 redstone families, distinct from electric packet wires. */
public final class SignalWireCatalog {
 private SignalWireCatalog(){}
 public record Family(String name,GTMaterial material,int range,boolean luminous){}
 public static List<Family> families(){return List.of(new Family("redalloy",Materials.RedAlloy,16,false),new Family("signalum",Materials.Signalum,64,false),new Family("lumium",Materials.Lumium,16,true));}
}
