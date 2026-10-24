package com.gregtech.gregtech.api.addon;

import com.gregtech.gregtech.api.addon.platform.LayoutPartContributor;
import com.gregtech.gregtech.api.addon.platform.PartBehavior;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/** Loads one {@link LayoutPartContributor} per addon and creates a behavior for each functional part. */
public final class LayoutPartHosts {
    private static final Map<String, LayoutPartContributor> CONTRIBUTORS = new LinkedHashMap<>();
    private static boolean collected;
    private static boolean collecting;

    private LayoutPartHosts() {}

    /**
     * Loads every contributor. The registry is published only after the whole pass succeeds, so a failure
     * leaves it empty and a later call can load it again instead of keeping a half-built map.
     */
    public static synchronized void collect() {
        if (collected || collecting) return;
        collecting = true;
        try {
            var found = new LinkedHashMap<String, LayoutPartContributor>();
            var names = new LinkedHashMap<String, Set<String>>();
            ClassLoader loader = AddonMachineContributor.class.getClassLoader();
            for (LayoutPartContributor contributor : ServiceLoader.load(LayoutPartContributor.class, loader)) {
                String id;
                try {
                    id = contributor.addonId();
                } catch (RuntimeException | Error failure) {
                    throw AddonFailures.of("Part contributor failed: " + contributor.getClass().getName(), failure);
                }
                if (!AddonNames.isAddonId(id))
                    throw new IllegalStateException("Part contributor has no addon id: " + contributor.getClass().getName());
                if (found.putIfAbsent(id, contributor) != null)
                    throw new IllegalStateException("Addon '" + id + "' registered two part factories");
                Set<String> parts;
                try {
                    parts = contributor.partNames();
                } catch (RuntimeException | Error failure) {
                    throw AddonFailures.of("Addon '" + id + "' part factory failed", failure);
                }
                names.put(id, parts == null ? Set.of() : Set.copyOf(parts));
            }
            CONTRIBUTORS.clear();
            CONTRIBUTORS.putAll(found);
            LayoutPartFactories.collect(names);
            collected = true;
        } finally {
            collecting = false;
        }
    }

    public static synchronized PartBehavior create(String addonId, String partName) {
        LayoutPartContributor contributor = CONTRIBUTORS.get(addonId);
        if (contributor == null)
            throw new IllegalStateException("Addon '" + addonId + "' structure part '" + partName + "' has no part factory");
        PartBehavior behavior;
        try {
            behavior = contributor.create(partName);
        } catch (RuntimeException | Error failure) {
            throw AddonFailures.of("Addon '" + addonId + "' structure part '" + partName + "' part factory failed", failure);
        }
        if (behavior == null)
            throw new IllegalStateException("Addon '" + addonId + "' structure part '" + partName + "' part factory returned null");
        return behavior;
    }
}
