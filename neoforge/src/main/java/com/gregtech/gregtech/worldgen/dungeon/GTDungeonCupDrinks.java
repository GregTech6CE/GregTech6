package com.gregtech.gregtech.worldgen.dungeon;

/** GT6's two dungeon cup drink pools; the original chooses from the global RNG, not the room RNG. */
final class GTDungeonCupDrinks {
    private static final String[] CORRIDOR = {null, null, null, null, null,
            "Purple_Drink", "Purple_Drink", "Purple_Drink", "Vodka", "Mead",
            "Whiskey_GlenMcKenner", "Wine_Grape_Purple"};
    private static final String[] BARRACKS = {null, null, null, null, null,
            "Purple_Drink", "Grenade_Juice", "Vodka", "Leninade", "Mead", "BAWLS",
            "Whiskey_GlenMcKenner", "Wine_Grape_Purple"};

    private GTDungeonCupDrinks() {}

    static boolean corridor(GTDungeonData data, int x, int y, int z) {
        return data.cup(x, y, z, data.select(CORRIDOR, x, y, z));
    }

    static boolean barracks(GTDungeonData data, int x, int y, int z) {
        String drink = data.select(BARRACKS, x, y, z);
        data.next1in2(); // GT6 selects one of two optional Hexorium blocks, both absent without that mod.
        return data.cup(x, y, z, drink);
    }

    static boolean library(GTDungeonData data, int x, int y, int z) {
        data.next1in2(); // Optional Hexorium block variant.
        data.next1in2(); // Optional Hexorium metadata.
        return data.cup(x, y, z, "Potion_NightVision_1L");
    }
}
