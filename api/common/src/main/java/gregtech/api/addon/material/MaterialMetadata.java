/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Immutable scalar metadata, with explicit type checking and no mutable value containers.
 * Initial values support boxed primitive numbers, boolean, String and the three key records.
 * A custom class or collection requires a future explicitly registered immutable value contract.
 * Classification metadata alone never grants processing or form generation capabilities. */
public final class MaterialMetadata {
    private final Map<PropertyKey<?>, Object> properties;
    private final Set<DataKey> flags;
    private final Set<DataKey> tags;

    private MaterialMetadata(Builder builder) {
        properties = Map.copyOf(builder.properties);
        flags = Set.copyOf(builder.flags);
        tags = Set.copyOf(builder.tags);
    }
    public static Builder builder() { return new Builder(); }
    public <T> Optional<T> property(PropertyKey<T> key) {
        for (var existing : properties.keySet()) if (existing.id().equals(key.id()) && !existing.type().equals(key.type()))
            throw new IllegalArgumentException("Property type conflict: " + key.id());
        return Optional.ofNullable(key.type().cast(properties.get(key)));
    }
    public Set<PropertyKey<?>> keys() { return properties.keySet(); }
    public Set<DataKey> flags() { return flags; }
    public Set<DataKey> tags() { return tags; }
    public static final class Builder {
        private final Map<PropertyKey<?>, Object> properties = new LinkedHashMap<>();
        private final java.util.Set<DataKey> flags = new java.util.LinkedHashSet<>();
        private final java.util.Set<DataKey> tags = new java.util.LinkedHashSet<>();
        public <T> Builder property(PropertyKey<T> key, T value) {
            java.util.Objects.requireNonNull(key);
            java.util.Objects.requireNonNull(value);
            if (!key.type().isInstance(value)) throw new IllegalArgumentException("Property type mismatch: " + key.id());
            if (!(value instanceof String || value instanceof Integer || value instanceof Long || value instanceof Float
                    || value instanceof Double || value instanceof Boolean || value instanceof Byte || value instanceof Short
                    || value instanceof MaterialKey || value instanceof FormKey || value instanceof DataKey))
                throw new IllegalArgumentException("Unsupported immutable property value: " + key.id());
            for (var existing : properties.keySet()) if (existing.id().equals(key.id()) && !existing.type().equals(key.type()))
                throw new IllegalArgumentException("Property type conflict: " + key.id());
            if (properties.putIfAbsent(key, value) != null) throw new IllegalArgumentException("Duplicate property: " + key.id());
            return this;
        }
        public Builder flag(DataKey key) { flags.add(java.util.Objects.requireNonNull(key)); return this; }
        public Builder tag(DataKey key) { tags.add(java.util.Objects.requireNonNull(key)); return this; }
        public MaterialMetadata build() { return new MaterialMetadata(this); }
    }
}
