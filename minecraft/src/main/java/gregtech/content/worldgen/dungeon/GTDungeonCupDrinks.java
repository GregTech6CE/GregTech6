package gregtech.content.worldgen.dungeon;

/** GT6's two dungeon cup drink pools; the original chooses from the global RNG, not the room RNG. */
final class GTDungeonCupDrinks {
    private static final String[] CORRIDOR = {
        null, null, null, null, null, "Mead", "Rum_White", "Rum_Dark", "Rum_White", "Mead", "Rum_Dark", "Wine_Lemon"
    };
    private static final String[] BARRACKS = {
        null,
        null,
        null,
        null,
        null,
        "Mead",
        "Grenade_Juice",
        "Rum_White",
        "Lemonade",
        "Mead",
        "Soda",
        "Rum_Dark",
        "Wine_Lemon"
    };

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
        data.next(2); // Keep the original layout roll; the night-vision cup is no longer granted.
        return false;
    }
}
