package com.gregtech.gregtech.content.transport;
import java.util.List;import java.util.Locale;
/** Original material rail identities, speeds, resistance and booster ingredients. */
public final class TrackCatalog {private TrackCatalog(){} public record Material(String name,float speed,float resistance,String booster){public String slug(){return name.toLowerCase(Locale.ROOT);}}
public static final List<Material> MATERIALS=List.of(new Material("Aluminium",0.20F,6F,"Silver"),new Material("Bronze",0.30F,8F,"Silver"),new Material("Magnalium",0.60F,12F,"Silver"),new Material("Steel",0.60F,12F,"Gold"),new Material("StainlessSteel",0.80F,10F,"Gold"),new Material("Tungsten",1.00F,20F,"Electrum"),new Material("Titanium",1.20F,16F,"Electrum"),new Material("Tungstensteel",1.40F,20F,"Platinum"),new Material("TungstenCarbide",1.60F,24F,"Platinum"),new Material("Adamantium",4.00F,100F,"Osmium"));
public static String materialName(String slug){for(var row:MATERIALS)if(row.slug().equals(slug))return row.name();return null;}
public static String boosterMaterial(String material){for(var row:MATERIALS)if(row.name().equals(material))return row.booster();return null;}
public static float speed(float materialSpeed,boolean straight,boolean loaded){return !straight?Math.min(materialSpeed,.4F):loaded?materialSpeed:Math.min(materialSpeed,1F);}
}
