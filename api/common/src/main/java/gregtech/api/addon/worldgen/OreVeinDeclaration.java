/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.worldgen;




/**
 * One large ore vein, with the same fields as a built-in {@code OreVein}.
 * Heights are the pre-remap values used by the built-in tables. The overworld generator stretches them.
 * The nether has no large-vein generator, so {@code minecraft:the_nether} is rejected.
 */
public record OreVeinDeclaration(
        String name,
        String dimension,
        int minY,
        int maxY,
        int weight,
        int density,
        int size,
        String top,
        String bottom,
        String between,
        String spread
) {
    public static final String OVERWORLD = "minecraft:overworld";
    public static final String END = "minecraft:the_end";

    public OreVeinDeclaration {
        name = WorldgenNames.check(name, "Ore vein");
        if (!OVERWORLD.equals(dimension) && !END.equals(dimension))
            throw new IllegalArgumentException("Ore vein '" + name
                    + "' dimension must be minecraft:overworld or minecraft:the_end: " + dimension);
        WorldgenNames.checkHeight(name, minY, maxY);
        if (weight <= 0 || density <= 0 || size <= 0)
            throw new IllegalArgumentException("Ore vein '" + name + "' weight, density and size must be positive");
        top = WorldgenNames.material(name, top);
        bottom = WorldgenNames.material(name, bottom);
        between = WorldgenNames.material(name, between);
        spread = WorldgenNames.material(name, spread);
    }
}
