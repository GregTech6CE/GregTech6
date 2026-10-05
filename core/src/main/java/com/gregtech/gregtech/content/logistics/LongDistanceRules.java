package com.gregtech.gregtech.content.logistics;
public final class LongDistanceRules {private LongDistanceRules(){}
 /** Kept for addon source compatibility; native source scans no longer impose this cap. */
 @Deprecated public static final int MAX_SCAN=4096;
 public static long loss(long distance){return Math.max(64,distance/8);}
 @Deprecated public static boolean unique(int sources,boolean includesSource,int receivers){return sources==1&&includesSource&&receivers==1;}
 public static boolean rescan(long now,long scanned,boolean valid){return scanned!=now&&(!valid||now-scanned>=20);}
}
