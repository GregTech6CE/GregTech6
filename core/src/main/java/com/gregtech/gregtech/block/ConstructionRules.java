package com.gregtech.gregtech.block;
/** Source construction palette, reinforcement material aliases and foam timing. */
public final class ConstructionRules {
 private ConstructionRules(){}public static final double ASPHALT_SPEED=1.3;public static final int FOAM_GRACE=100,FOAM_DRY_CHANCE=5900;
 public static final java.util.Set<String> IRON_FAMILY=java.util.Set.of("Iron","WroughtIron","IronCast","IronCompressed","PigIron","MeteoricIron","Meteorite","Enori","Steel","Knightmetal","MeteoricSteel");
 public static int tint(String dye){return switch(dye){case "black"->0x202020;case "red"->0xFF0000;case "green"->0x00FF00;case "brown"->0x604000;case "blue"->0x0000FF;case "purple"->0x800080;case "cyan"->0x00FFFF;case "light_gray"->0xC0C0C0;case "gray"->0x808080;case "pink"->0xFFC0C0;case "lime"->0x80FF80;case "yellow"->0xFFFF00;case "light_blue"->0x8080FF;case "magenta"->0xFF00FF;case "orange"->0xFF8000;default->0xFFFFFF;};}
 public static boolean dries(long age,java.util.function.IntUnaryOperator random){return age>=FOAM_GRACE&&random.applyAsInt(FOAM_DRY_CHANCE)==0;}
}
