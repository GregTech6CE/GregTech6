/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon;

import gregtech.api.addon.material.GregTechMaterials;
import gregtech.api.addon.material.GregTechPrefixes;

/**
 * The one core entry GregTech installs before material initialization.
 * Calls made before that fail immediately. There is one implementation, so this is not a service loader.
 */
public final class AddonRuntime {
    private static final InstalledService<Services> INSTALLED = new InstalledService<>();

    private AddonRuntime() {}

    /** GT bootstrap only. Addons must not install or replace the host implementation. */
    public static void bind(Services implementation) {
        INSTALLED.bind(implementation, "addon services", "GregTech addon API is already installed");
    }

    public static Services services() {
        return INSTALLED.get("GregTech addon API is not installed");
    }

    /**
     * GT bootstrap implementation contract, not an addon service-provider extension point.
     * Addons query {@link GregTechMaterials} and {@link GregTechPrefixes} instead.
     */
    public interface Services {
        GregTechMaterials.Catalog materials();

        /** True when {@code name} is a prefix GregTech already generates, including known aliases. */
        boolean prefixExists(String name);
    }
}
