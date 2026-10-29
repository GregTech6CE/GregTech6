package gregtech.api.machine;



/**
 * Per-machine-type face configuration for item, fluid, and energy I/O.
 *
 * <p>Mask bits reuse the numeric order of Minecraft {@code Direction} values
 * ({@code DOWN}=0, {@code UP}=1, {@code NORTH}=2, {@code SOUTH}=3, {@code WEST}=4,
 * {@code EAST}=5) so a mask converts without a lookup table. A set bit still means a
 * <b>machine-relative</b> face, not the absolute direction with that number:</p>
 * <ul>
 *   <li>0: bottom (absolute)</li>
 *   <li>1: top (absolute)</li>
 *   <li>2: right</li>
 *   <li>3: front</li>
 *   <li>4: left</li>
 *   <li>5: back</li>
 * </ul>
 *
 * <p>{@link MachineFace} exposes the same indices through {@link MachineFace#index()}.</p>
 *
 * <p>Auto I/O directions use {@code -1} for none / undefined (GT6 {@code SIDE_UNDEFINED}).</p>
 */
public record MachineFaceMasks(
        int itemInputs, int itemOutputs,
        int fluidInputs, int fluidOutputs,
        int energyInputs, int energyOutputs,
        int itemAutoInput, int itemAutoOutput,
        int fluidAutoInput, int fluidAutoOutput
) {
    // ── Machine-relative face indices ────────────────────────────────────
    // The numbers match Direction.get3DDataValue() for easy conversion, but each
    // constant names a face of the machine, not the absolute direction at that index.

    public static final int BOTTOM = 0;
    public static final int TOP    = 1;
    public static final int LEFT   = 4;
    public static final int RIGHT  = 2;
    public static final int FRONT  = 3;
    public static final int BACK   = 5;

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

    /** Returns whether the machine-relative face index {@code face} (0..5) is set in {@code mask}. */
    public static boolean has(int mask, int face) {
        return (mask & (1 << face)) != 0;
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

        // ── Machine-relative int overloads (primary API) ──────────────

        public Builder itemIn(int... dirs)    { itemIn    = faceMask(itemIn, dirs); return this; }
        public Builder itemOut(int... dirs)   { itemOut   = faceMask(itemOut, dirs); return this; }
        public Builder fluidIn(int... dirs)   { fluidIn   = faceMask(fluidIn, dirs); return this; }
        public Builder fluidOut(int... dirs)  { fluidOut  = faceMask(fluidOut, dirs); return this; }
        public Builder energyIn(int... dirs)  { energyIn  = faceMask(energyIn, dirs); return this; }
        public Builder energyOut(int... dirs) { energyOut = faceMask(energyOut, dirs); return this; }

        public Builder itemAutoIn(int d)    { itemAutoIn    = autoFace(d); return this; }
        public Builder itemAutoOut(int d)   { itemAutoOut   = autoFace(d); return this; }
        public Builder fluidAutoIn(int d)   { fluidAutoIn   = autoFace(d); return this; }
        public Builder fluidAutoOut(int d)  { fluidAutoOut  = autoFace(d); return this; }

        private static int faceMask(int current, int... dirs) {
            for (int dir : dirs) {
                if (dir < 0 || dir >= 6) throw new IllegalArgumentException("Machine face must be 0..5: " + dir);
                current |= 1 << dir;
            }
            return current;
        }

        private static int autoFace(int dir) {
            if (dir != AUTO_NONE && !autoValid(dir))
                throw new IllegalArgumentException("Auto direction must be -1 or 0..5: " + dir);
            return dir;
        }

        public MachineFaceMasks build() {
            return new MachineFaceMasks(itemIn, itemOut, fluidIn, fluidOut,
                    energyIn, energyOut, itemAutoIn, itemAutoOut, fluidAutoIn, fluidAutoOut);
        }
    }
}
