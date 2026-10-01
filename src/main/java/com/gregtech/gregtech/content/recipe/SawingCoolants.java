package com.gregtech.gregtech.content.recipe;

import java.util.List;

/** GT6 RM.sawing: multiplier applies to both time and coolant amount. */
public final class SawingCoolants {
    public record Variant(String field, int multiplier) {}
    private static final List<Variant> FOOD = List.of(new Variant("Water",4),new Variant("SpDew",4),
            new Variant("MnWtr",4),new Variant("DistW",3));
    private static final List<Variant> INDUSTRIAL = java.util.stream.Stream.concat(FOOD.stream(),
            java.util.stream.Stream.of(new Variant("Lubricant",1),new Variant("LubRoCant",1))).toList();
    private SawingCoolants() {}
    public static List<Variant> variants(boolean food) { return food ? FOOD : INDUSTRIAL; }
}
