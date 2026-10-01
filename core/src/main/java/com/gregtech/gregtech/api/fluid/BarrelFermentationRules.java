package com.gregtech.gregtech.api.fluid;
/** Original barrel time and output scaling. Recipe/FluidStack selection is a platform concern. */
public final class BarrelFermentationRules {
    private BarrelFermentationRules() {}
    public static long duration(long power,long ticks,long amount,long recipeInput){
        long work=Math.max(1,Math.abs(power*ticks)),input=Math.max(1,recipeInput);
        return (work*Math.max(1,amount)+input-1)/input;
    }
    public static long output(long recipeOutput,long amount,long recipeInput){return recipeOutput*amount/Math.max(1,recipeInput);}
}
