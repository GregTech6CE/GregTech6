/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.worldgen;




/**
 * One small ore, with the same fields as a built-in {@code SmallOre}.
 * Heights are the pre-remap values used by the built-in tables. The overworld generator stretches them.
 */
public record SmallOreDeclaration(
        String name,
        String dimension,
        int minY,
        int maxY,
        int amount,
        String material
) {
    public static final String OVERWORLD = OreVeinDeclaration.OVERWORLD;
    public static final String NETHER = "minecraft:the_nether";
    public static final String END = OreVeinDeclaration.END;

    public SmallOreDeclaration {
        name = WorldgenNames.check(name, "Small ore");
        if (!OVERWORLD.equals(dimension) && !NETHER.equals(dimension) && !END.equals(dimension))
            throw new IllegalArgumentException("Small ore '" + name
                    + "' dimension must be minecraft:overworld, minecraft:the_nether or minecraft:the_end: "
                    + dimension);
        WorldgenNames.checkHeight(name, minY, maxY);
        if (amount <= 0)
            throw new IllegalArgumentException("Small ore '" + name + "' amount must be positive");
        material = WorldgenNames.material(name, material);
    }
}
