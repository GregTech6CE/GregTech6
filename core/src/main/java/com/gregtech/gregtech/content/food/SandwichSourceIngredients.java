/* Derived from GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original authors and license retained; see NOTICE and source provenance. */
package com.gregtech.gregtech.content.food;

/** Original MultiItemFood/MultiItemBottles ingredient registrations, paired with existing port IDs. */
public final class SandwichSourceIngredients {
    private SandwichSourceIngredients() {}
    public record Entry(String item, int layer) {}
    public static final java.util.List<Entry> ROWS = java.util.List.of(
        new Entry("gregtech:honey_comb", 22), // MultiItemFood.java:226
        new Entry("gregtech:water_comb", 22), // MultiItemFood.java:227
        new Entry("gregtech:jungle_comb", 22), // MultiItemFood.java:232
        new Entry("gregtech:shroomy_comb", 22), // MultiItemFood.java:234
        new Entry("gregtech:royal_comb", 22), // MultiItemFood.java:239
        new Entry("gregtech:lemon_slice", 28), // MultiItemFood.java:274
        new Entry("gregtech:tomato_slice", 36), // MultiItemFood.java:280
        new Entry("gregtech:onion_slice", 31), // MultiItemFood.java:291
        new Entry("gregtech:cucumber_slice", 23), // MultiItemFood.java:297
        new Entry("gregtech:pickle_slice", 41), // MultiItemFood.java:299
        new Entry("gregtech:chili_pepper", 20), // MultiItemFood.java:307
        new Entry("gregtech:carrot_slice", 18), // MultiItemFood.java:331
        new Entry("gregtech:banana_slice", 17), // MultiItemFood.java:385
        new Entry("gregtech:ananas_slice", 16), // MultiItemFood.java:477
        new Entry("gregtech:cheese_slice", 19), // MultiItemFood.java:491
        new Entry("gregtech:fried_egg", 43), // MultiItemFood.java:499
        new Entry("gregtech:scrambled_egg", 211), // MultiItemFood.java:500
        new Entry("gregtech:sliced_egg", 44), // MultiItemFood.java:501
        new Entry("gregtech:egg_yolk", 211), // MultiItemFood.java:502
        new Entry("gregtech:egg_white", 215), // MultiItemFood.java:503
        new Entry("gregtech:raw_ham_slice", 26), // MultiItemFood.java:534
        new Entry("gregtech:cooked_ham_slice", 27), // MultiItemFood.java:535
        new Entry("gregtech:raw_bacon", 37), // MultiItemFood.java:541
        new Entry("gregtech:grilled_bacon", 38), // MultiItemFood.java:542
        new Entry("gregtech:raw_ribs", 29), // MultiItemFood.java:546
        new Entry("gregtech:grilled_ribs", 30), // MultiItemFood.java:547
        new Entry("gregtech:barbecue_ribs", 30), // MultiItemFood.java:548
        new Entry("gregtech:raw_rib_eye_steak", 32), // MultiItemFood.java:553
        new Entry("gregtech:grilled_rib_eye_steak", 33), // MultiItemFood.java:554
        new Entry("gregtech:dogmeat", 29), // MultiItemFood.java:558
        new Entry("gregtech:grilled_dogmeat", 30), // MultiItemFood.java:559
        new Entry("gregtech:mutton", 29), // MultiItemFood.java:563
        new Entry("gregtech:grilled_mutton", 30), // MultiItemFood.java:564
        new Entry("gregtech:horse_meat", 29), // MultiItemFood.java:568
        new Entry("gregtech:grilled_horse_meat", 30), // MultiItemFood.java:569
        new Entry("gregtech:mule_meat", 29), // MultiItemFood.java:572
        new Entry("gregtech:grilled_mule_meat", 30), // MultiItemFood.java:573
        new Entry("gregtech:donkey_meat", 29), // MultiItemFood.java:576
        new Entry("gregtech:grilled_donkey_meat", 30), // MultiItemFood.java:577
        new Entry("gregtech:scrap_meat", 29), // MultiItemFood.java:581
        new Entry("gregtech:chum", 39), // MultiItemFood.java:594
        new Entry("gregtech:toast", 254), // MultiItemFood.java:776
        new Entry("gregtech:toasted_toast", 253), // MultiItemFood.java:777
        new Entry("gregtech:empty_wax_pill", 42), // MultiItemFood.java:911
        new Entry("gregtech:radaway", 42), // MultiItemFood.java:912
        new Entry("gregtech:peppermint", 42), // MultiItemFood.java:913
        new Entry("gregtech:blue_pill", 42), // MultiItemFood.java:914
        new Entry("gregtech:red_pill", 42), // MultiItemFood.java:915
        new Entry("gregtech:antidote", 42), // MultiItemFood.java:916
        new Entry("gregtech:cure_all", 42), // MultiItemFood.java:921
        new Entry("gregtech:tofu_bar", 35), // MultiItemFood.java:925
        new Entry("gregtech:emerald_green_bar", 34), // MultiItemFood.java:926
        new Entry("gregtech:raw_meat_bar", 29), // MultiItemFood.java:927
        new Entry("gregtech:cooked_meat_bar", 30), // MultiItemFood.java:928
        new Entry("gregtech:chocolate_bar", 21), // MultiItemFood.java:929
        new Entry("gregtech:raw_fish_bar", 24), // MultiItemFood.java:931
        new Entry("gregtech:cooked_fish_bar", 25), // MultiItemFood.java:932
        new Entry("gregtech:butter", 11), // MultiItemFood.java:933
        new Entry("gregtech:salted_butter", 11), // MultiItemFood.java:934
        new Entry("gregtech:grape_vinegar", 10), // MultiItemBottles.java:58
        new Entry("gregtech:cave_johnsons_grenade_juice", 11), // MultiItemBottles.java:75
        new Entry("gregtech:chili_sauce", 1), // MultiItemBottles.java:106
        new Entry("gregtech:hot_sauce", 1), // MultiItemBottles.java:107
        new Entry("gregtech:diabolo_sauce", 1), // MultiItemBottles.java:108
        new Entry("gregtech:diablo_sauce", 1), // MultiItemBottles.java:109
        new Entry("gregtech:there_is_no_cow_sauce", 1), // MultiItemBottles.java:110
        new Entry("gregtech:barbecue_sauce", 3), // MultiItemBottles.java:111
        new Entry("gregtech:apple_cider_vinegar", 10), // MultiItemBottles.java:116
        new Entry("gregtech:olive_oil", 10), // MultiItemBottles.java:120
        new Entry("gregtech:sunflower_oil", 11), // MultiItemBottles.java:121
        new Entry("gregtech:nut_oil", 11), // MultiItemBottles.java:122
        new Entry("gregtech:seed_oil", 10), // MultiItemBottles.java:123
        new Entry("gregtech:hemp_oil", 10), // MultiItemBottles.java:124
        new Entry("gregtech:lin_oil", 10), // MultiItemBottles.java:125
        new Entry("gregtech:fish_oil", 11), // MultiItemBottles.java:126
        new Entry("gregtech:whale_oil", 3), // MultiItemBottles.java:127
        new Entry("gregtech:mayo", 15), // MultiItemBottles.java:134
        new Entry("gregtech:dressing", 10), // MultiItemBottles.java:135
        new Entry("gregtech:heavy_cream", 15), // MultiItemBottles.java:139
        new Entry("gregtech:honey", 11), // MultiItemBottles.java:157
        new Entry("gregtech:royal_jelly", 11), // MultiItemBottles.java:159
        new Entry("gregtech:ambrosia", 5), // MultiItemBottles.java:160
        new Entry("gregtech:rice_vinegar", 10), // MultiItemBottles.java:192
        new Entry("gregtech:chocolate_cream", 3), // MultiItemBottles.java:194
        new Entry("gregtech:nutella", 3), // MultiItemBottles.java:195
        new Entry("gregtech:green_slime_bottle", 10), // MultiItemBottles.java:204
        new Entry("gregtech:pink_slime_bottle", 9), // MultiItemBottles.java:206
        new Entry("gregtech:blue_slime_bottle", 4), // MultiItemBottles.java:208
        new Entry("gregtech:tomato_ketchup", 1), // MultiItemBottles.java:257
        new Entry("gregtech:maple_syrup", 3), // MultiItemBottles.java:266
        new Entry("gregtech:peanut_butter", 3), // MultiItemBottles.java:273
        new Entry("gregtech:rainbow_sap", 40), // MultiItemBottles.java:275
        new Entry("gregtech:coconut_cream", 15), // MultiItemBottles.java:337
        new Entry("gregtech:bottle_of_poison", 5), // MultiItemBottles.java:358
        new Entry("gregtech:bottle_oblood", 1) // MultiItemBottles.java:381
    );
}
