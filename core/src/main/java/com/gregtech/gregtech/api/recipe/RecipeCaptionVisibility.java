package com.gregtech.gregtech.api.recipe;

/** NEI_RecipeMap.drawExtras: zero power, hidden voltage and unit-work info pages. */
public record RecipeCaptionVisibility(boolean usage,boolean tier,boolean power) {
    public static RecipeCaptionVisibility of(long recipePower,boolean combinePower,boolean showVoltageAmperage) {
        boolean nonzero=recipePower!=0;
        boolean unit=recipePower==1 || recipePower==-1;
        return new RecipeCaptionVisibility(nonzero && !combinePower && (showVoltageAmperage || !unit),
                showVoltageAmperage,nonzero && showVoltageAmperage);
    }
}
