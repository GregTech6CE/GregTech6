package com.gregtech.gregtech.content.energy;
/** Opposing face pairs encoded down/up/north/south/west/east; at most a straight pair. */
public final class AxleConnectionRules {private AxleConnectionRules(){}public static boolean valid(int mask){return Integer.bitCount(mask)<=1||mask==3||mask==12||mask==48;}}
