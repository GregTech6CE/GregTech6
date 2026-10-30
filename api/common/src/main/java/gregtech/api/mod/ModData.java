/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.mod;

import gregtech.api.addon.InstalledService;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Predicate;
import org.jetbrains.annotations.ApiStatus;

/**
 * Mod ownership metadata for materials and recipes, analogous to GregTech {@code ModData}.
 * <p>
 * Internal: not part of the addon contract. {@link #MODS} is a read-only view with one entry per
 * mod, matched case-insensitively; registration happens through the constructor while the host
 * initializes its content. Registering an id that already exists, ignoring case, throws
 * {@link IllegalArgumentException}.
 */
@ApiStatus.Internal
public final class ModData {
    /** Concurrent: loader mod constructors may register in parallel; case-insensitive keeps one entry per mod. */
    private static final Map<String, ModData> REGISTRY =
            new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
    /** Read-only view of {@link #REGISTRY}; addons must not mutate it. */
    public static final Map<String, ModData> MODS = Collections.unmodifiableMap(REGISTRY);
    private static final InstalledService<Predicate<String>> PRESENCE = new InstalledService<>();

    /**
     * The platform must bind its loader query once, before any metadata or material holder initializes.
     * A second binding fails with the first binder's class and location.
     */
    public static void bindPresence(Predicate<String> resolver) {
        Objects.requireNonNull(resolver, "resolver");
        if (!PRESENCE.isBound() && !REGISTRY.isEmpty()) {
            throw new IllegalStateException("Mod presence must be bound before metadata initialization");
        }
        PRESENCE.bind(resolver, "resolver", "Mod presence is already bound");
    }

    private volatile boolean loaded;
    public final String id;
    public final String name;
    public final String prefix;

    public ModData(String id, String name) {
        this.id = id;
        this.name = name;
        this.prefix = id + ":";
        this.loaded = PRESENCE.get("Mod presence has not been bound by the platform").test(id);
        ModData existing = REGISTRY.putIfAbsent(id, this);
        if (existing != null) {
            throw new IllegalArgumentException("Mod " + id + " is already registered as " + existing.id);
        }
    }

    public ModData setLoaded(boolean loaded) {
        this.loaded = loaded;
        return this;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean owns(String registryName) {
        return loaded && registryName != null && registryName.startsWith(prefix);
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof ModData other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
