/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon;

/** One addon, identified by the loader mod id it registers from its mod constructor. */
public interface GregTechAddon {
    /** The addon's own loader mod ID. Register during the addon's mod constructor. */
    String id();

    /** Called once after GT material/item linking and built-in machine recipes are ready. */
    void onRecipesReady(Context context);

    /** Loader-neutral lifecycle information; Minecraft stacks remain platform types. */
    record Context(String platform, String minecraftVersion) {
        /**
         * Monotonic API version code ({@code major * 1000 + minor}) for an optional feature branch.
         * Compatibility is the loader version range.
         */
        public int apiVersion() { return GregTechAddons.apiVersion(); }
    }
}
