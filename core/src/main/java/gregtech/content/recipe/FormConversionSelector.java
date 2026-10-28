/* Gregorius Techneticies / GregTech-6 Team source semantics, LGPL-3.0-or-later. */
package gregtech.content.recipe;

/** Port selector: multiple decomposition outputs use fixed cells, never cyclic positions. */
public record FormConversionSelector(int variants, int offset) {
    public static final FormConversionSelector NONE = new FormConversionSelector(0, 0);

    public FormConversionSelector {
        if (variants < 0 || offset < 0 || (variants == 0 ? offset != 0 : offset >= variants))
            throw new IllegalArgumentException("Invalid source form selector");
    }

    public boolean matches(int gridSize, int firstOccupiedSlot, int occupiedCells) {
        if (variants == 0) return true;
        // Requested port behavior: preserve unrestricted single-route conversion, anchor multiple routes.
        return occupiedCells == 1
                && gridSize >= offset + 1
                && firstOccupiedSlot >= 0
                && firstOccupiedSlot < gridSize
                && (variants == 1 || firstOccupiedSlot == offset);
    }

    public int minimumGridSize() {
        return offset + 1;
    }
}
