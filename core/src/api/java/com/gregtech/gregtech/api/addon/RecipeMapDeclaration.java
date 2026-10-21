package com.gregtech.gregtech.api.addon;

import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * One addon machine recipe map. Slot counts match the built-in recipe-map spec.
 *
 * @deprecated Use {@link RecipeTable#machine(String)}. Removal is 0.2.
 */
@Deprecated(since = "0.0", forRemoval = true)
@AddonApiRemoval(id = "com.gregtech.gregtech.api.addon.RecipeMapDeclaration", removeAt = "0.2")
public record RecipeMapDeclaration(
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
    public RecipeMapDeclaration {
        if (name == null || !name.matches("[a-z0-9_.]+"))
            throw new IllegalArgumentException("Invalid recipe map name: " + name);
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(gui, "gui");
        if (inputItems < 0 || outputItems < 0 || minimalInputItems < 0
                || inputFluids < 0 || outputFluids < 0 || minimalInputFluids < 0 || minimalInputs < 0)
            throw new IllegalArgumentException("Negative slot count on " + name);
    }
}
