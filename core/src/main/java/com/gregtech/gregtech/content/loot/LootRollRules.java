package com.gregtech.gregtech.content.loot;
import java.util.function.IntUnaryOperator;
/** Original weighted row and inclusive stack/table-count sampling. */
public final class LootRollRules {private LootRollRules(){}public static int pick(int[] weights,IntUnaryOperator random){if(weights.length==0)return -1;int total=0;for(int w:weights)total+=w;int pick=random.applyAsInt(total);for(int i=0;i<weights.length;i++){pick-=weights[i];if(pick<0)return i;}return -1;}public static int count(int min,int max,IntUnaryOperator random){return min+(max>min?random.applyAsInt(max-min+1):0);}}
