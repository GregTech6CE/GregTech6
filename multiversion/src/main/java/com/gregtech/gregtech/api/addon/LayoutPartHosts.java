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

    private LayoutPartHosts() {}

    public static synchronized void collect() {
        if (collected) return;
        collected = true;
        var names = new LinkedHashMap<String, Set<String>>();
        ClassLoader loader = AddonMachineContributor.class.getClassLoader();
        for (LayoutPartContributor contributor : ServiceLoader.load(LayoutPartContributor.class, loader)) {
            String id;
            try {
                id = contributor.addonId();
            } catch (RuntimeException | Error failure) {
                throw new IllegalStateException("Part contributor failed: " + contributor.getClass().getName(), failure);
            }
            if (id == null || id.isBlank())
                throw new IllegalStateException("Part contributor has no addon id: " + contributor.getClass().getName());
            if (CONTRIBUTORS.putIfAbsent(id, contributor) != null)
                throw new IllegalStateException("Addon '" + id + "' registered two part factories");
            Set<String> parts;
            try {
                parts = contributor.partNames();
            } catch (RuntimeException | Error failure) {
                throw new IllegalStateException("Addon '" + id + "' part factory failed", failure);
            }
            names.put(id, parts == null ? Set.of() : Set.copyOf(parts));
        }
        LayoutPartFactories.collect(names);
    }

    public static synchronized PartBehavior create(String addonId, String partName) {
        LayoutPartContributor contributor = CONTRIBUTORS.get(addonId);
        if (contributor == null)
            throw new IllegalStateException("Addon '" + addonId + "' structure part '" + partName + "' has no part factory");
        PartBehavior behavior = contributor.create(partName);
        if (behavior == null)
            throw new IllegalStateException("Addon '" + addonId + "' structure part '" + partName + "' has no part factory");
        return behavior;
    }
}
