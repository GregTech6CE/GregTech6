/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Declares how a material participates in centrifuge and electrolyzer decomposition.
 *
 * <p>Fluid bindings identify native phase resources and retain both sides of their conversion:
 * a source quantity and the produced millibucket quantity. The value is caller-owned after
 * registration and must not be mutated.</p>
 */
public record MaterialDecompositionRule(Set<Route> routes, List<FluidBinding> fluids, boolean keepSolidInput) {
    public MaterialDecompositionRule {
        Objects.requireNonNull(routes, "routes");
        Objects.requireNonNull(fluids, "fluids");
        routes = routes.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(routes));
        fluids = List.copyOf(fluids);
        EnumSet<Phase> phases = EnumSet.noneOf(Phase.class);
        for (FluidBinding fluid : fluids) {
            Objects.requireNonNull(fluid, "fluid");
            if (!phases.add(fluid.phase()))
                throw new IllegalArgumentException("Duplicate decomposition phase: " + fluid.phase());
        }
    }

    public enum Route {
        CENTRIFUGE,
        ELECTROLYSER
    }

    public enum Phase {
        LIQUID,
        GAS,
        PLASMA
    }

    /**
     * A native fluid and its exact source conversion.
     *
     * @param fluid registered fluid path or identifier
     * @param sourceUnits source material units represented by {@code millibuckets}
     * @param millibuckets output volume represented by {@code sourceUnits}
     */
    public record FluidBinding(Phase phase, String fluid, long sourceUnits, long millibuckets) {
        public FluidBinding {
            Objects.requireNonNull(phase, "phase");
            if (fluid == null || fluid.isBlank())
                throw new IllegalArgumentException("Missing decomposition fluid");
            if (sourceUnits <= 0 || millibuckets <= 0)
                throw new IllegalArgumentException("Decomposition fluid quantities must be positive");
        }
    }
}
