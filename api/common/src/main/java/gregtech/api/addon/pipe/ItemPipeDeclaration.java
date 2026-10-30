/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.pipe;

import gregtech.api.addon.AddonNames;

/**
 * One addon item-pipe material. GregTech expands it into the six built-in sizes,
 * including the three restrictive sizes. {@code stepSize} is the medium-pipe routing
 * cost; larger sizes divide it the same way a built-in row does, so it must be at least 4.
 */
public record ItemPipeDeclaration(String idSuffix, String material, long stepSize, int invSize) {
    private static final long MIN_STEP_SIZE = 4;
    /** Leaves room for the per-size routing cost, which scales the step size by up to 100. */
    private static final long MAX_STEP_SIZE = Long.MAX_VALUE / 100;
    private static final int MAX_INVENTORY_SIZE = Integer.MAX_VALUE / 4;

    public ItemPipeDeclaration {
        if (!AddonNames.matches(AddonNames.LOWER_PATH, idSuffix))
            throw new IllegalArgumentException("Item pipe id must be a lower-case suffix: " + idSuffix);
        if (!AddonNames.matches(AddonNames.MATERIAL, material))
            throw new IllegalArgumentException(
                    "Item pipe '" + idSuffix + "' has an invalid material name: " + material);
        if (stepSize < MIN_STEP_SIZE || stepSize > MAX_STEP_SIZE)
            throw new IllegalArgumentException("Item pipe '" + idSuffix + "' step size must be from "
                    + MIN_STEP_SIZE + " through " + MAX_STEP_SIZE);
        if (invSize < 1 || invSize > MAX_INVENTORY_SIZE)
            throw new IllegalArgumentException("Item pipe '" + idSuffix + "' inventory size must be from 1 through "
                    + MAX_INVENTORY_SIZE);
    }
}
