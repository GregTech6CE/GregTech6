package com.gregtech.gregtech.block;
/** Exact4x4/16-per-face coin grid, shared re-packing and16x16 mint relief math. */
public final class CoinPileRules {private CoinPileRules(){}public static final int FACES=16,PER_FACE=16;
 public static int faceAt(double x,double z){return (int)(Math.min(.99,Math.max(0,x))*4)*4+(int)(Math.min(.99,Math.max(0,z))*4);}
 public static int pixelDepth(short[][] rows,int x,int z){return ((rows[0][x]>>>z)&1)+2*((rows[1][x]>>>z)&1);}
 public static void repack(byte[] faces,int total,java.util.function.IntUnaryOperator random){java.util.Arrays.fill(faces,(byte)0);while(total>0){int face=random.applyAsInt(FACES);if(faces[face]<PER_FACE){faces[face]++;total--;}}}
 public record Box(double x0,double y0,double z0,double x1,double y1,double z1){}
 public static Box cell(int face,int count){double x=(face/4)/4.,z=(face%4)/4.;return new Box(x,0,z,x+.25,count/16.,z+.25);}
}
