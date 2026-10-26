package gregtech.engine.multiblock.tracking;

/** Layout generation. Incremented on every tag reload, including when the overlay text is unchanged. */
public final class StructureEpoch {
    private static int epoch;

    private StructureEpoch() {}

    public static int get() {
        return epoch;
    }

    public static void bump() {
        bump(java.util.Set.of());
    }

    /** {@code affected} names the machines whose overlay layout changed; tag cells are always rechecked. */
    public static void bump(java.util.Set<String> affected) {
        epoch++;
        StructureScheduler.epochChanged(affected == null ? java.util.Set.of() : affected);
    }
}
