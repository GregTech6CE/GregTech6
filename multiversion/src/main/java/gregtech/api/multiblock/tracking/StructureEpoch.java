package gregtech.api.multiblock.tracking;

/** Layout generation. Incremented on every tag reload, including when the overlay text is unchanged. */
public final class StructureEpoch {
    private static int epoch;

    private StructureEpoch() {}

    public static int get() {
        return epoch;
    }

    public static void bump() {
        epoch++;
        StructureScheduler.enqueueAll();
    }
}
