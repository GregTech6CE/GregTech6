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

    private static final MaterialTextureSet[] MODELED_SETS = values();

    /**
     * Texture sets that have shared model JSON under {@code models/item/material/}. Every constant
     * currently qualifies, so this has the same elements as {@link #values()}; use this method
     * rather than assuming the two stay equal.
     */
    public static List<MaterialTextureSet> modeled() { return List.of(MODELED_SETS); }

    /** Lowercase folder name used in resource locations (MC normalizes paths to lowercase). */
    public String folder() {
        return name().toLowerCase(Locale.ROOT);
    }
}
