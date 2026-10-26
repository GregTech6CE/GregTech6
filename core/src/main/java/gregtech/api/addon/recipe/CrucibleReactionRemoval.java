/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;




import gregtech.api.addon.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Drops crucible rows with this output. An empty {@code inputs} list drops every row of that output.
 * Otherwise the input names must be the same set, ignoring order and ratios. A miss warns and does not fail startup.
 */
public record CrucibleReactionRemoval(String output, List<String> inputs) {
    public CrucibleReactionRemoval {
        Objects.requireNonNull(output, "output");
        if (!AddonNames.matches(AddonNames.MATERIAL, output))
            throw new IllegalArgumentException("Use a material name: " + output);
        Objects.requireNonNull(inputs, "inputs");
        var copied = new ArrayList<String>(inputs.size());
        for (String input : inputs) {
            Objects.requireNonNull(input, "input");
            if (!AddonNames.matches(AddonNames.MATERIAL, input))
                throw new IllegalArgumentException("Use a material name: " + input);
            copied.add(input);
        }
        inputs = List.copyOf(copied);
    }
}
