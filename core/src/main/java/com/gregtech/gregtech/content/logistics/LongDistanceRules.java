package com.gregtech.gregtech.content.logistics;
public final class LongDistanceRules {private LongDistanceRules(){}public static final int MAX_SCAN=4096;
 public static long loss(long distance){return Math.max(64,distance/8);}
 public static boolean unique(int sources,boolean includesSource,int receivers){return sources==1&&includesSource&&receivers==1;}
 public static boolean rescan(long now,long scanned,boolean valid){return scanned!=now&&(!valid||now-scanned>=20);}
}
