/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

/** Definition snapshot for this host session; revision is not a persistent material identity.
 * Unknown keys return empty. Unsupported services and unready hosts throw explicitly.
 * Content, resource tags, effective recipe sources and native bindings belong to later scoped services. */
public interface MaterialCatalogView {
    long revision();
    /** Resolves existing names and aliases to real materials; groups and sentinels return empty. */
    java.util.Optional<MaterialKey> resolve(String name);
    MaterialTagsView tags();
    java.util.Optional<MaterialView> find(MaterialKey key);
    java.util.List<MaterialView> materials();
}
