package com.gregtech.gregtech.api.addon;

import java.util.List;

/**
 * SPDX-License-Identifier: LGPL-3.0-or-later.
 * Early material entry discovered with {@link java.util.ServiceLoader}.
 * List this class in {@code META-INF/services/com.gregtech.gregtech.api.addon.AddonMaterialContributor}.
 * GregTech loads it while materials are still open. Return pure data.
 * {@link #link()} runs later, after the built-in directory has linked.
 */
public interface AddonMaterialContributor {
    /** The addon's own loader mod id, not {@code gt6ce}, {@code gregtech6} or {@code minecraft}. */
    String addonId();

    List<MaterialDeclaration> materials();

    List<MaterialFormRequest> forms();

    /** Compositions and other cross-material links. New materials still belong in {@link #materials()}. */
    default void link() {}
}
