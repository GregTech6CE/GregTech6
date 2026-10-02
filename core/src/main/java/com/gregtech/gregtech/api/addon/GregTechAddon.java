package com.gregtech.gregtech.api.addon;

/** SPDX-License-Identifier: LGPL-3.0-or-later */
public interface GregTechAddon {
    /** The addon's own loader mod ID. Register during the addon's mod constructor. */
    String id();

    /** Called once after GT material/item linking and built-in machine recipes are ready. */
    void onRecipesReady(Context context);

    /** Loader-neutral lifecycle information; Minecraft stacks remain platform types. */
    record Context(String platform, String minecraftVersion) {
        public int apiVersion() { return GregTechAddons.API_VERSION; }
    }
}
