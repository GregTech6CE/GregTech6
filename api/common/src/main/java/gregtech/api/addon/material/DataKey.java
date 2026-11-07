/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

/** Namespaced extension key; independent of Minecraft resource-location classes. */
public record DataKey(String namespace, String path) {
    public DataKey {
        if (namespace == null || !namespace.matches("[a-z0-9_.-]+") || path == null || !path.matches("[a-z0-9_/.-]+"))
            throw new IllegalArgumentException("Invalid data key: " + namespace + ":" + path);
    }
    @Override public String toString() { return namespace + ":" + path; }
}
