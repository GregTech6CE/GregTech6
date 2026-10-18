package com.gregtech.gregtech.recipe;

/** The uncropped grid position needed by original GT single-input form conversions. */
public interface CraftingFormPosition {
    void gregtech$setFormPosition(int width, int height, int left, int top);
    int gregtech$formGridSize();
    int gregtech$formFirstSlot();
}
