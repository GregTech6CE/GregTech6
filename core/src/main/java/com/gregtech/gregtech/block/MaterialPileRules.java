package com.gregtech.gregtech.block;
/** Original single-material pile capacity/height and ordered despawn-placement offsets. */
public final class MaterialPileRules {private MaterialPileRules(){}public static final int MAX_SIZE=64;public static final int[][] EXPIRE_OFFSETS={{0,0,0},{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,-1,-1},{0,-1,1},{-1,-1,0},{1,-1,0},{0,1,-1},{0,1,1},{-1,1,0},{1,1,0},{-1,0,-1},{1,0,1},{1,0,-1},{-1,0,1},{-1,-1,-1},{1,-1,1},{1,-1,-1},{-1,-1,1},{-1,1,-1},{1,1,1},{1,1,-1},{-1,1,1}};
 public static int add(int count,int offered){return Math.max(0,Math.min(MAX_SIZE-count,offered));}
 public static int take(int count,int amount){return Math.max(0,Math.min(count,amount));}
 public static int outlineHeight(boolean ingot,int count){int per=ingot?8:4,height=ingot?2:1;return Math.max(1,((count+per-1)/per)*height);}
 public static int collisionHeight(boolean ingot,int count){return count/(ingot?8:4)*(ingot?2:1);}
}
