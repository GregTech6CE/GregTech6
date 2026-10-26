package gregtech.engine.multiblock.tracking;

import java.util.ArrayList;
import java.util.List;

/** Test-only ordering log for chunk and controller lifecycle events. */
public final class StructureTrackingEvents {
    private static final List<String> LOG = new ArrayList<>();
    private static boolean enabled;

    private StructureTrackingEvents() {}

    public static void enable(boolean on) {
        enabled = on;
        if (!on) LOG.clear();
    }

    public static void record(String event) {
        if (enabled) LOG.add(event);
    }

    public static List<String> log() {
        return List.copyOf(LOG);
    }
}
