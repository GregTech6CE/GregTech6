package com.gregtech.gregtech.api.energy;



/**
 * Per-machine-type face configuration for item, fluid, and energy I/O.
 * Bitmask bits use the Minecraft DOWN..EAST side index (0=DOWN .. 5=EAST)
 * and represent <b>machine-relative</b> directions.
 * <p>
 * Auto I/O directions use {@code -1} for none / undefined (GT6 {@code SIDE_UNDEFINED}).
 */
public record MachineFaceMasks(
        int itemInputs, int itemOutputs,
        int fluidInputs, int fluidOutputs,
        int energyInputs, int energyOutputs,
        int itemAutoInput, int itemAutoOutput,
        int fluidAutoInput, int fluidAutoOutput
) {
    // ── Machine-relative direction constants ─────────────────────────────
    // Values match Direction.get3DDataValue() for easy conversion.
    // TOP/BOTTOM are absolute; LEFT/RIGHT/FRONT/BACK are relative to machine facing.

    public static final int BOTTOM = 0;  // Direction.DOWN
    public static final int TOP    = 1;  // Direction.UP
    public static final int LEFT   = 4;  // Direction.WEST   (machine left)
    public static final int RIGHT  = 2;  // Direction.NORTH  (machine right)
    public static final int FRONT  = 3;  // Direction.SOUTH  (machine front)
    public static final int BACK   = 5;  // Direction.EAST   (machine back)

    /** All six faces enabled for every I/O type. */
    public static final MachineFaceMasks ALL_SIDES = new MachineFaceMasks(
            0b111111, 0b111111, 0b111111, 0b111111,
            0b111111, 0, -1, -1, -1, -1);

    /** Standard machine: top in, bottom out, sides energy. */
    public static final MachineFaceMasks TOP_IN_BOTTOM_OUT = builder()
            .itemIn(TOP).itemOut(BOTTOM)
            .energyIn(LEFT, RIGHT, FRONT, BACK)
            .build();

    /** Furnace-style: sides in/out, bottom energy. */
    public static final MachineFaceMasks ALL_IN_ALL_OUT = builder()
            .itemIn(TOP, LEFT, RIGHT, FRONT, BACK)
            .itemOut(TOP, LEFT, RIGHT, FRONT, BACK)
            .energyIn(BOTTOM)
            .build();

    /** Fluid-capable: separate fluid faces. */
    public static final MachineFaceMasks TOP_IN_BOTTOM_FLUID = builder()
            .itemIn(TOP).itemOut(BOTTOM)
            .fluidIn(LEFT, RIGHT).fluidOut(BOTTOM)
            .energyIn(FRONT, BACK)
            .build();

    /** No energy input (non-powered machines). */
    public static final MachineFaceMasks NO_ENERGY = builder()
            .itemIn(TOP).itemOut(BOTTOM)
            .fluidIn(LEFT).fluidOut(FRONT)
            .build();

    // ── Bitmask helpers ──────────────────────────────────────────────────

    /** Check whether direction {@code d} is enabled in the given mask. */

    /** Check whether direction {@code d} is enabled in the given mask (int index). */
    public static boolean has(int mask, int dir3DData) {
        return (mask & (1 << dir3DData)) != 0;
    }

    // ── Auto I/O direction helpers ──────────────────────────────────────

    /** {@code -1} means "none" (GT6 {@code SIDE_UNDEFINED}). */
    public static final int AUTO_NONE = -1;

    public static boolean autoValid(int autoDir) {
        return autoDir >= 0 && autoDir < 6;
    }

    // ── Builder ──────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private int itemIn, itemOut, fluidIn, fluidOut, energyIn, energyOut;
        private int itemAutoIn = -1, itemAutoOut = -1, fluidAutoIn = -1, fluidAutoOut = -1;

        // ── Direction-enum overloads (delegate to int overloads) ─────



        // ── Machine-relative int overloads (primary API) ──────────────

        public Builder itemIn(int... dirs)    { for (int d : dirs) itemIn    |= (1 << d); return this; }
        public Builder itemOut(int... dirs)   { for (int d : dirs) itemOut   |= (1 << d); return this; }
        public Builder fluidIn(int... dirs)   { for (int d : dirs) fluidIn   |= (1 << d); return this; }
        public Builder fluidOut(int... dirs)  { for (int d : dirs) fluidOut  |= (1 << d); return this; }
        public Builder energyIn(int... dirs)  { for (int d : dirs) energyIn  |= (1 << d); return this; }
        public Builder energyOut(int... dirs) { for (int d : dirs) energyOut |= (1 << d); return this; }

        public Builder itemAutoIn(int d)    { itemAutoIn    = d; return this; }
        public Builder itemAutoOut(int d)   { itemAutoOut   = d; return this; }
        public Builder fluidAutoIn(int d)   { fluidAutoIn   = d; return this; }
        public Builder fluidAutoOut(int d)  { fluidAutoOut  = d; return this; }

        public MachineFaceMasks build() {
            return new MachineFaceMasks(itemIn, itemOut, fluidIn, fluidOut,
                    energyIn, energyOut, itemAutoIn, itemAutoOut, fluidAutoIn, fluidAutoOut);
        }
    }
}
