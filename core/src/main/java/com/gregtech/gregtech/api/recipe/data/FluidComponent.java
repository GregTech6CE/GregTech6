package com.gregtech.gregtech.api.recipe.data;

/** One fluid stack in millibuckets. An amount of 0 means any amount. */
public record FluidComponent(String fluid, long amount) {
    public FluidComponent {
        if (fluid == null || fluid.isBlank()) throw new IllegalArgumentException("fluid");
        if (amount < 0) throw new IllegalArgumentException("amount");
    }
}
