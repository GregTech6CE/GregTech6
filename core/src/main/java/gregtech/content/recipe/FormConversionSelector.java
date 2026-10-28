/* Gregorius Techneticies / GregTech-6 Team source semantics, LGPL-3.0-or-later. */
package gregtech.content.recipe;

/** Port selector: multiple single-input variants cycle through the source's modulo slots. */
public record FormConversionSelector(int variants, int offset) {
    public static final FormConversionSelector NONE = new FormConversionSelector(0, 0);

    public FormConversionSelector {
        if (variants < 0 || offset < 0 || (variants == 0 ? offset != 0 : offset >= variants))
            throw new IllegalArgumentException("Invalid source form selector");
    }

    public boolean matches(int gridSize, int firstOccupiedSlot, int occupiedCells) {
        if (variants == 0) return true;
        // AdvancedCrafting1ToY counts the empty slots before the item and selects
        // offset == empty % variants; anchoring each route to one absolute slot made
        // every grid position past the first variant craft nothing.
        return occupiedCells == 1
                && gridSize >= offset + 1
                && firstOccupiedSlot >= 0
                && firstOccupiedSlot < gridSize
                && firstOccupiedSlot % variants == offset;
    }

    public int minimumGridSize() {
        return offset + 1;
    }
}
