package com.gregtech.gregtech.api.machine.crucible;
/** Original GT6 warning pass color transform, shared by baked and vessel rendering. */
public final class CrucibleHullTint{private CrucibleHullTint(){}public static int meltdownRgb(int rgb){int r=Math.min(255,((rgb>>16)&255)*2+50),g=Math.min(255,((rgb>>8)&255)*2+50),b=Math.min(255,(rgb&255)/2+50);return(r<<16)|(g<<8)|b;}}
