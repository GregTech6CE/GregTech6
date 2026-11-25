/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.worldgen;

/** Height coordinates used by a world generation declaration. */
public enum HeightCoordinates {
    /** Preserve the original 0..255 declaration and Overworld depth mapping. */
    LEGACY,
    /** Accept modern world coordinates and clip them to the dimension's terrain at generation time. */
    ABSOLUTE;

    void check(String name, int minY, int maxY) {
        if (this == LEGACY) WorldgenNames.checkHeight(name, minY, maxY);
        else WorldgenNames.checkAbsoluteHeight(name, minY, maxY);
    }
}
