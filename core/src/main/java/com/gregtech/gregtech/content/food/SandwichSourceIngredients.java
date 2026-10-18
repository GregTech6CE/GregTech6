/* Derived from GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original authors and license retained; see NOTICE and source provenance. */
package com.gregtech.gregtech.content.food;

/** Original MultiItemFood/MultiItemBottles ingredient registrations, paired with existing port IDs. */
public final class SandwichSourceIngredients {
    private SandwichSourceIngredients() {}
    public record Entry(String item, int layer) {}
    public static final java.util.List<Entry> ROWS = java.util.List.of(
        new Entry("gt6ce:honey_comb", 22), // MultiItemFood.java:226
        new Entry("gt6ce:water_comb", 22), // MultiItemFood.java:227
        new Entry("gt6ce:jungle_comb", 22), // MultiItemFood.java:232
        new Entry("gt6ce:shroomy_comb", 22), // MultiItemFood.java:234
        new Entry("gt6ce:royal_comb", 22), // MultiItemFood.java:239
        new Entry("gt6ce:lemon_slice", 28), // MultiItemFood.java:274
        new Entry("gt6ce:tomato_slice", 36), // MultiItemFood.java:280
        new Entry("gt6ce:onion_slice", 31), // MultiItemFood.java:291
        new Entry("gt6ce:cucumber_slice", 23), // MultiItemFood.java:297
        new Entry("gt6ce:pickle_slice", 41), // MultiItemFood.java:299
        new Entry("gt6ce:chili_pepper", 20), // MultiItemFood.java:307
        new Entry("gt6ce:carrot_slice", 18), // MultiItemFood.java:331
        new Entry("gt6ce:banana_slice", 17), // MultiItemFood.java:385
        new Entry("gt6ce:ananas_slice", 16), // MultiItemFood.java:477
        new Entry("gt6ce:cheese_slice", 19), // MultiItemFood.java:491
        new Entry("gt6ce:fried_egg", 43), // MultiItemFood.java:499
        new Entry("gt6ce:scrambled_egg", 211), // MultiItemFood.java:500
        new Entry("gt6ce:sliced_egg", 44), // MultiItemFood.java:501
        new Entry("gt6ce:egg_yolk", 211), // MultiItemFood.java:502
        new Entry("gt6ce:egg_white", 215), // MultiItemFood.java:503
        new Entry("gt6ce:raw_ham_slice", 26), // MultiItemFood.java:534
        new Entry("gt6ce:cooked_ham_slice", 27), // MultiItemFood.java:535
        new Entry("gt6ce:raw_bacon", 37), // MultiItemFood.java:541
        new Entry("gt6ce:grilled_bacon", 38), // MultiItemFood.java:542
        new Entry("gt6ce:raw_ribs", 29), // MultiItemFood.java:546
        new Entry("gt6ce:grilled_ribs", 30), // MultiItemFood.java:547
        new Entry("gt6ce:barbecue_ribs", 30), // MultiItemFood.java:548
        new Entry("gt6ce:raw_rib_eye_steak", 32), // MultiItemFood.java:553
        new Entry("gt6ce:grilled_rib_eye_steak", 33), // MultiItemFood.java:554
        new Entry("gt6ce:dogmeat", 29), // MultiItemFood.java:558
        new Entry("gt6ce:grilled_dogmeat", 30), // MultiItemFood.java:559
        new Entry("gt6ce:mutton", 29), // MultiItemFood.java:563
        new Entry("gt6ce:grilled_mutton", 30), // MultiItemFood.java:564
        new Entry("gt6ce:horse_meat", 29), // MultiItemFood.java:568
        new Entry("gt6ce:grilled_horse_meat", 30), // MultiItemFood.java:569
        new Entry("gt6ce:mule_meat", 29), // MultiItemFood.java:572
        new Entry("gt6ce:grilled_mule_meat", 30), // MultiItemFood.java:573
        new Entry("gt6ce:donkey_meat", 29), // MultiItemFood.java:576
        new Entry("gt6ce:grilled_donkey_meat", 30), // MultiItemFood.java:577
        new Entry("gt6ce:scrap_meat", 29), // MultiItemFood.java:581
        new Entry("gt6ce:chum", 39), // MultiItemFood.java:594
        new Entry("gt6ce:toast", 254), // MultiItemFood.java:776
        new Entry("gt6ce:toasted_toast", 253), // MultiItemFood.java:777
        new Entry("gt6ce:empty_wax_pill", 42), // MultiItemFood.java:911
        new Entry("gt6ce:radaway", 42), // MultiItemFood.java:912
        new Entry("gt6ce:peppermint", 42), // MultiItemFood.java:913
        new Entry("gt6ce:blue_pill", 42), // MultiItemFood.java:914
        new Entry("gt6ce:red_pill", 42), // MultiItemFood.java:915
        new Entry("gt6ce:antidote", 42), // MultiItemFood.java:916
        new Entry("gt6ce:cure_all", 42), // MultiItemFood.java:921
        new Entry("gt6ce:tofu_bar", 35), // MultiItemFood.java:925
        new Entry("gt6ce:emerald_green_bar", 34), // MultiItemFood.java:926
        new Entry("gt6ce:raw_meat_bar", 29), // MultiItemFood.java:927
        new Entry("gt6ce:cooked_meat_bar", 30), // MultiItemFood.java:928
        new Entry("gt6ce:chocolate_bar", 21), // MultiItemFood.java:929
        new Entry("gt6ce:raw_fish_bar", 24), // MultiItemFood.java:931
        new Entry("gt6ce:cooked_fish_bar", 25), // MultiItemFood.java:932
        new Entry("gt6ce:butter", 11), // MultiItemFood.java:933
        new Entry("gt6ce:salted_butter", 11), // MultiItemFood.java:934
        new Entry("gt6ce:grape_vinegar", 10), // MultiItemBottles.java:58
        new Entry("gt6ce:cave_johnsons_grenade_juice", 11), // MultiItemBottles.java:75
        new Entry("gt6ce:chili_sauce", 1), // MultiItemBottles.java:106
        new Entry("gt6ce:hot_sauce", 1), // MultiItemBottles.java:107
        new Entry("gt6ce:diabolo_sauce", 1), // MultiItemBottles.java:108
        new Entry("gt6ce:diablo_sauce", 1), // MultiItemBottles.java:109
        new Entry("gt6ce:there_is_no_cow_sauce", 1), // MultiItemBottles.java:110
        new Entry("gt6ce:barbecue_sauce", 3), // MultiItemBottles.java:111
        new Entry("gt6ce:apple_cider_vinegar", 10), // MultiItemBottles.java:116
        new Entry("gt6ce:olive_oil", 10), // MultiItemBottles.java:120
        new Entry("gt6ce:sunflower_oil", 11), // MultiItemBottles.java:121
        new Entry("gt6ce:nut_oil", 11), // MultiItemBottles.java:122
        new Entry("gt6ce:seed_oil", 10), // MultiItemBottles.java:123
        new Entry("gt6ce:hemp_oil", 10), // MultiItemBottles.java:124
        new Entry("gt6ce:lin_oil", 10), // MultiItemBottles.java:125
        new Entry("gt6ce:fish_oil", 11), // MultiItemBottles.java:126
        new Entry("gt6ce:whale_oil", 3), // MultiItemBottles.java:127
        new Entry("gt6ce:mayo", 15), // MultiItemBottles.java:134
        new Entry("gt6ce:dressing", 10), // MultiItemBottles.java:135
        new Entry("gt6ce:heavy_cream", 15), // MultiItemBottles.java:139
        new Entry("gt6ce:honey", 11), // MultiItemBottles.java:157
        new Entry("gt6ce:royal_jelly", 11), // MultiItemBottles.java:159
        new Entry("gt6ce:ambrosia", 5), // MultiItemBottles.java:160
        new Entry("gt6ce:rice_vinegar", 10), // MultiItemBottles.java:192
        new Entry("gt6ce:chocolate_cream", 3), // MultiItemBottles.java:194
        new Entry("gt6ce:nutella", 3), // MultiItemBottles.java:195
        new Entry("gt6ce:green_slime_bottle", 10), // MultiItemBottles.java:204
        new Entry("gt6ce:pink_slime_bottle", 9), // MultiItemBottles.java:206
        new Entry("gt6ce:blue_slime_bottle", 4), // MultiItemBottles.java:208
        new Entry("gt6ce:tomato_ketchup", 1), // MultiItemBottles.java:257
        new Entry("gt6ce:maple_syrup", 3), // MultiItemBottles.java:266
        new Entry("gt6ce:peanut_butter", 3), // MultiItemBottles.java:273
        new Entry("gt6ce:rainbow_sap", 40), // MultiItemBottles.java:275
        new Entry("gt6ce:coconut_cream", 15), // MultiItemBottles.java:337
        new Entry("gt6ce:bottle_of_poison", 5), // MultiItemBottles.java:358
        new Entry("gt6ce:bottle_oblood", 1) // MultiItemBottles.java:381
    );
}
