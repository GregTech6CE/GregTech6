/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

/** Initial intrinsic scalar keys; no processing permission is inferred from these properties. */
public final class MaterialProperties {
    private MaterialProperties() {}
    private static <T> PropertyKey<T> key(String path, Class<T> type) { return new PropertyKey<>(new DataKey("gt6ce", path), type); }
    public static final PropertyKey<Integer> MELTING_POINT = key("melting_point_kelvin", Integer.class);
    public static final PropertyKey<Integer> BOILING_POINT = key("boiling_point_kelvin", Integer.class);
    public static final PropertyKey<Float> DENSITY = key("density", Float.class);
    public static final PropertyKey<String> FORMULA = key("formula", String.class);
}
