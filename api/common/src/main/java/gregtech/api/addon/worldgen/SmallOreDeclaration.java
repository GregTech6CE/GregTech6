/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.worldgen;

import java.util.Objects;

/**
 * One small ore, with the same fields as a built-in {@code SmallOre}.
 * The original constructor uses legacy heights. The overworld generator stretches them and other
 * dimensions use them as absolute Y values. Select {@link HeightCoordinates#ABSOLUTE} to declare
 * modern coordinates directly.
 * {@code dimension} accepts any resource location. The host's small-ore feature must be registered
 * for that dimension; the built-in biome modifiers cover the overworld, the nether and the end.
 */
public record SmallOreDeclaration(
        String name,
        String dimension,
        int minY,
        int maxY,
        int amount,
        String material,
        HeightCoordinates coordinates
) {
    public static final String OVERWORLD = OreVeinDeclaration.OVERWORLD;
    public static final String NETHER = "minecraft:the_nether";
    public static final String END = OreVeinDeclaration.END;

    public SmallOreDeclaration(String name, String dimension, int minY, int maxY, int amount, String material) {
        this(name, dimension, minY, maxY, amount, material, HeightCoordinates.LEGACY);
    }

    public SmallOreDeclaration {
        name = WorldgenNames.check(name, "Small ore");
        dimension = WorldgenNames.dimension(name, dimension);
        Objects.requireNonNull(coordinates, "coordinates").check(name, minY, maxY);
        if (amount <= 0)
            throw new IllegalArgumentException("Small ore '" + name + "' amount must be positive");
        material = WorldgenNames.material(name, material);
    }
}
