package com.gregtech.gregtech.api.addon;

import com.gregtech.gregtech.api.addon.platform.BehaviorMachineContributor;
import com.gregtech.gregtech.api.addon.platform.MachineBehavior;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/** Loads one {@link BehaviorMachineContributor} per addon and creates a behavior for each block entity. */
public final class BehaviorMachineHosts {
    private static final Map<String, BehaviorMachineContributor> CONTRIBUTORS = new LinkedHashMap<>();
    private static boolean collected;

    private BehaviorMachineHosts() {}

    public static synchronized void collect() {
        if (collected) return;
        collected = true;
        var types = new LinkedHashMap<String, Set<String>>();
        ClassLoader loader = AddonMachineContributor.class.getClassLoader();
        for (BehaviorMachineContributor contributor : ServiceLoader.load(BehaviorMachineContributor.class, loader)) {
            String id;
            try {
                id = contributor.addonId();
            } catch (RuntimeException | Error failure) {
                throw new IllegalStateException("Behavior machine contributor failed: " + contributor.getClass().getName(), failure);
            }
            if (id == null || id.isBlank())
                throw new IllegalStateException("Behavior machine contributor has no addon id: " + contributor.getClass().getName());
            if (CONTRIBUTORS.putIfAbsent(id, contributor) != null)
                throw new IllegalStateException("Addon '" + id + "' registered two behavior factories");
            Set<String> machines;
            try {
                machines = contributor.machineTypes();
            } catch (RuntimeException | Error failure) {
                throw new IllegalStateException("Addon '" + id + "' behavior factory failed", failure);
            }
            types.put(id, machines == null ? Set.of() : Set.copyOf(machines));
        }
        BehaviorMachineFactories.collect(types);
    }

    public static synchronized MachineBehavior create(String addonId, String machineType) {
        BehaviorMachineContributor contributor = CONTRIBUTORS.get(addonId);
        if (contributor == null)
            throw new IllegalStateException("Addon '" + addonId + "' machine '" + machineType + "' has no behavior factory");
        MachineBehavior behavior = contributor.create(machineType);
        if (behavior == null)
            throw new IllegalStateException("Addon '" + addonId + "' machine '" + machineType + "' has no behavior factory");
        return behavior;
    }
}
