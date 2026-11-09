/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import java.util.Map;

/**
 * A recipe object that carries a machine-recipe document for the GregTech overlay.
 *
 * <p>Implement this on the recipe object an addon's own recipe serializer creates, after
 * registering a {@link gregtech.api.addon.recipe.MachineRecipeDocumentType} for
 * {@link #documentType()}. The overlay reads the document, has the registered type validate it,
 * and applies the resulting addition or removal with the same order and priority rules as the
 * built-in {@code gt6ce:machine} types.</p>
 */
public interface MachineRecipeDocumentProvider {
    /** One registered document type id, for example {@code mymod:compressor}. */
    String documentType();

    /** The raw document object, using the shared field names. */
    Map<String, Object> document();

    /** A stable label for diagnostics. Override when the recipe has its own identity. */
    default String documentId() {
        return documentType();
    }
}
