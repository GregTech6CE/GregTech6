/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import gregtech.api.addon.AddonNames;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/**
 * Addon-declared machine-recipe datapack types. The built-in {@link MachineRecipeType} ids are
 * handled by the host directly and are not part of this registry.
 *
 * <p>Registration closes when
 * {@link gregtech.api.addon.GregTechAddons#dispatchRecipesReady(gregtech.api.addon.GregTechAddon.Context)}
 * starts, matching the addon lifecycle. Production code has no reset entry; the testing hook
 * serves the API test suite.</p>
 */
public final class MachineRecipeDocumentTypes {
    private static final Map<String, MachineRecipeDocumentType> TYPES = new TreeMap<>();
    private static boolean closed;

    private MachineRecipeDocumentTypes() {}

    public static synchronized void register(MachineRecipeDocumentType type) {
        Objects.requireNonNull(type, "type");
        String id = requireId(type.id());
        if (closed) throw new IllegalStateException("Machine recipe document type registration closed: " + id);
        if (TYPES.putIfAbsent(id, type) != null)
            throw new IllegalStateException("Duplicate machine recipe document type: " + id);
    }

    /** The registered type, or null when the host does not know this id. */
    public static synchronized @Nullable MachineRecipeDocumentType find(String id) {
        return id == null ? null : TYPES.get(id);
    }

    public static synchronized List<String> registeredIds() {
        return List.copyOf(TYPES.keySet());
    }

    /** Closes registration for the current lifecycle. Called by the host when recipe dispatch starts. */
    public static synchronized void close() {
        closed = true;
    }

    /** Clears registered types and reopens registration. */
    public static synchronized void resetForTesting() {
        TYPES.clear();
        closed = false;
    }

    private static String requireId(String id) {
        int colon = id == null ? -1 : id.indexOf(':');
        if (colon < 1
                || colon != id.lastIndexOf(':')
                || colon == id.length() - 1
                || !AddonNames.matches(AddonNames.RESOURCE_NAMESPACE, id.substring(0, colon))
                || !AddonNames.matches(AddonNames.RESOURCE_PATH, id.substring(colon + 1)))
            throw new IllegalArgumentException("Invalid machine recipe document type ID: " + id);
        return id;
    }
}
