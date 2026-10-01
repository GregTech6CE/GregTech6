package com.gregtech.gregtech.content.multiblock;
/** Original full tower shape; live byte range clamp agrees with brokestar and masson. */
public final class VonDaGraaggRules {private VonDaGraaggRules(){}public static final long CAPACITY=4096;public record Cell(int x,int y,int z,int partId,boolean energyInput){}public static java.util.List<Cell> layout(){var out=new java.util.ArrayList<Cell>();
        // GT6 MultiTileEntityVonDaGraagg.checkStructure2: omit only the four 5x5 corners.
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            if (Math.abs(x * z) >= 4) continue;
            for (int y = 0; y <= 1; y++) {
                if (x == 0 && y == 0 && z == 0) continue; // the controller
                out.add(new Cell(x,y,z,18028,true));
            }
        }
        for (int y = 2; y <= 6; y++)
            out.add(new Cell(0,y,0,18040,false));
        out.add(new Cell(0,7,0,18029,false));
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && z == 0) continue;
            out.add(new Cell(x,6,z,18029,false));
            if (x * z == 0) {
                out.add(new Cell(x,5,z,18029,false));out.add(new Cell(x,7,z,18029,false));
            }
        }
return java.util.List.copyOf(out);}
public static int range(long energy,boolean formed){return formed?(int)Math.min(255,Math.max(0,Math.min(energy,CAPACITY))/16):0;}
public static long afterDrain(long energy){return Math.max(0,energy-CAPACITY);}
public static boolean within(int dx,int dz,int range){return range>0&&Math.abs((long)dx)<=range&&Math.abs((long)dz)<=range;}
}
