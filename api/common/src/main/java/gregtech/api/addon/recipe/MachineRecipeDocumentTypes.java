/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import gregtech.api.addon.AddonNames;
import gregtech.api.addon.GregTechAddons;
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
 * {@link GregTechAddons#dispatchRecipesReady(gregtech.api.addon.GregTechAddon.Context)} starts.</p>
 */
public final class MachineRecipeDocumentTypes {
    private static final Map<String, MachineRecipeDocumentType> TYPES = new TreeMap<>();

    private MachineRecipeDocumentTypes() {}

    public static void register(MachineRecipeDocumentType type) {
        Objects.requireNonNull(type, "type");
        String id = requireId(type.id());
        synchronized (GregTechAddons.class) {
            if (GregTechAddons.phase() != GregTechAddons.Phase.COLLECTING)
                throw new IllegalStateException("Machine recipe document type registration closed: " + id);
            if (TYPES.putIfAbsent(id, type) != null)
                throw new IllegalStateException("Duplicate machine recipe document type: " + id);
        }
    }

    /** The registered type, or null when the host does not know this id. */
    public static @Nullable MachineRecipeDocumentType find(String id) {
        synchronized (GregTechAddons.class) {
            return id == null ? null : TYPES.get(id);
        }
    }

    public static List<String> registeredIds() {
        synchronized (GregTechAddons.class) {
            return List.copyOf(TYPES.keySet());
        }
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
