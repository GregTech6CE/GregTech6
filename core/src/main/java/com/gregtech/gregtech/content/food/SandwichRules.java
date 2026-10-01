package com.gregtech.gregtech.content.food;
/** Original GT6 layered sandwich limits, container quantities and food timing. */
public final class SandwichRules {private SandwichRules(){}public static final int SLOTS=16;public static int containerQuantity(int base,boolean bottle){return bottle?(base+3)/4:base;}public static int useDuration(int food){return Math.max(32,food*8);}public static float saturation(float maximum){return maximum+.5f;}}
