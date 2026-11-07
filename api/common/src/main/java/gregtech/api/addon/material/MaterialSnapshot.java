/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

/** One immutable material view. It cannot refer to mutable GTMaterial objects. */
public record MaterialSnapshot(MaterialKey key, MaterialInfo info, MaterialMetadata metadata) implements MaterialView {
    public MaterialSnapshot {
        java.util.Objects.requireNonNull(key);
        java.util.Objects.requireNonNull(info);
        java.util.Objects.requireNonNull(metadata);
    }
    @Override public <T> java.util.Optional<T> property(PropertyKey<T> key) { return metadata.property(key); }
    @Override public java.util.Set<DataKey> flags() { return metadata.flags(); }
}
