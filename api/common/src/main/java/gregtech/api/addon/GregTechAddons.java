/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Shared, deterministic addon lifecycle for both loaders. Registration closes when
 * {@link #dispatchRecipesReady} starts. The registry and phase have no reset entry, so a test
 * that needs a fresh lifecycle runs in its own JVM.
 */
public final class GregTechAddons {
    /**
     * Minor of the independent API artifact version ({@code 0.MINOR.PATCH}).
     * Development builds still need a matching SDK and host runtime; this number alone
     * does not check the installed mod or Minecraft version.
     *
     * <p>Not a compile-time constant. A {@code static final} primitive set to a literal is copied into
     * addon bytecode, so that addon would keep the minor it was compiled against. This field is
     * initialized from {@link #apiVersion()} and is read at runtime.</p>
     */
    public static final int API_VERSION = apiVersion();

    /** Current {@link #API_VERSION}. */
    public static int apiVersion() { return 3; }

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
     * A callback failure other than an {@link Error} is wrapped as {@link IllegalStateException} and
     * leaves {@link Phase#FAILED}; this includes a checked exception escaping through a sneaky throw.
     * An {@link Error} is rethrown unchanged and also leaves {@code FAILED}. Addons that already ran
     * are not rolled back.
     */
    public static void dispatchRecipesReady(GregTechAddon.Context context) {
        Objects.requireNonNull(context, "context");
        List<Map.Entry<String, GregTechAddon>> providers;
        synchronized (GregTechAddons.class) {
            if (phase != Phase.COLLECTING) throw new IllegalStateException("GT addon recipe lifecycle dispatched twice");
            phase = Phase.DISPATCHING;
            providers = ADDONS.entrySet().stream().map(entry -> Map.entry(entry.getKey(), entry.getValue())).toList();
        }
        for (var entry : providers) {
            try { entry.getValue().onRecipesReady(context); }
            catch (Throwable failure) {
                synchronized (GregTechAddons.class) { phase = Phase.FAILED; }
                throw AddonFailures.of("GT addon recipes failed: " + entry.getKey(), failure);
            }
        }
        synchronized (GregTechAddons.class) { phase = Phase.READY; }
    }
}
