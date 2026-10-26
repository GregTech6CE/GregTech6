package gregtech.api.addon.material;




import gregtech.api.addon.*;

import gregtech.api.material.MaterialDefinition;

import java.util.List;

/** SPDX-License-Identifier: LGPL-3.0-or-later */
/**
 * @deprecated Use {@link AddonMaterialContributor}. Removal is 0.10.
 */
@Deprecated(since = "0.0", forRemoval = true)
@AddonApiRemoval(id = "gregtech.api.addon.material.GregTechMaterialContributor", removeAt = "0.10")
public interface GregTechMaterialContributor {
    /** The addon's own loader mod id, not {@code gt6ce} or {@code gregtech6}. */
    String addonId();

    List<MaterialDefinition> materials();

    List<MaterialFormRequest> forms();

    /** Compositions and other cross-material links. New materials still belong in {@link #materials()}. */
    default void link() {}
}
