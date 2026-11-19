package gregtech.engine.addon;

import gregtech.api.addon.AddonRuntime;
import gregtech.api.addon.material.MaterialCatalogView;
import gregtech.api.addon.material.MaterialInfoTool;
import gregtech.engine.material.MaterialInfoRegistry;
import gregtech.engine.material.MaterialViews;
import gregtech.engine.prefix.PrefixRegistry;

/** SPDX-License-Identifier: LGPL-3.0-or-later. Installs the core addon entry. Call this before material initialization. */
public final class AddonCoreServices {
    private static boolean bound;

    private AddonCoreServices() {}

    public static synchronized void bind() {
        if (bound) return;
        AddonRuntime.bind(new Services());
        MaterialInfoTool.bindHost(MaterialInfoRegistry.host());
        bound = true;
    }

    private static final class Services implements AddonRuntime.Services {
        @Override
        public MaterialCatalogView materialCatalog() {
            return MaterialViews.catalog();
        }

        @Override
        public boolean prefixExists(String name) {
            return name != null && PrefixRegistry.byName(name) != null;
        }
    }
}
