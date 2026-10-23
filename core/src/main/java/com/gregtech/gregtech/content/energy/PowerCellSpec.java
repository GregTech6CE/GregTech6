/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Loader_MultiTileEntities 14600/14601/14700/14701 and TileEntityBase09PowerCell. */
package com.gregtech.gregtech.content.energy;

import java.util.List;

/** Fuel-filled, non-rechargeable portable cells. Energy and packet sizes are EU. */
public record PowerCellSpec(String id, int originalId, boolean fusion, String fuel,
                            long capacity, long voltage, int color) {
    public static final List<PowerCellSpec> ALL = List.of(
            new PowerCellSpec("power_cell_empty", 14700, false, "", 0, 0, 0x7F7F7F),
            new PowerCellSpec("power_cell_hydrogen", 14701, false, "Hydrogen", 3_200_000, 512, 0x0000FF),
            new PowerCellSpec("aneutronic_fusion_power_cell_empty", 14600, true, "", 0, 0, 0x7F7F7F),
            new PowerCellSpec("aneutronic_fusion_power_cell_helium_3", 14601, true, "Helium_3", 1_024_000_000, 8192, 0xFFFFFF));

    public boolean filled() { return capacity > 0; }
    public String emptyId() { return fusion ? "aneutronic_fusion_power_cell_empty" : "power_cell_empty"; }
    public int inset() { return fusion ? 4 : 6; }
    public int height() { return fusion ? 4 : 16; }
    public long usableRemainder(long charge) { return charge < voltage ? 0 : Math.min(capacity, charge); }
    public int display(long charge) { return charge <= 0 || capacity <= 0 ? 0 : 1 + (int) (Math.min(charge, capacity) * 14 / capacity); }
}
