package com.gregtech.gregtech.api.addon;

/** Shared checks for ore-vein and small-ore declarations. */
final class WorldgenNames {
    private WorldgenNames() {}

    static String check(String name, String kind) {
        if (name == null || !name.matches("[a-z][a-z0-9_]{0,63}"))
            throw new IllegalArgumentException(kind + " name must be a lower-case built-in style name: " + name);
        return name;
    }

    static void checkHeight(String name, int minY, int maxY) {
        if (minY < 0 || maxY > 255 || minY >= maxY)
            throw new IllegalArgumentException("'" + name + "' Y range must sit inside 0..255, with min below max");
    }

    static String material(String owner, String name) {
        if (name == null || !name.matches("[A-Za-z][A-Za-z0-9_]*"))
            throw new IllegalArgumentException("'" + owner + "' material name is invalid: " + name);
        return name;
    }
}
