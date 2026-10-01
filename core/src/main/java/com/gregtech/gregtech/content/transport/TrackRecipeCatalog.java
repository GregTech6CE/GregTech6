package com.gregtech.gregtech.content.transport;
import java.util.*;
/** Source rail patterns and activator yields; ingredient values use item: or rail: identities. */
public final class TrackRecipeCatalog {private TrackRecipeCatalog(){}
public static final String PLAIN="RSR|RSR|RSR",BOOSTER="RSR|GDG|RSR",DETECTOR="RSR|RPR|RDR",ACTIVATOR="RSR|RTR|RSR";
public record Activator(String material,int count){} public static final List<Activator> ACTIVATORS=List.of(new Activator("Aluminium",1),new Activator("Magnalium",1),new Activator("Bronze",1),new Activator("Iron",2),new Activator("Steel",3),new Activator("HSLASteel",3),new Activator("StainlessSteel",4),new Activator("Titanium",6),new Activator("Tungsten",6),new Activator("Tungstensteel",12),new Activator("TungstenCarbide",12),new Activator("Adamantium",64));
public record Row(String id,String output,int count,String pattern,Map<String,String> ingredients){}
public static List<Row> rows(){List<Row> rows=new ArrayList<>();
 for(var m:TrackCatalog.MATERIALS){String slug=m.slug();var base=Map.of("R","rail:"+m.name(),"S","stick:WoodTreated");
  rows.add(new Row("tracks/track_"+slug,"gregtech:track_"+slug,4,PLAIN,base));
  rows.add(new Row("tracks/track_booster_"+slug,"gregtech:track_booster_"+slug,4,BOOSTER,Map.of("R","rail:"+m.name(),"S","stick:WoodTreated","G","rail:"+m.booster(),"D","item:minecraft:redstone")));
  rows.add(new Row("tracks/track_detector_"+slug,"gregtech:track_detector_"+slug,4,DETECTOR,Map.of("R","rail:"+m.name(),"S","stick:WoodTreated","P","item:minecraft:stone_pressure_plate","D","item:minecraft:redstone")));
 }
 rows.add(new Row("tracks/vanilla/rail","minecraft:rail",4,PLAIN,Map.of("R","rail:Iron","S","stick:WoodTreated")));
 rows.add(new Row("tracks/vanilla/golden_rail","minecraft:powered_rail",4,BOOSTER,Map.of("R","rail:Iron","S","stick:WoodTreated","G","rail:Gold","D","item:minecraft:redstone")));
 rows.add(new Row("tracks/vanilla/detector_rail","minecraft:detector_rail",4,DETECTOR,Map.of("R","rail:Iron","S","stick:WoodTreated","P","item:minecraft:stone_pressure_plate","D","item:minecraft:redstone")));
 for(var a:ACTIVATORS)rows.add(new Row("tracks/vanilla/activator_rail_"+a.material().toLowerCase(Locale.ROOT),"minecraft:activator_rail",a.count(),ACTIVATOR,Map.of("R","rail:"+a.material(),"S","stick:WoodTreated","T","item:minecraft:redstone_torch")));
 return List.copyOf(rows);
}
}
