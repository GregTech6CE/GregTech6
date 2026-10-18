package com.gregtech.gregtech.api.addon.platform;

import net.minecraftforge.fluids.FluidStack;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/** Loader entry for types whose signatures differ between Forge and NeoForge. GregTech installs it once. */
public final class AddonLoader {
    private static Services services;

    private AddonLoader() {}

    public static synchronized void bind(Services implementation) {
        if (implementation == null) throw new IllegalArgumentException("addon loader services");
        if (services != null) throw new IllegalStateException("GregTech addon loader API is already installed");
        services = implementation;
    }

    public static synchronized Services services() {
        if (services == null) throw new IllegalStateException("GregTech addon loader API is not installed");
        return services;
    }

    public interface Services {
        /** Null when {@code name} is not a registered GregTech fluid field. */
        FluidStack fluid(String name, AddonFluids.Kind kind, int millibuckets);

        /** Null when no machine recipe map uses {@code name}. */
        AddonRecipes.Map map(String name);
    }
}
