package com.gregtech.gregtech.content.bumble;
/** Original36-slot/20-slot groups including the original advanced slot11 overlap. */
public final class BumbliarySlotLayouts {
 private BumbliarySlotLayouts(){}
    public static class Layout {
        private final boolean advanced;
        private final int slots, royal, drone, productRoll;
        private final int[] combs, drones, dead, accessible;

        public Layout(boolean advanced, int slots, int royal, int drone, int[] combs, int[] drones,
                      int[] dead, int[] accessible, int productRoll) {
            this.advanced = advanced;
            this.slots = slots;
            this.royal = royal;
            this.drone = drone;
            this.combs = combs;
            this.drones = drones;
            this.dead = dead;
            this.accessible = accessible;
            this.productRoll = productRoll;
        }

        /** The advanced machine (GT6 id 32007) or the standard one (GT6 id 32741). */
        public boolean advanced() { return advanced; }
        /** Total inventory size ({@code getDefaultInventory:339}, advanced {@code :340}). */
        public int slots() { return slots; }
        /** GT6's {@code SLOT_ROYAL}. */
        public int royal() { return royal; }
        /** GT6's {@code SLOT_DRONE}, the preferred drone slot. */
        public int drone() { return drone; }
        /** GT6's {@code SLOTS_COMBS}. */
        public int[] combs() { return combs.clone(); }
        /** GT6's {@code SLOTS_DRONE}, the spare drone slots. */
        public int[] drones() { return drones.clone(); }
        /** GT6's {@code SLOTS_DEAD}. */
        public int[] dead() { return dead.clone(); }
        /** What automation may reach: {@code getAccessibleSlotsFromSide2}. */
        public int[] accessible() { return accessible.clone(); }
        /** The product roll bound: {@code rng(10000)} or the advanced {@code rng(20000)}. */
        public int productRoll() { return productRoll; }
    }

    /** GT6's {@code SLOT_ROYAL}: one princess goes in, one queen comes out. */
    public static final int SLOT_ROYAL = 13;
    /** GT6's {@code SLOT_DRONE}: the drone the princess prefers over the spare slots. */
    public static final int SLOT_DRONE = 22;
    /** GT6's {@code SLOTS_COMBS}: what a working queen produces into ({@code :334}). */
    public static final int[] SLOTS_COMBS = {0, 1, 2, 6, 7, 8, 9, 10, 11, 15, 16, 17, 18, 19, 20, 24, 25, 26};
    /** GT6's {@code SLOTS_DRONE}: the spare drone slots, which keep the brood and the princesses. */
    public static final int[] SLOTS_DRONE = {3, 4, 5, 12, 14, 21, 23};
    /** GT6's {@code SLOTS_DEAD}: the output of every dead bee, and the only slots automation sees. */
    public static final int[] SLOTS_DEAD = {27, 28, 29, 30, 31, 32, 33, 34, 35};

    /** GT6's standard bumbliary ({@code MultiTileEntityBumbliary:333-343}). */
    public static final Layout LAYOUT = new Layout(false, 36, SLOT_ROYAL, SLOT_DRONE,
            SLOTS_COMBS, SLOTS_DRONE, SLOTS_DEAD, SLOTS_DEAD, 10000);

    /** GT6's {@code MultiTileEntityBumbliaryAdvanced} slot groups ({@code :333-337}). */
    public static final int ADV_SLOT_ROYAL = 7, ADV_SLOT_DRONE = 12;
    public static final int[] ADV_SLOTS_COMBS = {0, 4, 5, 9, 10, 11, 14};
    public static final int[] ADV_SLOTS_DRONE = {1, 2, 3, 6, 8, 11, 13};
    public static final int[] ADV_SLOTS_DEAD = {15, 16, 17, 18, 19};
    public static final int[] ADV_SLOTS_AUTO = {0, 4, 5, 9, 10, 11, 14, 15, 16, 17, 18, 19};

    /**
     * GT6's advanced bumbliary ({@code MultiTileEntityBumbliaryAdvanced:333-344}). Note that GT6 lists
     * slot 11 in both {@code SLOTS_COMBS} and {@code SLOTS_DRONE}; the port keeps that overlap exactly
     * as the original has it.
     */
    public static final Layout ADVANCED_LAYOUT = new Layout(true, 20, ADV_SLOT_ROYAL, ADV_SLOT_DRONE,
            ADV_SLOTS_COMBS, ADV_SLOTS_DRONE, ADV_SLOTS_DEAD, ADV_SLOTS_AUTO, 20000);

}
