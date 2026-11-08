/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import java.util.List;

/** Ordered byproducts and processing multipliers for ore processing. */
public record OreProcessingRule(List<MaterialKey> byproducts, int oreMultiplier, int processingMultiplier) {
    public OreProcessingRule {
        byproducts = List.copyOf(byproducts);
        if (oreMultiplier < 1
                || oreMultiplier > 127
                || processingMultiplier < 1
                || processingMultiplier > 127)
            throw new IllegalArgumentException("Ore multipliers must be 1..127");
    }
}
