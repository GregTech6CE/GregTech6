/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import java.util.Optional;

/** Burn duration and optional residue for one material unit. */
public record FuelRule(long burnTime, Optional<MaterialTarget> residue) {
    public FuelRule {
        if (burnTime < 0) throw new IllegalArgumentException("Negative fuel burn time");
        java.util.Objects.requireNonNull(residue, "residue");
    }
}
