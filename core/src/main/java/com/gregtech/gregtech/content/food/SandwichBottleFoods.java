/* Derived from GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original authors and license retained; see NOTICE and source provenance. */
package com.gregtech.gregtech.content.food;
import java.util.List;
/** GregTech-6 Team, LGPL-3.0-or-later: native sandwich bottles' original FoodStatDrink values. */
public final class SandwichBottleFoods {
    private SandwichBottleFoods() {}
    public record Food(String item, int food, float saturation, int alcohol, int caffeine,
            int dehydration, int sugar, int fat, int radiation, boolean milk, boolean extinguish, boolean explosive) {}
    public static final List<Food> ROWS = List.of(
        new Food("grape_vinegar", 2, 0.2f, 30, 0, 0, 20, 0, 0, false, false, false), // Loader_Fluids.java:559; MultiItemBottles.java:58
        new Food("cave_johnsons_grenade_juice", 0, 0.0f, 0, 0, 0, 0, 0, 0, false, false, true), // Loader_Fluids.java:614; MultiItemBottles.java:75
        new Food("chili_sauce", 2, 0.1f, 0, 0, 10, 10, 0, 0, false, false, false), // Loader_Fluids.java:565; MultiItemBottles.java:106
        new Food("hot_sauce", 2, 0.1f, 0, 0, 20, 10, 0, 0, false, false, false), // Loader_Fluids.java:566; MultiItemBottles.java:107
        new Food("diabolo_sauce", 2, 0.1f, 0, 0, 30, 10, 0, 0, false, false, false), // Loader_Fluids.java:567; MultiItemBottles.java:108
        new Food("diablo_sauce", 2, 0.1f, 0, 0, 40, 10, 0, 0, false, false, false), // Loader_Fluids.java:568; MultiItemBottles.java:109
        new Food("there_is_no_cow_sauce", 2, 0.1f, 0, 0, 99, 10, 0, 0, false, false, false), // Loader_Fluids.java:569; MultiItemBottles.java:110
        new Food("barbecue_sauce", 4, 0.3f, 0, 0, 0, 30, 0, 0, false, false, false), // Loader_Fluids.java:595; MultiItemBottles.java:111
        new Food("apple_cider_vinegar", 2, 0.2f, 30, 0, 0, 15, 0, 0, false, false, false), // Loader_Fluids.java:560; MultiItemBottles.java:116
        new Food("olive_oil", 2, 0.2f, 0, 0, 20, 0, 20, 0, false, false, false), // Loader_Fluids.java:582; MultiItemBottles.java:120
        new Food("sunflower_oil", 2, 0.2f, 0, 0, 20, 0, 20, 0, false, false, false), // Loader_Fluids.java:580; MultiItemBottles.java:121
        new Food("nut_oil", 2, 0.2f, 0, 0, 20, 0, 20, 0, false, false, false), // Loader_Fluids.java:581; MultiItemBottles.java:122
        new Food("seed_oil", 2, 0.2f, 0, 0, 20, 0, 20, 0, false, false, false), // Loader_Fluids.java:583; MultiItemBottles.java:123
        new Food("hemp_oil", 2, 0.2f, 0, 0, 20, 0, 20, 0, false, false, false), // Loader_Fluids.java:585; MultiItemBottles.java:124
        new Food("lin_oil", 2, 0.2f, 0, 0, 20, 0, 20, 0, false, false, false), // Loader_Fluids.java:584; MultiItemBottles.java:125
        new Food("fish_oil", 2, 0.2f, 0, 0, 20, 0, 20, 0, false, false, false), // Loader_Fluids.java:586; MultiItemBottles.java:126
        new Food("whale_oil", 2, 0.2f, 0, 0, 20, 0, 20, 0, false, false, false), // Loader_Fluids.java:587; MultiItemBottles.java:127
        new Food("mayo", 3, 0.5f, 0, 0, 10, 0, 20, 0, false, false, false), // Loader_Fluids.java:590; MultiItemBottles.java:134
        new Food("dressing", 1, 0.5f, 10, 0, 0, 0, 20, 0, false, false, false), // Loader_Fluids.java:591; MultiItemBottles.java:135
        new Food("heavy_cream", 2, 0.4f, 0, 0, 0, 20, 20, 0, false, false, false), // Loader_Fluids.java:592; MultiItemBottles.java:139
        new Food("honey", 1, 0.1f, 0, 0, 0, 40, 0, 0, true, false, false), // Loader_Fluids.java:574; MultiItemBottles.java:157
        new Food("royal_jelly", 2, 0.2f, 0, 0, 0, 40, 0, 0, true, false, false), // Loader_Fluids.java:577; MultiItemBottles.java:159
        new Food("ambrosia", 2, 0.2f, 30, 0, 0, 40, 0, 0, true, false, false), // Loader_Fluids.java:576; MultiItemBottles.java:160
        new Food("rice_vinegar", 2, 0.2f, 30, 0, 0, 10, 0, 0, false, false, false), // Loader_Fluids.java:562; MultiItemBottles.java:192
        new Food("chocolate_cream", 4, 0.2f, 0, 0, 0, 30, 20, 0, false, false, false), // Loader_Fluids.java:596; MultiItemBottles.java:194
        new Food("nutella", 8, 0.4f, 0, 0, 0, 20, 30, 0, false, false, false), // Loader_Fluids.java:597; MultiItemBottles.java:195
        new Food("green_slime_bottle", 2, 0.5f, 0, 0, 0, 0, 0, 0, false, false, false), // Loader_Fluids.java:633; MultiItemBottles.java:204
        new Food("pink_slime_bottle", 4, 0.5f, 0, 0, 0, 20, 0, 0, false, false, false), // Loader_Fluids.java:632; MultiItemBottles.java:206
        new Food("blue_slime_bottle", 2, 0.5f, 0, 0, 0, 0, 0, 0, false, false, false), // Loader_Fluids.java:631; MultiItemBottles.java:208
        new Food("tomato_ketchup", 4, 0.3f, 0, 0, 0, 30, 0, 0, false, false, false), // Loader_Fluids.java:594; MultiItemBottles.java:257
        new Food("maple_syrup", 4, 0.2f, 0, 0, 0, 40, 10, 0, false, false, false), // Loader_Fluids.java:599; MultiItemBottles.java:266
        new Food("peanut_butter", 8, 0.4f, 0, 0, 0, 20, 30, 0, false, false, false), // Loader_Fluids.java:598; MultiItemBottles.java:273
        new Food("rainbow_sap", 5, 0.5f, 0, 0, 0, 20, 0, 0, true, false, false), // Loader_Fluids.java:464; MultiItemBottles.java:275
        new Food("coconut_cream", 2, 0.4f, 0, 0, 0, 15, 15, 0, false, false, false), // Loader_Fluids.java:593; MultiItemBottles.java:337
        new Food("bottle_of_poison", 0, 0.0f, 0, 0, 0, 0, 0, 0, false, false, false) // Loader_Fluids.java:629; MultiItemBottles.java:358
    );
    public static Food forItem(String item) {
        if (!item.startsWith("gregtech:")) return null;
        return ROWS.stream().filter(row -> row.item().equals(item.substring(9))).findFirst().orElse(null);
    }
}
