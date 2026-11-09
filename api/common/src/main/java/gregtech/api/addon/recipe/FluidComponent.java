/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import org.jspecify.annotations.Nullable;

/**
 * One fluid identity or tag in millibuckets. Exactly one of {@code fluid} or {@code tag} is set.
 * Zero denotes a presence requirement on an input and any amount on a removal.
 */
public record FluidComponent(@Nullable String fluid, @Nullable String tag, long amount) {
    public FluidComponent {
        if ((fluid == null) == (tag == null)
                || fluid != null && fluid.isBlank()
                || tag != null && tag.isBlank())
            throw new IllegalArgumentException("fluid component needs exactly one of fluid or tag");
        if (amount < 0) throw new IllegalArgumentException("amount");
        if (fluid != null) MachineRecipeDocuments.checkId(fluid, "fluid");
        else MachineRecipeDocuments.checkId(tag, "tag");
    }

    public FluidComponent(String fluid, long amount) {
        this(fluid, null, amount);
    }
}
