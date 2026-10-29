package gregtech.api.mod;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mod ownership metadata for materials and recipes, analogous to GregTech {@code ModData}.
 */
public final class ModData {
    /** Concurrent: loader mod constructors may register in parallel. */
    public static final Map<String, ModData> MODS = new ConcurrentHashMap<>();
    private static volatile java.util.function.Predicate<String> presence;

    /** The platform must bind its loader query once, before any metadata or material holder initializes. */
    public static synchronized void bindPresence(java.util.function.Predicate<String> resolver) {
        if (presence != null) throw new IllegalStateException("Mod presence is already bound");
        if (!MODS.isEmpty()) throw new IllegalStateException("Mod presence must be bound before metadata initialization");
        presence = java.util.Objects.requireNonNull(resolver, "resolver");
    }

    private boolean loaded;
    public final String id;
    public final String name;
    public final String prefix;

    public ModData(String id, String name) {
        this.id = id;
        this.name = name;
        this.prefix = id + ":";
        if (presence == null) throw new IllegalStateException("Mod presence has not been bound by the platform");
        this.loaded = presence.test(id);
        MODS.put(id, this);
        MODS.put(id.toLowerCase(Locale.ROOT), this);
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
