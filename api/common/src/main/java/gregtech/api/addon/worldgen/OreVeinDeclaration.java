/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.worldgen;

/**
 * One large ore vein, with the same fields as a built-in {@code OreVein}.
 * Heights are the pre-remap values used by the built-in tables; the overworld generator stretches
 * them and other dimensions use them as absolute Y values.
 * {@code dimension} accepts any resource location. The host's large-vein feature must be registered
 * for that dimension; the built-in biome modifiers cover the overworld and the end.
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
    public static final String NETHER = "minecraft:the_nether";
    public static final String END = "minecraft:the_end";

    public OreVeinDeclaration {
        name = WorldgenNames.check(name, "Ore vein");
        dimension = WorldgenNames.dimension(name, dimension);
        WorldgenNames.checkHeight(name, minY, maxY);
        if (weight <= 0 || density <= 0 || size <= 0)
            throw new IllegalArgumentException("Ore vein '" + name + "' weight, density and size must be positive");
        top = WorldgenNames.material(name, top);
        bottom = WorldgenNames.material(name, bottom);
        between = WorldgenNames.material(name, between);
        spread = WorldgenNames.material(name, spread);
    }
}
