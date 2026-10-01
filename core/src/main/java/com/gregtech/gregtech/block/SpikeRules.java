package com.gregtech.gregtech.block;
/** Exact source five-family hazard table; platform adapters classify entities once. */
public final class SpikeRules {private SpikeRules(){}public static final double WALK_FACTOR=.1;
 public static float damage(String family,boolean secondary,boolean omni,boolean iron,boolean skeleton,boolean slime,boolean arthropod,boolean undead,boolean enderWere){float amount=switch(family){case "STEEL"->iron?0:8;case "SHARP"->secondary?10:iron||skeleton||slime?0:5;case "SUPER"->secondary?50:15;case "METAL"->{if(secondary?arthropod:slime)yield 20;yield iron||skeleton||(secondary&&slime)?0:2;}case "FANCY"->{if(secondary?enderWere:undead)yield 20;yield iron||skeleton||slime?0:2;}default->throw new IllegalArgumentException(family);};return omni?amount*.5f:amount;}
}
