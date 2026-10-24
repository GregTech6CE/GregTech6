package gregtech.internal.addon;

import gregtech.api.addon.AddonFailures;
import gregtech.api.addon.AddonMachineContributor;
import gregtech.api.addon.AddonNames;

import gregtech.api.addon.platform.BehaviorMachineContributor;
import gregtech.api.addon.platform.MachineBehavior;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/** Loads one {@link BehaviorMachineContributor} per addon and creates a behavior for each block entity. */
public final class BehaviorMachineHosts {
    private static final Map<String, BehaviorMachineContributor> CONTRIBUTORS = new LinkedHashMap<>();
    private static boolean collected;
    private static boolean collecting;

    private BehaviorMachineHosts() {}

    /**
     * Loads every contributor. The registry is published only after the whole pass succeeds, so a failure
     * leaves it empty and a later call can load it again instead of keeping a half-built map.
     */
    public static synchronized void collect() {
        if (collected || collecting) return;
        collecting = true;
        try {
            var found = new LinkedHashMap<String, BehaviorMachineContributor>();
            var types = new LinkedHashMap<String, Set<String>>();
            ClassLoader loader = AddonMachineContributor.class.getClassLoader();
            for (BehaviorMachineContributor contributor : ServiceLoader.load(BehaviorMachineContributor.class, loader)) {
                String id;
                try {
                    id = contributor.addonId();
                } catch (RuntimeException | Error failure) {
                    throw AddonFailures.of("Behavior machine contributor failed: " + contributor.getClass().getName(), failure);
                }
                if (!AddonNames.isAddonId(id))
                    throw new IllegalStateException("Behavior machine contributor has no addon id: " + contributor.getClass().getName());
                if (found.putIfAbsent(id, contributor) != null)
                    throw new IllegalStateException("Addon '" + id + "' registered two behavior factories");
                Set<String> machines;
                try {
                    machines = contributor.machineTypes();
                } catch (RuntimeException | Error failure) {
                    throw AddonFailures.of("Addon '" + id + "' behavior factory failed", failure);
                }
                types.put(id, machines == null ? Set.of() : Set.copyOf(machines));
            }
            CONTRIBUTORS.clear();
            CONTRIBUTORS.putAll(found);
            BehaviorMachineFactories.collect(types);
            collected = true;
        } finally {
            collecting = false;
        }
    }

    public static synchronized MachineBehavior create(String addonId, String machineType) {
        BehaviorMachineContributor contributor = CONTRIBUTORS.get(addonId);
        if (contributor == null)
            throw new IllegalStateException("Addon '" + addonId + "' machine '" + machineType + "' has no behavior factory");
        MachineBehavior behavior;
        try {
            behavior = contributor.create(machineType);
        } catch (RuntimeException | Error failure) {
            throw AddonFailures.of("Addon '" + addonId + "' machine '" + machineType + "' behavior factory failed", failure);
        }
        if (behavior == null)
            throw new IllegalStateException("Addon '" + addonId + "' machine '" + machineType + "' behavior factory returned null");
        return behavior;
    }
}
