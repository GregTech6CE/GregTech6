package gregtech.content.worldgen;

/**
 * GT6 {@code WorldgenStoneLayers}' 7-entry vertical sliding window.
 *
 * <p>{@code scan[3]} is the layer at the current {@code y}; {@code scan[1]}/{@code scan[5]} are
 * the layers two blocks below/above, {@code scan[0]}/{@code scan[6]} are the outer pair used for
 * the normal/small ore distinction, and {@code scan[2]}/{@code scan[4]} are the near pair around
 * layer boundaries.</p>
 */
public final class StoneLayerScan<T> {
    /** Layer lookup at world coordinates; the feature supplies noise sampling and tests can stub it. */
    @FunctionalInterface
    public interface Sampler<T> {
        T at(int x, int y, int z);
    }

    private static final int WINDOW = 7;

    private final Sampler<T> sampler;
    private final int x;
    private final int z;
    private final T[] scan;
    private int y;

    @SuppressWarnings("unchecked")
    public StoneLayerScan(Sampler<T> sampler, int x, int startY, int z) {
        this.sampler = sampler;
        this.x = x;
        this.z = z;
        this.y = startY;
        this.scan = (T[]) new Object[WINDOW];
        for (int k = 0; k < WINDOW; k++) {
            scan[k] = sampler.at(x, startY + k - 3, z);
        }
    }

    public int y() {
        return y;
    }

    public T current() {
        return scan[3];
    }

    public T below() {
        return scan[1];
    }

    public T above() {
        return scan[5];
    }

    public T outerBelow() {
        return scan[0];
    }

    public T outerAbove() {
        return scan[6];
    }

    public T nearBelow() {
        return scan[2];
    }

    public T nearAbove() {
        return scan[4];
    }

    /** {@code tScan[5] == tScan[1]}: the column is inside a single layer rather than on a boundary. */
    public boolean isInterior() {
        return scan[5] == scan[1];
    }

    /** {@code tScan[6] == tScan[0]}: deep core of one layer, eligible for normal-sized ores. */
    public boolean isCore() {
        return scan[6] == scan[0];
    }

    /** {@code tScan[4] != tScan[2]}: close to a layer transition. */
    public boolean isNearBoundary() {
        return scan[4] != scan[2];
    }

    /** Slides the window up by one block to {@code y + 1}. */
    public void advance() {
        y++;
        System.arraycopy(scan, 1, scan, 0, WINDOW - 1);
        scan[WINDOW - 1] = sampler.at(x, y + 3, z);
    }

    /**
     * Inclusive upper Y bound for scanning a chunk, extending one block above the highest filled
     * 16-block section so rocks resting directly on section boundaries (e.g. Y=63 or Y=143) are
     * still visited. Returns {@code minBuildHeight - 1} when the chunk has no filled sections.
     */
    public static int maxScanY(int minBuildHeight, int maxBuildHeight, int highestFilledSectionIndex) {
        if (highestFilledSectionIndex < 0) return minBuildHeight - 1;
        int sectionTopNext = minBuildHeight + (highestFilledSectionIndex + 1) * 16;
        return Math.min(maxBuildHeight - 1, sectionTopNext);
    }
}
