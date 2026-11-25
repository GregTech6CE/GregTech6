package gregtech.engine.stack;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;
//? if >=1.21.1 {
/*import net.neoforged.fml.util.thread.EffectiveSide;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
*///?} else {
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.server.ServerLifecycleHooks;
//?}

/**
 * Shared registry lookup entry point for level-less call sites.
 *
 * <p>Dynamic {@link HolderLookup.Provider} instances are never cached statically because each
 * world and server connection owns its own registry access. {@link #current()} resolves in the
 * fixed order: active server -> client connection -> built-in registry baseline
 * ({@link RegistryAccess#fromRegistryOfRegistries}).</p>
 */
public final class RegistryContext {
    private static volatile Supplier<HolderLookup.Provider> clientLookup = () -> null;

    private RegistryContext() {}

    /** Registers the client-side lookup supplier during client setup. */
    public static void setClientLookup(Supplier<HolderLookup.Provider> lookup) {
        clientLookup = Objects.requireNonNull(lookup, "client lookup supplier");
    }

    /**
     * Resolves a non-null lookup for level-less encoding/decoding: current server, then client
     * connection, then the built-in registry baseline (never {@link RegistryAccess#EMPTY}).
     */
    public static HolderLookup.Provider current() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) return server.registryAccess();
        var client = clientLookup.get();
        if (client != null) return client;
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    /** Returns {@code lookup} when non-null, otherwise falls back to {@link #current()}. */
    public static HolderLookup.Provider orCurrent(@Nullable HolderLookup.Provider lookup) {
        return lookup != null ? lookup : current();
    }

    /**
     * Resolves the live world/connection lookup for the calling logical side, or {@code null} when
     * no world is active.
     *
     * <p>Use this for data-pack registries such as 1.21.1 enchantments, where the built-in
     * baseline lacks dynamic entries and integrated-server holders must not be mixed across the
     * client connection boundary.</p>
     */
    public static @Nullable HolderLookup.Provider currentActive() {
        if (EffectiveSide.get().isClient()) return clientLookup.get();
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.registryAccess();
    }
}
