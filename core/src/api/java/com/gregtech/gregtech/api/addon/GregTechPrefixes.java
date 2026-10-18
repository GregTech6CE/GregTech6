package com.gregtech.gregtech.api.addon;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * Prefix names GregTech already generates, such as {@code dust}, {@code dustTiny}, {@code ingot} and {@code plantGtBerry}.
 * Addons cannot declare a prefix. Aliases known to GregTech, such as {@code dustTiny} for the tiny-dust form, resolve here.
 */
public final class GregTechPrefixes {
    private GregTechPrefixes() {}

    public static boolean exists(String name) { return AddonRuntime.services().prefixExists(name); }
}
