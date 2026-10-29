/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.pipe;




import gregtech.api.addon.*;

/**
 * One addon item-pipe material. GregTech expands it into the six built-in sizes,
 * including the three restrictive sizes. {@code stepSize} is the medium-pipe routing
 * cost; larger sizes divide it the same way a built-in row does, so it must be at least 4.
 */
public record ItemPipeDeclaration(String idSuffix, String material, long stepSize, int invSize) {
    public ItemPipeDeclaration {
        if (!AddonNames.matches(AddonNames.LOWER_PATH, idSuffix))
            throw new IllegalArgumentException("Item pipe id must be a lower-case suffix: " + idSuffix);
        if (material == null || material.isBlank())
            throw new IllegalArgumentException("Item pipe '" + idSuffix + "' needs a material");
        if (stepSize < 4 || stepSize > Long.MAX_VALUE / 100)
            throw new IllegalArgumentException("Item pipe '" + idSuffix + "' step size must be from 4 through " + (Long.MAX_VALUE / 100));
        if (invSize < 1 || invSize > Integer.MAX_VALUE / 4)
            throw new IllegalArgumentException("Item pipe '" + idSuffix + "' inventory size must be from 1 through " + (Integer.MAX_VALUE / 4));
    }
}
