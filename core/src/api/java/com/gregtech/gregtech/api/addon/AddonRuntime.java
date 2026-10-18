package com.gregtech.gregtech.api.addon;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * The one core entry GregTech installs before material initialization.
 * Calls made before that fail immediately. There is one implementation, so this is not a service loader.
 */
public final class AddonRuntime {
    private static Services services;

    private AddonRuntime() {}

    public static synchronized void bind(Services implementation) {
        if (implementation == null) throw new IllegalArgumentException("addon services");
        if (services != null) throw new IllegalStateException("GregTech addon API is already installed");
        services = implementation;
    }

    public static synchronized Services services() {
        if (services == null) throw new IllegalStateException("GregTech addon API is not installed");
        return services;
    }

    /** Read-only material and prefix queries. Item stacks stay on the platform entry. */
    public interface Services {
        GregTechMaterials.Catalog materials();

        /** True when {@code name} is a prefix GregTech already generates, including known aliases. */
        boolean prefixExists(String name);
    }
}
