package com.gregtech.gregtech.api.addon;

import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/** SPDX-License-Identifier: LGPL-3.0-or-later. Shared, deterministic addon lifecycle for both loaders. */
public final class GregTechAddons {
    /**
     * Minor of {@code mod_version} ({@code 0.MINOR.PATCH}), not an individual build identifier.
     * Development builds sharing a version still need a matching SDK and runtime.
     */
    public static final int API_VERSION = 0;
    private static final TreeMap<String, GregTechAddon> ADDONS = new TreeMap<>();
    public enum Phase { COLLECTING, DISPATCHING, READY, FAILED }
    private static Phase phase = Phase.COLLECTING;

    private GregTechAddons() {}

    /** Register from a mod constructor, before common setup; duplicate and late IDs fail explicitly. */
    public static synchronized void register(GregTechAddon addon) {
        Objects.requireNonNull(addon, "addon");
        String id = AddonNames.requireAddonId(Objects.requireNonNull(addon.id(), "addon ID"));
        if (phase != Phase.COLLECTING) throw new IllegalStateException("GT addon registration closed: " + id);
        if (ADDONS.putIfAbsent(id, addon) != null) throw new IllegalStateException("Duplicate GT addon ID: " + id);
    }

    public static synchronized List<String> registeredIds() { return List.copyOf(ADDONS.keySet()); }
    public static synchronized boolean recipesReady() { return phase == Phase.READY; }
    public static synchronized Phase phase() { return phase; }

    /**
     * Platform bootstrap entry; addon authors register a provider rather than calling this method.
     * A callback {@link RuntimeException} is wrapped as {@link IllegalStateException} and leaves
     * {@link Phase#FAILED}. An {@link Error} is rethrown unchanged and also leaves {@code FAILED}.
     * Addons that already ran are not rolled back.
     */
    public static void dispatchRecipesReady(GregTechAddon.Context context) {
        Objects.requireNonNull(context, "context");
        List<java.util.Map.Entry<String, GregTechAddon>> providers;
        synchronized (GregTechAddons.class) {
            if (phase != Phase.COLLECTING) throw new IllegalStateException("GT addon recipe lifecycle dispatched twice");
            phase = Phase.DISPATCHING;
            providers = ADDONS.entrySet().stream().map(entry -> java.util.Map.entry(entry.getKey(), entry.getValue())).toList();
        }
        for (var entry : providers) {
            try { entry.getValue().onRecipesReady(context); }
            catch (RuntimeException | Error failure) {
                synchronized (GregTechAddons.class) { phase = Phase.FAILED; }
                throw AddonFailures.of("GT addon recipes failed: " + entry.getKey(), failure);
            }
        }
        synchronized (GregTechAddons.class) { phase = Phase.READY; }
    }
}
