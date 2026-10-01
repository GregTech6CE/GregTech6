package com.gregtech.gregtech.content.tool;
/** The complete two-face 16-row GT6 die, including a deliberately blank custom pattern. */
public final class CoinStampPattern {public static final int FACES=2,ROWS=16;public static final String UNIQUE="gt.coin.unique";private boolean unique;private final short[][] shape=new short[FACES][ROWS];
 public boolean unique(){return unique;}public void unique(boolean value){unique=value;}
 public static String key(int face,int row){return "gt.coin.shape."+face+"."+row;}
 public void read(java.util.function.ToIntFunction<String> value){for(int face=0;face<FACES;face++)for(int row=0;row<ROWS;row++)shape[face][row]=(short)value.applyAsInt(key(face,row));}
 public void write(java.util.function.BiConsumer<String,Short> value){for(int face=0;face<FACES;face++)for(int row=0;row<ROWS;row++)value.accept(key(face,row),shape[face][row]);}
}
