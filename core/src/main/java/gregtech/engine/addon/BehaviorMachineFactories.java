package gregtech.engine.addon;

import java.util.Map;
import java.util.Set;

/** Machine types whose behavior factory was collected before {@link AddonMachines#install}. */
public final class BehaviorMachineFactories {
    private static boolean ready;
    private static Map<String, Set<String>> types = Map.of();

    private BehaviorMachineFactories() {}

    /** A second call keeps the first collection. Tests and the platform host both use this. */
    public static synchronized void collect(Map<String, Set<String>> found) {
        if (ready) return;
        var copy = new java.util.LinkedHashMap<String, Set<String>>();
        for (var entry : found.entrySet()) copy.put(entry.getKey(), Set.copyOf(entry.getValue()));
        types = Map.copyOf(copy);
        ready = true;
    }

    static synchronized boolean ready() {
        return ready;
    }

    static synchronized Set<String> types(String addonId) {
        return types.getOrDefault(addonId, Set.of());
    }
}
