package gregtech.client;

import net.minecraft.core.Direction;

/**
 * GT6 {@code MultiTileEntityMold} 5x5 cavity grid: the single geometry source for the block entity
 * renderer and the mold block item model.
 * <p>
 * {@code bit = 1} is a carved cell (cavity); every other cell is a raised 12/5px bump between
 * 1/16 and 3/16. GT6 hides a cell's bottom face and shows a side face only where the neighbouring
 * cell is carved — boundary cells get no riser because the hull's rim covers that plane.
 */
public final class MoldGridGeometry {
    public static final int CELLS = 5;
    public static final int ALL_CAVITIES = (1 << (CELLS * CELLS)) - 1;

    /** Grid area is the centre 12x12 of the 16x16 block texture; cell size is 12/5 = 2.4. */
    public static final float ORIGIN = 2F;

    public static final float CELL = 12F / CELLS;
    public static final float BOTTOM = 1F;
    public static final float TOP = 3F;

    private MoldGridGeometry() {}

    public static boolean solid(int shape, int row, int col) {
        // GT6 indexes mold cells x-major; {@code row} is the z index and {@code col} the x index.
        return (shape & (1 << (col * CELLS + row))) == 0;
    }

    /** Receives one visible face: x/z bounds in 0..16 block texture space and its 0..16 UVs. */
    public interface FaceSink {
        void accept(Direction side, float x0, float z0, float x1, float z1, float u0, float v0, float u1, float v1);
    }

    public static void forEachVisibleFace(int shape, FaceSink sink) {
        for (int row = 0; row < CELLS; row++) {
            for (int col = 0; col < CELLS; col++) {
                if (!solid(shape, row, col)) continue;
                float x0 = ORIGIN + col * CELL;
                float z0 = ORIGIN + row * CELL;
                float x1 = x0 + CELL;
                float z1 = z0 + CELL;
                sink.accept(Direction.UP, x0, z0, x1, z1, x0, z0, x1, z1);
                if (row > 0 && !solid(shape, row - 1, col))
                    sink.accept(Direction.NORTH, x0, z0, x1, z1, 16 - x1, 13, 16 - x0, 15);
                if (row < CELLS - 1 && !solid(shape, row + 1, col))
                    sink.accept(Direction.SOUTH, x0, z0, x1, z1, x0, 13, x1, 15);
                if (col > 0 && !solid(shape, row, col - 1)) sink.accept(Direction.WEST, x0, z0, x1, z1, z0, 13, z1, 15);
                if (col < CELLS - 1 && !solid(shape, row, col + 1))
                    sink.accept(Direction.EAST, x0, z0, x1, z1, 16 - z1, 13, 16 - z0, 15);
            }
        }
    }
}
