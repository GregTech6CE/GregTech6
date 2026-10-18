package com.gregtech.gregtech.api.addon;

import com.gregtech.gregtech.api.material.MaterialDefinition;

import java.util.List;

/** SPDX-License-Identifier: LGPL-3.0-or-later */
/**
 * @deprecated Use {@link AddonMaterialContributor}. Removal is 0.6.
 */
@Deprecated(since = "0.4", forRemoval = true)
@AddonApiRemoval(id = "com.gregtech.gregtech.api.addon.GregTechMaterialContributor", removeAt = "0.6")
public interface GregTechMaterialContributor {
    /** The addon's own loader mod id, not {@code gregtech} or {@code gregtech6}. */
    String addonId();

    List<MaterialDefinition> materials();

    List<MaterialFormRequest> forms();

    /** Compositions and other cross-material links. New materials still belong in {@link #materials()}. */
    default void link() {}
}
