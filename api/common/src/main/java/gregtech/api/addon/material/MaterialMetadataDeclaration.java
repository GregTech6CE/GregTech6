/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

/** Side metadata submitted by a contributor. Own namespaced tags may describe any real material.
 * Properties and flags require material ownership and the contributor's key namespace.
 * Metadata does not add native forms or grant host processing behaviors. */
public record MaterialMetadataDeclaration(String materialName, MaterialMetadata metadata) {
    public MaterialMetadataDeclaration {
        if (materialName == null || materialName.isBlank()) throw new IllegalArgumentException("Missing metadata material");
        java.util.Objects.requireNonNull(metadata, "metadata");
    }
}
