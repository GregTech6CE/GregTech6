package com.gregtech.gregtech.api.recipe;
import java.util.function.IntUnaryOperator;
/** Original per-output independent chance roll, including each parallel operation. */
public final class RecipeChanceRules {
    private RecipeChanceRules() {}
    public static long rollOutputCount(int outputCount,int chance,int processes,IntUnaryOperator random) {
        if(outputCount<=0||processes<=0)return 0;
        long attempts=(long)outputCount*processes;
        if(chance>=10000)return attempts;
        if(chance<=0)return 0;
        long count=0;
        for(long i=0;i<attempts;i++)if(random.applyAsInt(10000)<chance)count++;
        return count;
    }
}
