/* Derived from GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original authors and license retained; see NOTICE and source provenance. */
package com.gregtech.gregtech.content.food;

import java.util.List;

/** MultiTileEntitySandwich's setBlockBounds: coordinates in block units, shared by item and world. */
public final class SandwichGeometry {
    private SandwichGeometry() {}
    public record Box(double x0, double y0, double z0, double x1, double y1, double z1) {}
    private static Box px(double x0, double y0, double z0, double x1, double y1, double z1) {
        return new Box(x0 / 16, y0 / 16, z0 / 16, x1 / 16, y1 / 16, z1 / 16);
    }
    public static List<Box> boxes(int slot, SandwichLayerCatalog.Layer layer, int previousModel) {
        int top = slot + layer.thickness(), model = layer.footprint();
        if (model == 14) return List.of(px(1, slot, 1, 7, top, 7), px(1, slot, 9, 7, top, 15),
                px(9, slot, 1, 15, top, 7), px(9, slot, 9, 15, top, 15));
        if (model == 252 && slot > 0) {
            double offset = (16 - slot) * .0005;
            if (previousModel == 2 || previousModel == 3 || previousModel == 14 || previousModel == 252)
                return List.of(new Box(2 / 16.0 - offset, .5 / 16.0, 2 / 16.0 - offset,
                        14 / 16.0 + offset, top / 16.0, 14 / 16.0 + offset));
            return List.of(new Box(1 / 16.0 - offset, (slot - .5) / 16.0, 1 / 16.0 - offset,
                    15 / 16.0 + offset, top / 16.0, 15 / 16.0 + offset));
        }
        if (model == 253 || model == 254) return List.of(px(.5, slot, .5, 15.5, top, 15.5));
        int inset = model == 2 || model == 3 ? model : 1;
        return List.of(px(inset, slot, inset, 16 - inset, top, 16 - inset));
    }
}
