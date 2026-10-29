/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;




import gregtech.api.addon.*;

/**
 * Prefix names GregTech already generates, such as {@code dust}, {@code dustTiny}, {@code ingot} and {@code plantGtBerry},
 * plus prefixes an addon declared with {@link PrefixDeclaration}. Aliases known to GregTech, such as {@code casingSmall}
 * for the item casing, resolve here. {@link #exists} is true for an addon prefix after material startup.
 */
public final class GregTechPrefixes {
    private GregTechPrefixes() {}

    public static boolean exists(String name) { return AddonRuntime.services().prefixExists(name); }
}
