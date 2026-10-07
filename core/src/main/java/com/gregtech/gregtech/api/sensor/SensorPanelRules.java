package com.gregtech.gregtech.api.sensor;
public final class SensorPanelRules {private SensorPanelRules(){}
 public static long tpsFromElapsed(long millis,int rate){return millis>0?rate*100000L/millis:2000L;}
 public static long transferMaximum(long speed,long power){return java.math.BigInteger.valueOf(speed).multiply(java.math.BigInteger.valueOf(power)).min(java.math.BigInteger.valueOf(Long.MAX_VALUE)).max(java.math.BigInteger.ZERO).longValue();}
 public static int[] bounds(int facing){if(facing<0||facing>5)throw new IllegalArgumentException("Sensor facing");int[] b={0,0,0,16,16,16};int axis=facing<2?1:facing<4?2:0;if((facing&1)==0)b[axis]=14;else b[axis+3]=2;return b;}
 public static String unit(String kind){
        return switch (kind) {
            case "FLUID" -> "liter"; case "ENERGY" -> "eu"; case "PROGRESS" -> "scale";
            case "THERMOMETER" -> "kelvin"; case "TACHOMETER" -> "ru"; case "WEIGHTOMETRIC" -> "ton";
            case "WEIGHTOMETRIC_LIGHT" -> "gramm"; case "WEIGHTOMETRIC_MEDIUM" -> "kilogramm";
            case "WEIGHTOMETRIC_SUPER_HEAVY" -> "kiloton"; case "TPS" -> "clock";
            case "BUCKETOMETER" -> "cubicmeter"; case "KILOBUCKETOMETER" -> "cubicdecameter";
            case "GIBBLOMETER","KILOGIBBLOMETER" -> "gibbl"; case "LUMINOMETER" -> "lumin"; case "PLAYERCOUNTER" -> "greg";
            case "CHRONOMETER" -> "clock"; case "GEIGER" -> "neutron"; case "LASEROMETER" -> "lu";
            default -> null;
        };
}
 public static int color(String kind){
        return switch (kind) {
            case "FLUID","BUCKETOMETER","KILOBUCKETOMETER" -> 0x0000FF;
            case "ENERGY","THERMOMETER","TPS" -> 0xFF0000;
            case "TACHOMETER","CHRONOMETER","GEIGER" -> 0x00FF00;
            case "GIBBLOMETER","KILOGIBBLOMETER","LASEROMETER" -> 0xFFFF00;
            case "PLAYERCOUNTER","PROGRESS" -> 0x80C0FF;
            case "LUMINOMETER" -> 0xFFFF80;
            case "WEIGHTOMETRIC","WEIGHTOMETRIC_LIGHT","WEIGHTOMETRIC_MEDIUM","WEIGHTOMETRIC_SUPER_HEAVY" -> 0xC0C0C0;
            default -> 0xFFFFFF;
        };
}
 public static boolean validInputSide(int display, int input) { return display >= 0 && display < 6 && input >= 0 && input < 6 && display != input; }
}
