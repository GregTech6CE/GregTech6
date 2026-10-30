/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.machine;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Per-machine-type face configuration for item, fluid, and energy I/O.
 *
 * <p>Every channel is a set of machine-relative {@link MachineFace} values. An automatic
 * direction is a single {@link MachineFace}, or {@code null} when that channel has none.
 * The host converts these faces to its own side indices at the boundary; this type carries
 * no bitmask or {@code Direction} ordinal.</p>
 */
public record MachineFaceMasks(
        Set<MachineFace> itemInputs, Set<MachineFace> itemOutputs,
        Set<MachineFace> fluidInputs, Set<MachineFace> fluidOutputs,
        Set<MachineFace> energyInputs, Set<MachineFace> energyOutputs,
        @Nullable MachineFace itemAutoInput, @Nullable MachineFace itemAutoOutput,
        @Nullable MachineFace fluidAutoInput, @Nullable MachineFace fluidAutoOutput
) {
    private static final Set<MachineFace> EVERY_FACE = Set.copyOf(MachineFace.all());

    public MachineFaceMasks {
        itemInputs = copy(itemInputs, "item input");
        itemOutputs = copy(itemOutputs, "item output");
        fluidInputs = copy(fluidInputs, "fluid input");
        fluidOutputs = copy(fluidOutputs, "fluid output");
        energyInputs = copy(energyInputs, "energy input");
        energyOutputs = copy(energyOutputs, "energy output");
    }

    /** All six faces enabled for item, fluid and energy input; no energy output or automatic face. */
    public static final MachineFaceMasks ALL_SIDES = new MachineFaceMasks(
            EVERY_FACE, EVERY_FACE, EVERY_FACE, EVERY_FACE,
            EVERY_FACE, Set.of(), null, null, null, null);

    /** Standard machine: top in, bottom out, sides energy. */
    public static final MachineFaceMasks TOP_IN_BOTTOM_OUT = builder()
            .itemIn(MachineFace.TOP).itemOut(MachineFace.BOTTOM)
            .energyIn(MachineFace.LEFT, MachineFace.RIGHT, MachineFace.FRONT, MachineFace.BACK)
            .build();

    /** Furnace-style: sides in/out, bottom energy. */
    public static final MachineFaceMasks ALL_IN_ALL_OUT = builder()
            .itemIn(MachineFace.TOP, MachineFace.LEFT, MachineFace.RIGHT, MachineFace.FRONT, MachineFace.BACK)
            .itemOut(MachineFace.TOP, MachineFace.LEFT, MachineFace.RIGHT, MachineFace.FRONT, MachineFace.BACK)
            .energyIn(MachineFace.BOTTOM)
            .build();

    /** Fluid-capable: separate fluid faces. */
    public static final MachineFaceMasks TOP_IN_BOTTOM_FLUID = builder()
            .itemIn(MachineFace.TOP).itemOut(MachineFace.BOTTOM)
            .fluidIn(MachineFace.LEFT, MachineFace.RIGHT).fluidOut(MachineFace.BOTTOM)
            .energyIn(MachineFace.FRONT, MachineFace.BACK)
            .build();

    /** No energy input (non-powered machines). */
    public static final MachineFaceMasks NO_ENERGY = builder()
            .itemIn(MachineFace.TOP).itemOut(MachineFace.BOTTOM)
            .fluidIn(MachineFace.LEFT).fluidOut(MachineFace.FRONT)
            .build();

    public static Builder builder() { return new Builder(); }

    private static Set<MachineFace> copy(@Nullable Set<MachineFace> faces, String label) {
        if (faces == null) throw new IllegalArgumentException("Missing " + label + " faces");
        var copy = EnumSet.noneOf(MachineFace.class);
        for (MachineFace face : faces) {
            if (face == null) throw new IllegalArgumentException("Missing " + label + " face");
            copy.add(face);
        }
        return Collections.unmodifiableSet(copy);
    }

    public static class Builder {
        private final Set<MachineFace> itemIn = EnumSet.noneOf(MachineFace.class);
        private final Set<MachineFace> itemOut = EnumSet.noneOf(MachineFace.class);
        private final Set<MachineFace> fluidIn = EnumSet.noneOf(MachineFace.class);
        private final Set<MachineFace> fluidOut = EnumSet.noneOf(MachineFace.class);
        private final Set<MachineFace> energyIn = EnumSet.noneOf(MachineFace.class);
        private final Set<MachineFace> energyOut = EnumSet.noneOf(MachineFace.class);
        private @Nullable MachineFace itemAutoIn;
        private @Nullable MachineFace itemAutoOut;
        private @Nullable MachineFace fluidAutoIn;
        private @Nullable MachineFace fluidAutoOut;

        public Builder itemIn(MachineFace... faces)    { add(itemIn, faces, "item input"); return this; }
        public Builder itemOut(MachineFace... faces)   { add(itemOut, faces, "item output"); return this; }
        public Builder fluidIn(MachineFace... faces)   { add(fluidIn, faces, "fluid input"); return this; }
        public Builder fluidOut(MachineFace... faces)  { add(fluidOut, faces, "fluid output"); return this; }
        public Builder energyIn(MachineFace... faces)  { add(energyIn, faces, "energy input"); return this; }
        public Builder energyOut(MachineFace... faces) { add(energyOut, faces, "energy output"); return this; }

        /** Null clears the automatic direction for this channel. */
        public Builder itemAutoIn(@Nullable MachineFace face)   { itemAutoIn = face; return this; }
        public Builder itemAutoOut(@Nullable MachineFace face)  { itemAutoOut = face; return this; }
        public Builder fluidAutoIn(@Nullable MachineFace face)  { fluidAutoIn = face; return this; }
        public Builder fluidAutoOut(@Nullable MachineFace face) { fluidAutoOut = face; return this; }

        private static void add(Set<MachineFace> target, MachineFace[] faces, String label) {
            if (faces == null) throw new IllegalArgumentException("Missing " + label + " faces");
            for (MachineFace face : faces) {
                if (face == null) throw new IllegalArgumentException("Missing " + label + " face");
                target.add(face);
            }
        }

        public MachineFaceMasks build() {
            return new MachineFaceMasks(itemIn, itemOut, fluidIn, fluidOut,
                    energyIn, energyOut, itemAutoIn, itemAutoOut, fluidAutoIn, fluidAutoOut);
        }
    }
}
