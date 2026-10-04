/* Gregorius Techneticies / GregTech-6 Team source semantics, LGPL-3.0-or-later. */
package com.gregtech.gregtech.content.recipe;

/** AdvancedCrafting1ToY selects a prefix's output by the leading empty grid cells. */
public record FormConversionSelector(int variants, int offset) {
    public static final FormConversionSelector NONE = new FormConversionSelector(0, 0);

    public FormConversionSelector {
        if (variants < 0 || offset < 0 || (variants == 0 ? offset != 0 : offset >= variants))
            throw new IllegalArgumentException("Invalid source form selector");
    }

    public boolean matches(int gridSize, int firstOccupiedSlot, int occupiedCells) {
        if (variants == 0) return true;
        // The source's empty-cell early exit is equivalent to these checks for a one-cell input.
        return occupiedCells == 1 && gridSize >= offset + 1 && firstOccupiedSlot >= 0
                && firstOccupiedSlot < gridSize && firstOccupiedSlot % variants == offset;
    }

    public int minimumGridSize() { return offset + 1; }
}
