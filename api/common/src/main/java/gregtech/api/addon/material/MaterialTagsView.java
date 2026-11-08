/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

/** Descriptive classification and substitution-group queries. Neither modifies physical material facts. */
public interface MaterialTagsView {
    java.util.Set<DataKey> tags(MaterialKey material);
    java.util.Set<MaterialKey> members(DataKey tag);
    java.util.Optional<MaterialGroupView> group(MaterialGroupKey key);
    default boolean contains(MaterialKey material, DataKey tag) { return tags(material).contains(tag); }
}
