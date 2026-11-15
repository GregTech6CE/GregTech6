/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import java.util.List;
import java.util.Locale;

/**
 * Item icon set folder under {@code textures/item/material_icons/}, matching GregTech 6 texture sets.
 */
public enum MaterialTextureSet {
    METALLIC,
    DULL,
    SHINY,
    FINE,
    SAND,
    STONE,
    WOOD,
    DIAMOND,
    EMERALD,
    QUARTZ,
    RUBY,
    REDSTONE,
    ROUGH,
    POWDER,
    COPPER,
    CUBE,
    CUBE_SHINY,
    FIERY,
    FLINT,
    FLUID,
    FOOD,
    GAS,
    GEM_HORIZONTAL,
    GEM_VERTICAL,
    GLASS,
    HEX,
    LAPIS,
    LEAF,
    LIGNITE,
    MAGNETIC,
    NETHERSTAR,
    NONE,
    OPAL,
    PAPER,
    PLASMA,
    PRISMARINE,
    RAD,
    RUBBER,
    SHARDS,
    SPACE,
    BRICK;

    /**
     * Sets that own shared model JSON and icon templates. Constants outside this list remain
     * declared so addon code and API consumers keep compiling, but no material resolves to them
     * and no resources are shipped for them.
     */
    private static final List<MaterialTextureSet> MODELED_SETS = List.of(
            METALLIC,
            DULL,
            SHINY,
            FINE,
            SAND,
            STONE,
            WOOD,
            DIAMOND,
            EMERALD,
            QUARTZ,
            RUBY,
            REDSTONE,
            ROUGH,
            POWDER,
            COPPER,
            CUBE,
            CUBE_SHINY,
            FIERY,
            FLINT,
            FOOD,
            GEM_HORIZONTAL,
            GEM_VERTICAL,
            GLASS,
            LAPIS,
            LEAF,
            LIGNITE,
            MAGNETIC,
            NONE,
            OPAL,
            PAPER,
            PRISMARINE,
            RUBBER,
            SHARDS,
            SPACE,
            BRICK);

    /**
     * Texture sets that have shared model JSON under {@code models/item/material/} and icon
     * templates under {@code textures/{item,block}/material_icons/}. Iterate this rather than
     * {@link #values()}: the remaining constants have no generated model or texture resources.
     */
    public static List<MaterialTextureSet> modeled() { return MODELED_SETS; }

    /** Lowercase folder name used in resource locations (MC normalizes paths to lowercase). */
    public String folder() {
        return name().toLowerCase(Locale.ROOT);
    }
}
