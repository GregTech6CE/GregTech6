/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * One addon-declared datapack type that GregTech applies through the machine-recipe overlay.
 *
 * <p>An addon registers its own Minecraft recipe type and serializer, and its recipe objects
 * implement the platform's document-provider interface. The overlay asks this type to validate the
 * raw object and returns the same addition or removal document the built-in types produce. Both
 * readers may return null when the object is the other kind, but at least one must accept it.</p>
 *
 * <p>Register during the mod constructor, before {@code dispatchRecipesReady} closes the
 * registration.</p>
 */
public interface MachineRecipeDocumentType {
    /** The datapack type id, for example {@code mymod:compressor}. */
    String id();

    /** The addition this object describes, or null when the object is not an addition. */
    default @Nullable MachineRecipeDocument addition(Map<String, Object> json) {
        return null;
    }

    /** The removal this object describes, or null when the object is not a removal. */
    default @Nullable MachineRecipeRemovalDocument removal(Map<String, Object> json) {
        return null;
    }
}
