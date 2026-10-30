/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.worldgen;

import gregtech.api.addon.AddonNames;

/** Shared checks for ore-vein and small-ore declarations. */
final class WorldgenNames {
    private static final int MAX_Y = 255;

    private WorldgenNames() {}

    static String check(String name, String kind) {
        if (!AddonNames.matches(AddonNames.LOWER_PATH, name))
            throw new IllegalArgumentException(kind + " name must be a lower-case built-in style name: " + name);
        return name;
    }

    static void checkHeight(String name, int minY, int maxY) {
        if (minY < 0 || maxY > MAX_Y || minY >= maxY)
            throw new IllegalArgumentException("'" + name + "' Y range must sit inside 0.." + MAX_Y
                    + ", with min below max");
    }

    /** Any resource location, so addon dimensions are usable. */
    static String dimension(String owner, String dimension) {
        if (dimension == null) throw new IllegalArgumentException("'" + owner + "' dimension is missing");
        int colon = dimension.indexOf(':');
        if (colon <= 0 || colon != dimension.lastIndexOf(':') || colon == dimension.length() - 1
                || !AddonNames.matches(AddonNames.RESOURCE_NAMESPACE, dimension.substring(0, colon))
                || !AddonNames.matches(AddonNames.RESOURCE_PATH, dimension.substring(colon + 1)))
            throw new IllegalArgumentException(
                    "'" + owner + "' dimension must be a resource location: " + dimension);
        return dimension;
    }

    static String material(String owner, String name) {
        if (!AddonNames.matches(AddonNames.MATERIAL, name))
            throw new IllegalArgumentException("'" + owner + "' material name is invalid: " + name);
        return name;
    }
}
