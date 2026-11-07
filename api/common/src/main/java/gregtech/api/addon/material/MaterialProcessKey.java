/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

/** Typed identity of a material process or transformation rule and its cardinality. */
public record MaterialProcessKey<T>(DataKey id, Class<T> type, MaterialProcessCardinality cardinality) {
    public MaterialProcessKey {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(type, "type");
        java.util.Objects.requireNonNull(cardinality, "cardinality");
    }

    /** Declares a process that has at most one rule per material. */
    public static <T> MaterialProcessKey<T> single(DataKey id, Class<T> type) {
        return new MaterialProcessKey<>(id, type, MaterialProcessCardinality.SINGLE);
    }

    /** Declares an ordered multi-valued process for a material. */
    public static <T> MaterialProcessKey<T> multiple(DataKey id, Class<T> type) {
        return new MaterialProcessKey<>(id, type, MaterialProcessCardinality.MULTIPLE);
    }
}
