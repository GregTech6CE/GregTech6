package com.gregtech.gregtech.api.addon;

import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * One addon fuel map. Slot counts match a built-in fuel map. A fuel map is not a machine map:
 * a recipe machine cannot name it, and a fuel generator cannot name a machine map.
 *
 * @deprecated Use {@link RecipeTable#fuel(String)}. Removal is 0.2.
 */
@Deprecated(since = "0.0", forRemoval = true)
@AddonApiRemoval(id = "com.gregtech.gregtech.api.addon.FuelMapDeclaration", removeAt = "0.2")
public record FuelMapDeclaration(
        String name,
        String title,
        String gui,
        int inputItems,
        int outputItems,
        int minimalInputItems,
        int inputFluids,
        int outputFluids,
        int minimalInputFluids,
        int minimalInputs,
        boolean needsOutputs,
        boolean combinePower,
        boolean bucketIn,
        boolean bucketOut
) {
    public FuelMapDeclaration {
        if (name == null || !name.matches("[a-z0-9_.]+"))
            throw new IllegalArgumentException("Invalid fuel map name: " + name);
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(gui, "gui");
        if (inputItems < 0 || outputItems < 0 || minimalInputItems < 0
                || inputFluids < 0 || outputFluids < 0 || minimalInputFluids < 0 || minimalInputs < 0)
            throw new IllegalArgumentException("Negative slot count on " + name);
    }
}
