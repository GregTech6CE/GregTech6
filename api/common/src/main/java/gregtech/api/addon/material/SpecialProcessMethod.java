/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import java.util.Objects;

/**
 * One special process method. Target and reagent materials attach {@link SpecialProcessValue}
 * rules through the same {@link MaterialProcessKey}.
 */
public record SpecialProcessMethod(
        MaterialProcessKey<SpecialProcessValue> process,
        String recipeMap,
        String displayNameKey) {
    public SpecialProcessMethod {
        Objects.requireNonNull(process, "process");
        if (process.type() != SpecialProcessValue.class)
            throw new IllegalArgumentException("Special process method has the wrong process value type");
        if (recipeMap == null || recipeMap.isBlank())
            throw new IllegalArgumentException("Special process method has no recipe map");
        if (displayNameKey == null || displayNameKey.isBlank())
            throw new IllegalArgumentException("Special process method has no display name key");
    }

    /** Material classification tag assigned to every processable material. */
    public DataKey targetTag() {
        return new DataKey(process.id().namespace(), process.id().path() + "/target");
    }

    /** Material classification tag assigned to every processing reagent. */
    public DataKey reagentTag() {
        return new DataKey(process.id().namespace(), process.id().path() + "/reagent");
    }
}
