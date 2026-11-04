package gregtech.content.worldgen;

import java.util.function.Predicate;

/**
 * GT6 {@code WorldgenStoneLayers}' 7-entry sliding window.
 *
 * <p>{@code scan[3]} is the layer at the current y; {@code scan[1]}/{@code scan[5]} are the layers
 * two blocks below/above and {@code scan[0]}/{@code scan[6]} the outer pair used for the
 * normal/small decision. {@code scan[2]}/{@code scan[4]} are the near pair used by the random
 * small-gem roll.</p>
 *
 * <p>{@code noDeep} layers are replaced by the supplied deepslate layer below
 * {@code GTWorldgenScale.remapY(24)} (GT6 {@code y < 24}).</p>
 */
public final class StoneLayerScan<T> {
    /** Layer lookup; the feature supplies the noise-backed table, tests can supply a fake. */
    @FunctionalInterface
    public interface Sampler<T> {
        T at(int x, int y, int z);
    }

    private static final int WINDOW = 7;

    private final Sampler<T> sampler;
    private final Predicate<T> noDeep;
    private final T deepslate;
    private final int noDeepBelowY;
    private final int x;
    private final int z;
    private final T[] scan;
    private int y;

    @SuppressWarnings("unchecked")
    public StoneLayerScan(
            Sampler<T> sampler, Predicate<T> noDeep, T deepslate, int noDeepBelowY, int x, int startY, int z) {
        this.sampler = sampler;
        this.noDeep = noDeep;
        this.deepslate = deepslate;
        this.noDeepBelowY = noDeepBelowY;
        this.x = x;
        this.z = z;
        this.y = startY;
        this.scan = (T[]) new Object[WINDOW];
        for (int k = 0; k < WINDOW; k++) scan[k] = sample(startY + k - 3);
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

    /** GT6 {@code tScan[5] == tScan[1]}: the column is inside one layer. */
    public boolean isInterior() {
        return scan[5] == scan[1];
    }

    /** GT6 {@code tScan[6] == tScan[0]}: the column is in the core of the layer. */
    public boolean isCore() {
        return scan[6] == scan[0];
    }

    /** GT6 {@code tScan[4] != tScan[2]}: the column is near a layer boundary. */
    public boolean isNearBoundary() {
        return scan[4] != scan[2];
    }

    public void advance() {
        y++;
        System.arraycopy(scan, 1, scan, 0, WINDOW - 1);
        scan[WINDOW - 1] = sample(y + 3);
    }

    private T sample(int sampleY) {
        T layer = sampler.at(x, sampleY, z);
        return noDeep.test(layer) && sampleY < noDeepBelowY ? deepslate : layer;
    }
}
