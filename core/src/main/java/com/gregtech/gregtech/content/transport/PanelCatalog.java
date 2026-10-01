package com.gregtech.gregtech.content.transport;
import java.util.List;
/** Original six decorative panels and six one-pixel face bounds. */
public final class PanelCatalog {private PanelCatalog(){}public record Spec(String material,int tint,String mapColor,float hardness){public String id(){return "panel_"+material;}}
public static final List<Spec> ALL=List.of(new Spec("wood",12359778,"WOOD",2F),new Spec("concrete",8421504,"STONE",4F),new Spec("cfoam",13684944,"WOOL",1F),new Spec("asphalt",1710618,"COLOR_BLACK",3F),new Spec("colored_gray",8421504,"COLOR_GRAY",2F),new Spec("colored_black",1710618,"COLOR_BLACK",2F));
public static Spec get(String material){return ALL.stream().filter(s->s.material().equals(material)).findFirst().orElseThrow();}
public static double[] bounds(int face){return switch(face){case 0->new double[]{0,0,0,16,1,16};case 1->new double[]{0,15,0,16,16,16};case 2->new double[]{0,0,0,16,16,1};case 3->new double[]{0,0,15,16,16,16};case 4->new double[]{0,0,0,1,16,16};case 5->new double[]{15,0,0,16,16,16};default->throw new IllegalArgumentException("Invalid panel face");};}
}
